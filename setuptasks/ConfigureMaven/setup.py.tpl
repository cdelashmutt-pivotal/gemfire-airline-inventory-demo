import os
import os.path
import pwd
import shutil
import subprocess
import sys

if __name__ == '__main__':
  username = '{{ Servers[ServerNum].Installations[InstallationNum].Owner }}'
  uid = None
  gid = None
  for user in pwd.getpwall():
     if user[0] == username:
        uid = user[2]
        gid = user[3]
        break

  if uid is None or gid is None:
     sys.exit('could not find user: ' + username)

  here = os.path.dirname(os.path.abspath(sys.argv[0]))
  userDir = '/home/{0}'.format(username)
  m2Dir = os.path.join(userDir,'.m2')
  if not os.path.exists(m2Dir):
      os.mkdir(m2Dir)
      os.chown(m2Dir,uid,gid)

  shutil.copy(os.path.join(here,'settings.xml'), m2Dir)

  os.chown(os.path.join(m2Dir,'settings.xml'), uid, gid)
  print 'configured maven settings for ' + username
