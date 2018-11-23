package com.sequenceiq.cloudbreak.service.template;

import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.init.clustertemplate.ClusterTemplateLoaderService;
import com.sequenceiq.cloudbreak.repository.OrchestratorRepository;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterTemplateRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.ClusterCreationSetupService;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackTemplateService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.util.ConverterUtil;

@Service
public class ClusterTemplateService extends AbstractWorkspaceAwareResourceService<ClusterTemplate> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateService.class);

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private ClusterTemplateRepository clusterTemplateRepository;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private ClusterTemplateLoaderService clusterTemplateLoaderService;

    @Inject
    private OrchestratorRepository orchestratorRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private NetworkService networkService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ClusterCreationSetupService clusterCreationSetupService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private StackTemplateService stackTemplateService;

    @Override
    protected WorkspaceResourceRepository<ClusterTemplate, Long> repository() {
        return clusterTemplateRepository;
    }

    @Override
    protected void prepareDeletion(ClusterTemplate resource) {
        if (resource.getStatus() != ResourceStatus.USER_MANAGED) {
            throw new AccessDeniedException("Default template deletion is forbidden");
        }
    }

    @Override
    protected void prepareCreation(ClusterTemplate resource) {

        if (resource.getStackTemplate().getEnvironment() == null) {
            throw new CloudbreakServiceException("The environment cannot be null.");
        }

        Stack stackTemplate = resource.getStackTemplate();
        stackTemplate.setName(UUID.randomUUID().toString());
        if (stackTemplate.getOrchestrator() != null) {
            orchestratorRepository.save(stackTemplate.getOrchestrator());
        }

        Network network = stackTemplate.getNetwork();
        if (network != null) {
            network.setWorkspace(stackTemplate.getWorkspace());
            networkService.pureSave(network);
        }

        Cluster cluster = stackTemplate.getCluster();
        if (cluster != null) {
//            cluster.setType("TEMPLATE");
            cluster.setWorkspace(stackTemplate.getWorkspace());
            clusterService.saveWithRef(cluster);
        }

        stackTemplate = stackTemplateService.pureSave(stackTemplate);

        if (cluster != null) {
            cluster.setStack(stackTemplate);
            clusterService.saveWithRef(cluster);
        }

        if (stackTemplate.getInstanceGroups() != null && !stackTemplate.getInstanceGroups().isEmpty()) {
            instanceGroupService.saveAll(stackTemplate.getInstanceGroups(), stackTemplate.getWorkspace());
        }
    }

    @Override
    public Set<ClusterTemplate> findAllByWorkspace(Workspace workspace) {
        return getAllAvailableInWorkspace(workspace);
    }

    @Override
    public Set<ClusterTemplate> findAllByWorkspaceId(Long workspaceId) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        return getAllAvailableInWorkspace(workspace);
    }

    public Set<ClusterTemplate> getAllAvailableInWorkspace(Workspace workspace) {
        Set<ClusterTemplate> clusterTemplates = clusterTemplateRepository.findAllByNotDeletedInWorkspace(workspace.getId());
        if (clusterTemplateLoaderService.isDefaultClusterTemplateUpdateNecessaryForUser(clusterTemplates)) {
            LOGGER.info("Modifying clusterTemplates based on the defaults for the '{}' workspace.", workspace.getId());
            clusterTemplates = clusterTemplateLoaderService.loadClusterTemplatesForWorkspace(clusterTemplates, workspace, this::saveDefaultsWithReadRight);
            LOGGER.info("ClusterTemplate modifications finished based on the defaults for '{}' workspace.", workspace.getId());
        }
        return clusterTemplates;
    }

    private Iterable<ClusterTemplate> saveDefaultsWithReadRight(Iterable<ClusterTemplate> clusterTemplates) {
        return clusterTemplateRepository.saveAll(clusterTemplates);
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.CLUSTER_TEMPLATE;
    }

    public void delete(String name, Long workspaceId) {
        ClusterTemplate clusterTemplate = getByNameForWorkspaceId(name, workspaceId);
        deleteByNameFromWorkspace(name, workspaceId);
        stackTemplateService.delete(clusterTemplate.getStackTemplate());
    }
}
