#!python
#
# Copyright (c) 2015-2016 Pivotal Software, Inc. All Rights Reserved.
#
import jinja2
import jinja2.filters
import json
import os.path
import shutil
import sys

# global reference information
instanceProps = None
regionProps = None

def initializeRefData():
    global instanceProps, regionProps

    instanceProps = dict()
    regionProps = dict()

    #create instanceProps map
    ramInfo = [('m4.large',8),('m4.xlarge',16),('m4.2xlarge',32),('m4.4xlarge',64)]
    for itype, ram in ramInfo:
        instanceProps[itype] = dict()
        instanceProps[itype]['RAM'] = ram

    #and aws region map
    regions = ['us-east-1', 'us-east-2','us-west-1','us-west-2','ca-central-1','eu-west-1','eu-central-1','eu-west-2','ap-northeast-1','ap-northeast-2','ap-southeast-1','ap-southeast-2', 'ap-south-1','sa-east-1']
    amis =    ['ami-0b33d91d', 'ami-c55673a0', 'ami-165a0876', 'ami-f173cc91','ami-ebed508f', 'ami-70edb016	' ,'ami-af0fc0c0','ami-f1949e95','ami-56d4ad31','ami-dac312b4','ami-dc9339bf','ami-1c47407f', 'ami-f9daac96','ami-80086dec']
    regionInfo = zip(regions, amis)
    for region, ami in regionInfo:
        regionProps[region] = dict()
        regionProps[region]['AMI'] = ami

def renderTemplate(directory, templateFile, context, outDir):
    env = jinja2.Environment(loader=jinja2.FileSystemLoader(directory))
    env.trim_blocks = True
    env.lstrip_blocks = True
    template = env.get_template(templateFile)
    with open(os.path.join(outDir,'env.json'), 'w') as outfile:
        template.stream(context).dump(outfile)

def printUsageAndExit():
    print('usage: python3 generateAWSCluster.py')
    sys.exit(0)

def validate(ctx):
    for requiredKey in ['RegionName','SSHKeyPairName','SSHKeyPath','EnvironmentName']:
        if not requiredKey in ctx:
            sys.exit('cluster definition is missing required key: "{0}"'.format(requiredKey))


    for index, server in enumerate(ctx['Servers']):
        for  requiredKey in ['Name','PrivateIP','InstanceType','Roles','AZ']:
            if requiredKey not in server:
                sys.exit('server {0} is missing a required key: {1}'.format(index,requiredKey))

        if  server['InstanceType'] not in instanceProps:
            sys.exit('server {0} failed a validation: "InstanceType" must be one of the following - {1}'.format(index,','.join(list(instanceProps.keys))))

        if not server['PrivateIP'].startswith('192.168.'):
            sys.exit('server {0} failed a validation: "PrivateIP" must start with "192.168."'.format(index))

if __name__ == '__main__':
    #now dir should be set
    gemopsBase = os.path.dirname(os.path.abspath(sys.argv[0]))
    configFile = os.path.join(gemopsBase,'config','awscluster.json')

    if not os.path.isfile(configFile):
        sys.exit('missing required file: "{0}"'.format(configFile))

    initializeRefData()

    templateDir = os.path.join(gemopsBase, 'templates')
    templateFileName = 'env.json.tpl'

    with open(configFile, 'r') as contextFile:
        context = json.load(contextFile)

    validate(context)

    # now the context will be enhanced with additional information
    for server in context['Servers']:
        ram = instanceProps[server['InstanceType']]['RAM']
        reserve = ((ram ** 2)//4096 + ram // 16 ) * 1000 + 500 + 2000
        Xmx = ram * 1000 - reserve
        Xmn = Xmx // 5 - (Xmx ** 2)//500000 - 600
        server['RAM'] = instanceProps[server['InstanceType']]['RAM']
        server['XMX'] = Xmx
        server['XMN'] = Xmn


    # AMI can be at the top level, at least for now
    context['AMI'] = regionProps[context['RegionName']]['AMI']

    renderTemplate(templateDir,templateFileName,context, os.path.dirname(configFile))
    print('cluster configuration generated')
