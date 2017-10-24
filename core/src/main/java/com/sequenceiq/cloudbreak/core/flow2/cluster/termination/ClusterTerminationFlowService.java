package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_FAILED;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationResult;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterTerminationService;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;

@Service
public class ClusterTerminationFlowService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTerminationFlowService.class);

    @Inject
    private ClusterTerminationService terminationService;

    @Inject
    private EmailSenderService emailSenderService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private FlowMessageService flowMessageService;

    public void terminateCluster(ClusterViewContext context) {
        clusterService.updateClusterStatusByStackId(context.getStackId(), Status.DELETE_IN_PROGRESS);
        LOGGER.info("Cluster delete started.");
    }

    public void finishClusterTerminationAllowed(ClusterViewContext context, ClusterTerminationResult payload) {
        LOGGER.info("Terminate cluster result: {}", payload);
        StackView stack = context.getStack();
        ClusterView cluster = context.getCluster();
        if (cluster != null) {
            terminationService.finalizeClusterTermination(cluster.getId());
            clusterService.updateClusterStatusByStackId(stack.getId(), DELETE_COMPLETED);
            InMemoryStateStore.deleteCluster(cluster.getId());
            stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.AVAILABLE);
            if (cluster.getEmailNeeded()) {
                emailSenderService.sendTerminationSuccessEmail(cluster.getOwner(), cluster.getEmailTo(), cluster.getAmbariIp(), cluster.getName());
                flowMessageService.fireEventAndLog(stack.getId(), Msg.CLUSTER_EMAIL_SENT, DELETE_COMPLETED.name());
            }
        }
    }

    public void finishClusterTerminationNotAllowed(ClusterViewContext context, ClusterTerminationResult payload) {
        StackView stack = context.getStack();
        Long stackId = stack.getId();
        ClusterView cluster = context.getCluster();
        flowMessageService.fireEventAndLog(stackId, Msg.CLUSTER_DELETE_FAILED, DELETE_FAILED.name(), "Operation not allowed");
        clusterService.updateClusterStatusByStackId(stackId, AVAILABLE);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE);
        if (cluster.getEmailNeeded()) {
            sendDeleteFailedMail(cluster, stackId);
        }
    }

    public void handleClusterTerminationError(StackFailureEvent payload) {
        LOGGER.info("Handling cluster delete failure event.");
        Exception errorDetails = payload.getException();
        LOGGER.error("Error during cluster termination flow: ", errorDetails);
        Cluster cluster = clusterService.retrieveClusterByStackId(payload.getStackId());
        if (cluster != null) {
            cluster.setStatus(DELETE_FAILED);
            cluster.setStatusReason(errorDetails.getMessage());
            clusterService.updateCluster(cluster);
            flowMessageService.fireEventAndLog(cluster.getStack().getId(), Msg.CLUSTER_DELETE_FAILED, DELETE_FAILED.name(), errorDetails.getMessage());
            if (cluster.getEmailNeeded()) {
                sendDeleteFailedMail(cluster);
            }
        }
    }

    private void sendDeleteFailedMail(Cluster cluster) {
        emailSenderService.sendTerminationFailureEmail(cluster.getOwner(), cluster.getEmailTo(), cluster.getAmbariIp(), cluster.getName());
        flowMessageService.fireEventAndLog(cluster.getStack().getId(), Msg.CLUSTER_EMAIL_SENT, DELETE_FAILED.name());
    }

    private void sendDeleteFailedMail(ClusterView cluster, long stackId) {
        emailSenderService.sendTerminationFailureEmail(cluster.getOwner(), cluster.getEmailTo(), cluster.getAmbariIp(), cluster.getName());
        flowMessageService.fireEventAndLog(stackId, Msg.CLUSTER_EMAIL_SENT, DELETE_FAILED.name());
    }
}
