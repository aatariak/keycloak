package com.namir.aatariak.keycloak.provider.api.user;

import com.namir.aatariak.keycloak.provider.api.user.external.AatariakUser;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.LegacyUserCredentialManager;
import org.keycloak.models.*;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapter;

import javax.ws.rs.core.MultivaluedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserAdapter extends AbstractUserAdapter.Streams{

    private final AatariakUser user;

    public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, AatariakUser user) {
        super(session, realm, model);
        this.storageId = new StorageId(storageProviderModel.getId(), user.getEmail());
        this.user = user;
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public String getFirstName() {
        return user.getName();
    }

    @Override
    public String getEmail() { return user.getEmail(); }

    @Override
    public SubjectCredentialManager credentialManager() {
        return new LegacyUserCredentialManager(session, realm, this);
    }

    @Override
    public String getFirstAttribute(String name) {
        List<String> list = getAttributes().getOrDefault(name, List.of());
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
        attributes.add(UserModel.USERNAME, getEmail());
        attributes.add(UserModel.EMAIL, getEmail());
        attributes.add(UserModel.FIRST_NAME, getFirstName());
        attributes.add(UserModel.LAST_NAME, getLastName());
        return attributes;
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        Map<String, List<String>> attributes = getAttributes();
        return (attributes.containsKey(name)) ? attributes.get(name).stream() : Stream.empty();
    }

    @Override
    protected Set<RoleModel> getRoleMappingsInternal() {
        if (user.getRoles() != null) {
            return user.getRoles().stream().map(roleName -> new UserRoleModel(roleName, realm)).collect(Collectors.toSet());
        }
        return Set.of();
    }

    @Override
    protected Set<GroupModel> getGroupsInternal() {
//        if (user.getGroups() != null) {
//            return user.getGroups().stream().map(UserGroupModel::new).collect(Collectors.toSet());
//        }

        // Always return empty as I don't have groups in aatariak
        return Set.of();
    }
}
