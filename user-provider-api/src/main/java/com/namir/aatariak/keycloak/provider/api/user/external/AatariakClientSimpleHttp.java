package com.namir.aatariak.keycloak.provider.api.user.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.namir.aatariak.keycloak.provider.api.user.Constants;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

@Slf4j
public class AatariakClientSimpleHttp implements AatariakClient{

    private final CloseableHttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;
    private final String apiValue;

    public AatariakClientSimpleHttp(KeycloakSession session, ComponentModel model) {
        this.httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
        this.baseUrl = model.get(Constants.BASE_URL);
        this.apiKey = model.get(Constants.AUTH_API_KEY);
        this.apiValue = model.get(Constants.AUTH_API_VALUE);
    }

    @Override
    @SneakyThrows
    public List<AatariakUser> getUsers(String search, int first, int max) {
        SimpleHttp simpleHttp = SimpleHttp.doGet(baseUrl, httpClient).header(apiKey, apiValue)
                .param("first", String.valueOf(first))
                .param("max", String.valueOf(max));
        if (search != null) {
            simpleHttp.param("search", search);
        }
        return simpleHttp.asJson(new TypeReference<>() {});
    }

    @Override
    @SneakyThrows
    public Integer getUsersCount() {
        String url = String.format("%s/count", baseUrl);
        String count = SimpleHttp.doGet(url, httpClient).header(apiKey, apiValue).asString();
        return Integer.valueOf(count);
    }

    @Override
    @SneakyThrows
    public AatariakUser getUserById(String id) {
        String url = String.format("%s/%s", baseUrl, id);
        SimpleHttp.Response response = SimpleHttp.doGet(url, httpClient).header(apiKey, apiValue).asResponse();
        if (response.getStatus() == 404) {
            throw new WebApplicationException(response.getStatus());
        }
        return response.asJson(AatariakUser.class);
    }

    @Override
    public Boolean authenticate(CredentialData credentialData) {
        String url = String.format("%s/authenticate", baseUrl);
        try {
            SimpleHttp.Response response = SimpleHttp.doPost(url, httpClient).header(apiKey, apiValue).json(credentialData).asResponse();

            return response.getStatus() == 200;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
