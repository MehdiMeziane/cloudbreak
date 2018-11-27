package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.KdcV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.api.model.KerberosResponse;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.service.kdc.KdcService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(Transactional.TxType.NEVER)
@WorkspaceEntityType(KerberosConfig.class)
public class KdcV3Controller extends NotificationController implements KdcV3Endpoint {

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private KdcService kdcService;

    @Override
    public Set<KerberosResponse> listByWorkspace(Long workspaceId) {
        return kdcService.findKdcConfigWithoutTest(workspaceId).stream()
                .map(kdcConfig -> conversionService.convert(kdcConfig, KerberosResponse.class))
                .collect(Collectors.toSet());
    }

    @Override
    public KerberosResponse getByNameInWorkspace(Long workspaceId, String name) {
        KerberosConfig kerberosConfig = kdcService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(kerberosConfig, KerberosResponse.class);
    }

    @Override
    public KerberosResponse createInWorkspace(Long workspaceId, @Valid KerberosRequest request) {
        KerberosConfig newKdcConfig = conversionService.convert(request, KerberosConfig.class);
        KerberosConfig createdKdcConfig = kdcService.createForLoggedInUser(newKdcConfig, workspaceId);
        return conversionService.convert(createdKdcConfig, KerberosResponse.class);
    }

    @Override
    public KerberosResponse deleteInWorkspace(Long workspaceId, String name) {
        KerberosConfig deleted = kdcService.deleteByNameFromWorkspace(name, workspaceId);
        return conversionService.convert(deleted, KerberosResponse.class);
    }

    @Override
    public KerberosResponse attachToEnvironments(Long workspaceId, String name, @NotEmpty Set<String> environmentNames) {
        KerberosConfig attached = kdcService.attachToEnvironments(name, environmentNames, workspaceId);
        return conversionService.convert(attached, KerberosResponse.class);
    }

    @Override
    public KerberosResponse detachFromEnvironments(Long workspaceId, String name, @NotEmpty Set<String> environmentNames) {
        KerberosConfig detached = kdcService.detachFromEnvironments(name, environmentNames, workspaceId);
        return conversionService.convert(detached, KerberosResponse.class);
    }
}
