# Overview

This is a demonstration of an airline inventory system.  This project contains
the configuration, etc. to

# Setup

## GemFire

This demo uses gemfire 9.0.4.  It should be installed on your local machine.
You can get away with any 9.x version.

## Python

This project requires python3 for the AWS control scripts. In addition, the
_boto3_  and _jinja2_ packages must be installed.

## Maven

The code for the demo is in the _code_ folder and it is a maven project.

Unfortunately, some special setup is required to allow maven to download the
GemFire jars. If you don't already have one, you'll need to create an account
on _network.pivotal.io_.  Then edit _~/.m2/settings.xml_ (or create one) and
include your _network.pivotal.io_ credentials in a server declaration (see the
  example below)

``` xml
<settings>
   <servers>
       <server>
           <id>gemfire-release-repo</id>
           <username>me@myemail.com</username>
           <password>mypassword</password>
       </server>
   </servers>
</settings>
```

## AWS

An AWS Account with the proper permissions is required and a key-pair must
be configured as well.  See _gem-ops-suite/AWS_Setup.docx_ for step by step
instructions.

# Walk Through on AWS

## Provision the AWS Environment

Edit the cloud environment descriptor, _awscluster.json_.  Be sure to set the
key pair name and location to the name and location of the key pair you set up
in AWS.  You can also change the number of instances, types, sizes,
availability zones, etc.  A minimal example is show below:

```
{
  "EnvironmentName" : "FiservGemFireLab",
  "RegionName" : "us-east-1",      
  "SSHKeyPairName" : "yak-keypair",                       <<< Set This
  "SSHKeyPath": "/Users/rmay/Dowbloads/yak-keypair.pem",  <<< Set This
  "Servers" : [
    {
      "Name" : "gem1101",
        "PrivateIP" : "192.168.1.101",
        "AZ" : "A",
        "InstanceType" : "m4.xlarge",
        "Roles" : ["DataNode", "Locator"]
    },
    {
      "Name" : "gem1102",
      "PrivateIP" : "192.168.1.102",
      "AZ" : "A",
      "InstanceType" : "m4.xlarge",
      "Roles" : ["DataNode"]
    }
  ]
}
```

You can now start the AWS environment. The scripts are idempotent.  If
something goes wrong, fix the config file and repeat the  steps.  The setup
step will take a while if you are running from a machine connected over wifi
as it uploads artifacts directly from the local machine.

```
cp awscluster.json gem-ops-suite/config
python gem-ops-suite/generateAWSCluster.py
python gem-ops-suite/aws_provision_storage.py
python gem-ops-suite/aws_provision.py
python gem-ops-suite/setup.py
python gem-ops-suite/gf.py start
```

You now have an AWS cluster installed and started.  Each time the cluster
is provisioned, it will receive a new set of public IPs.  To see the latest IPs,
review _gem-ops-suite/aws_runtime.json_.  You will need the IP address of a
locator (usually gem1101) for the next step.

``` json
{
   "gem1101": "54.227.117.144",  <<< locator ip address
   "gem1102": "107.23.143.109"
}
```

Check out the pulse admin ui at: 54.227.117.144:17070/pulse
(use the correct IP for your locator). Log in with "admin"/"admin"

## Initialize the Cluster and Load Data

__In the commands below, be sure to substitute the correct locator ip for your cluster.__

```
cd code
mvn install

# this will update some configuration and stop the cluster because a restart is
# needed after this config change
gfsh -e "connect --locator=54.227.117.144[10000]" -e "run --file=cluster_init.gfsh"

# start the cluster again
python ../gem-ops-suite/gf.py start

# continue with disk store and region setup
gfsh -e "connect --locator=54.227.117.144[10000]" -e "run --file=setup_disk_stores.gfsh"
gfsh -e "connect --locator=54.227.117.144[10000]" -e "run --file=setup_regions.gfsh"

# now the cluster is initialized, load some data
python loader.py --locator=54.227.117.144[10000]
```

The cluster is now ready for use.  

## Run the Application

The UI is a standard WAR.  You can run the UI locally using the _jetty:run_
target or deploy the war file (in _target_ directory) to an app server.  

The web apps expects the following 2 system properties to be set:
- gemfire.locator.host=54.227.117.144
- gemfire.locator.port=10000

To run the application locally, you can use a command like the following:
```
mvn jetty:run -Dgemfire.locator.host=54.227.117.144 -Dgemfire.locator.port=10000
```

If you wish to create some external load (for example to demonstrate CQ), you
can use the "loadgen" program as follows (run from the _code_ directory ):
```
python loadgen.py --locator=54.227.117.144[10000] --threads=2 --intervalms=500 --from-date=20170901 --to-date=20170907 --looktobook=4
```

__Be sure to supply a date range that ends within one year of when the data
was loaded.__

## Shut Down at the End of the Day

This procedure will stop the cluster and undeploy the ec2 instances but
leave the data in tact.

```
# stop the cluster
gem-ops-suite/gf.py gfsh shutdown --include-locators=true

# unprovision ec2 instances
python gem-ops-suite/aws_destroy.py
```

## Start the Cluster Again and Restore All Data

```
python  gem-ops-suite/aws_provision.py
# it is sometimes necessary to wait a few seconds at this point
# if setup fails with an rsync error just try again
python  gem-ops-suite/setup.py
python  gem-ops-suite/gf.py start
```
__Note that you will get new IP addresses.  Check gem-ops-suite/aws_runtime.json
for the new ones __

## Shutdown and Clean Up All EC2 Instances
```
# stop the cluster
gem-ops-suite/gf.py gfsh shutdown --include-locators=true

# unprovision ec2 instances
python gem-ops-suite/aws_destroy.py

# remove the storage too
python gem-ops-suite/aws_destroy_storage.py
```
