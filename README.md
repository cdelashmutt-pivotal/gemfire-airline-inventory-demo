# Overview #
GemFire Ops Suite automates provisioning and management of
a GemFire cluster on AWS or on bare metal.

This document primarily describes the use of GemFire Ops Suite
with AWS.

GemFire Ops Suite relies on AWS Cloud Formation to configure AWS.
It splits a cluster into two Cloud Formation stacks.  One stack contains
all of the EBS volumes and the other contains the networking and
compute elements.  This allows the costly EC2 instances to be deployed
and undeployed as needed while preserving the data. This capability
represents a significant cost saving for environments, such as development
clusters,  that do not need to run 24x7 .

# Setup #
* The local machine requires python3 to be installed and the following python
packages
 * jina2
 * boto3
 * awscli
* You will need to register a key pair with AWS and you will need the
corresponding .pem file on your local machine.
* You will need an AWS AccessKeyId and SecretAccessKey. You can either use your
master key (which is dicouraged by Amazon) or create an IAM user.  The IAM
user you create will need to attach a user policy that grants access to all EC2, Cloud Formation and Elastic Load Balancing operations.  The following
policy definition can be used.

    ```json
    {
        "Version": "2012-10-17",
        "Statement": [
            {
                "Action": [
                    "ec2:*",
                    "cloudformation:*",
                    "elasticloudbalancing:*"
                ],
                "Effect": "Allow",
                "Resource": "*"
            }
        ]
    }
    ```

__For detailed AWS setup instructions, see "AWS_Setup.docx"__

# Setting up an AWS Cluster #
Configuring a cluster starts with a cluster configuration file like the
one below.  Only the most basic information is provided in this file. All
of the other configurations are provided by a set of templates called a
"plan".  A plan is an opinionated approach to AWS deployment and GemFire
cluster configuration.

Plans can be customized but that is not covered in this document.  
Currently, GemFire Ops Suite comes with one plan, the "Default Plan".  The
Default Plan is described in some detail below.

Here is a sample cluster definition file for a 3 node cluster.
```
{
  "EnvironmentName" : "Test",
  "RegionName" : "us-east-2",
  "SSHKeyPairName" : "lab-keypair",
  "SSHKeyPath": "/Users/me/Downloads/lab-keypair.pem",
  "Servers" : [
    {
      "Name" : "gem1101",
        "PrivateIP" : "192.168.1.101",
        "AZ" : "A",
        "InstanceType" : "m4.xlarge",
        "Roles" : ["DataNode", "Locator"]
    },
    {
      "Name" : "gem2101",
      "PrivateIP" : "192.168.2.101",
      "AZ" : "B",
      "InstanceType" : "m4.xlarge",
      "Roles" : ["DataNode", "Locator"]
    },
    {
      "Name" : "gem3101",
      "PrivateIP" : "192.168.3.101",
      "AZ" : "C",
      "InstanceType" : "m4.xlarge",
      "Roles" : ["DataNode", "Locator"]
    }
  ]
}

```
This file must be named "awscluster.json" and it must be in the "config"
directory. Once you have created your awscluster.json, generate all of the
configurations by running "generateAWSCluster.py".  You will need to read
"Instance Types Supported by the Default Plan" and
"Networking and the Default Plan" below to understand what options are
supported.

The setup steps are shown in concise form below:

```
cd ~/gem-ops-suite
cp samples/awscluster.json config
vi config/awscluster.json  #edit the cluster definition
python3 generateCluster.py
```

You now have a completely reproducible GemFire cluster on AWS.  The entire
project can be checked in to source control to allow versioning of the cluster.

# Provisioning the Cluster #

provision_aws_storage.py
   Provisions the EBS volumes for your cluster

destroy_aws_storage.py
   Destroys the EBS volumes for your cluster.  __This will cause data loss.__



# The Default Plan #

This section describes how the default plan provisions AWS based on the
cluster definition you provide (see example above).

## Instance Types Supported by the Default Plan ##
The default plan supports only the following instance types:

| Instance Type | vCPU | Cores | RAM     |
|---------------|------|-------|---------|
| m4.large      |  2   |   1   |     8g  |
| m4.xlarge     |  4   |   2   |    16g  |
| m4.2xlarge    |  8   |   4   |    32g  |
| m4.4xlarge    | 16   |   8   |    64g  |

## Networking and the Default Plan ##
A VPC is created, in the specified region (at the top of the file), to contain all of the machines. The VPC will have private IP addresses in the 192.168.\*.\* range. In the configuration file,  you must give each machine a private IP address in this range.

The "AZ" setting is used to specify the AWS availability zone that the
server will be provisioned in.  For example, if your "Region" setting
is "us-east-1", a server that specifies "B" for it's "AZ" would be
provisioned in the "us-east-1b" availability zone.  You can provision
your servers across any of the availability zones that exist in the
the region but there are some constraints on the private IP addresses
available in each AZ.

Within each AZ , a subnet will automatically be provisioned.  In the "A" availability zone, the subnet will be 192.168.__1__.\* , in the "B" availability zone, it will be 192.168.__2__.\* , etc..  The AZ you choose for an instance and its private IP address must be consistent.

## Storage and the Default Plan ##
The default plan also configures storage automatically.  Storage is configured as a collection of EBS volumes that can outlive the instances that they are attached to.  The table below describes the storage scheme.

| Server Type | Volume Size | Mount Point | Type | Purpose                                   |
| ------------|-------------|-------------|------|-------------------------------------------|
| All         | 10G         | /runtime    | gp2  | software installs, work directories, logs |
| Data Node   | 2 x RAM     | /data       | gp2  | gemfire disk stores                       |
| Data Node   | 4 x RAM     | /backup     | gp2  | backups of gemfire disk stores            |

## GemFire and the Default Plan ##
The default plans installs GemFire version 9.0.1 and Oracle JDK 1.8.0_92

Each data node will have the following RAM allocated.

|instance type |  RAM  | DataNode -Xmx | -Xmn     |  Effective Capacity |
|--------------|-------|---------------|----------|---------------------|
| m4.large     |  8g   |    5g         |  350m    |         3.9g        |
| m4.xlarge    | 16g   |   12g         | 1500m    |         8.8g        |
| m4.2xlarge   | 32g   |   27g         | 3250m    |        20.2g        |
| m4.4xlarge   | 64g   |   56g         | 4350m    |        43.9g        |


formula for reserve given size: =  x^2/4096 + x/16 + 1/2 (and 2 more G for locator)
formula for -Xmn (in m) given -Xmx (in m): x/5 - x^2/500000 - 600


## Known Issues and the Default Plan ##
Currently, due to a GemFire issue which will be resolved in version 9.0.2,
multiple locators are not supported.  Currently regardless of the cluster.json
file, a single locator will be deployed on the machine having private IP
address 192.168.1.101
