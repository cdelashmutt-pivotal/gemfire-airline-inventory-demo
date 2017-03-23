#
# Copyright (c) 2015-2016 Pivotal Software, Inc. All Rights Reserved.
#
import json
import os
import os.path
import pwd
import shutil
import subprocess

def basename(url):
    i = url.rindex('/')
    return url[i+1:]

def runQuietly(*args):
    p = subprocess.Popen(list(args), stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    output = p.communicate()
    if p.returncode != 0:
        raise Exception('"{0}" failed with the following output: {1}'.format(' '.join(list(args)), output[0]))

if __name__ == '__main__':
    ip = '{{Servers[ServerNum].PublicIpAddress }}'

    # locate the parent of cluster-home
    # unpack gemfire-manager into that directory, then rename it so that
    # the newly unpacked scripts are in cluster-home
    # move  cluster.json into cluster-home
    # unpack gemtools into cluster-home

    with open('/tmp/setup/cluster.json','r') as f:
        config = json.load(f)

    clusterHome = '{{ Servers[ServerNum].Installations[InstallationNum].ClusterHome }}'
    clusterParent = os.path.dirname(clusterHome)

    {% if "AWSAccessKeyId" in Servers[ServerNum].Installations[InstallationNum] %}

    AWS_ACCESS_KEY_ID = '{{ Servers[ServerNum].Installations[InstallationNum].AWSAccessKeyId }}'
    AWS_SECRET_ACCESS_KEY = '{{ Servers[ServerNum].Installations[InstallationNum].AWSSecretAccessKey }}'
    AWS_S3_BUCKET_REGION = '{{ Servers[ServerNum].Installations[InstallationNum].AWSS3Region }}'
    runQuietly('aws', 'configure', 'set', 'aws_access_key_id', AWS_ACCESS_KEY_ID)
    runQuietly('aws', 'configure', 'set', 'aws_secret_access_key', AWS_SECRET_ACCESS_KEY)
    runQuietly('aws', 'configure', 'set', 'default.region', AWS_S3_BUCKET_REGION)

    {% endif %}

    if not os.path.exists(clusterHome):
      os.makedirs(clusterHome)

    for script in ['cluster.py', 'gf.py', 'clusterdef.py','gemprops.py']:
      shutil.copy(os.path.join('/tmp/setup',script),clusterHome)

    print '{0} gemfire cluster control scripts installed in {1}'.format(ip, clusterHome)

    if os.path.exists(os.path.join(clusterHome,'gemtools')):
        shutil.rmtree(os.path.join(clusterHome,'gemtools'))
        print '{0} removing and reinstalling gemfire toolkit'.format(ip)

    {% if Servers[ServerNum].Installations[InstallationNum].GemToolsURL %}
    gemtoolsURL = '{{ Servers[ServerNum].Installations[InstallationNum].GemToolsURL }}'
    gemtoolsArchive = basename(gemtoolsURL)

    if gemtoolsURL.startswith('s3:'):
        runQuietly('aws', 's3', 'cp', gemtoolsURL, '/tmp/setup')
    else:
        runQuietly('wget', '-P', '/tmp/setup', gemtoolsURL)

    if gemtoolsArchive.endswith('.tar.gz'):
        runQuietly('tar', '-C', clusterHome, '-xzf', '/tmp/setup/' + gemtoolsArchive)
    elif gemtoolsArchive.endswith('.zip'):
        runQuietly('unzip', '/tmp/setup/' + gemtoolsArchive, '-d', clusterHome)

    print '{0} gemfire toolkit installed in {1}'.format(ip, os.path.join(clusterHome,'gemtools'))
    {% else %}
    gemtoolsArchive = '/tmp/setup/gemfire-toolkit-N-runtime.tar.gz'
    if os.path.exists(gemtoolsArchive):
      runQuietly('tar', '-C', clusterHome, '-xzf', gemtoolsArchive)
      print '{0} gemfire toolkit installed in {1}'.format(ip, os.path.join(clusterHome,'gemtools'))
    {% endif %}


    shutil.copy('/tmp/setup/cluster.json', clusterHome)
    if os.path.exists('/tmp/setup/config'):
      targetDir = os.path.join(clusterHome,'config')
      if os.path.exists(targetDir):
         shutil.rmtree(targetDir)

      shutil.copytree('/tmp/setup/config',targetDir)

    runQuietly('chown', '-R', '{0}:{0}'.format('{{ Servers[ServerNum].SSHUser }}'), clusterHome)
    print '{0} copied cluster definition into {1}'.format(ip, clusterHome)
