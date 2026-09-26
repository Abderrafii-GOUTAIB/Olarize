"""Shared local API client for Codex/terminal use. Never starts a second orchestrator."""
import argparse
import json
from pathlib import Path
import urllib.request
import urllib.error

parser=argparse.ArgumentParser(description='Use the same connected run state as the dashboard.')
parser.add_argument('path', help='API path such as status, stories, runs or runs/<id>/diff')
parser.add_argument('--body', help='JSON request object; omit for GET')
parser.add_argument('--port', type=int, default=4179)
args=parser.parse_args()
if not args.path or '..' in args.path or not all(c.isalnum() or c in '/_-' for c in args.path):
    raise SystemExit('Invalid API path.')
token=(Path(__file__).resolve().parent/'.state/session-token').read_text().strip()
headers={'Authorization':'Bearer '+token,'X-Workbench-Token':token,'Content-Type':'application/json'}
data=json.dumps(json.loads(args.body)).encode() if args.body else None
request=urllib.request.Request('http://127.0.0.1:'+str(args.port)+'/api/'+args.path, data=data, headers=headers)
try:
    with urllib.request.build_opener(urllib.request.ProxyHandler({})).open(request, timeout=60) as response: print(json.dumps(json.load(response),indent=2))
except urllib.error.HTTPError as error:
    raise SystemExit(error.read().decode())
