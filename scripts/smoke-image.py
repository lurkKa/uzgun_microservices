#!/usr/bin/env python3
"""Smoke test an already built image using isolated Docker containers; requires Docker + Python 3."""
import subprocess,json,time,urllib.request,urllib.error,os
image=os.environ.get('SMOKE_IMAGE', 'uzgun-review-app')
suffix=str(os.getpid()); net='uzgun-smoke-'+suffix; db=net+'-db'; app=net+'-app'
def run(*args):return subprocess.check_output(args,text=True).strip()
try:
 run('docker','network','create',net)
 run('docker','run','-d','--name',db,'--network',net,'-e','POSTGRES_DB=uzgun','-e','POSTGRES_USER=uzgun','-e','POSTGRES_PASSWORD=smoke-only','postgres:16-alpine')
 for i in range(60):
  p=subprocess.run(['docker','exec',db,'pg_isready','-U','uzgun','-d','uzgun'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
  if p.returncode==0:break
  time.sleep(.5)
 run('docker','run','-d','--name',app,'--network',net,'--read-only','--tmpfs','/tmp','--security-opt','no-new-privileges:true','-p','127.0.0.1::8080','-e','DB_URL=jdbc:postgresql://'+db+':5432/uzgun','-e','DB_USER=uzgun','-e','DB_PASSWORD=smoke-only','-e','JWT_ISSUER_URI=https://issuer.example.test',image)
 port=json.loads(run('docker','inspect',app))[0]['NetworkSettings']['Ports']['8080/tcp'][0]['HostPort']
 base='http://127.0.0.1:'+port
 for i in range(90):
  try:
   with urllib.request.urlopen(base+'/actuator/health/readiness',timeout=1) as r:
    if r.status==200:break
  except (OSError,urllib.error.URLError): pass
  time.sleep(.5)
 for path,expected in [('/actuator/health/liveness',200),('/actuator/health/readiness',200),('/user/permittedToCheckout',401)]:
  try:r=urllib.request.urlopen(base+path,timeout=2)
  except urllib.error.HTTPError as e:r=e
  body=r.read().decode();print(path,r.status,body);assert r.status==expected
 info=json.loads(run('docker','inspect',app))[0]
 print('Runtime user:',info['Config']['User'],'Read-only:',info['HostConfig']['ReadonlyRootfs'])
 logs=run('docker','logs',app)
 assert 'multiple SLF4J providers' not in logs
 assert '"log.level"' in logs or '"@timestamp"' in logs
 print('Structured production logs: verified')
 print('IMAGE SMOKE PASSED')
finally:
 for name in [app,db]:subprocess.run(['docker','rm','-f',name],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
 subprocess.run(['docker','network','rm',net],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
