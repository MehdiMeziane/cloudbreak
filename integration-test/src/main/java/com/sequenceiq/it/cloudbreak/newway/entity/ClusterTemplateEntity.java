package com.sequenceiq.it.cloudbreak.newway.entity;

import java.util.Collection;

import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateRequest;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateResponse;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class ClusterTemplateEntity extends AbstractCloudbreakEntity<ClusterTemplateRequest, ClusterTemplateResponse, ClusterTemplateEntity> {

    public ClusterTemplateEntity(TestContext testContext) {
        super(new ClusterTemplateRequest(), testContext);
    }

    public ClusterTemplateEntity() {
        super(ClusterTemplateEntity.class.getSimpleName().toUpperCase());
    }

    public ClusterTemplateEntity valid() {
        return withName(getNameCreator().getRandomNameForMock());
    }

    public ClusterTemplateEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public ClusterTemplateEntity withPlatform(String datalakeRequired) {
        getRequest().setDatalakeRequired(datalakeRequired);
        return this;
    }

    public ClusterTemplateEntity withStackTemplate(StackEntity stackTemplate) {
        getRequest().setStackTemplate(stackTemplate.getRequest());
        return this;
    }

    public ClusterTemplateEntity withStackTemplate(String key) {
        StackEntity stackTemplate = getTestContext().get(key);
        getRequest().setStackTemplate(stackTemplate.getRequest());
        return this;
    }

    @Override
    public Collection<ClusterTemplateResponse> getAll(CloudbreakClient client) {
        return client.getCloudbreakClient().clusterTemplateV3EndPoint().listByWorkspace(client.getWorkspaceId());
    }

    @Override
    public boolean deletable(ClusterTemplateResponse entity) {
        return entity.getName().startsWith("mock-");
    }

    @Override
    public void delete(ClusterTemplateResponse entity, CloudbreakClient client) {
        client.getCloudbreakClient().clusterTemplateV3EndPoint().deleteInWorkspace(client.getWorkspaceId(), entity.getName());
    }
}
