import os
import os.path
import pwd
import shutil
import subprocess
import sys

if __name__ == '__main__':
   username = '{{ Servers[ServerNum].Installations[InstallationNum].Owner }}'
   here = os.path.dirname(os.path.abspath(sys.argv[0]))
   targetPath = '/home/{0}/.bash_profile'.format(username)
   shutil.copy(os.path.join(here,'bash_profile'), targetPath) 

   uid = None
   gid = None
   for user in pwd.getpwall():
      if user[0] == username:
         uid = user[2]
         gid = user[3]
         break

   if uid is None or gid is None:
      sys.exit('could not find user: ' + username)

   os.chown(targetPath, uid, gid)
   print 'configured bash profile for ' + username