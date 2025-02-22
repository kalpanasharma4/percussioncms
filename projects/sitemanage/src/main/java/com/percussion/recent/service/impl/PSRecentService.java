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

package com.percussion.recent.service.impl;

import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.cms.IPSConstants;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.error.PSExceptionUtils;
import com.percussion.itemmanagement.service.impl.PSWorkflowHelper;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.data.PSWidgetContentType;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.pathmanagement.service.impl.PSPathUtils;
import com.percussion.recent.data.PSRecent.RecentType;
import com.percussion.recent.service.IPSRecentServiceBase;
import com.percussion.recent.service.rest.IPSRecentService;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.data.PSItemProperties;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.sitemanage.service.IPSSiteTemplateService;
import com.percussion.webservices.PSWebserviceUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Transactional(propagation=Propagation.REQUIRED)
@Component("recentService")
@Lazy
public class PSRecentService implements IPSRecentService
{
 
    @Autowired
    private @Qualifier("recentServiceBase") IPSRecentServiceBase recentService;

    @Autowired
    private @Qualifier("pathService") IPSPathService pathService;

    @Autowired
    private IPSIdMapper idMapper;
   
    @Autowired
    private @Qualifier("folderHelper") IPSFolderHelper  folderHelper;
    
    @Autowired
    private IPSAssetService assetService;

    @Autowired
    private IPSSiteTemplateService siteTemplateService;

    private static final Logger log = LogManager.getLogger(IPSConstants.CONTENTREPOSITORY_LOG);


    @Override
    public List<PSItemProperties> findRecentItem(boolean ignoreArchivedItems)
    {
        String user = PSWebserviceUtils.getUserName();
        List<String> recentEntries = recentService.findRecent(user, null, RecentType.ITEM);
        List<PSItemProperties> items = new ArrayList<>();
        List<String> toDelete = new ArrayList<>();
        for (String entry : recentEntries)
        {
            PSItemProperties itemProps = null;
            try
            {
                itemProps = folderHelper.findItemPropertiesById(entry);
                if (itemProps != null ) {
                    //don't return archived items and items with no path on home page.
                    if (ignoreArchivedItems && (itemProps.getStatus().equals(PSWorkflowHelper.WF_STATE_ARCHIVE) || itemProps.getPath() == null)) {
                        continue;
                    } else {
                        items.add(itemProps);
                    }
                }else {
                    log.debug("Removing recent item find returned null : {}",
                            entry);
                }
            }
            catch (Exception e)
            {
                log.debug("removing error entry from recent item list {}, Error: {}", entry,
                        PSExceptionUtils.getMessageForLog(e));
            }
            if (itemProps == null) {
                toDelete.add(entry);
            }
        }
        if (!toDelete.isEmpty()) {
            recentService.deleteRecent(user, null, RecentType.ITEM, toDelete);
        }
        return items;
    }

    @Override
    public List<PSTemplateSummary> findRecentTemplate(String siteName)
    {
        String user = PSWebserviceUtils.getUserName();
        List<String> recentEntries = recentService.findRecent(user, siteName, RecentType.TEMPLATE);
        List<PSTemplateSummary> templates = new ArrayList<>();
        List<String> toDelete = new ArrayList<>();

        Map<String,PSTemplateSummary> siteTemplateMap = new HashMap<>();
        // if we do not find site we will remove all entries for site that no
        // longer exist

        List<PSTemplateSummary> siteTemplates = siteTemplateService.findTemplatesBySite(siteName);

        for (PSTemplateSummary siteTemplate : siteTemplates)
        {
            siteTemplateMap.put(siteTemplate.getId(),siteTemplate);
           
        }

        for (String entry : recentEntries)
        {
            PSTemplateSummary template = siteTemplateMap.get(entry);
               
            // Cleanup old or invalid entries
            if (template == null)
            {
                log.debug("Removing recent template not a current site template :{}", entry);
                toDelete.add(entry);
            }
            else 
            {
                templates.add(template);
            }
        }

        if (!toDelete.isEmpty()) {
            recentService.deleteRecent(user, siteName, RecentType.TEMPLATE, toDelete);
        }
        return templates;
    }

    @Override
    public List<PSPathItem> findRecentSiteFolder(String siteName)
    {
        String user = PSWebserviceUtils.getUserName();
        List<String> recentEntries = recentService.findRecent(user, siteName, RecentType.SITE_FOLDER);
        List<PSPathItem> pathItems = new ArrayList<>();
        List<String> toDelete = new ArrayList<>();

        for (String entry : recentEntries)
        {
            PSPathItem pathItem = null;
            try
            {
                pathItem = pathService.find(entry);
                if (pathItem != null)
                    pathItems.add(pathItem);
                else
                    log.debug("Removing recent siteFolder entry find returned null : {}" , entry);
            }
            catch (Exception e)
            {
                log.debug("removing error entry from recent siteFolder list {}, Error: {}", entry,
                        PSExceptionUtils.getMessageForLog(e));
            }
            if (pathItem == null)
                toDelete.add(entry);
        }
        if (!toDelete.isEmpty())
            recentService.deleteRecent(user, siteName, RecentType.SITE_FOLDER, toDelete);
        return pathItems;
    }

    @Override
    public List<PSPathItem> findRecentAssetFolder()
    {
        String user = PSWebserviceUtils.getUserName();
        List<String> recentEntries = recentService.findRecent(user, null, RecentType.ASSET_FOLDER);
        List<PSPathItem> pathItems = new ArrayList<>();

        List<String> toDelete = new ArrayList<>();

        for (String entry : recentEntries)
        {
            PSPathItem pathItem = null;
            try
            {
                pathItem = pathService.find(entry);
                if (pathItem != null)
                    pathItems.add(pathItem);
                else
                    log.debug("Removing recent assetFolder entry find returned null :{}" , entry);
            }
            catch (Exception e)
            {
                log.debug("removing error entry from recent assetFolder list {}, Error: {}", entry,
                        PSExceptionUtils.getMessageForLog(e));
            }
            if (pathItem == null)
                toDelete.add(entry);
        }
        if (!toDelete.isEmpty())
            recentService.deleteRecent(user, null, RecentType.ASSET_FOLDER, toDelete);
        return pathItems;
    }

    @Override
    public void addRecentItem(String value)
    {
        String user = PSWebserviceUtils.getUserName();
        if (PSTypeEnum.LEGACY_CONTENT.getOrdinal() != idMapper.getGuid(value).getType())
            throw new IllegalArgumentException("Value must be an item guid");
        // store guid as a revisionless guid.
        PSLocator locator = new PSLocator(idMapper.getContentId(value));
        locator.setRevision(-1);
        value = idMapper.getString(locator);
        recentService.addRecent(user, null, RecentType.ITEM, value);
    }

    @Override
    public void addRecentTemplate(
    String siteName,
    String value)
    {
        String user = PSWebserviceUtils.getUserName();
        // Templates are stored as items. We do not check if it is a real item
        // here as that
        // requires accessing the full template currently. We will check on the
        // way out.
        if (PSTypeEnum.LEGACY_CONTENT.getOrdinal() != idMapper.getGuid(value).getType())
            throw new IllegalArgumentException("Value must be a template guid");
        // Not actually checking template exists for performance, check and
        // filter done on find.
        recentService.addRecent(user, siteName, RecentType.TEMPLATE, value);
    }

    @Override
    public void addRecentSiteFolder(String value)
    {
        String user = PSWebserviceUtils.getUserName();
        if(StringUtils.isBlank(value) || !(StringUtils.startsWith(value, "//") || StringUtils.startsWith(value, "/")))
            return;

        String folderPath = StringUtils.startsWith(value, "//")?value.substring(1):value;
        String siteName = PSPathUtils.getSiteFromPath(folderPath);

        if(siteName == null)
            return;
        // Not checking database for folder to improve performance, check done
        // on way out.
        
        recentService.addRecent(user, siteName, RecentType.SITE_FOLDER, folderPath);
    }

    @Override
    public void addRecentAssetFolder(String value)
    {
        String user = PSWebserviceUtils.getUserName();
        int pos = value.indexOf("Assets");
        if (pos >= 0 && pos <= 2)
            value = "/" + value.substring(pos);
        else
            return;

        // Not checking database for folder to improve performance, check done
        // on way out.

        recentService.addRecent(user, null, RecentType.ASSET_FOLDER, value);

    }
    
    @Override
    public void addRecentAssetType(String value)
    {
        String user = PSWebserviceUtils.getUserName();
        recentService.addRecent(user, null, RecentType.ASSET_TYPE, value);    
    }
    
    @Override
    public List<PSWidgetContentType> findRecentAssetType() throws PSDataServiceException {
        List<PSWidgetContentType> resultList = new ArrayList<>();
        String user = PSWebserviceUtils.getUserName();
        List<String> recentEntries = recentService.findRecent(user, null, RecentType.ASSET_TYPE);
        List<String> toDelete = new ArrayList<>();
        Map<String, PSWidgetContentType> widgetTypeMap = new HashMap<>();
        List<PSWidgetContentType> widgetTypes = assetService.getAssetTypes("yes");
        for (PSWidgetContentType wt : widgetTypes)
        {
            widgetTypeMap.put(wt.getWidgetId(), wt);
        }
        
        for (String entry : recentEntries)
        {
            PSWidgetContentType wtype = widgetTypeMap.get(entry);
               
            // Cleanup old or invalid entries
            if (wtype == null)
            {
                log.debug("Removing recent template not a current site template : {}" , entry);
                toDelete.add(entry);
            }
            else 
            {
                resultList.add(wtype);
            }
        }

        if (!toDelete.isEmpty())
            recentService.deleteRecent(user, null, RecentType.ASSET_TYPE, toDelete);
        return resultList;
    }

    @Override
    public void deleteUserRecent(String user)
    {
        recentService.deleteRecent(user, null, null);
    }

    @Override
    public void deleteSiteRecent(String siteName)
    {
        recentService.deleteRecent(null, siteName, null);
    }

    @Override
    public void updateSiteNameRecent(String oldSiteName, String newSiteName) {
        try {
            recentService.renameSiteRecent(oldSiteName, newSiteName);
        } catch (Exception e) {
            log.debug("Error updating PSX_RECENT table to rename site from:{}, to {}, Error: {} ",oldSiteName, newSiteName,
                    PSExceptionUtils.getMessageForLog(e));
        }
    }

    @Override
	public void addRecentItemByUser(String userName, String value) {
		recentService.addRecent(userName, null, RecentType.ITEM, value); 
	}

	@Override
	public void addRecentTemplateByUser(String userName, String siteName, String value) {
		recentService.addRecent(userName, null, RecentType.TEMPLATE, value);
	}

	@Override
	public void addRecentSiteFolderByUser(String userName, String value) {
		recentService.addRecent(userName, null, RecentType.SITE_FOLDER, value);
	}

	@Override
	public void addRecentAssetFolderByUser(String userName, String value) {
		recentService.addRecent(userName, null, RecentType.ASSET_FOLDER, value); 
	}

	@Override
	public void addRecentAssetTypeByUser(String userName, String value) {
		recentService.addRecent(userName, null, RecentType.ASSET_TYPE, value);  
	}
}
