/*
 *     Percussion CMS
 *     Copyright (C) 1999-2020 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percusssion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package com.percussion.rest.locationscheme;

import com.percussion.rest.extensions.Extension;
import com.percussion.rest.extensions.ExtensionFilterOptions;
import com.percussion.rest.extensions.ExtensionList;
import com.percussion.rest.extensions.IExtensionAdaptor;
import com.percussion.util.PSSiteManageBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@PSSiteManageBean(value="restLocationSchemeResource")
@Path("/locationschemes")
@XmlRootElement
@Api(value = "/locationschemes", description = "Location Scheme operations")
public class LocationSchemesResource {

    @Autowired
    ILocationSchemeAdaptor adaptor;

    @Autowired
    IExtensionAdaptor extensionAdaptor;

    @Context
    private UriInfo uriInfo;

    public LocationSchemesResource(){}

    @GET
    @Path("/generators")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value="Return a list of registered Location Scheme generators on the system", response=Extension.class, responseContainer = "List")
    public List<Extension> listLocationSchemeGenerators(){
        ExtensionFilterOptions filter = new ExtensionFilterOptions();

        filter.setInterfacePattern("com.percussion.extension.IPSAssemblyLocation");
        return new ExtensionList(extensionAdaptor.getExtensions(uriInfo.getBaseUri(),filter));
    }

}
