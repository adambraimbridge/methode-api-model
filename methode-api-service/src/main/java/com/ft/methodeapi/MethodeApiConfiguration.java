package com.ft.methodeapi;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.methodeapi.service.methode.MethodeConnectionConfiguration;
import com.google.common.base.MoreObjects;
import com.yammer.dropwizard.config.Configuration;

public class MethodeApiConfiguration extends Configuration {

    private String credentialsPath;
    private final MethodeConnectionConfiguration methodeConnectionConfiguration;
    private final MethodeConnectionConfiguration methodeTestConnectionConfiguration;

    public MethodeApiConfiguration(
                                    @JsonProperty("credentialsPath")String credentialsPath,
                                    @JsonProperty("methodeConnection") MethodeConnectionConfiguration methodeConnectionConfiguration,
                                    @JsonProperty("methodeTestConnection") MethodeConnectionConfiguration methodeTestConnectionConfiguration) {
        super();
        this.credentialsPath = credentialsPath;
        this.methodeConnectionConfiguration = methodeConnectionConfiguration;
        this.methodeTestConnectionConfiguration = methodeTestConnectionConfiguration;
    }

    @Valid
    @NotNull
    public String getCredentialsPath() {
        return credentialsPath;
    }

    @Valid
    @NotNull
    public MethodeConnectionConfiguration getMethodeConnectionConfiguration() {
        return methodeConnectionConfiguration;
    }

    @Valid
    @NotNull
    public MethodeConnectionConfiguration getMethodeTestConnectionConfiguration() {
        return methodeTestConnectionConfiguration;
    }
    
    protected MoreObjects.ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this)
                .add("super", super.toString())
                .add("methodeConnectionConfiguration", methodeConnectionConfiguration)
                .add("methodeTestConnectionConfiguration", methodeTestConnectionConfiguration);
    }
    
    @Override
    public String toString() {
        return toStringHelper().toString();
    }
}
