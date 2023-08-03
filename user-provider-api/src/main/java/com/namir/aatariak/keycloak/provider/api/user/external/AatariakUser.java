package com.namir.aatariak.keycloak.provider.api.user.external;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AatariakUser {
    private String id;
    private String name;
    private String email;
    private String password;
    private Boolean enabled;
    private String dateCreated;
    private String lastModifiedDate;
    private List<String> roles;
}
