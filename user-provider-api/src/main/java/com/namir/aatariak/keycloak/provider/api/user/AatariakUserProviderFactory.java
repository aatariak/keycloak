package com.namir.aatariak.keycloak.provider.api.user;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.utils.StringUtil;

import java.util.List;

public class AatariakUserProviderFactory implements UserStorageProviderFactory<AatariakUserProvider> {

    public static final String PROVIDER_ID = "aatariak-user-provider";

    @Override
    public AatariakUserProvider create(KeycloakSession keycloakSession, ComponentModel componentModel) {
        return new AatariakUserProvider(keycloakSession, componentModel);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property(Constants.BASE_URL, "Base URL", "Base URL of the API", ProviderConfigProperty.STRING_TYPE, "", null)
                .property(Constants.AUTH_API_KEY, "Api Key", "Api Key at the API", ProviderConfigProperty.STRING_TYPE, "", null)
                .property(Constants.AUTH_API_VALUE, "Api Value", "Api value at the API", ProviderConfigProperty.PASSWORD, "", null)
                .build();
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        if (StringUtil.isBlank(config.get(Constants.BASE_URL))
                || StringUtil.isBlank(config.get(Constants.AUTH_API_KEY))
                || StringUtil.isBlank(config.get(Constants.AUTH_API_VALUE))) {
            throw new ComponentValidationException("Configuration not properly set, please verify.");
        }
    }
}
