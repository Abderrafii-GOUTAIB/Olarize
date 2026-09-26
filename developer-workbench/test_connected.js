'use strict';
// Browser-independent interaction checks. These do not substitute for visual browser testing.
const fs=require('fs'),vm=require('vm'),assert=require('assert');
function node(){return {value:'',children:[],hidden:false,disabled:false,textContent:'',dataset:{},append(...items){this.children.push(...items)},replaceChildren(...items){this.children=items},setAttribute(k,v){this[k]=v},remove(){},focus(){}};}
const ids=new Map();
const html=fs.readFileSync('developer-workbench/connected.html','utf8');
for(const match of html.matchAll(/id="([^"]+)"/g)){assert(!ids.has(match[1]),'Duplicate HTML ID');ids.set(match[1],node());}
const tabs=['activity','changes','verification','pullrequest'].map(name=>Object.assign(node(),{dataset:{tab:name}}));
const context=vm.createContext({document:{getElementById(id){assert(ids.has(id),'Missing HTML element '+id);return ids.get(id)},createElement:node,querySelectorAll:()=>tabs},location:{hash:'',pathname:'/'},history:{replaceState(){}},URLSearchParams,sessionStorage:{getItem:()=>'',setItem(){}},setTimeout:()=>0,clearTimeout(){},confirm:()=>false,console,EventSource:class{close(){}}});
vm.runInContext(fs.readFileSync('developer-workbench/connected.js','utf8'),context);
const run=code=>vm.runInContext(code,context);
const s={id:'EMP-001',module:'employees',title:'Trusted company context',content:'Given authenticated membership, when selecting a company, then enforce scope.',version:'v1',approved:'v1',jira:'OLARIZE-1',url:'https://example.atlassian.net/browse/OLARIZE-1'};
context.fixture=s;
run('stories=[fixture];modules();renderStories();');
assert.equal(ids.get('stories').children.length,1);assert.equal(ids.get('start').disabled,false);assert.equal(ids.get('jira').hidden,false);
ids.get('story-editor').value='Unsaved new scope';ids.get('story-editor').oninput();assert.equal(ids.get('start').disabled,true);assert.equal(ids.get('publish-jira').disabled,true);
ids.get('query').value='EMP';ids.get('query').oninput();assert.equal(ids.get('story-editor').value,'Unsaved new scope');
run("module='leave';storyId='';renderStories()");assert.equal(ids.get('start').disabled,true);assert(ids.get('stories').children.length);
run("module='employees';stories=[{...fixture,approved:''}];renderStories()");assert.equal(ids.get('start').disabled,true);assert.equal(ids.get('approve').disabled,false);
run("runs=[{id:'abc',subject:'story:EMP-001',state:'human_review',stage:'human_review',data:{checks:[{command:['test'],passed:true,revision:'v1'}],findings:[],review:'No findings'}}];runId='abc';renderRuns();");assert.equal(ids.get('approve-pr').disabled,true);assert.equal(ids.get('test-results').children.length,1);
run("loadedRevision='v1';renderRun()");assert.equal(ids.get('approve-pr').disabled,false);
run("runs[0].state='running';renderRun()");assert.equal(ids.get('approve-pr').disabled,true);
run("runs[0].data.module_document='<script>untrusted text</script>';renderRun()");assert.equal(ids.get('module-document-text').textContent,'<script>untrusted text</script>');assert.equal(ids.get('module-document').hidden,false);
tabs[1].onclick();assert.equal(ids.get('changes').hidden,false);assert.equal(ids.get('activity').hidden,true);
console.log('PASS: HTML wiring, module/story selection, draft guard, unsaved-edit retention, missing-diff approval guard, run states, plain-text module rendering and tabs.');
