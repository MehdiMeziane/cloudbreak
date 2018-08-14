package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v3.RdsConfigV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigResponse;
import com.sequenceiq.cloudbreak.api.model.rds.RDSTestRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RdsTestResult;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

@Component
@Transactional(TxType.NEVER)
public class RdsConfigV3Controller extends NotificationController implements RdsConfigV3Endpoint {

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Override
    public Set<RDSConfigResponse> listByOrganization(Long organizationId) {
        return null;
    }

    @Override
    public RDSConfigResponse getByNameInOrganization(Long organizationId, String name) {
        return null;
    }

    @Override
    public RDSConfigResponse createInOrganization(Long organizationId, RDSConfigRequest request) {
        return null;
    }

    @Override
    public RDSConfigResponse deleteInOrganization(Long organizationId, String name) {
        return null;
    }

    @Override
    public RdsTestResult testRdsConnection(Long organizationId, RDSTestRequest rdsTestRequest) {
        return null;
    }

    @Override
    public RDSConfigRequest getRequestFromName(Long organizationId, String name) {
        return null;
    }
}