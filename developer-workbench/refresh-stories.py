"""Generate a safe, local browser snapshot from the approved requirements document."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
source = (ROOT / 'docs/requirements/employee.md').read_text()
stories = []
for match in re.finditer(r'^### (EMP-\d+) — (.+)\n([\s\S]*?)(?=^### |^## |\Z)', source, re.M):
    story_id, title, body = match.groups()
    def field(name):
        found = re.search(r'\*\*' + re.escape(name) + r':\*\*\s*(.*?)(?=\*\*|\n\n|\Z)', body, re.S)
        return found.group(1).strip() if found else ''
    link = re.search(r'\[(OLARIZE-\d+)\]\((https://goutaibabderrafii.atlassian.net/browse/OLARIZE-\d+)\)', body)
    scope = field('Scope')
    stories.append(dict(id=story_id, title=title, approved=field('Review status').startswith('APPROVED'),
                        summary=field('Story'), dependencies=field('Preconditions/dependencies'),
                        criteria=re.findall(r'^- (Given .+)$', body, re.M), source=match.group(0).strip(),
                        jira=link.group(1) if link else '', url=link.group(2) if link else '',
                        expanded=story_id != 'EMP-001'))
assert len(stories) == 19 and len({s['id'] for s in stories}) == 19
assert sum(s['approved'] for s in stories) == 17
payload = json.dumps(stories, ensure_ascii=True).replace('<', '\\u003c')
(ROOT / 'developer-workbench/stories.js').write_text('// Generated from docs/requirements/employee.md. Refresh with refresh-stories.py.\nconst STORIES = ' + payload + ';\n')
print(f'Generated {len(stories)} source stories; {sum(s["approved"] for s in stories)} approved.')
