package com.namir.aatariak.keycloak.provider.api.user.external;

import com.fasterxml.jackson.databind.JsonNode;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface AatariakClient {

    @GET
    List<AatariakUser> getUsers(@QueryParam("search") String search, @QueryParam("first") int first, @QueryParam("max") int max) throws IOException;

    @GET
    @Path("/count")
    Integer getUsersCount();

    @GET
    @Path("/{id}")
    AatariakUser getUserById(@PathParam("id") String id);

    @POST
    @Path("/authenticate")
    Boolean authenticate(CredentialData credentialData);

}
