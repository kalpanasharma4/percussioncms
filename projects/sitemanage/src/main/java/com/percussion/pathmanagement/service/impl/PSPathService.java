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
package com.percussion.pathmanagement.service.impl;

import com.percussion.auditlog.PSActionOutcome;
import com.percussion.auditlog.PSAuditLogService;
import com.percussion.auditlog.PSContentEvent;
import com.percussion.error.PSExceptionUtils;
import com.percussion.i18n.ui.PSI18NTranslationKeyValues;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pathmanagement.data.PSDeleteFolderCriteria;
import com.percussion.pathmanagement.data.PSFolderProperties;
import com.percussion.pathmanagement.data.PSItemByWfStateRequest;
import com.percussion.pathmanagement.data.PSMoveFolderItem;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.data.PSPathItemList;
import com.percussion.pathmanagement.data.PSRenameFolderItem;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.recycle.service.IPSRecycleService;
import com.percussion.security.PSOperationContext;
import com.percussion.security.SecureStringUtils;
import com.percussion.server.PSServer;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.data.PSItemProperties;
import com.percussion.share.data.PSItemPropertiesList;
import com.percussion.share.data.PSNoContent;
import com.percussion.share.data.PSPagedItemList;
import com.percussion.share.rx.PSLegacyExtensionUtils;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.PSSiteCopyUtils;
import com.percussion.share.service.exception.PSBeanValidationException;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSParametersValidationException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.ui.service.IPSUiService;
import com.percussion.ui.service.impl.PSCm1ListViewHelper;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.publishing.IPSPublishingWs;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.util.List;


/**
 * 
 * The unified path service loader will delegate to other path services.
 * 
 * @author adamgent
 * @see PSDispatchingPathService
 */
@Path("/path")
//@Component(value="pathService")
//@Lazy
public class PSPathService extends PSDispatchingPathService implements IPSPathService, ApplicationContextAware, InitializingBean  {
    private final PSAuditLogService psAuditLogService=PSAuditLogService.getInstance();
    private PSContentEvent psContentEvent;

    private final IPSFolderHelper folderHelper;
    
    private final IPSPublishingWs publishingWs;
    
    private final IPSIdMapper idMapper;

    private final IPSRecycleService recycleService;

    private PSPathOptions pathOptions;
    private ApplicationContext applicationContext;

    @Autowired
    public PSPathService(IPSFolderHelper folderHelper,
                         IPSPublishingWs publishingWs, IPSIdMapper idMapper, IPSUiService uiService,
                         IPSUserService userService, @Qualifier("cm1ListViewHelper") PSCm1ListViewHelper listViewHelper,
                         IPSRecycleService recycleService)
    {
        super(uiService, userService, listViewHelper, recycleService, folderHelper);
        this.folderHelper = folderHelper;
        this.publishingWs = publishingWs;
        this.idMapper     = idMapper;
        this.recycleService = recycleService;

        // hold instance for static access
        pathOptions = new PSPathOptions();
    }


    @Override
    @GET
    @Path("/item/{path:.*}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSPathItem find(@PathParam("path") String path) throws PSPathNotFoundServiceException, PSPathServiceException {
        try {
            if(!SecureStringUtils.isValidCMSPathString(path, PSOperationContext.SEARCH))
                throw new PSPathServiceException("Invalid path.");

            log.debug("Attempting to find item for path: {}", path);
            PSPathItem item = super.find(path);
            return folderHelper.setFolderAccessLevel(item);
        } catch (PSDataServiceException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new PSPathServiceException(e);
        } catch (PSNotFoundException e) {
            throw new PSPathNotFoundServiceException(e);
        }
    }

    @GET
    @Path("/item/id/{id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSPathItem findById(@PathParam("id") String id) throws PSPathServiceException, PSParametersValidationException {
        try {
            String guid = PSLegacyExtensionUtils.getGUID(id);
            if(!SecureStringUtils.isValidGuidId(guid))
                throw new PSPathServiceException("Invalid id.");

            return folderHelper.findItemById(guid);
        }catch (PSParametersValidationException pve){
            log.debug(pve.getMessage(), pve);
            throw pve;
        }
        catch (IPSDataService.DataServiceLoadException | PSValidationException | PSNotFoundException e) {
            log.error("Error: {} Id: {}",e.getMessage(), id);
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new PSPathServiceException(e);
        }
    }

    @Override
    @GET
    @Path("/itemProperties/{path:.*}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSItemProperties findItemProperties(@PathParam("path") String path) throws PSPathServiceException {
        try {
            if(!SecureStringUtils.isValidCMSPathString(path, PSOperationContext.SEARCH))
                throw new PSPathServiceException("Invalid path.");

            log.debug("Attempting to find item properties for path: {}", path);
            return super.findItemProperties(path);
        } catch (PSPathServiceException pse) {
            log.debug(pse.getMessage(),pse);
            throw pse;
        }catch (PSDataServiceException e){
            log.error("Error: {} Path: {}",e.getMessage(),path);
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new PSPathServiceException(e);
        }
    }

    @GET
    @Path("/folderProperties/{id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSFolderProperties findFolderProperties(@PathParam("id") String id) throws PSPathServiceException {
        try {
            if(!SecureStringUtils.isValidGuidId(id)){
                throw new PSPathServiceException("Invalid id.");
            }
            return folderHelper.findFolderProperties(id);
        } catch (PSValidationException e) {
            log.error("Error: {} Id: {}", PSExceptionUtils.getMessageForLog(e),id);
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new PSPathServiceException(e);
        }
    }

    @POST
    @Path("/saveFolderProperties")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSNoContent saveFolderProperties(PSFolderProperties props) throws PSValidationException
    {

        List<IPSSite> sites = publishingWs.getItemSites(idMapper.getGuid(props.getId()));
        if ((sites != null) && (!sites.isEmpty())) {
            PSSiteCopyUtils.throwCopySiteMessageIfNotAllowed(sites.get(0).getName(), "saveFolderProperties",
                    PSI18NTranslationKeyValues.getInstance().
                            getTranslationValue(PSSiteCopyUtils.class.getName() + PSSiteCopyUtils.CAN_NOT_UPDATE_FOLDER_PROPERTIES));
        }
        folderHelper.saveFolderProperties(props);
        return new PSNoContent("saveFolderProperties");

    }

    /**
     * It returns a list of children for the specified path. It allows to paginate results also.
     * 'startIndex' and 'maxResults' are used to ask for a particular page of data. 'child'
     * and 'maxResults' are used to return the page of data where 'child' is contained.
     * <p>
     * According to the given parameters, different functions are called, in this order:
     * <ul>
     *  <li>If 'child' and 'maxResults' are not null, then those two parameters are used
     *  to look for a page containing the 'child' entry.</li>
     *  <li>If 'startIndex' is not null, then it's used to look for the page starting with
     *  that entry. If 'maxResults' was specified, then the page will have, at maximum,
     *  'maxResults' entries. If it wasn't, all entries starting from 'startIndex' are
     *  returned.</li>
     *  <li>In this case, no paging is done. All children for the specified path are
     *  returned.</li>
     * </ul>
     *
     * @param path The path to look for its children.
     * @param startIndex The starting index. It can be <code>null</code>, in that case
     * it won't be used.
     * @param maxResults The maximum amount of results. It can be <code>null</code>, in
     * that case it won't be used.
     * @param sortColumn The column name to sort the path items by. For example: sys_title, sys_contentcreatedby.
     * See {@link PSCm1ListViewHelper} for all supported column names. May be empty <code>null</code> or
     * empty, in that case no sorting is done (both sortColumn and sortOrder are necessary to
     * carry out any sorting).
     * @param sortOrder The order to sort the path items by. It accepts two possible values:
     * 'asc' (ascendant) and 'desc' (descendant); case is ignored. May be empty <code>null</code> or
     * empty, in that case no sorting is done (both sortColumn and sortOrder are necessary to
     * carry out any sorting).
     * @param child The child name to look for, for example: page1. It can be <code>null</code>,
     * in that case it won't be used.
     * @param displayFormatId The display format id to use to format the results, may be <code>null</code>.
     * @param category Optional category to filter the resulting items by, the string value of one or more of the {@link IPSItemSummary.Category} values,
     * may be <code>null</code>, comma-separated list if multiple are
     * supplied.  Only used if startIndex is supplied.
     * @param type Optional type to filter the resulting items by, one or more of the possible values returned
     * by {@link IPSItemSummary#getType()}, may be <code>null</code>, comma-separated list if multiple are
     * supplied.  Only used if startIndex is supplied.
     * @param mustExist  Optional.  Set to true to require the path to be valid and existing.
     * @return An @{link PSPagedItemList} with information about a {@link PSPathItem} (specified, like
     * a list of children for a specified page of data and the total count of items that it has.
     * The child item will be present, if found, in the children list. If not found, the first
     * page is returned. Never <code>null</code>.  Child items will not have valid settings for
     * {@link PSPathItem#hasFolderChildren()}, {@link PSPathItem#hasItemChildren()}, and
     * {@link PSPathItem#hasSectionChildren()} - they will all return <code>false</code> irrespective of actual data.
     *
     * @throws PSPathNotFoundServiceException
     * @throws PSPathServiceException
     */
    @GET
    @Path("/paginatedFolder/{path:.*}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSPagedItemList findChildren(@PathParam("path") String path,
                                        @QueryParam("startIndex") Integer startIndex,
                                        @QueryParam("maxResults") Integer maxResults,
                                        @QueryParam("sortColumn") String sortColumn,
                                        @QueryParam("sortOrder") String sortOrder,
                                        @QueryParam("child") String child,
                                        @QueryParam("displayFormatId") Integer displayFormatId,
                                        @QueryParam("category") String category,
                                        @QueryParam("type") String type, @QueryParam("mustExist") boolean mustExist) throws PSPathNotFoundServiceException,
            PSPathServiceException {

        try {
            if(!SecureStringUtils.isValidCMSPathString(path, PSOperationContext.SEARCH)){
                log.warn("Invalid path provided: {}",path);
                throw new PSPathServiceException("Invalid path provided");
            }

            log.debug("Attempting to find children for path: {}", path);
            log.debug(
                    "Parameters set for find children: startIndex={}; maxResults={}; child={}",
                    startIndex, maxResults, child);

            PSPathItem psPathItem;

            //CMS-5620 - Handle file not there.
            if (mustExist) {
                try {
                    psPathItem = find(path);
                } catch (Exception e) {
                    throw new WebApplicationException(e, 404);
                }
                if (psPathItem == null) {
                    throw new WebApplicationException("Folder not found.", 404);
                }
            }

            /*
             * Depending on the parameters sent by the client, call different methods in
             * the super class.
             */
            PSPagedItemList extendedPathItem = null;
            if (child != null && maxResults != null)
                extendedPathItem = super.findChildren(path, maxResults, child, displayFormatId);
            else if (startIndex != null)
                extendedPathItem = super.findChildren(path, startIndex, maxResults, displayFormatId, sortColumn, sortOrder, category, type);
            else {
                List<PSPathItem> allItems = super.findChildren(path, displayFormatId, sortColumn, sortOrder);
                extendedPathItem = new PSPagedItemList(allItems, allItems.size(), 1);
            }

            extendedPathItem.setChildrenInPage(folderHelper.setFolderAccessLevel(extendedPathItem.getChildrenInPage()));

            return extendedPathItem;
        } catch (PSDataServiceException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new PSPathServiceException(e);
        }
    }

    @GET
    @Path("/folder/{path:.*}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<PSPathItem> findChildren(@PathParam("path") String path) throws PSPathServiceException {
        try
        {
            if(!SecureStringUtils.isValidCMSPathString(path, PSOperationContext.SEARCH))
                throw new PSPathServiceException("Invalid path.");

            // check child types
            PSPathOptions.setShouldCheckChildTypes(true);
            return new PSPathItemList(findChildren(path, null, null, null, null, null, null, null, null, false).getChildrenInPage());
        } catch (PSPathServiceException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw (e);
        } finally
        {
            PSPathOptions.setShouldCheckChildTypes(false);
        }

    }

    @GET
    @Path("/childFolders/{path:.*}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<PSPathItem> findChildFolders(@PathParam("path") String path) throws PSPathServiceException {
        try
        {
            if(!SecureStringUtils.isValidCMSPathString(path, PSOperationContext.SEARCH))
                throw new PSPathServiceException("Invalid path.");

            // get folders only
            PSPathOptions.setFolderChildrenOnly(true);
            return findChildren(path);
        }
        finally
        {
            PSPathOptions.setFolderChildrenOnly(false);
        }

    }

    @Override
    @GET
    @Path("/addFolder/{path:.*}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSPathItem addFolder(@PathParam("path") String path) throws PSValidationException, WebApplicationException, PSPathServiceException {
       try {
           if(!SecureStringUtils.isValidCMSPathString(path,PSOperationContext.EDIT))
               throw new PSPathServiceException("Invalid path.");

           log.debug("Attempting to add folder: {}", path);
           PSPathItem folder = super.addFolder(path);
           return folderHelper.setFolderAccessLevel(folder);
       } catch (PSPathServiceException  | IPSDataService.DataServiceNotFoundException | IPSDataService.DataServiceLoadException | PSNotFoundException e) {
           log.error("Path: {} Error: {}",path,e.getMessage());
           log.debug(PSExceptionUtils.getDebugMessageForLog(e));
          throw new PSPathServiceException(e);
       }
    }

    @Override
    @GET
    @Path("/addNewFolder/{path:.*}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSPathItem addNewFolder(@PathParam("path") String path) throws PSPathServiceException {
        try {
            if(!SecureStringUtils.isValidCMSPathString(path, PSOperationContext.CREATE)){
                throw new PSPathServiceException("Invalid path");
            }

            log.debug("Attempting to add new folder to: {}", path);
            PSPathItem folder = super.addNewFolder(path);
            return folderHelper.setFolderAccessLevel(folder);
        } catch (PSPathServiceException | PSDataServiceException | PSNotFoundException e) {
            log.error("Path: {} Error: {}",path,e.getMessage());
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new PSPathServiceException(e);
        }
    }


    @Override
    @POST
    @Path("/renameFolder")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSPathItem renameFolder(PSRenameFolderItem item) throws PSPathNotFoundServiceException,
            PSPathServiceException, PSBeanValidationException {

        try {
            log.debug("Attempting to rename folder: {} to: {}",
                    item.getPath(),
                    item.getName());
            PSSiteCopyUtils.throwCopySiteMessageIfNotAllowed(getSiteNameFromFolderPath(item.getPath()), "renameFolder",
                    PSSiteCopyUtils.CAN_NOT_EDIT_FOLDER_NAME);
            PSPathItem folder = super.renameFolder(item);
            return folderHelper.setFolderAccessLevel(folder);
        }catch (PSBeanValidationException bve){
            throw bve;
        }catch (PSDataServiceException | PSNotFoundException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e.getMessage());
        }
    }

    @Override
    @POST
    @Path("/moveItem")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSNoContent moveItem(PSMoveFolderItem request)
    {
        try {
            PSSiteCopyUtils.throwCopySiteMessageIfNotAllowed(getSiteNameFromFolderPath(request.getItemPath()), "moveItem",
                    PSSiteCopyUtils.CAN_NOT_MOVE_FOLDER);
            stripLeadingSlashForPaths(request);
            return super.moveItem(request);
        } catch (PSPathServiceException | PSDataServiceException | IPSItemWorkflowService.PSItemWorkflowServiceException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e.getMessage());
        }
    }

    @GET
    @Path("/lastExisting/{path:.*}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,MediaType.TEXT_PLAIN})
    public String findLastExistingPath(@PathParam("path") String path) throws PSPathServiceException {

        if(!SecureStringUtils.isValidCMSPathString(path, PSOperationContext.SEARCH))
            throw new PSPathServiceException("Invalid path.");

        log.debug("Attempting to find last existing path: {}", path);
        return super.findLastExistingPath(path);
    }

    @GET
    @Path("/validate/{path:.*}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,MediaType.TEXT_PLAIN})
    public String validateEnteredPath(@PathParam("path") String path) throws PSPathServiceException {

        if(!SecureStringUtils.isValidCMSPathString(path, PSOperationContext.SEARCH))
            throw new PSPathServiceException("Invalid path.");

        log.debug("Attempting to find existing path: {}",  path);
        try{
            return super.validateEnteredPath(path);
        }catch (PSPathServiceException pse){
            log.debug(pse.getMessage(),pse);
            throw pse;
        }
    }

    @GET
    @Path("/getAssetPaginationConfig")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,MediaType.TEXT_PLAIN})
    public String getAssetPaginationConfig() {
        return PSServer.getProperty("enablePagination", "false");
    }

    /**
     * Deletes a folder accordingly as specified by a given {@link PSDeleteFolderCriteria}.
     *
     * @param criteria never <code>null</code>.
     *
     * @return the number of items cannot be deleted in String format.
     *
     * @throws PSPathServiceException If the folder could not be deleted or other system failure.
     */
    @POST
    @Path("/deleteFolder")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public String deleteFolderService(PSDeleteFolderCriteria criteria) throws PSValidationException, PSPathServiceException, PSDataServiceException, PSNotFoundException {
        if(log.isDebugEnabled()) log.debug("Attempting to delete folder: " + criteria.getPath());

        String currentUser = (String)PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
        log.info(criteria.getPath() + " has been deleted by: " + currentUser);

        //CMS-7920 : finding item to get its type for next check.
        PSPathItem item = super.find(criteria.getPath());

        //CMS-7920 : The system folder delete was failing as they are file system files.
        //Applied this check to not to send system folders to recycle bin.
        if(item!=null && item.getType()!=null && !item.getType().equalsIgnoreCase(IPSPathService.FOLDER_TYPE_FILESYSTEM)){
            int iGuid = 0;
            if(!criteria.getGuid().isEmpty()) {
                iGuid = idMapper.getContentId(criteria.getGuid());
            }

            PSSiteCopyUtils.throwCopySiteMessageIfNotAllowed(getSiteNameFromFolderPath(criteria.getPath()),
                    "deleteFolderService" , PSSiteCopyUtils.CAN_NOT_DELETE_FOLDER);
            psContentEvent=new PSContentEvent(criteria.getGuid(),String.valueOf(iGuid),criteria.getPath(), PSContentEvent.ContentEventActions.delete, PSSecurityFilter.getCurrentRequest().getServletRequest(), PSActionOutcome.SUCCESS);
            psAuditLogService.logContentEvent(psContentEvent);

        }

        return String.valueOf(super.deleteFolder(criteria));

    }

    @PUT
    @Path("/restoreFolder/{id}")
    @Override
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSNoContent restoreFolder(@PathParam("id") String id) throws PSPathServiceException {

        if(!SecureStringUtils.isValidGuidId(id))
            throw new PSPathServiceException("Invalid id.");

        return super.restoreFolder(id);
    }


    @Override
    @GET
    @Path("/validateFolderDelete/{path:.*}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,MediaType.TEXT_PLAIN})
    public String validateFolderDelete(@PathParam("path") String path) throws PSValidationException, PSPathNotFoundServiceException,
            PSPathServiceException {
       try {
           if(!SecureStringUtils.isValidCMSPathString(path, PSOperationContext.DELETE))
               throw new PSPathServiceException("Invalid path.");

           log.debug("Attempting to validate folder: {} for delete", path);
           return super.validateFolderDelete(path);
       } catch (IPSDataService.DataServiceNotFoundException | IPSItemWorkflowService.PSItemWorkflowServiceException | IPSDataService.DataServiceLoadException | PSNotFoundException e) {
           throw new PSPathServiceException(e);
       }
    }

    @Override
    @POST
    @Path("/item/wfState")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<PSItemProperties> findItemProperties(PSItemByWfStateRequest request)
            throws  PSValidationException,WebApplicationException , PSPathServiceException
    {
        try {
            log.debug("Attempting to find item properties for: " + request.getPath() + ", " + request.getWorkflow()
                    + ", " + request.getState());

            return new PSItemPropertiesList(super.findItemProperties(request));
        } catch ( IPSDataService.DataServiceNotFoundException e) {
            throw new WebApplicationException(e);
        }
    }

    /**
     * The "path"s of the request are assumed from the "path" property of the
     * {@link com.percussion.pathmanagement.data.PSPathItem} object.
     * The path looks like:
     *      "/site/<site-name>/..." for a site item/folder
     *      "/Assets/<folder-name>/... for an item/folder under assets
     * However, the path-service is expecting path without leading "/"
     *      "site/<site-name>/..." for a site item/folder
     *      "Assets/<folder-name>/... for an item/folder under assets
     *
     * @param request
     */
    private void stripLeadingSlashForPaths(PSMoveFolderItem request)
    {
        String path = request.getTargetFolderPath();
        if (path.length() > 0 && path.charAt(0) == '/')
            request.setTargetFolderPath(path.substring(1));

        path = request.getItemPath();
        if (path.length() > 0 && path.charAt(0) == '/')
            request.setItemPath(path.substring(1));
    }

    /**
     *
     * @param path path of the folder or section
     * @return returns the site name from the path
     */
    private String getSiteNameFromFolderPath(String path)
    {
        String[] items = StringUtils.split(path, "/");
        return items[1];
    }


    /**
     * The log instance to use for this class, never <code>null</code>.
     */
    private static final Logger log = LogManager.getLogger(PSPathService.class);

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
    {
        this.applicationContext=applicationContext;

    }


    @Override
    // Break loop
    public void afterPropertiesSet() throws Exception
    {
        /*
        HashMap<String, IPSPathService> map = new HashMap<String, IPSPathService>();
        PSSitePathItemService site = applicationContext.getBean(PSSitePathItemService.class);
        PSAssetPathItemService assets = applicationContext.getBean(PSAssetPathItemService.class);
        PSDesignPathItemService design = applicationContext.getBean(PSDesignPathItemService.class);
        PSSearchPathItemService search = applicationContext.getBean(PSSearchPathItemService.class);
        map.put(site.getRootName(), site);
        map.put(assets.getRootName(), assets);
        map.put(design.getRootName(),design);
        map.put(search.getRootName(),search);
        setRegistry(map);
        */
    }


}
