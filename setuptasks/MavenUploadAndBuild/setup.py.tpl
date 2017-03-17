import os.path
import shutil
import subprocess

if __name__ == '__main__':
   targetDir = '{{ Servers[ServerNum].Installations[InstallationNum].TargetDir }}'
   owner = '{{ Servers[ServerNum].Installations[InstallationNum].Owner }}'

   if os.path.exists(targetDir):
      shutil.rmtree(targetDir)
      print 'removed {0}'.format(targetDir)

   shutil.copytree('/tmp/setup',targetDir)
   subprocess.check_call(['chown','-R',owner,targetDir])
   print 'copied maven project to {0}'.format(targetDir)

   mvnEnv = dict()
   mvnEnv['JAVA_HOME'] = '/runtime/java'
   subprocess.check_call(['sudo','-u',owner,'-E', '/runtime/maven/bin/mvn','clean', 'install'],cwd=targetDir, env=mvnEnv)
   print 'built maven project'
