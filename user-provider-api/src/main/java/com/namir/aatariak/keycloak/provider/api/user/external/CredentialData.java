package com.namir.aatariak.keycloak.provider.api.user.external;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredentialData {
    private String email;
    private String password;
}
