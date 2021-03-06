package com.sequenceiq.it.cloudbreak.newway.action;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

public class StackSyncPutAction implements ActionV2<StackEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackSyncPutAction.class);

    @Override
    public StackEntity action(TestContext testContext, StackEntity entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getGeneral().getName()));
        logJSON(LOGGER, format(" Stack put request:%n"), entity.getRequest());
        try (Response response = client.getCloudbreakClient()
                .stackV3Endpoint()
                .putSyncInWorkspace(client.getWorkspaceId(), entity.getName())) {
            logJSON(LOGGER, format(" Stack sync successfull:%n"), entity.getResponse());
            log(LOGGER, format(" ID: %s", entity.getResponse().getId()));
        }

        return entity;
    }

}