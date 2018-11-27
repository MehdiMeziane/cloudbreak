package com.sequenceiq.cloudbreak.service.kdc;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.KerberosConfigRepository;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentResourceRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.environment.AbstractEnvironmentAwareService;
import com.sequenceiq.cloudbreak.type.KerberosType;

@Service
public class KdcService extends AbstractEnvironmentAwareService<KerberosConfig> {

    @Inject
    private KerberosConfigRepository kerberosConfigRepository;

    @Inject
    private ClusterService clusterService;

    @Override
    protected EnvironmentResourceRepository<KerberosConfig, Long> repository() {
        return kerberosConfigRepository;
    }

    @Override
    protected void prepareCreation(KerberosConfig resource) {
        if (resource.getType().equals(KerberosType.CB_MANAGED)) {
            throw new BadRequestException("Cannot create CB_MANAGED KDC config for environment");
        }
    }

    @Override
    public Set<Cluster> getClustersUsingResource(KerberosConfig resource) {
        return clusterService.findByKdcConfig(resource.getId());
    }

    @Override
    public Set<Cluster> getClustersUsingResourceInEnvironment(KerberosConfig resource, Long environmentId) {
        return clusterService.findAllClustersByKdcConfigInEnvironment(resource, environmentId);
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.KERBEROS_CONFIG;
    }

    public Set<KerberosConfig> findKdcConfigWithoutTest(Long workspaceId) {
        return kerberosConfigRepository.findAllByWorkspaceId(workspaceId).stream()
                .filter(kerberosConfig -> !kerberosConfig.getType().equals(KerberosType.CB_MANAGED))
                .collect(Collectors.toSet());
    }
}
