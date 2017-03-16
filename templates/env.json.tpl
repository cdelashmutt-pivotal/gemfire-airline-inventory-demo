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
                    "Packages": ["gcc", "python35","python35-devel","python35-pip"]
                },
                {
                    "Name": "PipInstallPackages",
                    "Packages": ["netifaces"],
                    "PipProgramName" : "pip-3.5"
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
                            "ArchiveURL" : "http://download.pivotal.com.s3.amazonaws.com/gemfire/9.0.1/pivotal-gemfire-9.0.1.zip",
                            "RootDir" : "pivotal-gemfire-9.0.1",
                            "UnpackInDir" : "/runtime",
                            "LinkName" : "gemfire"
                        }
                        {% if  "ETL" in Server.Roles %}
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
                    "GemToolsURL": "https://s3-us-west-2.amazonaws.com/rmay.pivotal.io.software/gemfire-toolkit-2.1-runtime.tar.gz",
                    "ClusterHome" : "/runtime/gem_cluster_1",
                    "AdditionalFiles" : ["cluster.py","clusterdef.py","gemprops.py", "gf.py"]
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
