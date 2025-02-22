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
package com.percussion.assetmanagement.dao.impl;

import com.percussion.assetmanagement.dao.IPSAssetDao;
import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.assetmanagement.data.PSReportFailedToRunException;
import com.percussion.error.PSExceptionUtils;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.IPSNode;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.services.notification.PSNotificationHelper;
import com.percussion.share.dao.IPSContentItemDao;
import com.percussion.share.dao.PSJcrNodeFinder;
import com.percussion.share.dao.impl.PSContentItem;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.data.PSContentItemUtils;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.utils.request.PSRequestInfo;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.Validate.isTrue;

@Component("assetDao")
public class PSAssetDao implements IPSAssetDao
{

    /**
     * Logger for this service.
     */
    public static Logger log = LogManager.getLogger(PSAssetDao.class);
    
    private IPSContentItemDao contentItemDao;
    private IPSContentMgr contentMgr;
    private IPSIdMapper idMapper;
    
    @Autowired
    public PSAssetDao(IPSContentItemDao contentItemDao, IPSContentMgr contentMgr, IPSIdMapper idMapper)
    {
        super();
        this.contentItemDao = contentItemDao;
        this.contentMgr = contentMgr;
        this.idMapper = idMapper;
    }

    
    public IPSItemSummary addItemToPath(IPSItemSummary item, String folderPath) throws PSDataServiceException {
        return contentItemDao.addItemToPath(item, folderPath);
    }

    public void removeItemFromPath(IPSItemSummary item, String folderPath) throws PSDataServiceException {
        contentItemDao.removeItemFromPath(item, folderPath);
    }    

    public void delete(String id) throws PSDataServiceException {
        // Local content is not in a folder, also orphaned content.  Both of these types of asset should
        // be able to be deleted without validation.  Local content validation is based upon the page.
        boolean localOrOrphanedContent=false;
        String itemName = "";
        try {
            PSContentItem item = contentItemDao.find(id,true);
            itemName = item.getName();
            List<String> paths = item.getFolderPaths();
            if (paths==null || paths.isEmpty())
                localOrOrphanedContent=true;
            else
            {
                int index = paths.get(0).indexOf("/Assets");
                itemName = paths.get(0).substring(index) + "/" + itemName;
            }
                
        } catch (Exception e)
        {
          log.error("Error trying to find folder paths for item id {}",id);
        }

        contentItemDao.delete(id);
        if (StringUtils.isNotBlank(itemName))
        {
            String currentUser = (String)PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
            log.info( "{} has been deleted by: {} ",itemName,  currentUser);
        }
        PSNotificationHelper.notifyEvent(EventType.ASSET_DELETED, id);
    }

    public PSAsset find(String id) throws PSDataServiceException {
        return find(id, false);
    }

    public PSAsset find(String id, boolean isSummary) throws PSDataServiceException {
        PSContentItem contentItem = contentItemDao.find(id, isSummary);
        if (contentItem == null) return null;
        PSAsset asset = createAsset(contentItem);
        return asset;
    }

    public List<PSAsset> findAll() throws com.percussion.share.dao.IPSGenericDao.LoadException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("findAll is not yet supported");
    }

    public synchronized PSAsset save(PSAsset object) throws PSDataServiceException {
        PSContentItem contentItem = new PSContentItem();
        PSContentItemUtils.copyProperties(object, contentItem);
        contentItem = contentItemDao.save(contentItem);
        return createAsset(contentItem);
    }

    public Collection<PSAsset> findByTypeAndWf(String type, int workflowId, int stateId) throws LoadException {
        isTrue(isNotBlank(type), "type may not be blank");
        
        Map<String, String> whereFields = new HashMap<>();
        whereFields.put("sys_workflowid", String.valueOf(workflowId));
        if (stateId != -1)
        {
            whereFields.put("sys_contentstateid", String.valueOf(stateId));
        }
        
        return find(type, whereFields);
    }
    
    public Collection<PSAsset> findByTypeAndName(String type, String name) throws LoadException {
        isTrue(isNotBlank(type), "type may not be blank");
        isTrue(isNotBlank(name), "name may not be blank");
        
        Map<String, String> whereFields = new HashMap<>();
        whereFields.put("sys_title", name);
                
        return find(type, whereFields);
    }
    
    public Collection<PSAsset> findByType(String type) throws LoadException {
        isTrue(isNotBlank(type), "type may not be blank");
        
        Map<String, String> whereFields = new HashMap<>();
                
        return find(type, whereFields);
    }
    
    
    protected PSAsset createAsset(PSContentItem contentItem) {
        PSAsset asset = new PSAsset();
        PSContentItemUtils.copyProperties(contentItem, asset);
        return asset;
    }
    
    /**
     * Finds assets of the specified type which satisfy the specified where-clause fields in a jcr query.
     * 
     * @param type content type, assumed not <code>null</code>.
     * @param whereFields map of where field -> value, assumed not <code>null</code>.
     * @return collection of <code>PSAsset</code> objects, never <code>null</code>.
     */
    private Collection<PSAsset> find(String type, Map<String, String> whereFields) throws LoadException {
    	
    	List<PSAsset> assets = new ArrayList<>();
        
        PSJcrNodeFinder jcrNodeFinder = new PSJcrNodeFinder(contentMgr, type, "sys_title");
        List<IPSNode> nodes = jcrNodeFinder.find(whereFields);
        for (IPSNode node : nodes)
        {
            try {
                assets.add(find(idMapper.getString(node.getGuid())));
            } catch (PSDataServiceException e) {
                log.error(PSExceptionUtils.getMessageForLog(e));
                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                //continue processing
            }
        }        
        
        return assets;
    }


    @Override
    public void revisionControlOn(String id) throws LoadException {
        contentItemDao.revisionControlOn(id);
    }

        private static final String NONCOMPLIANT_IMAGE_ASSET_QUERY="select rx:sys_contentid from rx:percImageAsset WHERE rx:alttext is null or rx:displaytitle is null or rx:alttext like '%.%' or rx:displaytitle like '%.%' or rx:resource_link_title is null or rx:resource_link_title like '%.%' order by jcr:path asc";
	private static final String NONCOMPLIANT_IMAGE_ASSET_REPORT="Non Compliant Image Assets";
	@Override
	public List<PSAsset> findAllNonADACompliantImageAssets() throws PSReportFailedToRunException {
		return runReport(NONCOMPLIANT_IMAGE_ASSET_QUERY,NONCOMPLIANT_IMAGE_ASSET_REPORT);		
	}
    // Added check for AltText is blank or filename | CMS-3216
	private static final String NONCOMPLIANT_FILE_ASSET_QUERY="select rx:sys_contentid from rx:percFileAsset WHERE rx:displaytitle is null or rx:displaytitle like '%.%' or rx:alttext is null or rx:alttext like '%.%' order by jcr:path asc";
	private static final String NONCOMPLIANT_FILE_ASSET_REPORT="Non Compliant File Assets";

	@Override
	public List<PSAsset> findAllNonADACompliantFileAssets() throws PSReportFailedToRunException {
		return runReport(NONCOMPLIANT_FILE_ASSET_QUERY,NONCOMPLIANT_FILE_ASSET_REPORT);		
	}
	
	private static final String ALL_FILE_ASSET_QUERY="select rx:sys_contentid from rx:percFileAsset";
	private static final String ALL_FILE_ASSET_REPORT="All File Assets";

	@Override
	public List<PSAsset> findAllFileAssets() throws PSReportFailedToRunException {
		return runReport(ALL_FILE_ASSET_QUERY,ALL_FILE_ASSET_REPORT);		
	}
	
	private static final String ALL_IMAGE_ASSET_QUERY="select rx:sys_contentid from rx:percImageAsset order by jcr:path asc";
	private static final String ALL_IMAGE_ASSET_REPORT="All Image Assets";

    private static final String ALL_ENCRYPTED_CONTENT_ASSET_QUERY="select rx:sys_contentid from rx:percRssAsset,rx:percFormAsset ";
    private static final String ALL_ENCRYPTED_CONTENT_ASSET_REPORT="All Assets with Encrypted Content";

	@Override
	public List<PSAsset> findAllImageAssets() throws PSReportFailedToRunException {
		return runReport(ALL_IMAGE_ASSET_QUERY,ALL_IMAGE_ASSET_REPORT);		
	}

	public List<PSAsset> findAllAssetsUsingEncryption() throws PSReportFailedToRunException {
        return runReport(ALL_ENCRYPTED_CONTENT_ASSET_QUERY,ALL_ENCRYPTED_CONTENT_ASSET_REPORT);
    }
	
    private List<PSAsset> runReport(String query, String reportName) throws PSReportFailedToRunException{
	ArrayList<PSAsset> ret = new ArrayList<>();
		
		Query q = null;
		try {
			q = contentMgr.createQuery(query, Query.SQL);
		} catch (RepositoryException e) {
			log.error("An error occurred executing the query for the " + reportName + " report from the Content Repository.", e);
			throw new PSReportFailedToRunException(e);
		}
		
		QueryResult results = null;
		try {
			results = contentMgr.executeQuery(q, -1, null, null);
		} catch (RepositoryException e) {
			log.error("An error occurred executing the query for the " + reportName + " report from the Content Repository.", e);
			throw new PSReportFailedToRunException(e);
		}
	
		NodeIterator it;
		try {
			it = results.getNodes();
		} catch (RepositoryException e) {
			log.error("An error occurred retrieving the {} report from the Content Repository. Error: {}",  reportName,
                    PSExceptionUtils.getMessageForLog(e));
			log.debug(PSExceptionUtils.getDebugMessageForLog(e));
			return ret;
		}
		
         while (it.hasNext())
         {
             IPSNode node = (IPSNode) it.nextNode();
             PSContentItem contentItem=null;
             
			try {
				contentItem = contentItemDao.find(idMapper.getString(node.getGuid()), true);
			} catch (PSDataServiceException e) {
				log.error("An error occurred retrieving an Image Asset for the  {} report from the Content Repository. Error: {}",reportName,
                        PSExceptionUtils.getMessageForLog(e));
				log.debug(PSExceptionUtils.getDebugMessageForLog(e));
			}
            
			if(null != contentItem ){
            	ret.add(createAsset(contentItem));
            }
         }
        
         return ret;
		
    }
}
