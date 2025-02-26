/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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

import io.swagger.annotations.ApiOperation;
import org.apache.ambari.logsearch.common.ApiDocStorage;
import org.springframework.context.annotation.Scope;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("swagger.{type:json|yaml}")
@Named
@Scope("request")
public class SwaggerResource {

  @Inject
  private ApiDocStorage apiDocStorage;

  @GET
  @Produces({MediaType.APPLICATION_JSON, "application/yaml"})
  @ApiOperation(value = "The swagger definition in either JSON or YAML", hidden = true)
  public Response swaggerDefinitionResponse(@PathParam("type") String type) {
    Response response = Response.status(404).build();
    if (apiDocStorage.getSwagger() != null) {
      if ("yaml".equalsIgnoreCase(type)) {
        response = Response.ok().entity(apiDocStorage.getSwaggerYaml()).type("application/yaml").build();
      } else {
        response = Response.ok().entity(apiDocStorage.getSwagger()).build();
      }
    }
    return response;
  }
}
