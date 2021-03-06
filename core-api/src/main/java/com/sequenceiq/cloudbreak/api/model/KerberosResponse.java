package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class KerberosResponse extends KerberosBase {

    @ApiModelProperty(StackModelDescription.KERBEROS_MASTER_KEY)
    private SecretResponse masterKey;

    @ApiModelProperty(StackModelDescription.KERBEROS_ADMIN)
    private SecretResponse admin;

    @ApiModelProperty(StackModelDescription.KERBEROS_PASSWORD)
    private SecretResponse password;

    @ApiModelProperty(StackModelDescription.KERBEROS_PRINCIPAL)
    private SecretResponse principal;

    @ApiModelProperty(StackModelDescription.DESCRIPTOR)
    private SecretResponse descriptor;

    @ApiModelProperty(StackModelDescription.KRB_5_CONF)
    private SecretResponse krb5Conf;

    public SecretResponse getMasterKey() {
        return masterKey;
    }

    public void setMasterKey(SecretResponse masterKey) {
        this.masterKey = masterKey;
    }

    public SecretResponse getAdmin() {
        return admin;
    }

    public void setAdmin(SecretResponse admin) {
        this.admin = admin;
    }

    public SecretResponse getPassword() {
        return password;
    }

    public void setPassword(SecretResponse password) {
        this.password = password;
    }

    public SecretResponse getPrincipal() {
        return principal;
    }

    public void setPrincipal(SecretResponse principal) {
        this.principal = principal;
    }

    public SecretResponse getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(SecretResponse descriptor) {
        this.descriptor = descriptor;
    }

    public SecretResponse getKrb5Conf() {
        return krb5Conf;
    }

    public void setKrb5Conf(SecretResponse krb5Conf) {
        this.krb5Conf = krb5Conf;
    }
}
