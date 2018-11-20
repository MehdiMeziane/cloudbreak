package com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin;

import static com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin.AzureInteractiveLoginStatusCheckerTask.GRAPH_API_VERSION;
import static com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin.AzureInteractiveLoginStatusCheckerTask.GRAPH_WINDOWS;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.cloud.azure.AzureCredentialAppCreationCommand;

/**
 * Created by perdos on 10/18/16.
 */
@Service
public class ApplicationCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationCreator.class);

    @Inject
    private AzureCredentialAppCreationCommand appCreationCommand;

    public String createApplication(String accessToken, String tenantId, String secret) throws InteractiveLoginException {
        Response response = createApplicationWithGraph(accessToken, tenantId, secret);

        if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
            String application = response.readEntity(String.class);
            try {
                JsonNode applicationJson = new ObjectMapper().readTree(application);
                String appId = applicationJson.get("appId").asText();
                LOGGER.info("Application created with appId: " + appId);
                return appId;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            String errorResponse = response.readEntity(String.class);
            try {
                String errorMessage = new ObjectMapper().readTree(errorResponse).get("odata.error").get("message").get("value").asText();
                throw new InteractiveLoginException("AD Application creation error: " + errorMessage);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

    }

    private Response createApplicationWithGraph(String accessToken, String tenantId, String secret) {
        Client client = ClientBuilder.newClient();
        WebTarget resource = client.target(GRAPH_WINDOWS + tenantId);
        Builder request = resource.path("/applications").queryParam("api-version", GRAPH_API_VERSION).request();
        request.accept(MediaType.APPLICATION_JSON);
        request.header("Authorization", "Bearer " + accessToken);
        String appCreationJSON = appCreationCommand.generateJSON(secret);
        return request.post(Entity.entity(appCreationJSON, MediaType.APPLICATION_JSON));
    }
}
