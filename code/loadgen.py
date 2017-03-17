import os
import os.path
import subprocess
import sys

if __name__ == "__main__":
    here = os.path.dirname(sys.argv[0])
    projectDir = 'airline-inventory-load-generator'
    version = '1.0-SNAPSHOT'
    jarName = 'airline-inventory-load-generator-1.0-SNAPSHOT.jar'
    className = 'io.pivotal.pde.sample.airline.loadgen.LoadGen'

    if 'JAVA_HOME' not in os.environ:
        sys.exit('please set the JAVA_HOME environment variable')

    java = os.path.join(os.environ['JAVA_HOME'],'bin','java')

    cp = os.pathsep.join([os.path.join(here,projectDir,'target',jarName),os.path.join(here,projectDir,'target','dependency','*')])

    cmd = [java,'-cp',cp, className]
    if len(sys.argv) > 1:
        cmd = cmd + sys.argv[1:]

    subprocess.check_call(cmd)
