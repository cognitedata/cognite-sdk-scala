import glob
import json
import subprocess
import sys
import xml.etree.ElementTree as ET

MARKER = "<!-- slow-tests-comment -->"

results = []
for f in glob.glob("target/test-reports/*.xml"):
    try:
        root = ET.parse(f).getroot()
        name = root.get("name", f)
        time = float(root.get("time", 0))
        results.append((name, time))
    except Exception:
        pass

results.sort(key=lambda x: -x[1])
top3 = results[:3]

lines = [MARKER, "## Top 3 slowest test suites", "", "| # | Suite | Time |", "|---|-------|------|"]
for i, (name, time) in enumerate(top3, 1):
    lines.append(f"| {i} | `{name}` | {time:.1f}s |")

body = "\n".join(lines)
pr_number = sys.argv[1]
repo = sys.argv[2]

existing = json.loads(subprocess.run(
    ["gh", "api", f"repos/{repo}/issues/{pr_number}/comments", "--paginate"],
    capture_output=True, text=True, check=True
).stdout)

bot_comment = next((c for c in existing if MARKER in c.get("body", "")), None)

if bot_comment:
    subprocess.run(
        ["gh", "api", "--method", "PATCH",
         f"repos/{repo}/issues/comments/{bot_comment['id']}",
         "-f", f"body={body}"],
        check=True
    )
else:
    subprocess.run(
        ["gh", "pr", "comment", pr_number, "--body", body, "--repo", repo],
        check=True
    )
