package com.sequenceiq.cloudbreak.converter.v2;

import static com.gs.collections.impl.utility.StringIterate.isEmpty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.Tags;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentViewService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
public class StackV2RequestToStackTemplateConverter extends AbstractConversionServiceAwareConverter<StackV2Request, Stack> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackV2RequestToStackTemplateConverter.class);

    @Inject
    private CredentialService credentialService;

    @Inject
    private EnvironmentViewService environmentViewService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private CloudParameterService cloudParameterService;

    @Override
    public Stack convert(StackV2Request source) {
        Stack stack = new Stack();

        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        stack.setWorkspace(workspace);

        if (source.getGeneral() != null) {
            stack.setName(source.getGeneral().getName());
            stack.setDisplayName(source.getGeneral().getName());
            if (source.getGeneral().getCredentialName() != null) {
                stack.setCredential(credentialService.getByNameForWorkspace(source.getGeneral().getCredentialName(), workspace));
            }
            if (source.getGeneral().getEnvironmentName() != null) {
                stack.setEnvironment(environmentViewService.getByNameForWorkspace(source.getGeneral().getEnvironmentName(), workspace));
            }
        }
        if (source.getPlacement() != null) {
            stack.setAvailabilityZone(source.getPlacement().getAvailabilityZone());
            stack.setRegion(source.getPlacement().getRegion());
        }
        stack.setCloudPlatform(getPlatform(source, workspace));
        stack.setTags(getTags(source.getTags()));
        stack.setInputs(getInputs(source.getInputs()));
        StackAuthentication stackAuthentication = getConversionService().convert(source.getStackAuthentication(), StackAuthentication.class);
        stack.setStackAuthentication(stackAuthentication);
        stack.setPlatformVariant(source.getPlatformVariant());
        stack.setInstanceGroups(convertInstanceGroups(source, stack));
        if (source.getNetwork() != null) {
            stack.setNetwork(getConversionService().convert(source.getNetwork(), Network.class));
        }
        if (source.getCustomDomain() != null) {
            stack.setCustomDomain(source.getCustomDomain().getCustomDomain());
            stack.setCustomHostname(source.getCustomDomain().getCustomHostname());
            stack.setClusterNameAsSubdomain(source.getCustomDomain().isClusterNameAsSubdomain());
            stack.setHostgroupNameAsHostname(source.getCustomDomain().isHostgroupNameAsHostname());
        }

        if (source.getCluster() != null) {
            Cluster cluster = getConversionService().convert(source.getCluster(), Cluster.class);
            stack.setCluster(cluster);
        }
        stack.setType(StackType.TEMPLATE);
        return stack;

    }

    private Json getTags(Tags tags) {
        if (tags == null) {
            return null;
        }
        try {
            return new Json(new StackTags(tags.getUserDefinedTags(), tags.getApplicationTags(), tags.getDefaultTags()));
        } catch (Exception ignored) {
            throw new BadRequestException("Failed to convert dynamic tags.");
        }
    }

    private StackTags mergeTags(Map<String, String> applicationTags, Map<String, String> userDefinedTags, Map<String, String> defaultTags) {
        return new StackTags(userDefinedTags, applicationTags, defaultTags);
    }

    private Json getInputs(Map<String, Object> inputs) {
        if (inputs == null) {
            return null;
        }
        try {
            return new Json(new StackInputs(inputs, new HashMap<>(), new HashMap<>()));
        } catch (Exception ignored) {
            throw new BadRequestException("Failed to convert dynamic inputs.");
        }
    }

    private String getPlatform(StackV2Request source, Workspace workspace) {
        String ret = null;
        if (source.getGeneral() != null) {
            String cloudPlatform = null;
            if (source.getGeneral().getEnvironmentName() != null) {
                cloudPlatform = environmentViewService.getByNameForWorkspace(source.getGeneral().getEnvironmentName(), workspace).getCloudPlatform();
            } else if (source.getGeneral().getCredentialName() != null) {
                cloudPlatform = credentialService.getByNameForWorkspace(source.getGeneral().getCredentialName(), workspace).cloudPlatform();
            }
            if (isEmpty(cloudPlatform) && !isEmpty(source.getPlatformVariant())) {
                ret = cloudParameterService.getPlatformByVariant(source.getPlatformVariant());
            }
        }
        return ret;
    }

    private Set<InstanceGroup> convertInstanceGroups(StackV2Request source, Stack stack) {
        if (source.getInstanceGroups() == null) {
            return null;
        }
        List<InstanceGroupV2Request> instanceGroupRequests = source.getInstanceGroups();
        Set<InstanceGroup> convertedSet = new HashSet<>();
        instanceGroupRequests.stream()
                .map(ig -> getConversionService().convert(ig, InstanceGroup.class))
                .filter(Objects::nonNull)
                .forEach(ig -> {
                    ig.setStack(stack);
                    convertedSet.add(ig);
                });
        return convertedSet;
    }
}

