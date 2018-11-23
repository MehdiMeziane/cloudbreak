package com.sequenceiq.cloudbreak.converter.v2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.SharedServiceRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.CustomDomainSettings;
import com.sequenceiq.cloudbreak.api.model.v2.GeneralSettings;
import com.sequenceiq.cloudbreak.api.model.v2.ImageSettings;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.PlacementSettings;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.Tags;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackTemplateService;

@Component
public class StackTemplateToStackV2RequestConverter extends AbstractConversionServiceAwareConverter<Stack, StackV2Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackTemplateToStackV2RequestConverter.class);

    @Inject
    private ComponentConfigProvider componentConfigProvider;

    @Inject
    private StackTemplateService stackTemplateService;

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Override
    public StackV2Request convert(Stack source) {
        source = stackTemplateService.getByIdWithLists(source.getId());
        if (source.getCluster() != null) {
            source.setCluster(clusterService.findOneWithLists(source.getCluster().getId()));
        }
        StackV2Request stackV2Request = new StackV2Request();
        stackV2Request.setGeneral(getGeneral(source));
        stackV2Request.setPlacement(getPlacementSettings(source.getRegion(), source.getAvailabilityZone()));
        stackV2Request.setCustomDomain(getCustomDomainSettings(source.getCustomDomain(), source.getCustomHostname(),
                source.isHostgroupNameAsHostname(), source.isClusterNameAsSubdomain()));
        stackV2Request.setFlexId(source.getFlexSubscription() == null ? null : source.getFlexSubscription().getId());
        stackV2Request.setParameters(checkMap(source.getParameters()));
        stackV2Request.setStackAuthentication(getConversionService().convert(source.getStackAuthentication(), StackAuthenticationRequest.class));
        stackV2Request.setNetwork(getConversionService().convert(source.getNetwork(), NetworkV2Request.class));
        stackV2Request.setCluster(getConversionService().convert(source.getCluster(), ClusterV2Request.class));
        stackV2Request.setInstanceGroups(getInstanceGroups(source));
        prepareImage(source, stackV2Request);
        prepareTags(source, stackV2Request);
        prepareInputs(source, stackV2Request);
        prepareDatalakeRequest(source, stackV2Request);
        return stackV2Request;
    }

    private <T extends Collection> T checkCollection(T collection) {
        if (collection == null || collection.isEmpty()) {
            return null;
        }
        return collection;
    }

    private <T extends Map> T checkMap(T map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        return map;
    }

    private void prepareDatalakeRequest(Stack source, StackV2Request stackV2Request) {
        if (source.getDatalakeId() != null) {
            SharedServiceRequest sharedServiceRequest = new SharedServiceRequest();
            sharedServiceRequest.setSharedCluster(stackService.get(source.getDatalakeId()).getName());
            stackV2Request.getCluster().setSharedService(sharedServiceRequest);
        }
    }

    private PlacementSettings getPlacementSettings(String region, String availabilityZone) {
        if (region == null && availabilityZone == null) {
            return null;
        }
        PlacementSettings ps = new PlacementSettings();
        ps.setRegion(region);
        ps.setAvailabilityZone(availabilityZone);
        return ps;
    }

    private CustomDomainSettings getCustomDomainSettings(String customDomain, String customHostname,
            Boolean hostgroupNameAsHostname, Boolean clusterNameAsSubdomain) {
        if (customDomain == null && customHostname == null && hostgroupNameAsHostname == null && clusterNameAsSubdomain == null) {
            return null;
        }
        CustomDomainSettings cd = new CustomDomainSettings();
        cd.setCustomDomain(customDomain);
        cd.setCustomHostname(customHostname);
        cd.setHostgroupNameAsHostname(hostgroupNameAsHostname);
        cd.setClusterNameAsSubdomain(clusterNameAsSubdomain);
        return cd;
    }

    private GeneralSettings getGeneral(Stack source) {
        GeneralSettings generalSettings = new GeneralSettings();
        generalSettings.setName("");
        if (source.getCredential() == null) {
            return generalSettings;
        }
        if (source.getCredential() != null) {
            generalSettings.setCredentialName(source.getCredential().getName());
        }
        return generalSettings;
    }

    private void prepareImage(Stack source, StackV2Request stackV2Request) {
        try {
            Image image = componentConfigProvider.getImage(source.getId());
            ImageSettings is = new ImageSettings();
            is.setImageId(Strings.isNullOrEmpty(image.getImageId()) ? "" : image.getImageId());
            is.setImageCatalog(Strings.isNullOrEmpty(image.getImageCatalogName()) ? "" : image.getImageCatalogName());
            stackV2Request.setImageSettings(is);
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.error(e.toString());
        }
    }

    private void collectInformationsFromActualHostgroup(Cluster cluster, InstanceGroup instanceGroup, InstanceGroupV2Request instanceGroupV2Request) {
        HostGroup actualHostgroup = null;
        if (cluster != null && cluster.getHostGroups() != null) {
            for (HostGroup hostGroup : cluster.getHostGroups()) {
                if (hostGroup.getName().equals(instanceGroup.getGroupName())) {
                    actualHostgroup = hostGroup;
                }
            }
        }
        if (actualHostgroup != null) {
            instanceGroupV2Request.setRecoveryMode(actualHostgroup.getRecoveryMode());
            if (actualHostgroup.getRecipes() != null) {
                instanceGroupV2Request.setRecipeNames(new HashSet<>());
                for (Recipe recipe : actualHostgroup.getRecipes()) {
                    instanceGroupV2Request.getRecipeNames().add(recipe.getName());
                }
            }
        }
    }

    private void prepareTags(Stack source, StackV2Request stackV2Request) {
        try {
            if (source.getTags() != null) {
                StackTags stackTags = source.getTags().get(StackTags.class);
                if (stackTags.getUserDefinedTags() != null) {
                    Tags tags = new Tags();
                    tags.setApplicationTags(null);
                    tags.setDefaultTags(null);
                    tags.setUserDefinedTags(stackTags.getUserDefinedTags());
                    stackV2Request.setTags(tags);
                }
            }
        } catch (IOException e) {
            stackV2Request.setTags(null);
        }
    }

    private void prepareInputs(Stack source, StackV2Request stackV2Request) {
        try {
            if (source.getInputs() == null) {
                return;
            }
            StackInputs stackInputs = Strings.isNullOrEmpty(source.getInputs().getValue()) ? null : source.getInputs().get(StackInputs.class);
            if (stackInputs != null && stackInputs.getCustomInputs() != null) {
                stackV2Request.setInputs(stackInputs.getCustomInputs());
            }
        } catch (IOException e) {
            stackV2Request.setInputs(null);
        }
    }

    private List<InstanceGroupV2Request> getInstanceGroups(Stack stack) {
        List<InstanceGroupV2Request> ret = new ArrayList<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            InstanceGroupV2Request instanceGroupV2Request = getConversionService().convert(instanceGroup, InstanceGroupV2Request.class);
            collectInformationsFromActualHostgroup(stack.getCluster(), instanceGroup, instanceGroupV2Request);
            ret.add(instanceGroupV2Request);
        }
        return checkCollection(ret);
    }

}
