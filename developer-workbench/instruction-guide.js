'use strict';
const $=id=>document.getElementById(id);
const stages=[
 ['rules','Understand the rules','Agent reads relevant guidance','Start with AGENTS.md and project.md. They route the developer to the relevant files. You do not run these Markdown documents; they guide the agent’s decisions.'],
 ['stories','Specify the module','Story skill → draft requirements','An agent follows olarize-module-stories to draft business rules and user stories. Product and security references shape the requirements. You review the result before any Jira publication.'],
 ['approve','Approve & select','Your decision → Jira backlog','You approve exact story content, then authorize Jira publication. The MCP configuration connects the agent to Jira. Select one approved story and check its prerequisites before development.'],
 ['build','Implement one story','Developer + feature skill','The developer follows olarize-feature and the relevant engineering rules. It writes real Java code, migrations and tests. The workbench can coordinate this sequence; it does not replace business rules.'],
 ['tests','Verify the behavior','Tests + build evidence','Run the backend verification script and relevant checks. Unit tests cover rules; PostgreSQL integration tests cover real persistence/security. The Git hook is only a fast staged-file check, not the test suite.'],
 ['review','Review independently','Separate reviewer session','The reviewer follows olarize-review and reports actionable findings. The developer fixes issues and retests. In the dashboard this is a separate read-only CLI session; in Codex it may use the named reviewer role.'],
 ['publish','Inspect → PR → CI','Your decision + GitHub checks','Inspect the diff and results, then approve publication. The hook runs on commit; GitHub CI runs for its configured PR events. PR creation, merging and production deployment remain distinct actions.']
];
let stage='',selected='AGENTS.md';
function make(tag,cls,text){const x=document.createElement(tag);if(cls)x.className=cls;if(text!==undefined)x.textContent=text;return x;}
for(const group of [...new Set(CATALOGUE.map(e=>e.group))]){const o=make('option','',group);o.value=group;$('group').append(o);}
function render(){
 $('steps').replaceChildren(...stages.map(([key,title,owner],i)=>{const b=make('button','step');b.setAttribute('aria-pressed',String(stage===key));b.append(make('span','',String(i+1).padStart(2,'0')),make('b','',title),make('small','',owner));b.onclick=()=>{stage=key;$('group').value='';$('search').value='';render();};return b;}));
 $('step-explanation').textContent=stage?stages.find(s=>s[0]===stage)[3]:'Choose a step to see the relevant files. Only the guidance needed for the task should be loaded; a simple employee API does not require payroll, RAG or AWS instructions.';
 const q=$('search').value.toLowerCase(),g=$('group').value;
 const visible=CATALOGUE.filter(e=>(!stage||e.stages.includes(stage))&&(!g||e.group===g)&&(e.path+' '+e.name+' '+e.purpose).toLowerCase().includes(q));
 $('count').textContent=visible.length+' of '+CATALOGUE.length+' mapped files';
 if(!visible.some(e=>e.path===selected))selected=visible[0]?.path||'';
 $('files').replaceChildren(...visible.map(e=>{const b=make('button','file '+(e.path===selected?'selected':''));b.setAttribute('aria-pressed',String(e.path===selected));b.append(make('small','',e.group),make('b','',e.name),make('code','',e.path));b.onclick=()=>{selected=e.path;render();};return b;}));
 $('detail').hidden=!selected;if(!selected){$('files').append(make('p','','No matching file. Clear the search or select another category.'));return;}
 const e=CATALOGUE.find(e=>e.path===selected);
 for(const [id,key] of [['detail-group','group'],['detail-name','name'],['detail-path','path'],['purpose','purpose'],['when','when'],['example','example'],['mode','mode']])$(id).textContent=e[key];
 $('open-file').href='../'+e.path;
}
$('all').onclick=()=>{stage='';$('group').value='';$('search').value='';render();};$('search').oninput=render;$('group').onchange=render;render();
