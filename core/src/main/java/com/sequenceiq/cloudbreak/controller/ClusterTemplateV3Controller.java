package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.ClusterTemplateV3EndPoint;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateRequest;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateResponse;
import com.sequenceiq.cloudbreak.controller.validation.credential.CredentialValidator;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.template.ClusterTemplateService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.util.ConverterUtil;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(Transactional.TxType.NEVER)
@WorkspaceEntityType(ClusterTemplate.class)
public class ClusterTemplateV3Controller extends NotificationController implements ClusterTemplateV3EndPoint {

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private UserService userService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private StackCommonService stackCommonService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private ClusterTemplateService clusterTemplateService;

    @Inject
    private CredentialValidator credentialValidator;

    @Override
    public ClusterTemplateResponse createInWorkspace(Long workspaceId, @Valid ClusterTemplateRequest request) {
//        credentialValidator.validateCredentialCloudPlatform(request.getCloudPlatform());
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        ClusterTemplate clusterTemplate = clusterTemplateService.create(converterUtil.convert(request, ClusterTemplate.class), workspaceId, user);
        return converterUtil.convert(clusterTemplate, ClusterTemplateResponse.class);
    }

    @Override
    public Set<ClusterTemplateResponse> listByWorkspace(Long workspaceId) {
        return converterUtil.convertAllAsSet(clusterTemplateService.findAllByWorkspaceId(workspaceId), ClusterTemplateResponse.class);
    }

    @Override
    public ClusterTemplateResponse getByNameInWorkspace(Long workspaceId, String name) {
        return converterUtil.convert(clusterTemplateService.getByNameForWorkspaceId(name, workspaceId), ClusterTemplateResponse.class);
    }

    @Override
    public void deleteInWorkspace(Long workspaceId, String name) {
        clusterTemplateService.delete(name, workspaceId);
    }
}
