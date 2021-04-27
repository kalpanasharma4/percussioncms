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
package com.percussion.pagemanagement.dao.impl;

import static com.percussion.services.utils.orm.PSDataCollectionHelper.MAX_IDS;
import static com.percussion.util.PSSqlHelper.qualifyTableName;

import static org.apache.commons.lang.StringUtils.join;
import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.percussion.pagemanagement.dao.IPSPageDaoHelper;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pathmanagement.data.PSFolderProperties;
import com.percussion.searchmanagement.data.PSSearchCriteria;
import com.percussion.services.error.PSRuntimeException;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.PSWorkflowServiceLocator;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author miltonpividori
 *
 */
@Component("pageDaoHelper")
@Lazy
@Transactional(propagation = Propagation.SUPPORTS, noRollbackFor = Exception.class)
public class PSPageDaoHelper implements IPSPageDaoHelper
{
    private IPSContentWs contentWs;
    
    private IPSFolderHelper folderHelper;
    
    private IPSIdMapper idMapper;

    @Autowired
    private SessionFactory sessionFactory;
    
    
    @Autowired
    public PSPageDaoHelper(IPSContentWs contentWs, IPSFolderHelper folderHelper, IPSIdMapper idMapper)
    {
        this.contentWs = contentWs;
        this.folderHelper = folderHelper;
        this.idMapper = idMapper;
    }
    
    /*
     * (non-Javadoc)
     * @see com.percussion.pagemanagement.dao.IPSPageDaoHelper#setWorkflowAccordingToParentFolder(com.percussion.pagemanagement.data.PSPage)
     */
    public void setWorkflowAccordingToParentFolder(PSPage page)
    {
        notNull(page, "page cannot be null");
        notEmpty(page.getFolderPaths(), "page.folderpaths cannot be null");
        
        // Get the parent folder and set the correct workflow id for the new page
        page.setWorkflowId(getWorkflowIdForPath(page.getFolderPaths().get(0)));
    }
    
    public int getWorkflowIdForPath(String folderPath)
    {
        notEmpty(folderPath);
        
        int workflowId;
        IPSGuid parentFolderGuid = contentWs.getIdByPath(folderPath);
        if (parentFolderGuid != null)
        {
            PSFolderProperties parentFolder = folderHelper.findFolderProperties(idMapper.getString(parentFolderGuid));
            workflowId = folderHelper.getValidWorkflowId(parentFolder);
        }
        else
        {
            IPSWorkflowService workflowService = PSWorkflowServiceLocator.getWorkflowService();
            workflowId = workflowService.getDefaultWorkflowId().getUUID();
        }
        
        return workflowId;
    }
    
    @Transactional
    public Collection<Integer> findPageIdsByTemplate(String templateId)
    {
        Session sess = getSession();
        try
        {
            String sql = "select distinct CONTENTID from " + qualifyTableName("CT_PAGE") + " where TEMPLATEID ='"
                    + templateId + "'";
            
            SQLQuery query = sess.createSQLQuery(sql);
            return query.list();
        }
        catch (SQLException e)
        {
            String error = "Failed to get the fully qualified table name for 'CT_PAGE'";
            ms_logger.error(error, e);
            throw new PSRuntimeException(error, e);
        }
    }
    
    /* (non-Javadoc)
     * @see com.percussion.pagemanagement.dao.IPSPageDaoHelper#findPageIdsByTemplateInRecentRevision(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Transactional
    public Collection<Integer> findPageIdsByTemplateInRecentRevision(String deletedTemplate)
    {
        notEmpty(deletedTemplate);
        
        Session sess = getSession();
        try
        {
            String sql = "select distinct P.CONTENTID " +
                         "from " + qualifyTableName("CT_PAGE") + " as P " +
                         "inner join " + qualifyTableName("CONTENTSTATUS") + " as CS ON P.CONTENTID = CS.CONTENTID " +
                         "where TEMPLATEID = '" + deletedTemplate + "' " +
                         "    and (CS.CURRENTREVISION = P.REVISIONID " +
                         "         OR CS.TIPREVISION = P.REVISIONID) ";
            
            SQLQuery query = sess.createSQLQuery(sql);
            List<Integer> results = query.list();
            if(results == null) 
            {
                return new ArrayList<Integer>();
            }
            return query.list();
        }
        catch (SQLException e)
        {
            String error = "Failed to get the fully qualified table name for 'CT_PAGE'";
            ms_logger.error(error, e);
            throw new PSRuntimeException(error, e);
        }
        
    }
    
    /* (non-Javadoc)
     * @see com.percussion.pagemanagement.dao.IPSPageDaoHelper#replaceTemplateForPageInOlderRevisions(java.lang.String)
     */
    public void replaceTemplateForPageInOlderRevisions(String deletedTemplate)
    {
        notEmpty(deletedTemplate);
        
        // get the pages that we need to update
        Collection<Integer> pages = findPageIdsByTemplate(deletedTemplate);
        
        // get the new template to use for each page
        Map<String, String> mapPageToTemplate = findTemplateUsedByCurrentRevisionOfPages(new ArrayList<Integer>(pages));
        
        // make the update of each page revision
        replaceTemplateForPages(mapPageToTemplate, deletedTemplate);
    }

    /**
     * Update old pages revisions that used the deleted template to use the one
     * that is being use in the current revision of the page. 
     * 
     * @param mapPageToTemplate {@link Map}<{@link String}, {@link String}> with
     *            the pages ids and the templates ids to update.
     * @param deletedTemplate {@link String} with the id of the template that
     *            was deleted. Assumed not <code>null</code>.
     */
    @Transactional
    public void replaceTemplateForPages(Map<String, String> mapPageToTemplate, String deletedTemplate)
    {
        Session sess = getSession();
        try
        {
            String tableName = qualifyTableName("CT_PAGE");
            for(String pageToUpdate : mapPageToTemplate.keySet())
            {
                String sql = "UPDATE " + tableName + " "
                           + "SET TEMPLATEID = '" + mapPageToTemplate.get(pageToUpdate)+ "' "
                           + "WHERE CONTENTID = " + Integer.parseInt(pageToUpdate) + " " 
                           + "    AND TEMPLATEID = '"+ deletedTemplate +"' "; 

                SQLQuery query = sess.createSQLQuery(sql);
                query.executeUpdate();
            }
        }
        catch (SQLException e)
        {
            String error = "Failed to get the fully qualified table name for 'CT_PAGE' or 'CONTENTSTATUS'";
            ms_logger.error(error, e);
            throw new PSRuntimeException(error, e);
        }    
    }

    /* (non-Javadoc)
     * @see com.percussion.pagemanagement.dao.IPSPageDaoHelper#findTemplateUsedByCurrentRevisionOfPages(List<Integer)
     */
    public Map<String, String> findTemplateUsedByCurrentRevisionOfPages(List<Integer> pages)
    {
        if(isEmpty(pages))
        {
            return new HashMap<String, String>();
        }
        
        if (pages.size() < MAX_IDS)
        {
            return findTemplateUsedByCurrentRevision(pages);
        }
        else
        {
            // we need to paginate the query to avoid oracle problems
            Map<String, String> mapPageToTemplate = new HashMap<String, String>();
            for (int i = 0; i < pages.size(); i += MAX_IDS)
            {
                int end = (i + MAX_IDS > pages.size()) ? pages.size() : i + MAX_IDS;
                // make the query
                mapPageToTemplate.putAll(findTemplateUsedByCurrentRevision(pages.subList(i, end)));
            }
            return mapPageToTemplate;
        }
    }
    
    /* (non-Javadoc)
     * @see com.percussion.pagemanagement.dao.IPSPageDaoHelper#findImportedPageIdsByTemplate(java.util.Collection<Integer>, java.lang.String)
     */
    @Transactional(noRollbackFor = Exception.class)
    public Collection<Integer> findImportedPageIdsByTemplate(String templateId, List<Integer> pages )
    {
        
        if(isEmpty(pages))
        {
            return new ArrayList<Integer>();
        }
        
        if (pages.size() < MAX_IDS)
        {
            return findPageIdsByTemplateAndImportedPageIds(templateId, pages);
        }
        else
        {
            // we need to paginate the query to avoid oracle problems
            List<Integer> results =  new ArrayList<Integer>();
            for (int i = 0; i < pages.size(); i += MAX_IDS)
            {
                int end = (i + MAX_IDS > pages.size()) ? pages.size() : i + MAX_IDS;
                // make the query
                results.addAll(findPageIdsByTemplateAndImportedPageIds(templateId, pages.subList(i, end)));
            }
            return results;
        }
    }

    /**
     * Makes the query to find the template used by pages in the current
     * revision. 
     * 
     * @param pages {@link List}<{@link Integer}> with the ids of the pages we
     *            need to retrieve. Assumed not <code>null</code>.
     * @return {@link Map}<{@link String}, {@link String}> never
     *         <code>null</code> but may be empty.
     */
    @Transactional
    public Map<String, String> findTemplateUsedByCurrentRevision(List<Integer> pages)
    {
        Map<String, String> mapPageToTemplate = new HashMap<String, String>();

        Session sess = getSession();
        try
        {
            String sql = "SELECT P.CONTENTID, P.TEMPLATEID " 
                       + "FROM " + qualifyTableName("CT_PAGE")
                       + " AS P INNER JOIN " + qualifyTableName("CONTENTSTATUS") 
                       + " AS CS ON P.CONTENTID = CS.CONTENTID "
                       + "WHERE P.CONTENTID IN (" + join(pages, ",") + ") "
                       + "    AND CS.CURRENTREVISION = P.REVISIONID ";

            SQLQuery query = sess.createSQLQuery(sql);
            List<Object[]> results = query.list();
            for(Object[] row : results)
            {
                mapPageToTemplate.put(row[0].toString(), row[1].toString());
            }
            return mapPageToTemplate;
        }
        catch (SQLException e)
        {
            String error = "Failed to get the fully qualified table name for 'CT_PAGE' or 'CONTENTSTATUS'";
            ms_logger.error(error, e);
            throw new PSRuntimeException(error, e);
        }
    }
    
    /**
     * Makes the query to find the imported page ids that are using the
     * unassigned template Id.
     * 
     * @param templateId {@link String} with the template id being used. Must
     *            not be blank.
     * @param pages {@link List}<{@link Integer}> with the ids of the pages we
     *            need to retrieve. Assumed not <code>null</code>.
     * @return {@link Map}<{@link String}, {@link String}> never
     *         <code>null</code> but may be empty.
     */
    @SuppressWarnings("unchecked")
    @Transactional(noRollbackFor = Exception.class)
    public Collection<Integer> findPageIdsByTemplateAndImportedPageIds(String templateId, List<Integer> pages)
    {
        Session sess = getSession();
        try
        {
            String sql = "select distinct CONTENTID from " + qualifyTableName("CT_PAGE") + " where TEMPLATEID ='"
                    + templateId + "' AND CONTENTID in (" + join(pages, ",") + ") ";
           
            SQLQuery query = sess.createSQLQuery(sql);
            return query.list();
        }
        catch (SQLException e)
        {
            String error = "Failed to get the fully qualified table name for 'CT_PAGE'";
            ms_logger.error(error, e);
            throw new PSRuntimeException(error, e);
        }
     
    }
    
    public Map<String, String> findLinkTextForCurrentRevisionOfPages(List<Integer> pages)
    {
        if(isEmpty(pages))
        {
            return new HashMap<String, String>();
        }
        
        if (pages.size() < MAX_IDS)
        {
            return findLinkTextForCurrentRevision(pages);
        }
        else
        {
            // we need to paginate the query to avoid oracle problems
            Map<String, String> mapPageToLinkText = new HashMap<String, String>();
            for (int i = 0; i < pages.size(); i += MAX_IDS)
            {
                int end = (i + MAX_IDS > pages.size()) ? pages.size() : i + MAX_IDS;
                // make the query
                mapPageToLinkText.putAll(findLinkTextForCurrentRevision(pages.subList(i, end)));
            }
            return mapPageToLinkText;
        }
    }
    
    @Transactional
    public Map<String, String> findLinkTextForCurrentRevision(List<Integer> pages)
    {
        Map<String, String> mapPageToLinkText = new HashMap<String, String>();

        Session sess = getSession();
        try
        {
            String sql = "SELECT P.CONTENTID, P.RESOURCE_LINK_TITLE " 
                       + "FROM " + qualifyTableName("CT_PAGE")
                       + " AS P INNER JOIN " + qualifyTableName("CONTENTSTATUS") 
                       + " AS CS ON P.CONTENTID = CS.CONTENTID "
                       + "WHERE P.CONTENTID IN (" + join(pages, ",") + ") "
                       + "    AND CS.CURRENTREVISION = P.REVISIONID ";

            SQLQuery query = sess.createSQLQuery(sql);
            List<Object[]> results = query.list();
            for(Object[] row : results)
            {
                mapPageToLinkText.put(row[0].toString(), row[1].toString());
            }
            return mapPageToLinkText;
        }
        catch (SQLException e)
        {
            String error = "Failed to get the fully qualified table name for 'CT_PAGE' or 'CONTENTSTATUS'";
            ms_logger.error(error, e);
            throw new PSRuntimeException(error, e);
        }
    }
    
    private Session getSession()
    {
        return this.sessionFactory.getCurrentSession();
    }
    
   
    private static Logger ms_logger = Logger.getLogger(PSPageDaoHelper.class);

    @Transactional(noRollbackFor = Exception.class)
    public Collection<Integer> findContentIdsByTemplateAndImportedPageIds(PSSearchCriteria criteria, List<Integer> contentIDs)
    {
        Session sess = getSession();
        try
        {
            String sql = "";
            if(contentIDs.isEmpty()){
                contentIDs.add(0);
            }
            if(criteria.getFolderPath().contains("Assets")){
                sql = "select CS.CONTENTID from " + qualifyTableName("CONTENTSTATUS")
                        + " AS CS WHERE CS.CONTENTID IN (" + join(contentIDs, ",") + ") ";
                sql = formGetByStatusSQLQuery(criteria, sql);
            }else{
                sql = "SELECT DISTINCT P.CONTENTID "
                        + "FROM " + qualifyTableName("CT_PAGE")
                        + " AS P INNER JOIN " + qualifyTableName("CONTENTSTATUS")
                        + " AS CS ON P.CONTENTID = CS.CONTENTID "
                        + " WHERE P.CONTENTID IN (" + join(contentIDs, ",") + ") ";
                sql = formGetByStatusSQLQuery(criteria, sql);
            }

            SQLQuery query = sess.createSQLQuery(sql);
            return query.list();
        }
        catch (SQLException e)
        {
            String error = "Failed to get the fully qualified table name for 'CT_PAGE'";
            ms_logger.error(error, e);
            throw new PSRuntimeException(error, e);
        }
    }

    private String formGetByStatusSQLQuery(PSSearchCriteria criteria, String sql){

        if(criteria.getSearchFields().containsKey("templateid")){
            sql = sql + " AND P.TEMPLATEID='"+criteria.getSearchFields().get("templateid")+"'";
        }
        if(criteria.getSearchFields().containsKey("sys_contenttypeid")){
            sql = sql + " AND CS.CONTENTTYPEID="+criteria.getSearchFields().get("sys_contenttypeid");
        }
        if(criteria.getSearchFields().containsKey("sys_contentstateid")){
            sql = sql + " AND CS.CONTENTSTATEID="+criteria.getSearchFields().get("sys_contentstateid");
        }
        if(criteria.getSearchFields().containsKey("sys_workflowid")){
            sql = sql + " AND CS.WORKFLOWAPPID="+criteria.getSearchFields().get("sys_workflowid");
        }
        if(criteria.getSearchFields().containsKey("sys_contentlastmodifier")){
            sql = sql + " AND CS.CONTENTLASTMODIFIER LIKE '%"+criteria.getSearchFields().get("sys_contentlastmodifier")+"%'";
        }
        return  sql;
    }

}
