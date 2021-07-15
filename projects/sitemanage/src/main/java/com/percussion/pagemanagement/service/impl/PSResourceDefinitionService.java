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
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */
package com.percussion.pagemanagement.service.impl;

import com.percussion.pagemanagement.dao.IPSResourceDefinitionGroupDao;
import com.percussion.pagemanagement.dao.impl.PSResourceDefinitionUniqueId;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSAssetResource;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSResourceDefinition;
import com.percussion.pagemanagement.data.PSThemeResource;
import com.percussion.pagemanagement.service.IPSResourceDefinitionService;
import com.percussion.share.dao.IPSGenericDao.LoadException;
import com.percussion.share.service.IPSDataService.DataServiceLoadException;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSParameterValidationUtils;
import com.percussion.theme.data.PSThemeSummary;
import com.percussion.theme.service.IPSThemeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("resourceDefinitionService")
@Lazy
public class PSResourceDefinitionService implements IPSResourceDefinitionService
{
    private IPSResourceDefinitionGroupDao dao;
    private IPSThemeService themeService;

    @Autowired
    public PSResourceDefinitionService(IPSResourceDefinitionGroupDao dao, IPSThemeService themeService)
    {
        super();
        this.dao = dao;
        this.themeService = themeService;
    }

    /**
     * {@inheritDoc}
     */
    public String createUniqueId(String groupId, String id) throws PSResourceDefinitionInvalidIdException {
        PSResourceDefinitionUniqueId uid = new PSResourceDefinitionUniqueId(groupId, id);
        return uid.getUniqueId();
    }

    public void delete(String id) throws PSDataServiceException {
        dao.delete(id);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PSResourceDefinitionGroup find(String id) throws DataServiceLoadException,
            PSResourceDefinitionGroupNotFoundException
    {
        PSResourceDefinitionGroup rdg;
        try {
           rdg = dao.find(id);
        } catch (PSDataServiceException e) {
            throw new DataServiceLoadException(e.getMessage(),e);
        }
        if (rdg == null)
            throw new PSResourceDefinitionGroupNotFoundException("No resource group found for id: " + id);
        return rdg;
    }

    /**
     * {@inheritDoc}
     */
    public List<PSResourceDefinitionGroup> findAll() throws PSDataServiceException {
        return dao.findAll();
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<PSResourceDefinition> findAllResources() throws PSDataServiceException {
        return dao.findAllResources();
    }
    
    /**
     * {@inheritDoc}
     */
    public PSResourceDefinition findResource(String uniqueId) throws PSDataServiceException {
        PSParameterValidationUtils.rejectIfBlank("findResource", "uniqueId", uniqueId);
        PSResourceDefinition rd = findThemeCSSResource(uniqueId);
        if (rd != null) return rd;
        rd = dao.findResource(uniqueId);
        if(rd == null)
            throw new PSResourceDefinitionNotFoundException("No resource found for uniqueId: " + uniqueId);
        return rd;
    }

    public PSResourceDefinitionGroup save(PSResourceDefinitionGroup object) throws PSDataServiceException {
        return dao.save(object);
    }

    /**
     * {@inheritDoc}
     */
    public PSAssetResource findDefaultAssetResourceForType(String contentType) throws PSDataServiceException
    {
        PSAssetResource resource = dao.findAssetResourceForType(contentType);
        if (resource == null) 
            throw new PSResourceDefinitionNotFoundException("Not primary asset for content type: " + contentType);
        return resource;
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<PSAssetResource> findAssetResourcesForType(String contentType) throws PSDataServiceException {
        return dao.findAssetResourcesForType(contentType);
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<PSAssetResource> findAssetResourcesForLegacyTemplate(String template) throws PSDataServiceException {
        return dao.findAssetResourcesForLegacyTemplate(template);
    }

    /**
     * Checks to see if this resource is a theme resource. If it
     * is a resource definition is returned.
     * @param uniqueId valid unique id, never <code>null</code> or empty.
     * @return maybe <code>null</code> if no theme is found for the given unique id..
     */
    private PSResourceDefinition findThemeCSSResource(String uniqueId) throws PSDataServiceException {
        PSResourceDefinitionUniqueId uid = new PSResourceDefinitionUniqueId(uniqueId);
        if (THEME_GROUP_NAME.equals(uid.getGroupId())) {
            PSThemeSummary sum = themeService.find(uid.getLocalId());
            if (sum != null) {
                PSThemeResource themeResource = new PSThemeResource();
                themeResource.setThemeSummary(sum);
                if (sum.getName() != null) {
                    themeResource.setId(sum.getName());
                    themeResource.setGroupId(THEME_GROUP_NAME);
                    themeResource.setUniqueId(uid.getUniqueId());
                }
                return themeResource;
            }
        }
        return null;
    }
    
    

}
