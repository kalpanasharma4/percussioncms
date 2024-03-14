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
package com.percussion.itemmanagement.service.impl;

import com.percussion.assetmanagement.dao.IPSAssetDao;
import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.assetmanagement.data.PSReportFailedToRunException;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.assetmanagement.service.impl.PSAssetRestService;
import com.percussion.auditlog.PSActionOutcome;
import com.percussion.auditlog.PSAuditLogService;
import com.percussion.auditlog.PSContentEvent;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.cms.objectstore.PSCoreItem;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.error.PSExceptionUtils;
import com.percussion.itemmanagement.data.PSAssetSiteImpact;
import com.percussion.itemmanagement.data.PSComment;
import com.percussion.itemmanagement.data.PSItemDates;
import com.percussion.itemmanagement.data.PSPageLinkedToItem;
import com.percussion.itemmanagement.data.PSRevision;
import com.percussion.itemmanagement.data.PSRevisionsSummary;
import com.percussion.itemmanagement.data.PSSoProMetadata;
import com.percussion.itemmanagement.service.IPSItemService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pathmanagement.data.PSFolderPermission;
import com.percussion.security.PSEncryptor;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.content.data.PSItemStatus;
import com.percussion.services.content.data.PSItemSummary;
import com.percussion.services.contentchange.IPSContentChangeService;
import com.percussion.services.contentchange.data.PSContentChangeEvent;
import com.percussion.services.contentchange.data.PSContentChangeType;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.linkmanagement.IPSManagedLinkDao;
import com.percussion.services.linkmanagement.data.PSManagedLink;
import com.percussion.services.notification.IPSNotificationListener;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.publisher.data.PSItemPublishingHistory;
import com.percussion.services.publisher.data.PSItemPublishingHistoryList;
import com.percussion.services.purge.IPSSqlPurgeHelper;
import com.percussion.services.purge.PSSqlPurgeHelperLocator;
import com.percussion.services.relationship.IPSRelationshipService;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.system.IPSSystemService;
import com.percussion.services.system.data.PSContentStatusHistory;
import com.percussion.services.useritems.IPSUserItemsDao;
import com.percussion.services.useritems.data.PSUserItem;
import com.percussion.services.workflow.data.PSAssignmentTypeEnum;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.share.dao.IPSContentItemDao;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.dao.PSDateUtils;
import com.percussion.share.dao.impl.PSContentItem;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.data.PSItemProperties;
import com.percussion.share.data.PSItemPropertiesList;
import com.percussion.share.data.PSNoContent;
import com.percussion.share.rx.PSLegacyExtensionUtils;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.validation.PSValidationErrorsBuilder;
import com.percussion.util.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.io.PathUtils;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSWebserviceUtils;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.publishing.IPSPublishingWs;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.percussion.share.service.exception.PSParameterValidationUtils.rejectIfBlank;
import static com.percussion.share.service.exception.PSParameterValidationUtils.validateParameters;
import static com.percussion.webservices.PSWebserviceUtils.getStateById;
import static com.percussion.webservices.PSWebserviceUtils.getWorkflow;
import static java.util.Arrays.asList;

/**
 * @author Jose_Annunziato
 *
 */
@Path("/item")
@PSSiteManageBean("itemRestService")
public class PSItemService implements IPSItemService
{
	private static int MAX_PAGES_SITEIMPACT=25; //Stop looking for relationships after we hit this # of Pages.

    static final Map<String , String> UNIQUE_FIELDS = new HashMap<String , String>() {{
        put("percCalendarAsset",    "calendar_unique_name");
        put("percPollAsset",    "pollUniqueName");
    }};


    IPSIdMapper idMapper;
    IPSSystemService systemService;
    IPSWorkflowHelper workflowHelper;
    IPSContentWs contentWs;
    IPSWidgetAssetRelationshipService waRelService;
    IPSItemWorkflowService itemWfService;
    IPSFolderHelper folderHelper;
    IPSContentItemDao contentItemDao;
    IPSAssetDao assetDao;
    IPSTemplateService templateService;
    IPSUserItemsDao userItemDao;
    IPSNotificationService notificationService;
    IPSPublisherService pubService;
    IPSManagedLinkDao linkService;
    @Autowired
    IPSPublishingWs publishingWs;
    @Autowired
    IPSContentChangeService changeService;
    @Autowired
    IPSRelationshipService relationshipService;
    private PSAuditLogService psAuditLogService=PSAuditLogService.getInstance();
    private PSContentEvent psContentEvent;
    private SecureKeyRotationListener secureKeyRotationListener;



    @Autowired
    public PSItemService(IPSIdMapper idMapper, IPSSystemService systemService, IPSWorkflowHelper workflowHelper,
                         IPSContentWs contentWs, IPSWidgetAssetRelationshipService waRelService, IPSItemWorkflowService itemWfService,
                         IPSFolderHelper folderHelper, @Qualifier("contentItemDao") IPSContentItemDao contentItemDao, IPSAssetDao assetDao, IPSTemplateService templateService,
                         IPSUserItemsDao userItemDao, IPSNotificationService notificationService, IPSPublisherService pubService, IPSManagedLinkDao linkService)
    {
        super();
        this.idMapper = idMapper;
        this.systemService = systemService;
        this.workflowHelper = workflowHelper;
        this.contentWs = contentWs;
        this.waRelService = waRelService;
        this.itemWfService = itemWfService;
        this.folderHelper = folderHelper;
        this.contentItemDao = contentItemDao;
        this.assetDao = assetDao;
        this.templateService = templateService;
        this.userItemDao = userItemDao;
        this.notificationService = notificationService;
        this.pubService = pubService;
        this.linkService = linkService;

        notificationService.addListener(EventType.PAGE_DELETE, new IPSNotificationListener()
        {
            @Override
            public void notifyEvent(PSNotificationEvent event)
            {
                deleteUserItems((String) event.getTarget(), false);
            }
        });
        notificationService.addListener(EventType.USER_DELETE, new IPSNotificationListener()
        {
            @Override
            public void notifyEvent(PSNotificationEvent event)
            {
                deleteUserItems((String) event.getTarget(), true);
            }
        });

        //Adding Secure Key Rotation Listener, such that Assets with Encryption can republish
        //pages after reencrypting using new key on rotation of secure key.
        secureKeyRotationListener = new SecureKeyRotationListener();
        PSEncryptor encryptor = PSEncryptor.getInstance("AES", PathUtils.getRxDir(null).getAbsolutePath().concat(PSEncryptor.SECURE_DIR));
        encryptor.addPropertyChangeListener(secureKeyRotationListener);
    }

    @GET
    @Path("revisions/{id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSRevisionsSummary getRevisions(@PathParam("id") String id) throws PSItemServiceException {

        try {
            rejectIfBlank("getRevisions", "id", id);
            String guid = PSLegacyExtensionUtils.getGUID(id);
            PSRevisionsSummary revSummary = new PSRevisionsSummary();
            try {
                validateItemRestorable(guid);
                revSummary.setRestorable(true);
            } catch (PSValidationException | PSNotFoundException e) {
                revSummary.setRestorable(false);
            }
            // revisions for the client side
            List<PSRevision> revisions = new ArrayList<>();
            // get the history details (revisions) for the page or asset from Rhythmyx
            List<PSContentStatusHistory> _revisions = systemService.findContentStatusHistory(idMapper.getGuid(guid));
            List<PSComment> comments = createCommentsFromHistory(_revisions);
            PSComponentSummary sum = null;
            try {
                sum = workflowHelper.getComponentSummary(guid);
            } catch (Exception e) {
                throw new PSItemServiceException("The page you are trying to act on no longer exists in the system.", e);
            }

            if (_revisions.isEmpty()) {
                if (sum.getCurrentLocator().getRevision() == 1) {
                    // item has been created, but not checked in, so create a revision from the summary
                    revisions.add(getRevision(sum));
                }
            } else {
                List<PSRevision> revs = getRevisions(_revisions);
                revisions.addAll(revs);
                // When user edits an item there is no entry for the edit revision in history table.
                //until he checks in the item.
                //The following code adds an entry from the item summary if the item is checked out to the current user
                //and the head revision is not in the revisions.
                int headRev = sum.getHeadLocator().getRevision();
                if (workflowHelper.isCheckedOutToCurrentUser(guid)) {
                    boolean found = false;
                    for (PSRevision rev : revs) {
                        if (rev.getRevId() == headRev) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
						revisions.add(getRevision(sum));
					}
                }
            }
            revSummary.setRevisions(revisions);
            revSummary.setComments(comments);
            // return the revisions to the client
            return revSummary;
        } catch (PSValidationException e) {
           throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("pubhistory/{id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<PSItemPublishingHistory> getPublishingHistory(@PathParam("id") String id) throws PSItemServiceException {
        try{
            rejectIfBlank("getPublishingHistory", "id", id);
            List<PSItemPublishingHistory> pubHistoryList = new ArrayList<>();

        	pubHistoryList = pubService.findItemPublishingHistory(idMapper.getGuid(id));
            return new PSItemPublishingHistoryList(pubHistoryList);
        }
        catch(Exception e){
        	log.error("Error fetching the publishing history for the supplied id: {} Error: {}", id, PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        	throw new WebApplicationException("An unexpected error occurred while fetching the publishing history, " +
        			"see log for details.");
        }
    }

    @GET
    @Path("lastComment/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getLastComment(@PathParam("id") String id) throws PSItemServiceException {
    	String lastComment = "";
        List<PSContentStatusHistory> historyEntries = systemService.findContentStatusHistory(idMapper.getGuid(id));
        Collections.reverse(historyEntries);
        for(int i=0; i < historyEntries.size(); i++){
        	PSContentStatusHistory entry = historyEntries.get(i);
        	if(ArrayUtils.contains(WORKFLOW_TRANSITION_IGNORE_LIST, entry.getTransitionLabel())) {
				continue;
			}
        	lastComment = entry.getTransitionComment();
        	break;
        }
    	return lastComment;
    }

    /**
     * Creates comments list from history entries.
     * @param historyEntries assumed not <code>null</code>.
     * @return List of PSComment objects never <code>null</code> may be empty.
     */
    private List<PSComment> createCommentsFromHistory(List<PSContentStatusHistory> historyEntries) {
    	List<PSComment> comments = new ArrayList<>();
    	for (PSContentStatusHistory entry : historyEntries) {
			if(StringUtils.isNotBlank(entry.getTransitionComment())){
				comments.add(new PSComment(entry.getTransitionComment(), entry.getActor(), entry.getTransitionLabel(), entry.getEventTime()));
			}
		}
    	class CommentComparator implements Comparator<PSComment> {
    	    @Override
    	    public int compare(PSComment o1, PSComment o2) {
    	        return o2.getCommentDate().compareTo(o1.getCommentDate());
    	    }
    	}
    	Collections.sort(comments,new CommentComparator());
    	return comments;
	}

	@GET
    @Path("restoreRevision/{id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSNoContent restoreRevision(@PathParam("id") String id) {
        try {
            rejectIfBlank("restoreRevision", "id", id);
            //If item can't be restored this method throws exception with a business message.
            validateItemRestorable(id);
            PSComponentSummary sum = workflowHelper.getComponentSummary(id);

            //Create a list of ids and add the supplied item id first.
            List<String> ids = new ArrayList<>();
            ids.add(id);
            Set<String> lids = waRelService.getLocalAssets(id);
            ids.addAll(lids);
            try {
                //prepare the item for promotion
                prepareForRestore(id);
                contentWs.promoteRevisions(idMapper.getGuids(ids));
                //Adjust the relationships
                sum = workflowHelper.getComponentSummary(id);
                String cid = idMapper.getGuid(sum.getHeadLocator()).toString();
                if(!lids.isEmpty())
                {
                    waRelService.adjustLocalContentRelationships(cid);
                }

            } catch (Exception e) {
                throw new WebApplicationException("An unexpected error occurred while restoring the prior revision, " +
                        "see log for details.", e);
            }

            return new PSNoContent("Item with id " + id + " has been restored.");
        } catch (PSValidationException | PSItemServiceException | PSNotFoundException | IPSWidgetAssetRelationshipService.PSWidgetAssetRelationshipServiceException e) {
            log.error("Restore Revision Failed. Error: {}",PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(PSExceptionUtils.getMessageForLog(e));
        }
    }

	@Override
    public List<PSAssetSiteImpact> getAssetSiteImpact(List<String> assetIds){

		ArrayList<PSAssetSiteImpact> ret = new ArrayList<>();


	    for(String id : assetIds){

	    	Set<String> ownerPages = new HashSet<>();
		    Set<String> ownerTemplates = new HashSet<>();
		    Set<IPSSite> ownerSites = new HashSet<>();

			PSAssetSiteImpact impact = new PSAssetSiteImpact();

			try{
				fillOwners(id, ownerPages, ownerTemplates);
				getManagedLinkOwners(id, ownerPages, ownerTemplates,MAX_PAGES_SITEIMPACT);
			}catch(Exception e){
				log.error("Error processing Site Impact for owner Pages and Templates for Asset id: {} Error: {}" ,id, PSExceptionUtils.getMessageForLog(e));
				log.debug(PSExceptionUtils.getDebugMessageForLog(e));
				//continue
			}

			for (String page : ownerPages)
            {
                try{
                	impact.getOwnerPages().add(folderHelper.findItemPropertiesById(page));
                }catch(Exception e){
                	log.error("Error processing Site Impact with owner Page {} for Asset id: {} Error: {}",page, id, PSExceptionUtils.getMessageForLog(e));
                	log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                }

                List<IPSSite> sites = new ArrayList<>();
                try{
                	sites = folderHelper.getItemSites(page);
                }catch(Exception e){
                	log.error("Error processing Site Impact detecting owner Site for Page {} for Asset id: {} Error: {}",
                             page,
                             id,
                             PSExceptionUtils.getMessageForLog(e));
                	log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                }

                ownerSites.addAll(sites);
            }

			for (String templateId : ownerTemplates){

				try{
					impact.getOwnerTemplates().add(templateService.find(templateId));
				}catch(Exception e){
					log.error("Error processing Site Impact for Template " + templateId + " for Asset id:" + id, e);
				}

				List<IPSSite> sites = new ArrayList<>();
				try{
					sites = folderHelper.getItemSites(templateId);
				}catch(Exception e){
					log.error("Error processing Site Impact detecting owner Site for Template " + templateId + " for Asset id:" + id, e);
				}

				for(IPSSite site : sites){
                	ownerSites.add(site);
                }
			}

			impact.setOwnerSites(ownerSites);

			ret.add(impact);
		}

		return ret;
    }

	//FIXME: This really needs to be re-factored so that it is working with Jackson or Jaxb or something.
    @GET
    @Path("siteimpact/asset/{assetId}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getAssetSiteImpact(@PathParam("assetId") String assetId)
    {
        JSONObject result = new JSONObject();
        Set<String> ownerPages = new HashSet<>();
        Set<String> ownerTemplates = new HashSet<>();
        try
        {
            fillOwners(assetId, ownerPages, ownerTemplates);

            getManagedLinkOwners(assetId, ownerPages, ownerTemplates, MAX_PAGES_SITEIMPACT);
            JSONArray pageArray = new JSONArray();
            for (String page : ownerPages)
            {
            	PSItemProperties itemProps=null;
            	try{
                 itemProps = folderHelper.findItemPropertiesById(page);
            	}catch(Exception e){
            		log.error("An error occurred while processing Asset Impact checking item properties for Page: {} Error: {}", page,PSExceptionUtils.getMessageForLog(e));
            		log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            	}
            	if(itemProps != null) {
					pageArray.add(itemProps);
				}
            }
            JSONArray templateArray = new JSONArray();
            for (String templateId : ownerTemplates)
            {
            	PSTemplateSummary template = null;
            	try{
            		template = templateService.find(templateId);
            	}catch(Exception e){
            		log.error("An error occurred while processing Asset Impact with Template {} Error: {}",templateId,PSExceptionUtils.getMessageForLog(e));
            		log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            	}


            	List<IPSSite> sites = folderHelper.getItemSites(templateId);
                if (sites != null && !sites.isEmpty())
                {
                    JSONObject templateItem = new JSONObject();

                    if(template != null) {
						templateItem.put("template", template);
					}

                    if(sites!=null) {
                        if (sites.get(0) != null)
                            templateItem.put("site", sites.get(0).getName());
                    }
                    templateArray.add(templateItem);
                } else {
                	 JSONObject templateItem = new JSONObject();

                     if(null != template){
                    	 templateItem.put("template", template);
                     }

                     templateArray.add(templateItem);

                     if(template != null)
                    	 log.warn("template {} with id {} is not associated to any sites",template.getName(),template.getId());
                }

            }
            result.put("pages", pageArray);
            result.put("templates", templateArray);
        }
        catch(Exception e)
        {
        	log.error("An unexpected error occurred while fetching the site impact details for the asset.",e);
            throw new WebApplicationException("An unexpected error occurred while fetching the site impact details for the asset.",e);
        }
        return result.toString();
    }

    /**
     * Helper function that fills the supplied set of owner pages and templates with the owners of the of the given asset,
     * calls it self if the owner of the given asset is an asset.
     * @param assetId assumed not blank.
     * @param ownerPages assumed not <code>null</code>, may be empty.
     * @param ownerTemplates assumed not <code>null</code>, may be empty.
     */
    private void fillOwners(String assetId, Set<String> ownerPages, Set<String> ownerTemplates) throws PSValidationException {
    	Set<String> owners=null;

    	owners = waRelService.getRelationshipOwners(assetId,true);

    	if(owners != null){
        for (String owner : owners)
        {
            IPSWorkflowHelper.PSItemTypeEnum typeEnum = workflowHelper.getItemType(owner);

            if(typeEnum.equals(IPSWorkflowHelper.PSItemTypeEnum.PAGE))
                ownerPages.add(owner);
            else if(typeEnum.equals(IPSWorkflowHelper.PSItemTypeEnum.TEMPLATE))
                ownerTemplates.add(owner);
            else if(typeEnum.equals(IPSWorkflowHelper.PSItemTypeEnum.ASSET))
                fillOwners(owner, ownerPages, ownerTemplates);
        }
    	}
    }

    private void getManagedLinkOwners(String assetId,Set<String> ownerPages, Set<String> ownerTemplates, int limit ){


    	IPSGuid assetGuid = idMapper.getGuid(assetId);
    	List<PSManagedLink> links = linkService.findLinksByChildId(idMapper.getContentId(assetGuid));

    	for(PSManagedLink link:links ){

    		if(limit <=0 || ownerPages.size()<= limit){
	    		if(link.getParentId() > 0){
	    		    try {
                        PSContentItem parent = contentItemDao.find(idMapper.getGuidFromContentId(link.getParentId()).toString());

                        if (parent.isPage()) {
                            ownerPages.add(parent.getId());
                        } else {
                            fillOwners(parent.getId(), ownerPages, ownerTemplates);
                        }
                    } catch (PSDataServiceException e) {
                        log.warn(PSExceptionUtils.getMessageForLog(e));
                        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                        //continue processing
                    }
                }else{
	    			log.warn("Managed Link with a parentId of -1 detected for Link Id: {} . Skipping link in Site Impact.",link.getLinkId() );
	    			//TODO: Add autofix here once we know what the correct fix is.
	    		}
    		}else{
    			//we've hit the limit on Pages so return to the caller.
    			return;
    		}
    	}
    }

	/**
     * Used to generate a revision map from the content status history of an item.
     *
     * @param history content status history list, assumed not <code>null</code>.
     *
     * @return a map where the key is a revision number and the value is a map (sorted descending) of content status
     * history id's to content status history objects.
     */
    private Map<Integer, Map<Long, PSContentStatusHistory>> buildRevisionMap(List<PSContentStatusHistory> history)
    {
        Map<Integer, Map<Long, PSContentStatusHistory>> revMap =
            new HashMap<>();

        HistoryIdComparator hComp = new HistoryIdComparator();
        for (PSContentStatusHistory h : history)
        {
            Map<Long, PSContentStatusHistory> rMap;
            int hRev = h.getRevision();
            if (!revMap.containsKey(hRev))
            {
                rMap = new TreeMap<>(hComp);
                revMap.put(hRev, rMap);
            }
            else
            {
                rMap = revMap.get(hRev);
            }

            rMap.put(h.getId(), h);
        }

        return revMap;
    }

    /**
     * Used to get the latest meaningful revisions from the content status history of an item.  We are not concerned
     * with Quick Edit revisions unless this is all that is available.
     *
     * @param history content status history list, assumed not <code>null</code>.
     *
     * @return list of revisions for the item, never <code>null</code>, may be empty.
     */
    private List<PSRevision> getRevisions(List<PSContentStatusHistory> history)
    {
        List<PSRevision> revisions = new ArrayList<>();

        Map<Integer, Map<Long, PSContentStatusHistory>> revMap = buildRevisionMap(history);
        for (Integer rev : revMap.keySet())
        {
            PSRevision psRevision = null;
            String prevStateName = "";

            Map<Long, PSContentStatusHistory> rMap = revMap.get(rev);
            for (Long historyId : rMap.keySet())
            {
                PSContentStatusHistory hist = rMap.get(historyId);
                String currentStateName = hist.getStateName();
                if (prevStateName.equalsIgnoreCase("Quick Edit") &&  currentStateName.equalsIgnoreCase("Quick Edit"))
                {
                    // keep the latest quick edit revision
                    continue;
                }

                prevStateName = currentStateName;
                String lastModifierName = currentStateName.equalsIgnoreCase("Live") ?
                        getLastModifierFromCheckOut(rMap, historyId) : hist.getLastModifierName();
                psRevision = new PSRevision(hist.getRevision(),
                            PSDateUtils.getDateToString(hist.getLastModifiedDate()),
                            lastModifierName,
                            prevStateName);
                String transitionLabel = hist.getTransitionLabel();
                if (!transitionLabel.equalsIgnoreCase("CheckOut") && !transitionLabel.equalsIgnoreCase("Edit"))
                {
                    // found the revision with the correct state
                    break;
                }
            }

            if (psRevision != null)
            {
                // add it to the list of revisions for the item
                revisions.add(psRevision);
            }
        }

        return revisions;
    }

    /**
     * Finds the last modifier name of the first occurrence of a checkout transition, starting from the specified id.
     *
     * @param rMap map of history id to content status history, ordered descending.  Assumed not <code>null</code>.
     * @param startId the history id from which to start the search.  Assumed not <code>null</code>.
     *
     * @return the first checkout user name found.  May be empty if a name could not be found.
     */
    private String getLastModifierFromCheckOut(Map<Long, PSContentStatusHistory> rMap, Long startId)
    {
        String lastCheckOutUser = "";

        for (Long historyId : rMap.keySet())
        {
            if (historyId >= startId)
            {
                continue;
            }

            PSContentStatusHistory hist = rMap.get(historyId);
            if (hist.getTransitionLabel().equalsIgnoreCase("CheckOut"))
            {
                lastCheckOutUser = hist.getLastModifierName();
                break;
            }
        }

        return lastCheckOutUser;
    }

    /**
     * Creates a new revision from a specified component summary.
     *
     * @param sum the component summary, assumed not <code>null</code>.
     *
     * @return the newly created {@link PSRevision} object, never <code>null</code>.
     */
    private PSRevision getRevision(PSComponentSummary sum)
    {
        PSWorkflow wf = getWorkflow(sum.getWorkflowAppId());
        String stateName = getStateById(wf, sum.getContentStateId()).getName();

        return new PSRevision(sum.getHeadLocator().getRevision(), PSDateUtils.getDateToString(sum.getContentLastModifiedDate()),
                sum.getContentLastModifier(), stateName);
    }

    /**
     * Comparator used for sorting the content status history map for each revision.  Entries are sorted in
     * descending order.
     *
     * @author peterfrontiero
     */
    private class HistoryIdComparator implements Comparator<Object>
    {
        public int compare(Object id1, Object id2)
        {
            if ((Long) id1 < (Long) id2)
            {
                return 1;
            }
            else if ((Long) id1 > (Long) id2)
            {
                return -1;
            }
            else
            {
                return 0;
            }
        }
    }

    /**
     * Converts the given from save to the display format and from display format to save format depending on the supplied flag.
     *
     * @param inputDate date String expected to be in MM/dd/yyyy HH:mm format if the isForSave is true otherwise
     * yyyy-MM-dd HH:mm:ss.S, may be <code>null</code> or empty.
     * @return date String in the following format yyyy-MM-dd HH:mm:ss.S if isForSave is true otherwise MM/dd/yyyy HH:mm,
     * returns empty string if the inputDate is blank.
     * @throws ParseException
     */
    private String convertDate(String inputDate, boolean isForSave) throws ParseException
    {
    	if(StringUtils.isBlank(inputDate)) {
			return "";
		}
    	FastDateFormat format1 =  FastDateFormat.getInstance(isForSave?"MM/dd/yyyy hh:mm a":"yyyy-MM-dd HH:mm:ss.S");
        FastDateFormat format2 = FastDateFormat.getInstance(isForSave?"yyyy-MM-dd HH:mm:ss.S":"MM/dd/yyyy hh:mm a");
        Date dbDate = format1.parse(inputDate);
        String date = format2.format(dbDate);
        return date;
    }

    /**
     * Prepares the supplied item for restoring a prior revision. Assumes that the item is valid for restoring.
     * Make sure to call {@link #validateItemRestorable(String)} before calling this method.
     * Checks in the item if it is checked out to the current user.
     * Transitions to Quick edit state if the item is in Live or Pending state.
     * Uses the head locator to checkin and transition.
     * @param id the ID of the item, must not be blank. Expects the string representation of guid.
     */
    private void prepareForRestore(String id) throws PSValidationException, IPSItemWorkflowService.PSItemWorkflowServiceException {
        rejectIfBlank("isCheckedOutToCurrentUser", "id", id);
        PSComponentSummary sum = workflowHelper.getComponentSummary(id);

        String cid = idMapper.getGuid(sum.getHeadLocator()).toString();

        //Checkin the item if it is checked out to the current user.
        if(workflowHelper.isCheckedOutToCurrentUser(cid))
        {
        	itemWfService.checkIn(cid);
        }
        //If the item is in pending or live state then move to quick edit state.
        if(workflowHelper.isPending(cid) || workflowHelper.isLive(cid))
        {
            itemWfService.transition(cid, PSWorkflowHelper.WF_STATE_QUICKEDIT);
        }
    }

    /**
     * Validates whether the item can be restored or not, if not throws appropriate exception.
     * @see IPSItemService#restoreRevision(String) for the cases in which the item is not restorable.
     *
     * @param id the ID of the item, must not be blank. Expects the string representation of guid.
     * @throws PSItemServiceException when the item is not valid for revision promotion.
     */
    private void validateItemRestorable(String id) throws PSItemServiceException, PSValidationException, PSNotFoundException {
        rejectIfBlank("validateItemPromotable", "id", id);
        PSComponentSummary sum = workflowHelper.getComponentSummary(id);
        String type = workflowHelper.isPage(id)?PAGE:ASSET;
		String opName = "Restore Revision";
        PSNoContent validationResponse = new PSNoContent("Success");

        //throw exception if the user is not an assignee
        PSAssignmentTypeEnum asmtType = null;
        try
        {
	        List<PSAssignmentTypeEnum> atypes = systemService.getContentAssignmentTypes(asList(idMapper.getGuid(id)));
	        asmtType = atypes.get(0);
        }
        catch(Exception e)
        {
        	Object[] args = {type};
			
			String invalidMessage = new String(MessageFormat.format("An unexpected error occurred while promoting this {0}" +
        			", see log for details.",args).getBytes(StandardCharsets.UTF_8));
                	validationResponse.setOperation(invalidMessage);
                    validateParameters(opName).reject(opName, invalidMessage).throwIfInvalid();

        }
        if(asmtType == PSAssignmentTypeEnum.READER || asmtType == PSAssignmentTypeEnum.NONE)
        {
        	Object[] args = {type};
			
			String invalidMessage = new String(MessageFormat.format("You are not authorized to restore this {0}.",args).getBytes(StandardCharsets.UTF_8));
                	validationResponse.setOperation(invalidMessage);
                    validateParameters(opName).reject(opName, invalidMessage).throwIfInvalid();
        }

    	//throw exception if the user does not have folder permission
        IPSGuid pfGuid = folderHelper.getParentFolderId(idMapper.getGuid(id));
        PSFolderPermission.Access access = folderHelper.getFolderAccessLevel(pfGuid.toString());
        if(access == PSFolderPermission.Access.READ)
        {
			
        	Object[] args = {type};
			String invalidMessage = new String(MessageFormat.format("You do not have permission to restore this {0}.",args).getBytes(StandardCharsets.UTF_8));
                	validationResponse.setOperation(invalidMessage);
                    validateParameters(opName).reject(opName, invalidMessage).throwIfInvalid();
        }

        //if item is checked out to some one else throws exception.
        if(StringUtils.isNotBlank(sum.getCheckoutUserName()) && !workflowHelper.isCheckedOutToCurrentUser(id))
        {
						
        	Object[] args = {type,sum.getCheckoutUserName()};
			String invalidMessage = new String(MessageFormat.format("User {1} is editing this {0}. You cannot restore " +
        			"earlier revisions.", args).getBytes(StandardCharsets.UTF_8));
                	validationResponse.setOperation(invalidMessage);
                    validateParameters(opName).reject(opName, invalidMessage).throwIfInvalid();
        	
        }

    }

    /**
     * Check the validity of the date againt startdate with current date and end date with current date
     * if req.startdate >= currentdate or req.enddate >=currentdate then that is valid. other wise throws the error.
     * @param req
     * @throws
     */
    private PSNoContent dateValidation(PSItemDates req)
    {
    	PSNoContent validationResponse = new PSNoContent("Success");
        try
        {   String opName = "dateValidation";
            FastDateFormat formatter =
                    FastDateFormat.getInstance("MM/dd/yyyy hh:mm a");
            Date currentDate = new Date();
            String currentDateString = formatter.format(currentDate);
            Date currentDateFormat = formatter.parse(currentDateString);
            //Generate the message to pass in
        	String invalidMessage = new String("Invalid date range:\n".getBytes(StandardCharsets.UTF_8));

            if(!StringUtils.isBlank(req.getStartDate()))
            {
                Date startDate = formatter.parse(req.getStartDate());
                if(!(startDate.getTime() >= currentDateFormat.getTime()))
                {
                    //Update the invalid message: start date can not be greater than current date
                    invalidMessage = invalidMessage.concat("Publish date or time precedes CM1 date of " + currentDateFormat + ".");
                	validationResponse.setOperation(invalidMessage);
                    validateParameters(opName).reject(opName, invalidMessage).throwIfInvalid();
                }
            }
            if(!StringUtils.isBlank(req.getEndDate()))
            {
                Date endDate = formatter.parse(req.getEndDate());
                if(!(endDate.getTime() >= currentDateFormat.getTime()))
                {
                    //Update the invalid message: end date can not be greater than current date
                    invalidMessage = invalidMessage.concat("Removal date or time precedes CM1 date of " + currentDateFormat + ".");
                	validationResponse.setOperation(invalidMessage);
                    validateParameters(opName).reject(opName, invalidMessage).throwIfInvalid();
                }
            }

            if(!StringUtils.isBlank(req.getStartDate()) && !StringUtils.isBlank(req.getEndDate()))
            {
                Date startDate = formatter.parse(req.getStartDate());
                Date endDate = formatter.parse(req.getEndDate());
                if(!(endDate.getTime() > startDate.getTime()))
                {
                    //Update the invalid message: end date can not be greater than current date
                    invalidMessage = invalidMessage.concat("Removal Date precedes Publish Date.\n");
                    validationResponse.setOperation(invalidMessage);
                    validateParameters(opName).reject(opName, invalidMessage).throwIfInvalid();
                }
            }
        }
        catch(Exception e)
        {
        	validationResponse.setResult("error");
        }
		return validationResponse;

    }

    @GET
    @Path("getitemdates/{id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSItemDates getItemDates(@PathParam("id") String id) throws PSItemServiceException {

        try {
            rejectIfBlank("getItemDates", "id", id);

            PSContentItem item = contentItemDao.find(id, true);
            Map<String,Object> fields = item.getFields();
            PSItemDates itemDates = new PSItemDates();
            String startDateField = fields.get(START_DATE) != null ? fields.get(START_DATE).toString() : "";
            String endDateField = fields.get(END_DATE) != null ? fields.get(END_DATE).toString() : "";

            try {
                startDateField = convertDate(startDateField, false);
                endDateField = convertDate(endDateField, false);
            } catch (ParseException e) {
                Object[] args = {startDateField, endDateField};
                throw new WebApplicationException(MessageFormat.format(
                        "Error when converting one of the following date: {0}, {1}", args));
            }

            itemDates.setItemId(id);
            itemDates.setStartDate(startDateField.toLowerCase());
            itemDates.setEndDate(endDateField.toLowerCase());

            return itemDates;
        } catch (PSDataServiceException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(PSExceptionUtils.getMessageForLog(e));
        }
    }

    @SuppressWarnings("unchecked")
    @POST
    @Path("/setitemdates")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSNoContent setItemDates(PSItemDates req) throws Exception
    {
        PSNoContent validationResponse = dateValidation(req);
        if (!validationResponse.getOperation().equals("Success"))
        {
            String opName = "setItemDates";
            PSValidationErrorsBuilder builder = validateParameters(opName).reject("Set Publish Dates",validationResponse.getOperation());
            builder.throwIfInvalid();

        }
        String id = req.getItemId();
        IPSGuid guid = idMapper.getGuid(id);

        //if item is checked out to some one else throws exception.
        PSComponentSummary sum = workflowHelper.getComponentSummary(id);
        String type = workflowHelper.isPage(id)?PAGE:ASSET;
        if(StringUtils.isNotBlank(sum.getCheckoutUserName()) && !workflowHelper.isCheckedOutToCurrentUser(id))
        {
            Object[] args = {type,sum.getCheckoutUserName()};
            throw new PSItemServiceException(MessageFormat.format("User {1} is editing this {0}. " +
                    "You cannot modify this item.", args));
        }
        String startDate = "";
        String endDate = "";

        try
        {
            startDate = convertDate(req.getStartDate(), true);
            endDate = convertDate(req.getEndDate(), true);
            startDate = StringUtils.isBlank(startDate)?null:startDate;
            endDate = StringUtils.isBlank(endDate)?null:endDate;
        }
        catch (ParseException e)
        {
            Object[] args = {req.getStartDate(),req.getEndDate()};
            throw new PSItemServiceException(MessageFormat.format(
               "Error when converting one of the following date: {0}, {1}", args));
        }


        PSItemStatus status = contentWs.prepareForEdit(guid);
        if (status.isDidCheckout())
        {
            //Need to update the parent relationship's dependent revision for the asset
            waRelService.updateLocalRelationshipAsset(id);
        }

        PSContentItem item = contentItemDao.find(id);
        Map<String,Object> fields = item.getFields();
        fields.put(START_DATE, startDate);
        fields.put(END_DATE, endDate);
        item.setFields(fields);
        contentItemDao.save(item);
try{
    List<String> paths = folderHelper.findPaths(idMapper.getString(guid));
    String dependentId = guid.toString().substring(guid.toString().lastIndexOf("-") + 1, id.length());

    if(! "".equals(startDate)) {
        psContentEvent = new PSContentEvent(guid.toString(), String.valueOf(dependentId), paths.get(0), PSContentEvent.ContentEventActions.pagePublishSchedule, PSSecurityFilter.getCurrentRequest().getServletRequest(), PSActionOutcome.SUCCESS);
        psAuditLogService.logContentEvent(psContentEvent);
    }
    if(! "".equals(endDate)){
        psContentEvent = new PSContentEvent(guid.toString(), String.valueOf(dependentId), paths.get(0), PSContentEvent.ContentEventActions.pageRemovalSchedule, PSSecurityFilter.getCurrentRequest().getServletRequest(), PSActionOutcome.SUCCESS);
        psAuditLogService.logContentEvent(psContentEvent);
    }
}
catch (Exception e){
    List<String> paths = folderHelper.findPaths(idMapper.getString(guid));
    String dependentId = guid.toString().substring(guid.toString().lastIndexOf("-") + 1, id.length());

    psContentEvent = new PSContentEvent(guid.toString(), String.valueOf(dependentId), paths.get(0), PSContentEvent.ContentEventActions.pagePublishSchedule, PSSecurityFilter.getCurrentRequest().getServletRequest(), PSActionOutcome.FAILURE);
    psAuditLogService.logContentEvent(psContentEvent);
}
        String comments = req.getComments();
        if(comments == null || comments.trim().equals("")){
            comments = "Auto approval while scheduling";
        }

        //Below code has been done to approve page after scheduling
        try{
            itemWfService.performApproveTransition(id,true,comments);

        }
        catch(IPSItemWorkflowService.PSItemWorkflowServiceException iwe)
        {
            log.error("PSItemWorkflowServiceException occur while approving page", iwe);

        }
        catch(Exception e)
        {
            log.error("Unexpected error occurred while approving the item while scheduling page",e);


        }
        return new PSNoContent("Item with id " + id + " has been Updated");
    }

    @GET
    @Path("getsoprometadata/{id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Deprecated
    public PSSoProMetadata getSoProMetadata(@PathParam("id") String id)
    {
        try {
            rejectIfBlank("getSoProMetadata", "id", id);

            PSContentItem item = contentItemDao.find(id, true);
            Map<String, Object> fields = item.getFields();
            Object meta = fields.get(SOPRO_METADATA);
            String metadata = (meta != null) ? meta.toString() : "";

            return new PSSoProMetadata(id, metadata);

        } catch (PSDataServiceException e) {
            throw new WebApplicationException(PSExceptionUtils.getMessageForLog(e));
        }
    }

    @POST
    @Path("/setsoprometadata")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Deprecated
    public PSNoContent setSoProMetadata(PSSoProMetadata req) throws PSItemServiceException
    {
        try {
                String id = req.getItemId();

                // If item is checked out to some one else throw exception
                PSComponentSummary sum = workflowHelper.getComponentSummary(id);
                String type = workflowHelper.isPage(id)?PAGE:ASSET;
                if (StringUtils.isNotBlank(sum.getCheckoutUserName()) &&
                    !workflowHelper.isCheckedOutToCurrentUser(id))
                {
                    throw new PSItemServiceException(MessageFormat.format(
                            "User {1} is editing this {0}. You cannot modify this item.",
                            type, sum.getCheckoutUserName()));
                }


            PSContentItem item = contentItemDao.find(id);
            Map<String, Object> fields = item.getFields();
            fields.put(SOPRO_METADATA, req.getMetadata());
            contentItemDao.save(item);
            return new PSNoContent("Item with id " + id + " has been Updated");
        } catch (PSDataServiceException | PSNotFoundException e) {
            throw new WebApplicationException(e);
        }


    }

    /* (non-Javadoc)
     * @see com.percussion.itemmanagement.service.IPSItemService#copyFolder(java.lang.String, java.lang.String, java.lang.String)
     */
    public Map<String,String> copyFolder(String srcPath, String destFolder, String name) throws IPSItemWorkflowService.PSItemWorkflowServiceException, PSErrorResultsException, PSDataServiceException {
        Map<String,String> assetMap = new HashMap<>();

        try {
            copyFolder(assetMap,srcPath, destFolder, name);
        } catch (Exception e) {
            throw new IPSItemWorkflowService.PSItemWorkflowServiceException(e);
        }

        return assetMap;
    }

    @GET
    @Path("findLinkedItems/{id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<PSPageLinkedToItem> findPagesLinkedToItem(@PathParam("id") String id) {
        try {
            rejectIfBlank("findPagesLinkedToItem", "id", id);

            List<PSPageLinkedToItem> linkedPages = new ArrayList<>();
            int contentId = idMapper.getContentId(id);


                List<PSManagedLink> ownerLinks = linkService.findLinksByChildId(contentId);
                for (PSManagedLink link : ownerLinks) {
                    final String s = idMapper.getGuidFromContentId(link.getParentId()).toString();
                    PSLocator depLocator = new PSLocator(link.getParentId(), -1);
                    try {
                        linkedPages.addAll(getPageLinkItem(depLocator, s, null));
                    } catch (Exception e) {
                        log.warn(PSExceptionUtils.getMessageForLog(e));
                    }
                }

            List<PSPageLinkedToItem> relationships = getLinkedItemFromRelationship(contentId);
            linkedPages.addAll(relationships);
            return linkedPages;
        } catch (PSValidationException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(PSExceptionUtils.getMessageForLog(e));
        }
    }

    private List<PSPageLinkedToItem> getPageLinkItem(PSLocator locator,String defaultString, PSRelationship relationship) throws Exception {

        List<PSPageLinkedToItem> items = new ArrayList<>();
        PSRelationshipFilter filter = new PSRelationshipFilter();
        filter.setDependent(locator);
        filter.setName(PSRelationshipConfig.TYPE_LOCAL_CONTENT);

        final List<PSItemSummary> owners = contentWs.findOwners(idMapper.getGuid(locator), filter, false);
        for (PSItemSummary owner : owners) {
            //In case RichTextEditor is associated with the current page and is having a link to someother page
            // then don't want to return the same page as dependent
            if(owner.getGUID() == null || relationship==null || relationship.getDependent() ==null){
                continue;
            }
            if(owner.getGUID().getUUID() == relationship.getDependent().getId() ){
                continue;
            }
            List<String> paths = null;
            if (defaultString != null) {
                paths = folderHelper.findPaths(idMapper.getString(owner.getGUID()));
            }
            String pagePath = null;
            if (paths != null && paths.size() > 0) {
                pagePath = paths.get(0) + "/" + owner.getName();
            } else {
                pagePath = owner.getName();
            }
            if (pagePath != null) {
                if(relationship == null){
                    PSPageLinkedToItem item = new PSPageLinkedToItem(owner.getGUID().toString(), pagePath,null);
                    items.add(item);
                }else{
                    PSPageLinkedToItem item = new PSPageLinkedToItem(owner.getGUID().toString(), pagePath,relationship.getGuid().toString());
                    items.add(item);
                }

            }
        }
            return items;
    }

    private List<PSPageLinkedToItem> getLinkedItemFromRelationship(int itemId)  {
        List<PSPageLinkedToItem> linkedPages = new ArrayList<>();
        try{
        // get all dependent relationships for the current content id and delete
        PSRelationshipFilter filter = new PSRelationshipFilter();
        filter.setCategory(PSRelationshipFilter.FILTER_CATEGORY_ACTIVE_ASSEMBLY);
        filter.setCommunityFiltering(false); // do not filter by community
        filter.setDependentId(itemId); // disregard dependent revision
        List<PSRelationship> relationships = this.relationshipService.findByFilter(filter);
        List<Integer> owners = new ArrayList<>();
        if(relationships != null && relationships.size()>0) {
            for (PSRelationship relationship : relationships
            ) {
                int ownerId = relationship.getOwner().getId();
                //Trying to avoid returning multiple of same owner or if Relationship is deleted (dependent revision = -1)
                if(!owners.contains(Integer.valueOf(ownerId)) && relationship.getDependent().getRevision() != -1){
                    owners.add(Integer.valueOf(ownerId));
                    final String s = idMapper.getGuidFromContentId(ownerId).toString();
                    linkedPages.addAll(getPageLinkItem(relationship.getOwner(),s,relationship));
                }
            }
        }

        } catch (Exception e) {
            // here it may be better not to throw exception, just proceed with the 'remove from site' as it was previous behavior.
            log.error("Unable to retrieve pages which link to current item: " + itemId, e);
        }
        return linkedPages;

    }
   
    
    /**
     * Copy folder method is passed assetMap to store current state of copied assets while recursing to
     * allow for rollback on exception.
     * 
     * @param assetMap
     * @param srcPath
     * @param destFolder
     * @param name
     */
    private void copyFolder(Map<String, String> assetMap, String srcPath, String destFolder, String name) throws Exception {
        if (StringUtils.isBlank(srcPath))
        {
            throw new IllegalArgumentException("srcPath may not be blank");
        }
        
        if (StringUtils.isBlank(destFolder))
        {
            throw new IllegalArgumentException("destFolder may not be blank");
        }
        
        if (StringUtils.isBlank(name))
        {
            throw new IllegalArgumentException("name may not be blank");
        }

            List<IPSItemSummary> sums = folderHelper.findItems(srcPath);

            String folderPath = folderHelper.concatPath(destFolder, name);
            IPSItemSummary destSum = null;
            try {
                destSum = folderHelper.findFolder(folderPath);
            } catch (Exception e) {
                // folder doesn't exist
            }

            if (destSum == null) {
                contentWs.addFolder(name, destFolder, srcPath, false);
            }

            for (IPSItemSummary sum : sums) {
                String sumName = sum.getName();
                if (sum.isFolder()) {

                    copyFolder(assetMap, folderHelper.concatPath(srcPath, sumName), folderPath, sumName);
                } else {
                    // copy the item (currently the item is assumed to be an asset), eventually this should delegate to
                    // the copy methods of the page service (for site sub-folder copy) and asset service (when copy
                    // asset is implemented).
                    String id = sum.getId();

                    List<PSCoreItem> items = contentWs.newCopies(Collections.singletonList(idMapper.getGuid(id)),
                            Collections.singletonList(folderPath), null, false);
                    PSCoreItem item = items.get(0);
                    PSLocator locator = (PSLocator) item.getLocator();

                    String copyAssetId = idMapper.getString(locator);

                    itemWfService.checkOut(copyAssetId);
                    PSAsset copyAsset = assetDao.find(copyAssetId);
                    Map<String, Object> fieldMap = copyAsset.getFields();
                    String type = copyAsset.getType();
                    if (type.equals(PSAssetRestService.FORM_CONTENT_TYPE)) {
                        // form names must be unique across the system
                        int i = 2;
                        String copyNameBase = sumName + "-copy";
                        String copyName = copyNameBase;
                        while (!assetDao.findByTypeAndName(type, copyName).isEmpty()) {
                            copyName = copyNameBase + '-' + i++;
                        }

                        String renderedForm =
                                (String) fieldMap.get(PSAssetRestService.FORM_RENDEREDFORM_FIELD_NAME);
                        renderedForm = StringUtils.replaceOnce(renderedForm, "{jQuery('#" + sumName + "')",
                                "{jQuery('#" + copyName + "')");
                        renderedForm = StringUtils.replaceOnce(renderedForm, "<form name=\"" + sumName + "\"",
                                "<form name=\"" + copyName + "\"");
                        renderedForm = StringUtils.replaceOnce(renderedForm, "<input value=\"" + sumName + "\"",
                                "<input value=\"" + copyName + "\"");
                        fieldMap.put(PSAssetRestService.FORM_RENDEREDFORM_FIELD_NAME, renderedForm);

                        String formData = (String) fieldMap.get(PSAssetRestService.FORM_FORMDATA_FIELD_NAME);
                        formData = StringUtils.replaceOnce(formData, "{\"name\":\"" + sumName + "\"",
                                "{\"name\":\"" + copyName + "\"");
                        fieldMap.put(PSAssetRestService.FORM_FORMDATA_FIELD_NAME, formData);

                        sumName = copyName;
                    } else if (UNIQUE_FIELDS.containsKey(type)) {


                        String uniqueField = UNIQUE_FIELDS.get(type);

                        // name must be unique across the system
                        int i = 2;
                        String unique_name = StringUtils.defaultString((String) fieldMap.get(uniqueField), sumName);

                        String copyNameBase = unique_name + "-copy";
                        String titleCopyNameBase = sumName + "-copy";

                        String copyName = copyNameBase;
                        String titleCopyName = titleCopyNameBase;
                        HashSet<String> names = new HashSet<>();
                        HashSet<String> titles = new HashSet<>();
                        for (PSAsset asset : assetDao.findByType(type)) {
                            names.add((String) asset.getFields().get(uniqueField));
                            titles.add((String) asset.getFields().get("sys_title"));
                        }

                        while (names.contains(copyName)) {
                            copyName = copyNameBase + '-' + i++;
                        }

                        while (titles.contains(titleCopyName)) {
                            titleCopyName = titleCopyNameBase + '-' + i++;
                        }

                        if (unique_name != null) {
							fieldMap.put(uniqueField, copyName);
						}

                        sumName = titleCopyName;
                    }

                    fieldMap.put("sys_title", sumName);
                    copyAsset.setName(sumName);
                    assetDao.save(copyAsset);
                    assetMap.put(id, idMapper.getString(locator));
                    itemWfService.checkIn(copyAssetId);
                }
            }
    }
    
    @PUT
    @Path ("/addtomypages/{pageId}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSNoContent addToMyPages(@PathParam("pageId") String pageId)
    {
        try {
            rejectIfBlank("isMyPage", "pageId", pageId);

            int id = idMapper.getContentId(pageId);
            addUserItem(PSWebserviceUtils.getUserName(), id, PSUserItemTypeEnum.FAVORITE_PAGE);

            return new PSNoContent("Successfully added page to my pages");
        } catch (PSValidationException | IPSGenericDao.SaveException e) {
           throw new WebApplicationException(PSExceptionUtils.getMessageForLog(e));
        }
    }

    @PUT
    @Path ("/addtomypages/{userName}/{pageId}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSNoContent addToMyPages(@PathParam("userName") String userName, @PathParam("pageId") String pageId)
    {
        try {
            rejectIfBlank("isMyPage", "userName", userName);
            rejectIfBlank("isMyPage", "pageId", pageId);

            int id = idMapper.getContentId(pageId);
            addUserItem(userName, id, PSUserItemTypeEnum.FAVORITE_PAGE);

            return new PSNoContent("Successfully added page to my pages");
        } catch (PSValidationException | IPSGenericDao.SaveException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e);
        }
    }
    
    @DELETE
    @Path("/removefrommypages/{pageId}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSNoContent removeFromMyPages(@PathParam("pageId") String pageId)
    {
        try {
            rejectIfBlank("isMyPage", "pageId", pageId);

            int id = idMapper.getContentId(pageId);
            removeUserItem(PSWebserviceUtils.getUserName(), id);

        return new PSNoContent("Successfully removed page from my pages");
        } catch (PSValidationException e) {
            throw new WebApplicationException(PSExceptionUtils.getMessageForLog(e));
        }
    }

    @GET
    @Path("/ismypage/{pageId}")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean isMyPage(@PathParam("pageId") String pageId)
    {
        try {
            rejectIfBlank("isMyPage", "pageId", pageId);
            boolean result = false;

            int id = idMapper.getContentId(pageId);
            PSUserItem userItem = userItemDao.find(PSWebserviceUtils.getUserName(), id);
            result = userItem != null;
            return result;
        } catch (PSValidationException e) {
           throw new WebApplicationException(PSExceptionUtils.getMessageForLog(e));
        }
    }

    /*
     * (non-Javadoc)
     * @see com.percussion.itemmanagement.service.IPSItemService#getUserItems(java.lang.String)
     */
    public List<PSUserItem> getUserItems(String userName)
    {
        userName = StringUtils.defaultString(userName);
        return userItemDao.find(userName);
    }

    /*
     * (non-Javadoc)
     * @see com.percussion.itemmanagement.service.IPSItemService#getUserItems(int)
     */
    public List<PSUserItem> getUserItems(int itemId)
    {
        return userItemDao.find(itemId);
    }

    /*
     * (non-Javadoc)
     * @see com.percussion.itemmanagement.service.IPSItemService#addUserItem(java.lang.String, int, com.percussion.itemmanagement.service.IPSItemService.PSUserItemTypeEnum)
     */
    public void addUserItem(String userName, int itemId, PSUserItemTypeEnum type) throws IPSGenericDao.SaveException {
        Validate.notEmpty(userName);
        PSUserItem userItem = userItemDao.find(userName, itemId);
        if(userItem == null)
        {
            userItem = new PSUserItem();
            userItem.setUserName(userName);
            userItem.setItemId(itemId);
            userItem.setType(type.toString());
            userItemDao.save(userItem);
        }
    }

    /*
     * (non-Javadoc)
     * @see com.percussion.itemmanagement.service.IPSItemService#removeUserItem(java.lang.String, int)
     */
    public void removeUserItem(String userName, int itemId)
    {
        Validate.notEmpty(userName);
        PSUserItem userItem = userItemDao.find(userName, itemId);
        if(userItem != null)
        {
            userItemDao.delete(userItem); 
        }        
    }

    /*
     * (non-Javadoc)
     * @see com.percussion.itemmanagement.service.IPSItemService#deleteUserItems(int)
     */
    public void deleteUserItems(int itemId)
    {
        List<PSUserItem> userItems = userItemDao.find(itemId);
        for (PSUserItem userItem : userItems)
        {
            userItemDao.delete(userItem); 
        }
    }

    /*
     * (non-Javadoc)
     * @see com.percussion.itemmanagement.service.IPSItemService#deleteUserItems(java.lang.String)
     */
    public void deleteUserItems(String userName)
    {
        Validate.notEmpty(userName);
        List<PSUserItem> userItems = userItemDao.find(userName);
        for (PSUserItem userItem : userItems)
        {
            userItemDao.delete(userItem); 
        }
        
    }

    /**
     * Deletes all the user items either for a user or for an item
     * 
     * @param source assumed to be username if the isUser is <code>true</code>
     *            otherwise assumed to be content item guid.
     * @param isUser set to <code>true</code> if the source is user name,
     *            <code>false</code> if the source is content item guid
     */
    private void deleteUserItems(String source, boolean isUser)
    {
        if(isUser)
        {
            deleteUserItems(source);
        }
        else
        {
            int id = idMapper.getContentId(source);
            deleteUserItems(id);
        }
    }
    
    
    /**
     * Cleanup assets folder on error
     * 
     * @param itemMap  map from copyFolder contains all known id maps until error. 
     * @param folderName  Root destination folder
     * @throws PSItemServiceException
     */
    @Override
    public void rollBackCopiedFolder(Map<String, String> itemMap, String folderName) throws PSItemServiceException
    {
       
        IPSSqlPurgeHelper purgeHelper = PSSqlPurgeHelperLocator.getPurgeHelper();
        if (itemMap!=null)
        {
            ArrayList<PSLocator> itemsToDelete = new ArrayList<>();
            for (String guid : itemMap.values())
            {
                itemsToDelete.add(idMapper.getLocator(guid));
            }
            try 
            {
                purgeHelper.purgeAll(itemsToDelete);
            }
            catch (Exception e)
            {
                throw new PSItemServiceException("Cannot purge items in copy rollback",e);
            }
            
        }
      
        if (folderName!=null)
        {
            Number id;
            PSLocator locator = null;
            try
            {
                id = folderHelper.findLegacyFolderIdFromPath(folderName);
                if (id!=null) {
					locator = new PSLocator(id.intValue());
				}
            }
            catch (Exception e)
            {
                log.warn("cannot find copied folder during rollback "+folderName + " it may not have been created",e);
            }
            
            if (locator!=null)
            {
                try
                {
                    purgeHelper.purgeAll(Collections.singletonList(locator));
                } 
                catch (Exception e)
                {
                    log.error("Cannot rollback copied folder "+folderName+" with id "+locator.getId());
                }
            }
        }
    }

    @Override
    @GET
    @Path("mycontent")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<PSItemProperties> getMyContent()
    {
        String user = PSWebserviceUtils.getUserName();
        List<PSUserItem> userItems = getUserItems(user);
        List<PSItemProperties> items = new ArrayList<>();
        for (PSUserItem uItem : userItems)
        {
            PSItemProperties itemProps = null;
            try {
                itemProps = folderHelper.findItemPropertiesById(idMapper.getGuid(new PSLocator(uItem.getItemId())).toString());
                if (itemProps != null) {
					items.add(itemProps);
				}
            } catch (Exception e) {
                log.error(PSExceptionUtils.getMessageForLog(e));
                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                //continue processing
            }
        }
        return new PSItemPropertiesList(items);
    }


    public class SecureKeyRotationListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            try {
                Set processedPages = new HashSet();
                Collection<PSAsset> assets = assetDao.findAllAssetsUsingEncryption();
                for (PSAsset asset:assets) {
                    Set<String> pages = waRelService.getRelationshipOwners(asset.getId());
                    for(String page : pages) {
                        if(workflowHelper.isTemplate(page)){
                            Collection<Integer> pageIds = templateService.getPageIdsForTemplate(page);
                            for(Integer pageContentId:pageIds){
                                if (processedPages.contains(pageContentId.intValue())) {
                                    continue;
                                }
                                if(addToIncrementalQueue(pageContentId)) {
                                    processedPages.add(pageContentId.intValue());
                                }
                            }
                        }else if(workflowHelper.isPage(page)) {
                            int contentId = ((PSLegacyGuid) idMapper.getGuid(page)).getContentId();
                            if (processedPages.contains(contentId)) {
                                continue;
                            }
                            if(addToIncrementalQueue(contentId)) {
                                processedPages.add(contentId);
                            }
                        }
                        }
                    }

            } catch (PSReportFailedToRunException | PSValidationException | PSNotFoundException | IPSGenericDao.LoadException  e) {
               log.error("Secure Key Rotation Failed to get Asset Pages with Encryption. ERROR: {}",PSExceptionUtils.getMessageForLog(e));
            }
        }

        private boolean addToIncrementalQueue(int pageId){
            try {
                IPSGuid contentGuid = PSGuidUtils.makeGuid(pageId, PSTypeEnum.LEGACY_CONTENT);
                PSContentChangeEvent changeEvent = new PSContentChangeEvent();
                changeEvent.setChangeType(PSContentChangeType.PENDING_LIVE);
                changeEvent.setContentId(pageId);
                IPSSite site = publishingWs.getItemSites(contentGuid).get(0);
                changeEvent.setSiteId(site.getSiteId());
                changeService.contentChanged(changeEvent);

            } catch (IPSGenericDao.SaveException  e) {
                log.error("Secure Key Rotation Failed to add page : {} to Incremental Queue. ERROR: {}", pageId, PSExceptionUtils.getMessageForLog(e));
                return false;
            }
            return true;
        }
    }
    
    public  static final String PAGE = "page";
    public  static final String ASSET = "asset";
    public  static final String START_DATE = "sys_contentstartdate";
    public  static final String END_DATE = "sys_contentexpirydate";
    public  static final String SOPRO_METADATA = "social_promotion_metadata";
    private static final String[] WORKFLOW_TRANSITION_IGNORE_LIST = {"CheckOut", "CheckIn", "Edit", "Live"};
    
    /**
     * Logger for this service.
     */
    public static final Logger log = LogManager.getLogger(PSItemService.class);



}
