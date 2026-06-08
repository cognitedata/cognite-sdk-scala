import glob
import os
import subprocess
import sys
import xml.etree.ElementTree as ET

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

lines = ["## Top 3 slowest test suites", "", "| # | Suite | Time |", "|---|-------|------|"]
for i, (name, time) in enumerate(top3, 1):
    lines.append(f"| {i} | `{name}` | {time:.1f}s |")

body = "\n".join(lines)
pr_number = sys.argv[1]
repo = sys.argv[2]
subprocess.run(["gh", "pr", "comment", pr_number, "--body", body, "--repo", repo], check=True)
