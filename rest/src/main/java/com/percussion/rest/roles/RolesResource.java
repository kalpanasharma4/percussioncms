/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.rest.roles;

import com.percussion.error.PSExceptionUtils;
import com.percussion.rest.Status;
import com.percussion.rest.errors.BackendException;
import com.percussion.util.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

@PSSiteManageBean(value="restRolesResource")
@Path("/roles")
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Tag(name = "Roles", description = "Role operations")
public class RolesResource {

	private static final Logger log = LogManager.getLogger(RolesResource.class);

	 @Autowired
	 private IRoleAdaptor roleAdaptor;

    @Context
	 private UriInfo uriInfo;
	 
	 public RolesResource(){}

	   	@GET
	    @Path("/{roleName}")
	    @Produces(
	    {MediaType.APPLICATION_JSON})
	    @Operation(summary = "Get a Role",
				description = "Will get the Role specified by the roleName.",
				responses = {
	      @ApiResponse(responseCode = "200", description = "OK", content=@Content(
	      		schema=@Schema(implementation = Role.class)
		  )),
	      @ApiResponse(responseCode = "404", description = "Role not found"),
	      @ApiResponse(responseCode = "500", description = "Error message") 
	    })
	    public Role getRoleByName(@PathParam("roleName") String roleName)
	    {
			try {
				Role r = null;

				roleName = java.net.URLDecoder.decode(roleName, "UTF-8");

				r = roleAdaptor.getRole(uriInfo.getBaseUri(), roleName);
				return r;
			} catch (BackendException | UnsupportedEncodingException e) {
				log.error(PSExceptionUtils.getMessageForLog(e));
				log.debug(PSExceptionUtils.getDebugMessageForLog(e));
				throw new WebApplicationException(e);
			}
		}
	   	
	    @DELETE
	    @Path("/{roleName}")
	    @Consumes(MediaType.APPLICATION_JSON)
	    @Produces(MediaType.APPLICATION_JSON)
	    @Operation(summary = "Delete a Role",
				description = "Will delete the specified roleName from the system.  If the Role is a Directory based Group, the link to the directory is removed but the Group will not be removed from the remote Directory."
	         , responses = {
	      @ApiResponse(responseCode = "200", description = "OK", content=@Content(
	      		schema=@Schema(implementation = Status.class)
		  )),
	      @ApiResponse(responseCode = "404", description = "Role not found"),
	      @ApiResponse(responseCode = "500", description = "Error message") 
	    })
	    public Status deleteRole(@Parameter(description= "The roleName of the Role to delete." ,
				name="roleName" ) @PathParam("roleName")
	    String roleName)
	    {
	    	int retCode = 404;
	    	String message = "Role not found";
	    	
	        // Path param should be url decoded by default.  CXF jars interacting when running in cm1
	        try
	        {
	        	roleName = java.net.URLDecoder.decode(roleName, "UTF-8");
	        }
	        catch (UnsupportedEncodingException e)
	        {
	           retCode = 500;
	           message = e.getMessage();
	        }
	        
	        try{
	        	
	        	roleAdaptor.deleteRole(uriInfo.getBaseUri(), roleName);
	        	retCode = 200;
	        	message = "OK";
	        }catch(Exception e){
	        	retCode = 500;
	        	message = e.getMessage();
	        }
	        return new Status(retCode,  message);
	    }
	 
	    @PUT
	    @Path("/")
	    @Consumes(MediaType.APPLICATION_JSON)
	    @Produces(MediaType.APPLICATION_JSON)
	    @Operation(summary = "Create or update a Role",
				description = "Creates or Updates the specified Role.  Returns the resulting Role.",
				responses= {
	      @ApiResponse(responseCode = "500", description = "Error"),
	      @ApiResponse(responseCode = "200", description = "OK", content=@Content(
	      		schema=@Schema(implementation = Role.class)
		  ))
	      
	    })
	    public Role updateRole(@Parameter(description= "The body containing a JSON payload" ,
                name="body" ) Role role)
	    {
	    	Role ret = null;
	   
	        ret = roleAdaptor.updateRole(uriInfo.getBaseUri(), role);
	        
	        return ret;
	 	 }
	    
	    
	    @GET
	    @Path("/list/{pattern}")
	    @Produces(
	    {MediaType.APPLICATION_JSON})
		@Operation(summary = "Find available roles on the system by % wild card pattern",
		responses = {
				@ApiResponse(responseCode = "200", description = "OK", content=@Content(
						array=@ArraySchema(schema = @Schema(implementation = Role.class))
				)),
				@ApiResponse(responseCode = "500", description = "Error")
		})

	    public RoleList findRoles()
	    {
	    	try {
				List<Role> ret;

				ret = roleAdaptor.findRoles(uriInfo.getBaseUri(), "%");

				return new RoleList(ret);
			} catch (BackendException e) {
				log.error(PSExceptionUtils.getMessageForLog(e));
				log.debug(PSExceptionUtils.getDebugMessageForLog(e));
				throw new WebApplicationException(e);
			}

		}
	    
	    public IRoleAdaptor getRoleAdaptor()
	    {
	        return roleAdaptor;
	    }

	    public void setRoleAdaptor(IRoleAdaptor roleAdaptor)
	    {
	        this.roleAdaptor = roleAdaptor;
	    }

}
