import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import zipfile

version = os.environ["RELEASE_VERSION"]
commit = os.environ["GITHUB_SHA"]
assert re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+(?:-alpha\.[0-9]+)?", version)
assert re.fullmatch(r"[0-9a-f]{40}", commit)
jar = Path(f"versions/1.21.1/build/libs/litematica-printer-{version}+1.21.1.jar")
with zipfile.ZipFile(jar) as archive:
    metadata = json.loads(archive.read("fabric.mod.json"))
    assert metadata["version"] == f"{version}+1.21.1"
    assert metadata["depends"]["minecraft"] == "1.21.1"
Path("dist").mkdir(exist_ok=True)
shutil.copyfile(jar, Path("dist") / jar.name)
digest = hashlib.sha256(jar.read_bytes()).hexdigest()
Path("dist/SHA256SUMS-mc1.21.1.txt").write_text(
    f"# source-commit: {commit}\n{digest}  {jar.name}\n", encoding="utf-8")
