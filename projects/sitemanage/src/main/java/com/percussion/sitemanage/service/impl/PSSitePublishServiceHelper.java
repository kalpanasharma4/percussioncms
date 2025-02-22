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
package com.percussion.sitemanage.service.impl;

import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.cms.IPSConstants;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.pagemanagement.data.PSWidgetContentType;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.services.error.PSRuntimeException;
import com.percussion.sitemanage.service.IPSSitePublishServiceHelper;
import com.percussion.util.PSSiteManageBean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.percussion.services.utils.orm.PSDataCollectionHelper.MAX_IDS;
import static com.percussion.util.PSSqlHelper.qualifyTableName;
import static org.apache.commons.lang.StringUtils.join;

@Repository("sitePublishServiceHelper")
@PSSiteManageBean
@Lazy
@Transactional
public class PSSitePublishServiceHelper implements IPSSitePublishServiceHelper
{
    private static final long MAX_RELATED_ITEMS = 1000;
    private static final long MAX_LOOPS = 10;

	@PersistenceContext
	private EntityManager entityManager;

	private Session getSession(){
		return entityManager.unwrap(Session.class);
	}


	List<Integer> publishableContentTypeIds;
    List<Integer> nonBinaryContentTypeIds;
	String allSharedAssetContentTypeIds;
	String allValidRelationshipConfigs;

    private IPSAssetService assetService;
    private List<String> binaryAssetTypes = Arrays.asList("percFileAsset", "percImageAsset", "percFlashAsset");
	private String binaryAssetTypesStr = "'percPage','percFileAsset', 'percImageAsset', 'percFlashAsset'";
	private String invalidRelationshipConfigName = "'RecycledContent','LocalContent'";
    
    @Autowired
    public PSSitePublishServiceHelper(IPSAssetService assetService)
    {
    	this.assetService = assetService;
    }

	@Override
	public Collection<Integer> findRelatedItemIds(Set<Integer> contentIds) {
		Set<Integer> results = new HashSet<>();
		if (contentIds.isEmpty()) {
			return results;
		}
		Session sess = getSession();
		try {
			Set<Integer> cids = new HashSet<>();
			cids.addAll(contentIds);
			int l=0;
			for (; l<MAX_LOOPS; l++) {
				Set<Integer> relatedIds = getRelatedItemIds(sess, cids);
				if (relatedIds.isEmpty()) {
					break;
				}
				relatedIds.removeAll(contentIds);
				relatedIds.removeAll(results);
				results.addAll(relatedIds);
				cids.clear();
				cids.addAll(relatedIds);
			}
			if (l == MAX_LOOPS) {
				throw new RuntimeException("Could not find the related items within the number of iterations. Looks like the items are deeply related.");
			}
		} catch (SQLException e) {
			String errMsg = "SQL error occurred while getting related content ids for incremental publishing.";
			ms_logger.error(errMsg, e);
			throw new PSRuntimeException(errMsg, e);
		}
		return results;
	}


    private Set<Integer> getRelatedItemIds(Session sess, Set<Integer> cids) throws SQLException {
		Set<Integer> results = new HashSet<>();
		for (int i = 0; i < cids.size(); i += MAX_IDS) {
			int end = (i + MAX_IDS > cids.size()) ? cids.size() : i + MAX_IDS;
			// lets get the direct publishable related items
			results.addAll(getPublishableRelatedItemIds(sess, cids));
			// lets get the direct non-publishable related items
			Set<Integer> ncids = getNonPublishableRelatedItemIds(sess, cids);
			// now lets get the direct publishable related items
			if (!ncids.isEmpty()) {
				//Want to add shared Assets in relatd List.
				results.addAll(getSharedAssetsRelatedItemIds(sess,cids,ncids));
				results.addAll(getPublishableRelatedItemIds(sess, ncids));
			}
		}
		return results;
	}

	private Set<Integer> getSharedAssetsRelatedItemIds(Session sess, Set<Integer> cids, Set<Integer> ncids) throws SQLException{
		String sql = String.format(
    	"SELECT DISTINCT REL.DEPENDENT_ID FROM %s as CS1, %s as CS2, "
				+ "%s as ST, %s as REL  WHERE REL.OWNER_ID IN (%s) AND "
				+ "REL.OWNER_ID = CS1.CONTENTID AND REL.OWNER_REVISION = CS1.CURRENTREVISION AND "
				+ "REL.DEPENDENT_ID = CS2.CONTENTID AND REL.DEPENDENT_ID IN (%s) AND CS2.CONTENTTYPEID in (%s) AND ST.WORKFLOWAPPID = CS2.WORKFLOWAPPID AND "
				+ "ST.STATEID = CS2.CONTENTSTATEID AND REL.CONFIG_ID IN (%s) AND ST.CONTENTVALID IN('n','i')", qualifyTableName("CONTENTSTATUS"),
				qualifyTableName("CONTENTSTATUS"), qualifyTableName("STATES"), qualifyTableName("PSX_OBJECTRELATIONSHIP"),
				join(cids, ","),join(ncids, ","),allSharedAssetContentTypeIds,allValidRelationshipConfigs);
		SQLQuery query = sess.createSQLQuery(sql);
		return new HashSet(query.list());
	}
	
    private Set<Integer> getPublishableRelatedItemIds(Session sess, Set<Integer> cids) throws SQLException{
    	String sql = String.format(
        		"SELECT DISTINCT REL.DEPENDENT_ID FROM %s as CS1, %s as CS2, "
        	    		+ "%s as ST, %s as REL  WHERE REL.OWNER_ID IN (%s) AND "
        	    		+ "REL.OWNER_ID = CS1.CONTENTID AND REL.OWNER_REVISION = CS1.CURRENTREVISION AND "
        	    		+ "REL.DEPENDENT_ID = CS2.CONTENTID	AND CS2.CONTENTTYPEID in (%s) AND ST.WORKFLOWAPPID = CS2.WORKFLOWAPPID AND "
        	    		+ "ST.STATEID = CS2.CONTENTSTATEID AND ST.CONTENTVALID IN('n','i')", qualifyTableName("CONTENTSTATUS"),
        	    		qualifyTableName("CONTENTSTATUS"), qualifyTableName("STATES"), qualifyTableName("PSX_OBJECTRELATIONSHIP"),
        	    		join(cids, ","), join(getPublishableContentTypeIds(sess), ","));
        SQLQuery query = sess.createSQLQuery(sql);
        return new HashSet(query.list());
    }

    private Set<Integer> getNonPublishableRelatedItemIds(Session sess, Set<Integer> cids) throws SQLException{
        String sql = String.format(
        		"SELECT DISTINCT REL.DEPENDENT_ID FROM %s as CS1, %s as CS2, "
        	    		+ "%s as ST, %s as REL  WHERE REL.OWNER_ID IN (%s) AND "
        	    		+ "REL.OWNER_ID = CS1.CONTENTID AND REL.OWNER_REVISION = CS1.CURRENTREVISION AND "
        	    		+ "REL.DEPENDENT_ID = CS2.CONTENTID AND CS2.CONTENTTYPEID in (%s)", qualifyTableName("CONTENTSTATUS"),
        	    		qualifyTableName("CONTENTSTATUS"), qualifyTableName("STATES"), qualifyTableName("PSX_OBJECTRELATIONSHIP"),
        	    		join(cids, ","), join(getNonPublishableContentTypeIds(sess), ","));
        SQLQuery query = sess.createSQLQuery(sql);
        return new HashSet(query.list());
    }
        
	private void initTypeIds(Session sess) {
		try {
			PSItemDefManager defMgr = PSItemDefManager.getInstance();
			long pageContentTypeId = defMgr
					.contentTypeNameToId(IPSPageService.PAGE_CONTENT_TYPE);
			if(allSharedAssetContentTypeIds == null) {
				allSharedAssetContentTypeIds = join(loadSharedAssetContentType(sess), ",");
			}
			if(allValidRelationshipConfigs == null) {
				allValidRelationshipConfigs = join(loadValidRelationshipConfigNames(sess), ",");
			}
			List<PSWidgetContentType> assetTypes = assetService.getAssetTypes("no");
			publishableContentTypeIds = new ArrayList<>();
			nonBinaryContentTypeIds = new ArrayList<>();
			for (PSWidgetContentType type : assetTypes) {
				try {
					if (binaryAssetTypes.contains(type.getContentTypeName())) {
						publishableContentTypeIds.add(Integer.valueOf(type.getContentTypeId()));
					} else {
						nonBinaryContentTypeIds.add(Integer.valueOf(type.getContentTypeId()));
					}
				} catch (Exception e) {
					String errMsg = "The supplied content type id of the widget doesn't belong to a valid content type, ignoring the widget."
							+ "widget name: " + type.getWidgetLabel() + " content type:" + type.getContentTypeName();
					ms_logger.info(errMsg, e);
				}
			}
			publishableContentTypeIds.add(Integer.valueOf("" + pageContentTypeId));
		} catch (Exception e) {
			String errMsg = "Failed to initialize content type ids while getting related content ids for incremental publishing.";
			ms_logger.error(errMsg, e);
			throw new PSRuntimeException(errMsg, e);
		}		
	}

	private List loadSharedAssetContentType(Session sess)throws SQLException{
		String sql = String.format(
				"SELECT DISTINCT CONTENTTYPEID FROM %s WHERE CONTENTTYPENAME NOT IN (%s)" , qualifyTableName("CONTENTTYPES")
				,binaryAssetTypesStr);
		SQLQuery query = sess.createSQLQuery(sql);
		return query.list();
	}

	private List loadValidRelationshipConfigNames(Session sess)throws SQLException{
		String sql = String.format(
				"SELECT DISTINCT CONFIG_ID FROM %s WHERE CONFIG_NAME NOT IN (%s)" , qualifyTableName("PSX_RELATIONSHIPCONFIGNAME")
				,invalidRelationshipConfigName);
		SQLQuery query = sess.createSQLQuery(sql);
		return query.list();
	}

	private List<Integer> getPublishableContentTypeIds(Session sess) {
		if (publishableContentTypeIds == null) {
			initTypeIds(sess);
		}
		return publishableContentTypeIds;
	}

	private List<Integer> getNonPublishableContentTypeIds(Session sess) {
		if (nonBinaryContentTypeIds == null) {
			initTypeIds(sess);
		}
		return nonBinaryContentTypeIds;
	}





	private static final Logger ms_logger = LogManager.getLogger(IPSConstants.PUBLISHING_LOG);
}
