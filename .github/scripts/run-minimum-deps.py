"""Compile every production component and exercise minimum and current dependencies."""
from pathlib import Path
import hashlib
import json
import os
import shutil
import subprocess
import sys
ROOT=Path(__file__).resolve().parents[2]
mc=sys.argv[1]
minimum=json.loads((ROOT/'.github/minimum-dependencies.json').read_text())[mc]
original=json.loads((ROOT/'.github/mapped-ci-inputs.json').read_text())
wrapper=str(Path(os.environ.get('MAPPED_CI_GRADLE', ROOT/'gradlew')).resolve())
env=os.environ.copy()
def gradle(folder,*tasks):
    subprocess.run([wrapper,*tasks,'--no-daemon','--max-workers=2','--console=plain','--stacktrace','-Dorg.gradle.jvmargs=-Xmx2G -Dfile.encoding=UTF-8'],cwd=folder,env=env,check=True)
companions={}
for repo,sha in minimum['companions'].items():
    folder=ROOT/'.ci/minimum-source'/repo/mc
    folder.mkdir(parents=True,exist_ok=True)
    subprocess.run(['git','init',str(folder)],check=True)
    subprocess.run(['git','-C',str(folder),'fetch','--depth=1','https://github.com/water2004/'+repo+'.git',sha],check=True)
    subprocess.run(['git','-C',str(folder),'checkout','--detach','FETCH_HEAD'],check=True)
    overlay=ROOT/'.github/minimum-overlays'/mc/repo
    for file in overlay.rglob('*'):
        if file.is_file():
            destination=folder/file.relative_to(overlay);destination.parent.mkdir(exist_ok=True,parents=True);shutil.copy2(file,destination)
    gradle(folder,'remapJar','test','runGameTest')
    jars=[p for p in (folder/'build/libs').glob('*.jar') if not any(x in p.name for x in ('sources','-dev'))]
    assert len(jars)==1,jars
    jar=jars[0]
    companions['quick' if repo=='quickshulker' else 'chain']={'name':jar.name,'url':jar.as_uri(),'sha256':hashlib.sha256(jar.read_bytes()).hexdigest()}
gradle(ROOT,'-Ptargets='+mc,':core:test',':'+mc+':remapJar',':'+mc+':remapSmokeJar')
for profile in ('minimum','current'):
    inputs=json.loads(json.dumps(original))
    inputs[mc].update(companions)
    if profile=='minimum':inputs[mc]['dependencies']=minimum['dependencies']
    (ROOT/'.github/mapped-ci-inputs.json').write_text(json.dumps(inputs,indent=2),encoding='utf-8')
    if profile=='current':
        env['MAPPED_CI_LOADER']='0.19.3'
    else:
        env['MAPPED_CI_LOADER']=minimum['loader']
    subprocess.run([sys.executable,str(ROOT/'.github/scripts/run-mapped-ci.py'),mc,'functional'],cwd=ROOT,env=env,check=True)
    source=ROOT/'.ci/results'/mc
    expected_loader=env['MAPPED_CI_LOADER']
    for mode in ('full','legacy','nochain'):
        console=(source/mode/'console.log').read_text(encoding='utf-8',errors='replace')
        assert ('Fabric Loader '+expected_loader) in console,(mc,mode,'actual loader mismatch',expected_loader)
    destination=ROOT/'.ci/minimum-results'/mc/profile
    destination.parent.mkdir(exist_ok=True,parents=True)
    shutil.move(source,destination)
    (destination/'dependency-profile.json').write_text(json.dumps({'profile':profile,'expectedLoader':expected_loader,'inputs':inputs[mc]},indent=2),encoding='utf-8')
print('MINIMUM_AND_CURRENT_PASS',mc,flush=True)
