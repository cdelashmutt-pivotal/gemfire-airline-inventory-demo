{
  "AWSTemplateFormatVersion": "2010-09-09",
  "Resources": {
    {######### VPC Definitions #############}
    "VPC" : {
       "Type" : "AWS::EC2::VPC",
       "Properties" : {
          "CidrBlock" : "192.168.0.0/16",
          "Tags" : [
            {
              "Key": "Name",
              "Value": "{{ EnvironmentName }}VPC"
            }
          ]
       }
    },
    "SubnetA" : {
      "Type" : "AWS::EC2::Subnet",
      "Properties" : {
         "AvailabilityZone" : "{{ RegionName }}a",
         "CidrBlock" : "192.168.1.0/24",
         "MapPublicIpOnLaunch" : true,
          "Tags" : [
            {
              "Key": "Name",
              "Value": "{{ EnvironmentName }}SubnetA"
            }
          ],
         "VpcId" : { "Ref" : "VPC" }
      }
    },
    "SubnetB" : {
      "Type" : "AWS::EC2::Subnet",
      "Properties" : {
         "AvailabilityZone" : "{{ RegionName }}b",
         "CidrBlock" : "192.168.2.0/24",
         "MapPublicIpOnLaunch" : true,
          "Tags" : [
            {
              "Key": "Name",
              "Value": "{{ EnvironmentName }}SubnetB"
            }
          ],
         "VpcId" : { "Ref" : "VPC" }
      }
    },
    "SubnetC" : {
      "Type" : "AWS::EC2::Subnet",
      "Properties" : {
         "AvailabilityZone" : "{{ RegionName }}c",
         "CidrBlock" : "192.168.3.0/24",
         "MapPublicIpOnLaunch" : true,
          "Tags" : [
            {
              "Key": "Name",
              "Value": "{{ EnvironmentName }}SubnetC"
            }
          ],
         "VpcId" : { "Ref" : "VPC" }
      }
    },
    "InternetGateway":{
      "Type" : "AWS::EC2::InternetGateway",
      "Properties" : {
        "Tags" : [
          {
            "Key": "Name",
            "Value": "{{ EnvironmentName }}InternetGateway"
          }
        ]
      }
    },
    "RouteTable" : {
      "Type" : "AWS::EC2::RouteTable",
      "Properties" : {
        "VpcId" : {"Ref": "VPC"},
        "Tags" : [
          {
            "Key": "Name",
            "Value" : "{{ EnvironmentName }}RouteTable"
          }
        ]
      }
    },
    "RouteToInternet" : {
      "Type" : "AWS::EC2::Route",
      "Properties" : {
        "DestinationCidrBlock" : "0.0.0.0/0",
        "GatewayId" : {"Ref": "InternetGateway"},
        "RouteTableId" : {"Ref":  "RouteTable"}
      },
      "DependsOn": ["VPCGatewayAttachment"]
    },
    "SubnetARouteTableAssociation" : {
      "Type" : "AWS::EC2::SubnetRouteTableAssociation",
      "Properties" : {
         "RouteTableId" : {"Ref" : "RouteTable"},
         "SubnetId" : {"Ref" : "SubnetA"}
      }
    },
    "SubnetBRouteTableAssociation" : {
      "Type" : "AWS::EC2::SubnetRouteTableAssociation",
      "Properties" : {
         "RouteTableId" : {"Ref" : "RouteTable"},
         "SubnetId" : {"Ref" : "SubnetB"}
      }
    },
    "SubnetCRouteTableAssociation" : {
      "Type" : "AWS::EC2::SubnetRouteTableAssociation",
      "Properties" : {
         "RouteTableId" : {"Ref" : "RouteTable"},
         "SubnetId" : {"Ref" : "SubnetC"}
      }
    },
    "VPCGatewayAttachment":{
      "Type" : "AWS::EC2::VPCGatewayAttachment",
      "Properties" : {
         "InternetGatewayId" : {"Ref" : "InternetGateway"  },
         "VpcId" : {"Ref" : "VPC"}
      }
    },
    "SecurityGroup":{
      "Type" : "AWS::EC2::SecurityGroup",
      "Properties" : {
         "GroupDescription" : "{{ EnvironmentName }}SecurityGroup",
         "VpcId" : {"Ref" : "VPC"},
         {#####  When No Egress Rules are Specified Default Allows All #####}
         "SecurityGroupIngress" : [
            {
              "IpProtocol" : "-1",
               "CidrIp" : { "Fn::GetAtt": ["VPC","CidrBlock"]}
            },
            {
              "IpProtocol" : "tcp",
               "FromPort" : "22",
               "ToPort" : "22",
               "CidrIp" : "0.0.0.0/0"
            },
            {
              "IpProtocol" : "tcp",
               "FromPort" : "10000",
               "ToPort" : "19999",
               "CidrIp" : "0.0.0.0/0"
            }
          ],
          "Tags" : [
            {
              "Key": "Name",
              "Value" : "{{ EnvironmentName }}SecurityGroup"
            }
          ]
      }

    },
    {######### Server Definitions ###########}
    {% for Server in Servers %}
    "{{ Server.Name }}" : {
      "Type" : "AWS::EC2::Instance",
      "Properties" : {
         "AvailabilityZone" : "{{ RegionName }}{{ Server.AZ | lower }}",
         {% if Server.InstanceType.startswith('m4') %}
         "EbsOptimized" : true,
         {% else%}
         "EbsOptimized" : false,
         {% endif %}
         "ImageId" : "{{ Server.ImageId }}",
         "InstanceInitiatedShutdownBehavior" : "stop",
         "InstanceType" : "{{ Server.InstanceType }}",
         "KeyName" : "{{ KeyPair }}",
         "PrivateIpAddress" : "{{ Server.PrivateIP }}",
         "SecurityGroupIds" : [ { "Ref" : "SecurityGroup"}],
         "SubnetId" : { "Ref" : "Subnet{{ Server.AZ }}" },
         "Tags" : [
            {
              "Key": "Name",
              "Value" : "{{ EnvironmentName }}Server{{ Server.Name }}"
            },
            {
              "Key": "Environment",
              "Value" : "{{ EnvironmentName }}"
            }
          ],
          {#### ESB Volumes Are Attached Here ####}
         "Volumes" : [
         {% for Device in Server.BlockDevices if Device.DeviceType == 'EBS' %}
          {
            "Device" : "{{ Device.Device }}",
            "VolumeId" : "{{ Device.EBSVolumeId }}"
          }
        {% if not loop.last %},{% endif %}
        {% endfor %}
         ] ,
         {#### Ephemeral Volumes are Mapped Here ####}
         "BlockDeviceMappings" : [
         {% for Device in Server.BlockDevices if Device.DeviceType == 'Ephemeral' %}
            {
              "DeviceName" : "{{ Device.Device }}",
              "VirtualName" : "ephemeral{{ loop.index0 }}"
            } {% if not loop.last %},{% endif %}
          {% endfor %}
         ]
      }
    }
    {% if not loop.last %}, {% endif %}
    {% endfor %}
  },
  "Description": "{{ EnvironmentName }} Stack"
}
