import importlib.util
import json
import os
from pathlib import Path
import sqlite3
import subprocess
import tempfile
import threading
import unittest
from unittest.mock import patch
from urllib.request import Request, build_opener, ProxyHandler
from urllib.error import HTTPError

spec=importlib.util.spec_from_file_location('workbench',Path(__file__).with_name('workbench.py'))
w=importlib.util.module_from_spec(spec)
spec.loader.exec_module(w)
TEST_ROOT=Path(__file__).resolve().parents[1]/'.state'
TEST_ROOT.mkdir(exist_ok=True)

class WorkbenchTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory(prefix='test-',dir=str(TEST_ROOT))
        self.root=Path(self.temp.name)
        subprocess.run(['git','-c','init.templateDir=','init','-q',str(self.root)],check=True)
        self.engine=w.Engine(self.root,launch=False)
        content='As HR I need a company-scoped employee. Given authorized HR, when creating, then persist once.'
        self.story=dict(id='EMP-101',module='employees',title='Create test employee',content=content,version=w.digest(content),approved=w.digest(content),jira='OLARIZE-101',url='https://example.atlassian.net/browse/OLARIZE-101',source='synthetic fixture')
        self.engine.db.put_story(self.story)
        self.payload=dict(story=self.story['id'],version=self.story['version'],scope='Backend only. Trusted membership prerequisites are implemented.',prefix='feature')
    def tearDown(self): self.temp.cleanup()
    def start(self): return self.engine.start('implementation',self.payload)
    def workspace(self):
        (self.root/'.gitignore').write_text('developer-workbench/.state/\n')
        (self.root/'feature.txt').write_text('original\n')
        subprocess.run(['git','add','.'],cwd=str(self.root),check=True)
        subprocess.run(['git','-c','user.name=Fixture','-c','user.email=fixture@example.invalid','commit','-qm','initial'],cwd=str(self.root),check=True)
        return self.engine.git('rev-parse','HEAD')

    def test_duplicate_start_shares_run_and_old_run_is_reused(self):
        first=self.start(); second=self.start()
        self.assertEqual(first['id'],second['id'])
        self.engine.db.update(first['id'],state='failed')
        self.assertEqual(first['id'],self.start()['id'])

    def test_database_lock_prevents_competing_run(self):
        a,created=self.engine.db.create('implementation','story:X',{})
        b,again=self.engine.db.create('implementation','story:X',{})
        self.assertTrue(created); self.assertFalse(again); self.assertEqual(a['id'],b['id'])

    def test_requires_approval_and_current_version(self):
        self.story['approved']=''
        self.engine.db.put_story(self.story)
        with self.assertRaises(w.Problem): self.start()
        self.engine.db.edit_story if hasattr(self.engine.db,'edit_story') else None
        self.engine.edit_story(self.story['id'],{'version':self.story['version']},approve=True)
        self.payload['version']='stale'
        with self.assertRaises(w.Problem): self.start()

    def test_edit_invalidates_draft_approval_and_stale_edit_rejected(self):
        self.story['jira']='';self.story['url']='';self.engine.db.put_story(self.story)
        changed=self.engine.edit_story(self.story['id'],dict(version=self.story['version'],content=self.story['content']+' Additional requirement.'))
        self.assertEqual(changed['approved'],'')
        with self.assertRaises(w.Problem): self.engine.edit_story(changed['id'],dict(version=self.story['version']),approve=True)

    def test_published_story_edit_is_not_silent_sync(self):
        with self.assertRaises(w.Problem): self.engine.edit_story(self.story['id'],dict(version=self.story['version'],content='Changed published behavior that would require synchronization.'))

    def test_events_replay_from_cursor(self):
        r=self.start();self.engine.db.event(r['id'],'test','token=not-a-real-token')
        events=self.engine.db.events(r['id']);self.assertNotIn('not-a-real-token',events[-1]['message'])
        self.assertEqual(self.engine.db.events(r['id'],events[-1]['id']),[])

    def test_restart_marks_active_execution_interrupted(self):
        r=self.start();self.engine.db.update(r['id'],state='running')
        restarted=w.Engine(self.root,launch=False)
        self.assertEqual(restarted.db.run(r['id'])['state'],'interrupted')

    def test_path_escape_rejected(self):
        with self.assertRaises(w.Problem): self.engine.inside(self.root/'..'/'outside')
        (self.root/'link').symlink_to(self.root.parent,target_is_directory=True)
        with self.assertRaises(w.Problem): self.engine.inside(self.root/'link'/'file')

    def test_remote_parsing(self):
        self.engine.git('remote','add','origin','https://github.com/Acme/Example.git')
        self.assertEqual(self.engine.repo(),'Acme/Example')
        self.engine.git('remote','set-url','origin','https://user:token@github.com/Acme/Example.git')
        with self.assertRaises(w.Problem): self.engine.repo()

    def test_cancel_prevents_new_stage(self):
        r=self.start();self.engine.action(r['id'],'cancel',{})
        with self.assertRaises(w.Problem): self.engine.alive(r['id'])
        with self.assertRaises(w.Problem): self.engine.db.update(r['id'],state='running')

    def test_stale_diff_blocks_publication(self):
        base=self.workspace();r=self.start();(self.root/'feature.txt').write_text('changed\n')
        revision=self.engine.snapshot(self.root)
        self.engine.db.update(r['id'],state='human_review',worktree=str(self.root),base=base,reviewed=revision,verified=revision)
        (self.root/'feature.txt').write_text('changed again\n')
        with self.assertRaises(w.Problem): self.engine.action(r['id'],'publish',{'revision':revision})
        self.assertNotIn('approved_revision',self.engine.db.run(r['id'])['data'])

    def test_untracked_file_is_in_diff_and_snapshot(self):
        base=self.workspace();r=self.start();before=self.engine.snapshot(self.root)
        (self.root/'new.txt').write_text('<script>untrusted fixture</script>\n')
        self.engine.db.update(r['id'],worktree=str(self.root),base=base)
        changed=self.engine.changes(r['id'])
        self.assertNotEqual(before,changed['revision']);self.assertIn('new.txt',changed['diff']);self.assertIn('<script>',changed['diff'])

    def test_publication_requires_human_review(self):
        r=self.start()
        with self.assertRaises(w.Problem): self.engine.action(r['id'],'publish',{'revision':'anything'})

    def test_protected_workflow_changes_rejected(self):
        base=self.workspace();r=self.start();(self.root/'.github').mkdir();(self.root/'.github/unsafe.yml').write_text('fixture')
        self.engine.db.update(r['id'],base=base,worktree=str(self.root))
        with self.assertRaises(w.Problem): self.engine.guard_changes(r['id'],self.root)

    def test_failed_verification_blocks_review(self):
        base=self.workspace();r=self.start();self.engine.db.update(r['id'],base=base,worktree=str(self.root))
        with patch.object(self.engine,'command',side_effect=w.Problem('Synthetic test failure')):
            with self.assertRaises(w.Problem): self.engine.verify(r['id'],self.root)
        result=self.engine.db.run(r['id']);self.assertFalse(result['data']['checks'][0]['passed']);self.assertNotIn('reviewed',result['data'])

    def test_independent_review_no_findings_reaches_human_gate(self):
        base=self.workspace();r=self.start();self.engine.db.update(r['id'],base=base,worktree=str(self.root))
        def fake_agent(key,prompt,cwd,schema,**kwargs):
            if schema is w.REVIEW_SCHEMA:return dict(blocked=False,summary='Independent synthetic review.',findings=[])
            (self.root/'feature.txt').write_text('implemented fixture\n')
            return dict(blocked=False,summary='Implemented synthetic story.')
        def fake_verify(key,cwd):self.engine.db.update(key,verified=self.engine.snapshot(cwd),checks=[dict(command=['synthetic-test'],passed=True)])
        with patch.object(self.engine,'agent',side_effect=fake_agent),patch.object(self.engine,'verify',side_effect=fake_verify):self.engine.develop(r['id'])
        result=self.engine.db.run(r['id']);self.assertEqual(result['state'],'human_review');self.assertEqual(result['data']['verified'],result['data']['reviewed']);self.assertNotIn('pr',result['data'])

    def test_run_deduplication_is_not_limited_to_display_history(self):
        first=self.start();self.engine.db.update(first['id'],state='failed')
        for i in range(105):
            r,_=self.engine.db.create('discovery','module:fixture'+str(i),{})
            self.engine.db.update(r['id'],state='completed')
        self.assertEqual(len(self.engine.db.runs(limit=100)),100)
        self.assertEqual(first['id'],self.start()['id'])

    def test_unpushed_publication_correction_reenters_development(self):
        r=self.start();self.engine.db.update(r['id'],state='failed',stage='publication')
        with patch.object(self.engine,'spawn') as spawn:
            self.engine.action(r['id'],'answer',{'feedback':'Fix the changed code and verify it again.'})
            self.assertEqual(spawn.call_args[0][1],self.engine.develop)

    def test_pushed_publication_retry_reconciles_remote(self):
        r=self.start();self.engine.db.update(r['id'],state='failed',stage='publication',published_head='abc')
        with patch.object(self.engine,'spawn') as spawn:
            self.engine.action(r['id'],'retry',{})
            self.assertEqual(spawn.call_args[0][1],self.engine.publish)

    def test_changed_commit_tree_is_never_pushed(self):
        base=self.workspace();r=self.start();(self.root/'feature.txt').write_text('approved change\n')
        revision=self.engine.snapshot(self.root)
        self.engine.db.update(r['id'],state='running',stage='publication',worktree=str(self.root),base=base,approved_revision=revision,branch='feature/OLARIZE-101-fixture')
        commands=[]
        def command(key,args,cwd,**kwargs):
            commands.append(args)
            if args[:2]==['git','commit']:
                (self.root/'feature.txt').write_text('unapproved hook change\n')
                subprocess.run(['git','add','feature.txt'],cwd=str(cwd),check=True)
                args=['git','-c','user.name=Fixture','-c','user.email=fixture@example.invalid','-c','commit.gpgsign=false']+args[1:]
            subprocess.run(args,cwd=str(cwd),check=True,capture_output=True)
        with patch.object(self.engine,'command',side_effect=command),patch.object(self.engine,'github',return_value=[]),patch.object(self.engine,'repo',return_value='Acme/Fixture'):
            with self.assertRaises(w.Problem):self.engine.publish(r['id'])
        self.assertFalse(any(args[:2]==['git','push'] for args in commands))

    def test_jira_answer_is_in_ephemeral_agent_prompt(self):
        r=self.engine.start('jira',dict(story=self.story['id'],version=self.story['version']))
        self.engine.db.update(r['id'],feedback='Use the already verified OLARIZE board.')
        with patch.object(self.engine,'agent',return_value=None) as agent:self.engine.jira_publish(r['id'])
        self.assertIn('Use the already verified OLARIZE board.',agent.call_args[0][1])

    def test_cli_login_access_requires_explicit_permission(self):
        r=self.start()
        with self.assertRaises(w.Problem):self.engine.agent(r['id'],'No tools.',self.root,w.RESULT_SCHEMA)

    def test_jira_requires_exact_approval(self):
        with self.assertRaises(w.Problem):self.engine.start('jira',dict(story=self.story['id'],version='stale'))

    def test_ci_uses_current_pr_head(self):
        r=self.start();self.engine.db.update(r['id'],state='pr_created',pr_number=12,published_head='old')
        calls=[]
        def github(method,path,payload=None):
            calls.append(path)
            if path.startswith('/pulls/'):return dict(head={'sha':'new'},state='open',merged=False)
            return {'check_runs':[]} if 'check-runs' in path else {'statuses':[]}
        with patch.object(self.engine,'github',side_effect=github):result=self.engine.ci(r['id'])
        self.assertFalse(result['reviewed_head_matches']);self.assertIn('/commits/new/check-runs?per_page=100',calls)

class HttpTests(unittest.TestCase):
    def setUp(self):
        WorkbenchTests.setUp(self)
        self.server=w.LocalServer(('127.0.0.1',0),w.Handler)
        self.server.daemon_threads=True;self.server.engine=self.engine;self.server.token='synthetic-local-session';self.server.authority='127.0.0.1:'+str(self.server.server_address[1])
        self.origin='http://'+self.server.authority
        threading.Thread(target=lambda:self.server.serve_forever(poll_interval=0.02),daemon=True).start()
    def tearDown(self):
        self.server.shutdown();self.server.server_close();WorkbenchTests.tearDown(self)
    def request(self,path,body=None,auth=False,origin=None):
        headers={'Content-Type':'application/json'}
        if auth:headers.update({'Authorization':'Bearer '+self.server.token,'X-Workbench-Token':self.server.token})
        if origin:headers['Origin']=origin
        req=Request(self.origin+path,data=json.dumps(body).encode() if body is not None else None,headers=headers)
        try:
            with build_opener(ProxyHandler({})).open(req,timeout=5) as r:return r.status,json.load(r)
        except HTTPError as e:return e.code,json.load(e)
    def test_session_api_requires_auth(self):
        self.assertEqual(self.request('/api/stories')[0],401)
        self.assertEqual(self.request('/api/stories',auth=True)[0],200)
    def test_cross_origin_post_is_denied(self):
        self.assertEqual(self.request('/api/runs',{'kind':'discovery','module':'leave'},auth=True,origin='https://attacker.invalid')[0],403)
    def test_session_exchange_requires_origin_and_token(self):
        self.assertEqual(self.request('/api/session',{'token':'wrong'},origin=self.origin)[0],401)
        self.assertEqual(self.request('/api/session',{'token':self.server.token},origin=self.origin)[0],200)
    def test_no_private_static_files(self):
        self.assertEqual(self.request('/.env')[0],404)
        self.assertEqual(self.request('/service/workbench.py')[0],404)
        self.assertEqual(self.request('/.state/session-token')[0],404)
    def test_duplicate_start_through_http(self):
        a=self.request('/api/runs',dict(self.payload,kind='implementation'),auth=True)
        b=self.request('/api/runs',dict(self.payload,kind='implementation'),auth=True)
        self.assertEqual(a[0],200);self.assertEqual(a[1]['id'],b[1]['id'])

if __name__=='__main__':unittest.main(verbosity=2)
