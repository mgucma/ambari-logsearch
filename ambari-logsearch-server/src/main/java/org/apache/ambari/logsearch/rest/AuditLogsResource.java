/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ambari.logsearch.rest;

import static org.apache.ambari.logsearch.doc.DocConstants.AuditOperationDescriptions.*;

import java.util.List;
import java.util.Map;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.StatusMessage;
import org.apache.ambari.logsearch.manager.AuditLogsManager;
import org.apache.ambari.logsearch.model.metadata.AuditFieldMetadataResponse;
import org.apache.ambari.logsearch.model.request.impl.body.*;
import org.apache.ambari.logsearch.model.request.impl.query.*;
import org.apache.ambari.logsearch.model.response.AuditLogResponse;
import org.apache.ambari.logsearch.model.response.BarGraphDataListResponse;
import org.springframework.context.annotation.Scope;

import freemarker.template.TemplateException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

@Api(value = "audit/logs", description = "Audit log operations", authorizations = {@Authorization(value = "basicAuth")})
@Path("audit/logs")
@Named
@Scope("request")
public class AuditLogsResource {

  @Inject
  private AuditLogsManager auditLogsManager;

  @GET
  @Path("/schema/fields")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_AUDIT_SCHEMA_FIELD_LIST_OD)
  public AuditFieldMetadataResponse getSolrFieldListGet() {
    return auditLogsManager.getAuditLogSchemaMetadata();
  }

  @POST
  @Path("/schema/fields")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_AUDIT_SCHEMA_FIELD_LIST_OD)
  public AuditFieldMetadataResponse getSolrFieldListPost() {
    return auditLogsManager.getAuditLogSchemaMetadata();
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_AUDIT_LOGS_OD)
  public AuditLogResponse getAuditLogsGet(@BeanParam AuditLogQueryRequest auditLogRequest) {
    return auditLogsManager.getLogs(auditLogRequest);
  }

  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_AUDIT_LOGS_OD)
  public AuditLogResponse getAuditLogsPost(AuditLogBodyRequest auditLogRequest) {
    return auditLogsManager.getLogs(auditLogRequest);
  }

  @DELETE
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(PURGE_AUDIT_LOGS_OD)
  public StatusMessage deleteAuditLogs(AuditLogBodyRequest auditLogRequest) {
    return auditLogsManager.deleteLogs(auditLogRequest);
  }

  @GET
  @Path("/components")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_AUDIT_COMPONENTS_OD)
  public Map<String, String> getAuditComponentsGet(@QueryParam(LogSearchConstants.REQUEST_PARAM_CLUSTER_NAMES) @Nullable String clusters) {
    return auditLogsManager.getAuditComponents(clusters);
  }

  @POST
  @Path("/components")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_AUDIT_COMPONENTS_OD)
  public Map<String, String> getAuditComponentsPost(@Nullable ClusterBodyRequest clusterBodyRequest) {
    return auditLogsManager.getAuditComponents(clusterBodyRequest != null ? clusterBodyRequest.getClusters() : null);
  }

  @GET
  @Path("/bargraph")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_AUDIT_LINE_GRAPH_DATA_OD)
  public BarGraphDataListResponse getAuditBarGraphDataGet(@BeanParam AuditBarGraphQueryRequest request) {
    return auditLogsManager.getAuditBarGraphData(request);
  }

  @POST
  @Path("/bargraph")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_AUDIT_LINE_GRAPH_DATA_OD)
  public BarGraphDataListResponse getAuditBarGraphDataPost(AuditBarGraphBodyRequest request) {
    return auditLogsManager.getAuditBarGraphData(request);
  }

  @GET
  @Path("/resources/{top}")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_TOP_AUDIT_RESOURCES_OD)
  public BarGraphDataListResponse getResourcesGet(@BeanParam TopFieldAuditLogQueryRequest request) {
    return auditLogsManager.topResources(request);
  }

  @POST
  @Path("/resources/{top}")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_TOP_AUDIT_RESOURCES_OD)
  public BarGraphDataListResponse getResourcesPost(TopFieldAuditLogBodyRequest request, @PathParam(LogSearchConstants.REQUEST_PARAM_TOP) Integer top) {
    request.setTop(top); // TODO: set this in the request
    return auditLogsManager.topResources(request);
  }

  @GET
  @Path("/export")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(EXPORT_USER_TALBE_TO_TEXT_FILE_OD)
  public Response exportUserTableToTextFileGet(@BeanParam UserExportQueryRequest request) throws TemplateException {
    return auditLogsManager.export(request);
  }

  @POST
  @Path("/export")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(EXPORT_USER_TALBE_TO_TEXT_FILE_OD)
  public Response exportUserTableToTextFilePost(UserExportBodyRequest request) throws TemplateException {
    return auditLogsManager.export(request);
  }

  @GET
  @Path("/serviceload")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_SERVICE_LOAD_OD)
  public BarGraphDataListResponse getServiceLoadGet(@BeanParam AuditServiceLoadQueryRequest request) {
    return auditLogsManager.getServiceLoad(request);
  }

  @POST
  @Path("/serviceload")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_SERVICE_LOAD_OD)
  public BarGraphDataListResponse getServiceLoadPost(AuditServiceLoadBodyRequest request) {
    return auditLogsManager.getServiceLoad(request);
  }

  @GET
  @Path("/clusters")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_AUDIT_CLUSTERS_OD)
  public List<String> getClustersForAuditLogGet() {
    return auditLogsManager.getClusters();
  }

  @POST
  @Path("/clusters")
  @Produces({MediaType.APPLICATION_JSON})
  @Consumes({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_AUDIT_CLUSTERS_OD)
  public List<String> getClustersForAuditLogPost() {
    return auditLogsManager.getClusters();
  }
}