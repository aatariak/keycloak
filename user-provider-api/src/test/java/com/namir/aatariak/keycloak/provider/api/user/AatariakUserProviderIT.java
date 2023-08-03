package com.namir.aatariak.keycloak.provider.api.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@Slf4j
@Testcontainers
public class AatariakUserProviderIT {
    static final String REALM = "aatariak";

    static Network network = Network.newNetwork();

    @Container
    private static final GenericContainer postgreSQLContainer = new PostgreSQLContainer("postgres:11.1")
            .withDatabaseName("integration-tests-db")
            .withUsername("sa")
            .withPassword("sa")
            .withNetwork(network)
            .withNetworkAliases("my-db")
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\n", 1));

    @Container
    private static final GenericContainer aatariakMigrations = new GenericContainer<>(
            DockerImageName.parse("023231733398.dkr.ecr.eu-north-1.amazonaws.com/aatariak-migrations:master")
                    .asCompatibleSubstituteFor("aatariak-migrations")
    )
            .dependsOn(postgreSQLContainer)
            .withNetwork(network)
            .withCommand("-url=jdbc:postgresql://my-db:5432/integration-tests-db -user=sa -password=sa -connectRetries=2 migrate").withLogConsumer(new Slf4jLogConsumer(log))
            .waitingFor(Wait.forLogMessage(".*A Flyway report has been generated here.*\\n", 1));

    @Container static final GenericContainer aatariakRest = new GenericContainer<>(
            DockerImageName.parse("023231733398.dkr.ecr.eu-north-1.amazonaws.com/aatariak-rest:main")
                    .asCompatibleSubstituteFor("aatariak-rest")
    )
            .dependsOn(aatariakMigrations, postgreSQLContainer)
            .withNetwork(network)
            .withEnv("BOOT_ACTIVE_PROFILE", "dev")
            .withEnv("DB_NAME", "integration-tests-db")
            .withEnv("DB_HOST", "my-db")
            .withEnv("DB_PORT", "5432")
            .withEnv("DB_USERNAME", "sa")
            .withEnv("DB_PASSWORD", "sa")
            .withEnv("API_SECURITY_KEY", "faridmousayouhebalcousa")
            .withEnv("DEFAULT_USER_PASSWORD", "faridmousayouhebalcousa")
            .withNetworkAliases("aatariak-rest")
            .withExposedPorts(80, 8888)
            .withLogConsumer(new Slf4jLogConsumer(log));


    @Container
    private static final KeycloakContainer keycloak = new KeycloakContainer()
            .withRealmImportFile("/aatariak-realm.json")
            .withProviderClassesFrom("target/classes")
            .withNetwork(network).dependsOn(aatariakRest);


    @ParameterizedTest
    @ValueSource(strings = { KeycloakContainer.MASTER_REALM, REALM })
    public void testRealms(String realm) {
        String accountServiceUrl = given().when().get(keycloak.getAuthServerUrl() + "realms/" + realm)
                .then().statusCode(200).body("realm", equalTo(realm))
                .extract().path("account-service");

        given().when().get(accountServiceUrl).then().statusCode(200);
    }

    @Test
    public void testAccessingUsersAsAdmin() {
        Keycloak kcAdmin = keycloak.getKeycloakAdminClient();
        UsersResource usersResource = kcAdmin.realm(REALM).users();
        List<UserRepresentation> users = usersResource.search("namir");
        assertThat(users, is(not(empty())));

        String userId = users.get(0).getId();
        UserResource userResource = usersResource.get(userId);
        assertThat(userResource.toRepresentation().getEmail(), is("namirabboud@gmail.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "namirabboud@gmail.com" })
    public void testLoginAsUserAndCheckAccessToken(String userIdentifier) throws IOException {
        String accessTokenString = requestToken(userIdentifier, "faridmousayouhebalcousa")
                .then().statusCode(200).extract().path("access_token");

        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String,Object>> typeRef = new TypeReference<>() {};

        byte[] tokenPayload = Base64.getDecoder().decode(accessTokenString.split("\\.")[1]);
        Map<String, Object> payload = mapper.readValue(tokenPayload, typeRef);

        assertThat(payload.get("preferred_username"), is("namirabboud@gmail.com"));
        assertThat(payload.get("email"), is("namirabboud@gmail.com"));
        assertThat(payload.get("given_name"), is("namir"));
    }

    private Response requestToken(String username, String password) {
        String tokenEndpoint = given().when().get(keycloak.getAuthServerUrl() + "realms/" + REALM + "/.well-known/openid-configuration")
                .then().statusCode(200).extract().path("token_endpoint");
        return given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("username", username)
                .formParam("password", password)
                .formParam("grant_type", "password")
                .formParam("client_id", KeycloakContainer.ADMIN_CLI_CLIENT)
                .formParam("scope", "openid")
                .when().post(tokenEndpoint);
    }

}
