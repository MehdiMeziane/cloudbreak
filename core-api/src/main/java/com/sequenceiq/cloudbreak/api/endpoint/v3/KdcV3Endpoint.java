package com.sequenceiq.cloudbreak.api.endpoint.v3;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.api.model.KerberosResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{workspaceId}/kdc")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{workspaceId}/kdc", description = ControllerDescription.KDC_V3_DESCRIPTION, protocols = "http,https")
public interface KdcV3Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.KdcOpDescription.LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = Notes.KDC_NOTES,
            nickname = "listKdcByWorkspace")
    Set<KerberosResponse> listByWorkspace(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.KdcOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.KDC_NOTES,
            nickname = "getKdcInWorkspace")
    KerberosResponse getByNameInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.KdcOpDescription.CREATE_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.KDC_NOTES,
            nickname = "createKdcInWorkspace")
    KerberosResponse createInWorkspace(@PathParam("workspaceId") Long workspaceId, @Valid KerberosRequest request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.KdcOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.KDC_NOTES,
            nickname = "deleteKdcInWorkspace")
    KerberosResponse deleteInWorkspace(@PathParam("workspaceId") Long workspaceId,  @PathParam("name") String name);

    @PUT
    @Path("{name}/attach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.KdcOpDescription.ATTACH_TO_ENVIRONMENTS, produces = ContentType.JSON, notes = Notes.KDC_NOTES,
            nickname = "attachKdcToEnvironments")
    KerberosResponse attachToEnvironments(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name, @NotEmpty Set<String> environmentNames);

    @PUT
    @Path("{name}/detach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.KdcOpDescription.DETACH_FROM_ENVIRONMENTS, produces = ContentType.JSON, notes = Notes.KDC_NOTES,
            nickname = "detachKdcFromEnvironments")
    KerberosResponse detachFromEnvironments(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @NotEmpty Set<String> environmentNames);

}
