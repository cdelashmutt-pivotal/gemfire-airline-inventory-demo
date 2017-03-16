#
# Copyright (c) 2015-2016 Pivotal Software, Inc. All Rights Reserved.
#
import subprocess

def runQuietly(*args):
    p = subprocess.Popen(list(args), stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    output = p.communicate()
    if p.returncode != 0:
        raise Exception('"{0}" failed with the following output: {1}'.format(' '.join(list(args)), output[0]))

if __name__ == '__main__':
    pip = 'pip'
    {% if Servers[ServerNum].Installations[InstallationNum].PipProgramName %}
    pip = '{{ Servers[ServerNum].Installations[InstallationNum].PipProgramName }}'
    {% endif %}
    ip='{{ Servers[ServerNum].PublicIpAddress }}'
    {% for package in Servers[ServerNum].Installations[InstallationNum].Packages %}
    package = '{{ package }}'
    runQuietly(pip,'install', package)
    print '{0} - installed {1} python package'.format(ip, package)
    {% endfor %}
