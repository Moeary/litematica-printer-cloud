import json
import os
from pathlib import Path
import re
import subprocess

repository = os.environ["GITHUB_REPOSITORY"]
tag = os.environ["RELEASE_TAG"]
version = os.environ["RELEASE_VERSION"]
commit = os.environ["GITHUB_SHA"]
assert tag == f"v{version}"
assert os.environ["GITHUB_REF_NAME"] == f"v{version}+1.21.1"
assert re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+(?:-alpha\.[0-9]+)?", version)
source = json.loads(subprocess.check_output(
    ["gh", "api", f"repos/{repository}/commits/v{version}+1.21.1"]))
assert source["sha"] == commit
release = json.loads(subprocess.check_output(
    ["gh", "api", f"repos/{repository}/releases/tags/{tag}"]))
assert not release["draft"] and not release.get("immutable", False)
assert release["prerelease"] == ("-" in version)
checksum = "SHA256SUMS-mc1.21.1.txt"
for asset in release["assets"]:
    if asset["name"] == checksum:
        existing = subprocess.check_output(
            ["gh", "api", f"repos/{repository}/releases/assets/{asset['id']}",
             "-H", "Accept: application/octet-stream"], text=True)
        match = re.search(r"^# source-commit: ([0-9a-f]{40})$", existing, re.MULTILINE)
        assert match and match[1] == commit, "Already published from another commit"
assets = [f"dist/litematica-printer-{version}+1.21.1.jar", f"dist/{checksum}"]
assert all(Path(asset).is_file() for asset in assets)
subprocess.run(["gh", "release", "upload", tag, *assets,
                "--repo", repository, "--clobber"], check=True)
