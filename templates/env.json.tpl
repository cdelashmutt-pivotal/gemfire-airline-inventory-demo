{
    "EnvironmentName" : "{{ EnvironmentName }}",
    "RegionName" : "{{ RegionName }}",
    "KeyPair" : "{{ SSHKeyPairName }}",
    "SSHKeyPath" : "{{ SSHKeyPath }}",
    "Servers" : [
    {% for Server in Servers %}
        {
            "Name" : "{{ Server.Name }}",
            "ImageId" : "{{ AMI }}",
            "InstanceType" : "{{ Server.InstanceType }}",
            "PrivateIP" : "{{ Server.PrivateIP }}",
            "AZ" : "{{ Server.AZ }}",
            "SSHUser" : "ec2-user",
            "XMX" : "{{ Server.XMX }}",
            "XMN" : "{{ Server.XMN }}",
            "BlockDevices" : [
              {
                  "Size": 10,
                  "Device" : "/dev/xvdf",
                  "MountPoint" : "/runtime",
                  "Owner" : "ec2-user",
                  "FSType" : "ext4",
                  "DeviceType" : "EBS",
                  "EBSVolumeType" : "gp2"
    {% if "DataNode" in Server.Roles %}
              },
              {
                  "Size": {{ Server.RAM * 2 }},
                  "Device" : "/dev/xvdg",
                  "MountPoint" : "/data",
                  "Owner" : "ec2-user",
                  "FSType" : "ext4",
                  "DeviceType" : "EBS",
                  "EBSVolumeType" : "gp2"
              },
              {
                  "Size": {{ Server.RAM * 4 }},
                  "Device" : "/dev/xvdh",
                  "MountPoint" : "/backup",
                  "Owner" : "ec2-user",
                  "FSType" : "ext4",
                  "DeviceType" : "EBS",
                  "EBSVolumeType" : "gp2"
      {% endif %}
              }
            ],
            "Installations" : [
                {
                    "Name": "AddHostEntries"
                },
                {
                    "Name": "YumInstallPackages",
                    "Packages": ["gcc", "python-devel","python-pip"]
                },
                {
                    "Name": "PipInstallPackages",
                    "Packages": ["netifaces"]
                },
                {
                    "Name": "MountStorage"
                },
                {
                    "Name" : "CopyArchives",
                    "Archives" : [
                        {
                            "Name" : "JDK 1.8.0_92",
                            "ArchiveURL" : "https://s3-us-west-2.amazonaws.com/rmay.pivotal.io.software/jdk-8u92-linux-x64.tar.gz",
                            "RootDir" : "jdk1.8.0_92",
                            "UnpackInDir" : "/runtime",
                            "LinkName" : "java"
                        },
                        {
                            "Name" : "GemFire 9.0.1",
                            "ArchiveURL" : "https://s3-us-west-2.amazonaws.com/rmay.pivotal.io.software/pivotal-gemfire-9.0.1.tar.gz",
                            "RootDir" : "pivotal-gemfire-9.0.1",
                            "UnpackInDir" : "/runtime",
                            "LinkName" : "gemfire"
                        }
                        {% if Server.Type == "ETL" %}
                        , {
                            "Name" : "Apache Maven 3.3.9",
                            "ArchiveURL" : "https://s3-us-west-2.amazonaws.com/rmay.pivotal.io.software/apache-maven-3.3.9-bin.tar.gz",
                            "RootDir" : "apache-maven-3.3.9",
                            "UnpackInDir" : "/runtime",
                            "LinkName" : "maven"
                        }
                        {% endif %}
                    ]
                },
                {
                    "Name" : "ConfigureProfile",
                    "Owner" : "ec2-user"
                }
                {% if  "DataNode" in Server.Roles %}
                , {
                    "Name" : "InstallGemFireCluster",
                    "ClusterScriptsURL": "https://s3-us-west-2.amazonaws.com/rmay.pivotal.io.software/gemfire-manager-1.6.zip",
                    "GemToolsURL": "https://s3-us-west-2.amazonaws.com/rmay.pivotal.io.software/gemfire-toolkit-2.1-runtime.tar.gz",
                    "ClusterHome" : "/runtime/gem_cluster_1"
                }
                {% endif %}
                {% if "ETL" in Server.Roles %}
                ,{
                  "Name" : "ConfigureMaven",
                  "Owner" : "ec2-user"
                }
                , {
                    "Name" : "people-loader",
                    "TargetDir" : "/runtime/people-loader",
                    "Owner" : "ec2-user"
                }
                {% endif %}
            ]
        } {%- if not loop.last %},{% endif %}
        {% endfor %}
    ]
}
