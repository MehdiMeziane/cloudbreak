package com.sequenceiq.cloudbreak.repository.cluster;

import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action.READ;

import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByTarget;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = ClusterTemplate.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisableHasPermission
@WorkspaceResourceType(resource = WorkspaceResource.CLUSTER_TEMPLATE)
public interface ClusterTemplateRepository extends WorkspaceResourceRepository<ClusterTemplate, Long> {

    @Override
    @DisableHasPermission
    @CheckPermissionsByTarget(action = READ, targetIndex = 0)
    <S extends ClusterTemplate> Iterable<S> saveAll(Iterable<S> entities);

    @Query("SELECT c FROM ClusterTemplate c LEFT JOIN FETCH c.stackTemplate s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig "
            + "LEFT JOIN FETCH ig.instanceMetaData WHERE c.workspace.id= :workspaceId AND c.status <> 'DEFAULT_DELETED'")
    @CheckPermissionsByReturnValue
    Set<ClusterTemplate> findAllByNotDeletedInWorkspace(@Param("workspaceId") Long workspaceId);

    @Query("SELECT c FROM ClusterTemplate c LEFT JOIN FETCH c.stackTemplate s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig "
            + "LEFT JOIN FETCH ig.instanceMetaData WHERE c.workspace.id= :workspaceId AND c.name in :names AND c.status <> 'DEFAULT_DELETED'")
    @CheckPermissionsByReturnValue
    List<ClusterTemplate> findAllByNameAndWorkspaceIdWithList(@Param("names") List<String> names, @Param("workspaceId") Long workspaceId);
}
