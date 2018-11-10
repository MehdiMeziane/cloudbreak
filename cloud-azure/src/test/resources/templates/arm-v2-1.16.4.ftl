<#setting number_format="computer">
{
    "$schema": "https://schema.management.azure.com/schemas/2015-01-01/deploymentTemplate.json#",
    "contentVersion": "1.0.0.0",
    "codeGrantFlowInitParams" : {
        "userImageStorageAccountName": {
            "type": "string",
            "defaultValue" : "${storage_account_name}"
        },
        "userImageStorageContainerName" : {
            "type" : "string",
            "defaultValue" : "${image_storage_container_name}"
        },
        "userDataStorageContainerName" : {
            "type" : "string",
            "defaultValue" : "${storage_container_name}"
        },
        "userImageVhdName" : {
            "type" : "string",
            "defaultValue" : "${storage_vhd_name}"
        },
        "adminUsername" : {
          "type" : "string",
          "defaultValue" : "${credential.loginUserName}"
        },
        <#if existingVPC>
        "resourceGroupName" : {
          "type": "string",
          "defaultValue" : "${resourceGroupName}"
        },
        "existingVNETName" : {
          "type": "string",
          "defaultValue" : "${existingVNETName}"
        },
        "existingSubnetName" : {
          "type": "string",
          "defaultValue" : "${existingSubnetName}"
        },
        <#else>
        "virtualNetworkNamePrefix" : {
            "type": "string",
            "defaultValue" : "${stackname}"
        },
        </#if>
        "vmNamePrefix" :{
            "type": "string",
            "defaultValue" : "${stackname}"
        },
        "publicIPNamePrefix" :{
            "type": "string",
            "defaultValue" : "${stackname}"
        },
        "nicNamePrefix" : {
            "type": "string",
            "defaultValue" : "${stackname}"
        },
        "sshKeyData" : {
            "type" : "string",
            "defaultValue" : "${credential.publicKey}"
        },
        "region" : {
          "type" : "string",
          "defaultValue" : "${region}"
        },
        "subnet1Name": {
            "type": "string",
            "defaultValue": "${stackname}subnet"
        },
        <#if !existingVPC>
        "subnet1Prefix": {
           "type": "string",
           "defaultValue": "${subnet1Prefix}"
        },
        </#if>
        "sshIPConfigName": {
            "type": "string",
            "defaultValue": "${stackname}ipcn"
        }
    },
  	"variables" : {
      "userImageName" : "[concat('https://',codeGrantFlowInitParams('userImageStorageAccountName'),'.blob.core.windows.net/',codeGrantFlowInitParams('userImageStorageContainerName'),'/',codeGrantFlowInitParams('userImageVhdName'))]",
      "osDiskVhdName" : "[concat('https://',codeGrantFlowInitParams('userImageStorageAccountName'),'.blob.core.windows.net/',codeGrantFlowInitParams('userDataStorageContainerName'),'/',codeGrantFlowInitParams('vmNamePrefix'),'osDisk')]",
      <#if existingVPC>
      "vnetID": "[resourceId(codeGrantFlowInitParams('resourceGroupName'),'Microsoft.Network/virtualNetworks',codeGrantFlowInitParams('existingVNETName'))]",
      "subnet1Ref": "[concat(variables('vnetID'),'/subnets/',codeGrantFlowInitParams('existingSubnetName'))]",
      <#else>
      "vnetID": "[resourceId('Microsoft.Network/virtualNetworks',codeGrantFlowInitParams('virtualNetworkNamePrefix'))]",
      "subnet1Ref": "[concat(variables('vnetID'),'/subnets/',codeGrantFlowInitParams('subnet1Name'))]",
      </#if>
      <#list igs as group>
      "${group.compressedName}secGroupName": "${group.compressedName}${stackname}sg",
          <#if group.availabilitySetName?? && group.availabilitySetName?has_content>
          "${group.compressedName}AsName": "${group.availabilitySetName}",
          "${group.compressedName}AsFaultDomainCount": ${group.platformFaultDomainCount},
          "${group.compressedName}AsUpdateDomainCount": ${group.platformUpdateDomainCount},
          </#if>
      </#list>
      "sshKeyPath" : "[concat('/home/',codeGrantFlowInitParams('adminUsername'),'/.ssh/authorized_keys')]"
  	},
    "resources": [
            <#list igs as group>
                <#if group.availabilitySetName?? && group.availabilitySetName?has_content>
            {
                "type": "Microsoft.Compute/availabilitySets",
                "name": "[variables('${group.compressedName}AsName')]",
                "apiVersion": "2017-03-30",
                "location": "[resourceGroup().location]",
                <#if group.managedDisk == true>
                "sku": {
                    "name": "Aligned"
                },
                </#if>
                "properties": {
                    "platformFaultDomainCount": "[variables('${group.compressedName}AsFaultDomainCount')]",
                    "platformUpdateDomainCount": "[variables('${group.compressedName}AsUpdateDomainCount')]"
                }
            },
                </#if>
            </#list>
            <#if !existingVPC>
            {
                 "apiVersion": "2015-05-01-preview",
                 "type": "Microsoft.Network/virtualNetworks",
                 <#if !noFirewallRules>
                 "dependsOn": [
                    <#list igs as group>
                        "[concat('Microsoft.Network/networkSecurityGroups/', variables('${group.compressedName}secGroupName'))]"<#if (group_index + 1) != igs?size>,</#if>
                    </#list>
                 ],
                 </#if>
                 <#if userDefinedTags?? && userDefinedTags?has_content>
                 "tags": {
                      <#list userDefinedTags?keys as key>
                      "${key}": "${userDefinedTags[key]}"<#if (key_index + 1) != userDefinedTags?size>,</#if>
                      </#list>
                 },
                 </#if>
                 "name": "[codeGrantFlowInitParams('virtualNetworkNamePrefix')]",
                 "location": "[codeGrantFlowInitParams('region')]",
                 "properties": {
                     "addressSpace": {
                         "addressPrefixes": [
                             "[codeGrantFlowInitParams('subnet1Prefix')]"
                         ]
                     },
                     "subnets": [
                         {
                             "name": "[codeGrantFlowInitParams('subnet1Name')]",
                             "properties": {
                                 "addressPrefix": "[codeGrantFlowInitParams('subnet1Prefix')]"
                             }
                         }
                     ]
                 }
             },
             </#if>
             <#if !noFirewallRules>
             <#list igs as group>
             {
               "apiVersion": "2015-05-01-preview",
               "type": "Microsoft.Network/networkSecurityGroups",
               "name": "[variables('${group.compressedName}secGroupName')]",
               "location": "[codeGrantFlowInitParams('region')]",
               <#if userDefinedTags?? && userDefinedTags?has_content>
               "tags": {
                    <#list userDefinedTags?keys as key>
                    "${key}": "${userDefinedTags[key]}"<#if (key_index + 1) != userDefinedTags?size>,</#if>
                    </#list>
               },
               </#if>
               "properties": {
               "securityRules": [
                   {
                       "name": "endpoint1outr",
                       "properties": {
                           "protocol": "*",
                           "sourcePortRange": "*",
                           "destinationPortRange": "*",
                           "sourceAddressPrefix": "*",
                           "destinationAddressPrefix": "*",
                           "access": "Allow",
                           "priority": 101,
                           "direction": "Outbound"
                       }
                   },
                   <#list securities[group.name] as port>
                   {
                       "name": "endpoint${port_index}inr",
                       "properties": {
                           "protocol": "${port.capitalProtocol}",
                           "sourcePortRange": "*",
                           "destinationPortRange": "${port.port}",
                           "sourceAddressPrefix": "${port.cidr}",
                           "destinationAddressPrefix": "*",
                           "access": "Allow",
                           "priority": ${port_index + 102},
                           "direction": "Inbound"
                       }
                   }<#if (port_index + 1) != securities[group.name]?size>,</#if>
                   </#list>
                   ]
               }
             },
             </#list>
             </#if>
             <#list groups?keys as instanceGroup>
             <#list groups[instanceGroup] as instance>
                 <#if !noPublicIp>
                 {
                   "apiVersion": "2015-05-01-preview",
                   "type": "Microsoft.Network/publicIPAddresses",
                   "name": "[concat(codeGrantFlowInitParams('publicIPNamePrefix'), '${instance.instanceId}')]",
                   "location": "[codeGrantFlowInitParams('region')]",
                   <#if userDefinedTags?? && userDefinedTags?has_content>
                   "tags": {
                        <#list userDefinedTags?keys as key>
                        "${key}": "${userDefinedTags[key]}"<#if (key_index + 1) != userDefinedTags?size>,</#if>
                        </#list>
                   },
                   </#if>
                   "properties": {
                       <#if instanceGroup == "GATEWAY">
                       "publicIPAllocationMethod": "Static"
                       <#else>
                       "publicIPAllocationMethod": "Dynamic"
                       </#if>
                   }
                 },
                 </#if>
                 {
                   "apiVersion": "2015-05-01-preview",
                   "type": "Microsoft.Network/networkInterfaces",
                   "name": "[concat(codeGrantFlowInitParams('nicNamePrefix'), '${instance.instanceId}')]",
                   "location": "[codeGrantFlowInitParams('region')]",
                   <#if userDefinedTags?? && userDefinedTags?has_content>
                   "tags": {
                        <#list userDefinedTags?keys as key>
                        "${key}": "${userDefinedTags[key]}"<#if (key_index + 1) != userDefinedTags?size>,</#if>
                        </#list>
                   },
                   </#if>
                   "dependsOn": [
                       <#if !noFirewallRules>
                       "[concat('Microsoft.Network/networkSecurityGroups/', variables('${instance.groupName?replace('_', '')}secGroupName'))]"
                       </#if>
                       <#if !noPublicIp>
                       <#if !noFirewallRules>,</#if>
                       "[concat('Microsoft.Network/publicIPAddresses/', codeGrantFlowInitParams('publicIPNamePrefix'), '${instance.instanceId}')]"
                       </#if>
                       <#if !existingVPC>
                       <#if !noFirewallRules || !noPublicIp>,</#if>
                       "[concat('Microsoft.Network/virtualNetworks/', codeGrantFlowInitParams('virtualNetworkNamePrefix'))]"
                       </#if>
                   ],
                   "properties": {
                       <#if !noFirewallRules>
                       "networkSecurityGroup":{
                            "id": "[resourceId('Microsoft.Network/networkSecurityGroups/', variables('${instance.groupName?replace('_', '')}secGroupName'))]"
                       },
                       </#if>
                       "ipConfigurations": [
                           {
                               "name": "ipconfig1",
                               "properties": {
                                   "privateIPAllocationMethod": "Dynamic",
                                   <#if !noPublicIp>
                                   "publicIPAddress": {
                                       "id": "[resourceId('Microsoft.Network/publicIPAddresses',concat(codeGrantFlowInitParams('publicIPNamePrefix'), '${instance.instanceId}'))]"
                                   },
                                   </#if>
                                   "subnet": {
                                       "id": "[variables('subnet1Ref')]"
                                   }
                               }
                           }
                       ]
                   }
                 },
                 {
                   "apiVersion": "2016-04-30-preview",
                   "type": "Microsoft.Compute/virtualMachines",
                   "name": "[concat(codeGrantFlowInitParams('vmNamePrefix'), '${instance.instanceId}')]",
                   "location": "[codeGrantFlowInitParams('region')]",
                   "dependsOn": [
                    <#if instance.availabilitySetName?? && instance.availabilitySetName?has_content>
                       "[concat('Microsoft.Compute/availabilitySets/', '${instance.availabilitySetName}')]",
                    </#if>
                       "[concat('Microsoft.Network/networkInterfaces/', codeGrantFlowInitParams('nicNamePrefix'), '${instance.instanceId}')]"
                   ],
                   <#if userDefinedTags?? && userDefinedTags?has_content>
                   "tags": {
                        <#list userDefinedTags?keys as key>
                        "${key}": "${userDefinedTags[key]}"<#if (key_index + 1) != userDefinedTags?size>,</#if>
                        </#list>
                   },
                   </#if>
                   "properties": {
                        <#if instance.availabilitySetName?? && instance.availabilitySetName?has_content>
                        "availabilitySet": {
                            "id": "[resourceId('Microsoft.Compute/availabilitySets', '${instance.availabilitySetName}')]"
                        },
                        </#if>
                       "hardwareProfile": {
                           "vmSize": "${instance.flavor}"
                       },
                       "osProfile": {
                           "computername": "${instance.hostName}",
                           "adminUsername": "[codeGrantFlowInitParams('adminUsername')]",
                           <#if disablePasswordAuthentication == false>
                           "adminPassword": "${credential.password}",
                           </#if>
                           "linuxConfiguration": {
                               "disablePasswordAuthentication": "${disablePasswordAuthentication?c}",
                               "ssh": {
                                   "publicKeys": [
                                    <#if disablePasswordAuthentication == true>
                                       {
                                           "path": "[variables('sshKeyPath')]",
                                           "keyData": "[codeGrantFlowInitParams('sshKeyData')]"
                                       }
                                    </#if>
                                   ]
                               }
                           },
                           <#if instanceGroup == "CORE">
                           "customData": "${corecustomData}"
                           </#if>
                           <#if instanceGroup == "GATEWAY">
                           "customData": "${gatewaycustomData}"
                           </#if>
                       },
                       "storageProfile": {
                           "osDisk" : {
                               <#if instance.managedDisk == false>
                               "image" : {
                                    "uri" : "[variables('userImageName')]"
                               },
                               "vhd" : {
                                    "uri" : "[concat(variables('osDiskVhdName'), '${instance.instanceId}','.vhd')]"
                               },
                               </#if>
                               "name" : "[concat(codeGrantFlowInitParams('vmNamePrefix'),'-osDisk', '${instance.instanceId}')]",
                               "osType" : "linux",
                               "createOption": "FromImage"
                           },
                           <#if instance.managedDisk == true>
                           "imageReference": {
                               "id": "${customImageId}"
                           },
                           </#if>
                           "dataDisks": [
                           <#list instance.volumes as volume>
                               {
                                   "name": "[concat('datadisk', '${instance.instanceId}', '${volume_index}')]",
                                   "diskSizeGB": ${volume.size},
                                   "lun":  ${volume_index},
                                   <#if instance.managedDisk == false>
                                   "vhd": {
                                        "Uri": "[concat('${instance.attachedDiskStorageUrl}',codeGrantFlowInitParams('userDataStorageContainerName'),'/',codeGrantFlowInitParams('vmNamePrefix'),'datadisk','${instance.instanceId}', '${volume_index}', '.vhd')]"
                                   },
                                   </#if>
                                   "caching": "None",
                                   "createOption": "Empty"
                               } <#if (volume_index + 1) != instance.volumes?size>,</#if>
                           </#list>
                           ]
                       },
                       "networkProfile": {
                           "networkInterfaces": [
                               {
                                   "id": "[resourceId('Microsoft.Network/networkInterfaces',concat(codeGrantFlowInitParams('nicNamePrefix'), '${instance.instanceId}'))]"
                               }
                           ]
                       }
                       <#if instance.managedDisk == false>
                       ,"diagnosticsProfile": {
                         "bootDiagnostics": {
                           "enabled": true,
                           "storageUri": "${instance.attachedDiskStorageUrl}"
                         }
                       }
                       </#if>
                   }
                 }<#if (instance_index + 1) != groups[instanceGroup]?size>,</#if>
             </#list>
             <#if (instanceGroup_index + 1) != groups?size>,</#if>
             </#list>

     	]
}