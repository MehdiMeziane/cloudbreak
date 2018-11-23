package com.sequenceiq.cloudbreak.converter.clustertemplate;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateRequest;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeRequired;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.util.ConverterUtil;

@Component
public class ClusterTemplateRequestToClusterTemplateConverter extends AbstractConversionServiceAwareConverter<ClusterTemplateRequest, ClusterTemplate> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateRequestToClusterTemplateConverter.class);

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public ClusterTemplate convert(ClusterTemplateRequest source) {
        ClusterTemplate clusterTemplate = new ClusterTemplate();
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        clusterTemplate.setWorkspace(workspace);
        Stack stack = converterUtil.convert(source.getStackTemplate(), Stack.class);
        clusterTemplate.setStackTemplate(stack);
        clusterTemplate.setName(source.getName());
        clusterTemplate.setDescription(source.getDescription());
        clusterTemplate.setDatalakeRequired(DatalakeRequired.valueOf(source.getDatalakeRequired()));
        clusterTemplate.setStatus(ResourceStatus.USER_MANAGED);
        return clusterTemplate;
    }

    private Set<ClusterComponent> getAmbariAndHdpRepoConfig(Cluster cluster, ClusterV2Request request) throws IOException {
        Set<ClusterComponent> components = new HashSet<>();
        if (request.getAmbari().getAmbariRepoDetailsJson() != null) {
            AmbariRepo ambariRepo = converterUtil.convert(request.getAmbari().getAmbariRepoDetailsJson(), AmbariRepo.class);
            components.add(new ClusterComponent(ComponentType.AMBARI_REPO_DETAILS, new Json(ambariRepo), cluster));
        }
        if (request.getAmbari().getAmbariStackDetails() != null) {
            StackRepoDetails stackRepoDetails = converterUtil.convert(request.getAmbari().getAmbariStackDetails(), StackRepoDetails.class);
            components.add(new ClusterComponent(ComponentType.HDP_REPO_DETAILS, new Json(stackRepoDetails), cluster));
        }
        return components;
    }
}
