package com.namir.aatariak.keycloak.provider.api.user;

import com.namir.aatariak.keycloak.provider.api.user.external.AatariakClient;
import com.namir.aatariak.keycloak.provider.api.user.external.AatariakClientSimpleHttp;
import com.namir.aatariak.keycloak.provider.api.user.external.AatariakUser;
import com.namir.aatariak.keycloak.provider.api.user.external.CredentialData;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.*;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;

import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class AatariakUserProvider implements UserStorageProvider,
        UserLookupProvider,
        UserQueryProvider,
        CredentialInputUpdater,
        CredentialInputValidator
{

    private final KeycloakSession session;
    private final ComponentModel model;
    private final AatariakClient client;

    protected Map<String, UserModel> loadedUsers = new HashMap<>();

    public AatariakUserProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;
        this.client = new AatariakClientSimpleHttp(session, model);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realmModel, UserModel userModel, String credentialType) {
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realmModel, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) {
            return false;
        }

        boolean isAuthenticated = client.authenticate(new CredentialData(user.getUsername(), input.getChallengeResponse()));
        return isAuthenticated;
    }

    @Override
    public boolean updateCredential(RealmModel realmModel, UserModel userModel, CredentialInput credentialInput) {
        return false;
    }

    @Override
    public void disableCredentialType(RealmModel realmModel, UserModel userModel, String s) {

    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realmModel, UserModel userModel) {
        return Stream.empty();
    }

    @Override
    public void close() {
    }

    @Override
    public UserModel getUserById(RealmModel realmModel, String email) {
        log.info("getUserById: actually getting by email {}", StorageId.externalId(email));
        return findUser(realmModel, StorageId.externalId(email));
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String email) {
        log.info("getUserByUsername: {}", email);
        return findUser(realm, StorageId.externalId(email));
    }

    @Override
    public UserModel getUserByEmail(RealmModel realmModel, String s) {
        return null;
    }

    @Override
    public int getUsersCount(RealmModel realm, boolean includeServiceAccount) {
        return client.getUsersCount();
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        log.info("searchForUserStream, search={}, first={}, max={}", search, firstResult, maxResults);
        try {
            return toUserModelStream(client.getUsers(search, firstResult, maxResults), realm);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        log.info("searchForUserStream, params={}, first={}, max={}", params, firstResult, maxResults);
        try {
            return toUserModelStream(client.getUsers(null, firstResult, maxResults), realm);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realmModel, GroupModel groupModel, Integer integer, Integer integer1) {
        return null;
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realmModel, String s, String s1) {
        return null;
    }

    private UserModel findUser(RealmModel realm, String identifier) {
        UserModel adapter = loadedUsers.get(identifier);
        if (adapter == null) {
            try {
                AatariakUser peanut = client.getUserById(identifier);
                adapter = new UserAdapter(session, realm, model, peanut);
                loadedUsers.put(identifier, adapter);
            } catch (WebApplicationException e) {
                log.warn("User with identifier '{}' could not be found, response from server: {}", identifier, e.getResponse().getStatus());
            }
        } else {
            log.info("Found user data for {} in loadedUsers.", identifier);
        }
        return adapter;
    }

    private Stream<UserModel> toUserModelStream(List<AatariakUser> peanuts, RealmModel realm) {
        log.info("Received {} users from provider", peanuts.size());
        return peanuts.stream().map(user -> new UserAdapter(session, realm, model, user));
    }
}
