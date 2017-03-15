#
# Copyright (c) 2015-2016 Pivotal Software, Inc. All Rights Reserved.
#
# note: this is python3!
import boto3
import botocore.exceptions
import jinja2
import json
import os
import os.path
import sys
import time


def printStackEvent(event):
    print('{0} {1} {2} {3}'.format(event['Timestamp'],event['ResourceType'],event['LogicalResourceId'],event['ResourceStatus']))

def monitorCFStack(boto3client, stackName, NotFoundOK = False):
    lastSeenEventId = None
    stackStatus = None #CREATE_IN_PROGRESS | CREATE_FAILED | CREATE COMPLETE

    time.sleep(5)

    while True:
        # loop over all events, possibly using multiple calls, don't
        # don't print the ones that have already been seen
        nextToken = None
        eventList = [] #will be used to reverse the order of returned events
        eventListFilled = False
        try:
            describeEventsResponse = cf.describe_stack_events(StackName = context['EnvironmentName'])
        except botocore.exceptions.ClientError as x:
            if NotFoundOK and x.response['Error']['Message'].endswith('does not exist'):
                print('stack does not exist - continuing')
                return True
            else:
                sys.exit('boto3 describe_stack_events api failed with message: {0}'.format( x.response['Error']['Message']))

        if 'NextToken' in describeEventsResponse:
            nextToken = describeEventsResponse['NextToken']

        for event in describeEventsResponse['StackEvents']:
            if lastSeenEventId is not None and event['EventId'] == lastSeenEventId:
                eventListFilled = True
                break

            eventList.insert(0,event)

        while not eventListFilled and nextToken is not None:
            try:
                describeEventsResponse = cf.describe_stack_events(StackName = context['EnvironmentName'], NextToken = nextToken)
            except botocore.exceptions.ClientError as x:
                if NotFoundOK and x.response['Error']['Message'].endswith('does not exist'):
                    print('stack does not exist - continuing')
                    return True
                else:
                    sys.exit('boto3 describe_stack_events api failed with message: {0}'.format( x.response['Error']['Message']))

            if 'NextToken' in describeEventsResponse:
                nextToken = describeEventsResponse['NextToken']

            for event in describeEventsResponse['StackEvents']:
                if lastSeenEventId is not None and event['EventId'] == lastSeenEventId:
                    eventListFilled = True
                    break

                eventList.insert(0,event)

        # now eventList has all unseen events in chrono order
        # this can be empty if no new events have occurred since the last time they were checked
        if len(eventList) > 0:
            lastSeenEventId = eventList[-1]['EventId']
            for event in eventList:
                printStackEvent(event)
                if event['ResourceType'] == 'AWS::CloudFormation::Stack':
                    stackStatus = event['ResourceStatus']

        if stackStatus is not None and not stackStatus.endswith('_IN_PROGRESS'):
            break

        time.sleep(5)

    if stackStatus.endswith('COMPLETE'):
        return True
    else:
        return False


if __name__ == '__main__':
    assert sys.version_info >= (3,0)

    here = os.path.dirname(os.path.abspath(sys.argv[0]))
    configDir = os.path.join(here,'config')
    configFile = os.path.join(configDir,'env.json')
    templateDir = os.path.join(here,'templates')
    storageMapFile = os.path.join(here,'aws_runtime_storage.json')
    instanceMapFile = os.path.join(here,'aws_runtime.json')

    #read the environment file
    env = jinja2.Environment(loader=jinja2.FileSystemLoader(configDir))
    with open(configFile, 'r') as contextFile:
        context = json.load(contextFile)

    # set up boto3 client for cloudformation

    cf = boto3.client('cloudformation',
                       region_name=context['RegionName'])

    stackName = context['EnvironmentName']
    cf.delete_stack(StackName = stackName)
    stackStatus = monitorCFStack(cf, context['EnvironmentName'], NotFoundOK = True)

    if stackStatus:
        print('Cloud Formation Stack Deleted')
    if not stackStatus:
        print('Cloud Formation Stack Delete Failed')

    if os.path.exists(instanceMapFile):
        os.remove(instanceMapFile)
        print('removed {0}'.format(instanceMapFile))
