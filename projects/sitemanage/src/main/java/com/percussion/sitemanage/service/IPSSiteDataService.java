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
package com.percussion.sitemanage.service;

import com.percussion.foldermanagement.service.IPSFolderService;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.pubserver.data.PSPubServer;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.data.PSEnumVals;
import com.percussion.share.data.PSMapWrapper;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSSpringValidationException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.validation.PSValidationErrors;
import com.percussion.sitemanage.data.*;
import com.percussion.sitemanage.error.PSSiteImportException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.util.List;

/**
 * CRUDS sites.
 * 
 * @author adamgent
 *
 */
public interface IPSSiteDataService extends IPSDataService<PSSite,PSSiteSummary, String> {
    /**
     * The publishing type. Used to indicate which mechanism to be used to
     * publish to the live site.
     */
    public enum PublishType{
        /**
         * Publishing defaults to local
         */
        filesystem,
        /**
         * Publishing will be done via FTP
         */
        ftp,
        /**
         * Publishing will be done via SFTP
         */
        sftp,
        /**
         * publishing will be done to database
         */
        database,
        /**
         * Publishing defaults to local and avoid meta data indexer
         */
        filesystem_only,
        /**
         * Publishing will be done via FTP and avoid meta data indexer
         */
        ftp_only,
        /**
         * Publishing will be done via FTPS and avoid meta data indexer
         */
        ftps,
        /**
         * Publishing will be done via FTPS and avoid meta data indexer
         */
        ftps_only,
        /**
         * Publishing will be done via SFTP and avoid meta data indexer
         */
        sftp_only,
        /**
         * Publishing will be done using amazon api
         */
        amazon_s3,
        /**
         * Publishing will be done via amazon api and avoid meta data indexer
         */
        amazon_s3_only;
        
        
        public boolean isFtpType()
        {
            return equals(ftp) || equals(ftp_only) || equals(sftp) || equals(sftp_only) || equals(ftps) || equals(ftps_only);
        }
     }
    
    public PSSiteSummary find(String id) throws DataServiceLoadException, PSValidationException, IPSGenericDao.LoadException;

    /**
     * Finds the site and adds the publishing info based on the includePubInfo flag.
     * @param id site name
     * @param includePubInfo if <code>true</code> adds the publishing info, the value may be <code>null</code>
     * @return site summary 
     * @throws DataServiceLoadException
     */
    public PSSiteSummary find(String id, boolean includePubInfo) throws DataServiceLoadException, PSValidationException, IPSGenericDao.LoadException;
    
    /**
     * Finds the site summary by the legacy ID.
     * 
     * @param id the legacy ID of the site, not <code>null</code>.
     * @param isValidate it is <code>true</code> if wants to validate the site that contains "category" folder;
     * otherwise don't validate the returned site object. The returned object should not to be validated if it is
     * used for assembly process, such as previewing or publishing. 
     * 
     * @return the site with the specified ID, never <code>null</code>.
     * 
     * @throws DataServiceLoadException if cannot find the specified site.
     */
    public PSSiteSummary findByLegacySiteId(String id, boolean isValidate) throws DataServiceLoadException, PSValidationException;
    
    public PSSiteSummary findByPath(String path) throws DataServiceNotFoundException, PSValidationException;

    public List<PSSiteSummary> findAll();

    /***
     * Gets the publishing server info for the supplied siteid
     * @param siteId
     * @return may be null or a publishing server
     */
    public PSPubInfo getS3PubServerInfo(long siteId);
    /**
     * Returns the site summaries, adds the publishing info if includePubInfo is <code>true</code>.
     * @param includePubInfo if <code>true</code> adds the publishing info, the value may be <code>null</code>
     * if the site's default server is not amazon s3 server.
     * @return List<PSSiteSummary> of all sites.
     */
    public List<PSSiteSummary> findAll(boolean includePubInfo);

    public void delete(String id) throws PSDataServiceException;

    /**
     * Creates the specified site and its related components.
     * 
     * @param site the new site, not <code>null</code> and the name of the site
     * cannot be one of the existing site.
     * 
     * @return the created site, never <code>null</code>.
     */
    public PSSite save(PSSite site) throws PSDataServiceException;
    
    /**
     * Creates the specified site importing its content from an external URL.
     * Its related components are also created.
     * 
     * @param request the {@link HttpServletRequest} object that represents that request.
     * @param site the new site with only name and baseUrl set. Not
     *            <code>null</code> and the name of the site cannot be one of
     *            the existing site.
     * 
     * @return the created site, never <code>null</code>.
     * @throws PSSiteImportException if an unexpected error occurred during site
     *             import.
     * @deprecated use the async method instead.
     */
    @Deprecated
    public PSSite createSiteFromUrl(@Context HttpServletRequest request, PSSite site) throws PSSiteImportException, PSValidationException;
    
    /**
     * Creates the specified site importing its content from an external URL.
     * Its related components are also created.
     * 
     * @param request the {@link HttpServletRequest} object that represents that request.
     * @param config the import configuration for the new site with only name and baseUrl set. Not
     *            <code>null</code> and the name of the site cannot be one of
     *            the existing site.
     * 
     * @return the job id of the running job that is importing the site, never
     *         <code>null</code>.
     * @throws PSSiteImportException if an unexpected error occurred during site
     *             import.
     */
    public Long createSiteFromUrlAsync(@Context HttpServletRequest request, PSSiteImportConfiguration config) throws PSValidationException, IPSFolderService.PSWorkflowNotFoundException;
    
    /**
     * Once the import from url job is completed, it gets the site that was
     * imported in the process.
     * 
     * @param jobId the id of the job that was running to import the site. This
     *            id is the same that is returned by the method
     *            createSiteFromUrl.
     * @return the imported site, can be <code>null</code> if the job hasn't
     *         finished, or if it wasn't possible to retrieve result from the
     *         job.
     */
    public PSSite getImportedSite(Long jobId);

    public PSValidationErrors validate(PSSite site) throws PSValidationException;

    /**
     * Gets the properties of the specified site.
     * 
     * @param siteName the name of the site, not blank.
     * 
     * @return the specified site properties, never <code>null</code>.
     */
    public PSSiteProperties getSiteProperties(String siteName) throws IPSSiteSectionService.PSSiteSectionException, PSValidationException, PSNotFoundException;
    
    /**
     * Updates the specified site properties.
     * 
     * @param props the new properties of the site, never <code>null</code>
     * 
     * @return the updated properties, never <code>null</code>.
     */
    public PSSiteProperties updateSiteProperties(PSSiteProperties props) throws PSDataServiceException, PSNotFoundException;
    
    /**
     * Gets the publishing properties of the specified site
     * @param siteName name of the site, not blank
     * @return sites publishing properties never <code>null</code>.
     */
    public PSSitePublishProperties getSitePublishProperties(String siteName) throws PSValidationException, PSNotFoundException;
   
    /**
     * Updates the specified site with publish properties, the specified site is
     * in publishprops
     * 
     * @param publishProps publish properties of the site. never
     *            <code>null</code>
     * @return the updated publish properties never <code>null</code>.
     * @throws IOException if an error takes place when handling the secure
     *             configuration files.
     */
    public PSSitePublishProperties updateSitePublishProperties(PSSitePublishProperties publishProps) throws DataServiceSaveException, PSNotFoundException;
    
    /**
     * Finds all choices.  A choice is comprised of a value (site name).  See {@link PSEnumVals.EnumVal} for details.
     *
     * @return the choices as a {@link PSEnumVals} object, never <code>null</code>.
     */
    public PSEnumVals getChoices();
    
    /**
     * Finds all choices.  A choice is comprised of a value (site name).  See {@link PSEnumVals.EnumVal} for details.
     *
     * @return the choices as a {@link PSEnumVals} object, never <code>null</code>.
     */
    
    /**
     * The site name of the source and target site that is currently getting copied.
     * Empty map if a copy is not currently running.
     * 
     * @return Source and Target in a {@link PSMapWrapper} object.
     */
    public PSMapWrapper getCopySiteInfo();
    
    /**
     * Create a full publish edition for the specified publish server.
     * 
     * @param site the site in question, not <code>null</code>.
     * @param pubServer the pubServer associated to the edition, not <code>null</code>.
     * @param isDefaultServer boolean flag that indicates whether the publish server is the default, not <code>null</code>.
     * 
     */
    public void createPublishingItemsForPubServer(IPSSite site, PSPubServer pubServer, boolean isDefaultServer) throws PSNotFoundException;

    /**
     * Update the publish edition to set the default publish server.
     * 
     * @param site the site in question, not <code>null</code>.
     * @param pubServer the pubServer associated to the edition, not <code>null</code>.
     * 
     */
    public void setPublishServerAsDefault(IPSSite site, PSPubServer pubServer) throws PSNotFoundException;
    
    /**
     * Update the publish edition to set the default publish server.
     * 
     * @param site the site in question, not <code>null</code>.
     * @param publishServerType the publish type for the server, not <code>null</code>.
     * @param deliveryRootPath the root path, not <code>null</code>.
     * 
     * @return the default publishing root for the publishing server, never <code>null</code>.
     */
    String getDefaultPublishingRoot(IPSSite site, String publishServerType, String deliveryRootPath);
    
    /**
     * Return the base path from the specified publishing root location.
     * 
     * @param serverRootPath the publishing root location, never blank.
     * @param siteName never blank.
     * 
     * @return the base path, never <code>null</code>.
     */
    String getBasePublishingRoot(String serverRootPath, String siteName);
    
    /**
     * Delete all the publishing items associated to the specified publish server.
     * 
     * @param pubServer the pubServer associated to the edition, not <code>null</code>.
     * 
     */
    public void deletePublishingItemsByPubServer(PSPubServer pubServer) throws PSNotFoundException;
    
    /**
     * Update a full publish edition for the specified publish server.
     * 
     * @param site the site in question, not <code>null</code>.
     * @param pubServer the pubServer associated to the edition, not <code>null</code>.
     * @param oldServer the old pubServer needed to check previous settings, not <code>null</code>.
     * @param isDefaultServer boolean flag that indicates whether the publish server is the default, not <code>null</code>.
     * 
     */
    public void updateServerEditions(IPSSite site, PSPubServer oldServer, PSPubServer pubServer, boolean isDefaultServer) throws PSNotFoundException;
    
    /**
     * Gets the statistics of the specified site.
     * 
     * @param siteId id of the site, not blank
     * @return site statistics never <code>null</code>.
     */
    public PSSiteStatisticsSummary getSiteStatistics(String siteId) throws PSDataServiceException;
   
    
    /**
     * Loads all the json files from {@value #SAAS_SITE_CONFIG_FOLDER_PATH} folder and
     * if the file is a valid site config json file then returns the map of site name
     * and file name.
     * @param filterUsedSites if a site exists for a configuration then it will be filtered 
     * if this flag is set to <code>true</code>.
     * @return map of site names and config file names.
     */
    public PSMapWrapper getSaaSSiteNames(boolean filterUsedSites) throws DataServiceLoadException;
   
    public PSSaasSiteConfig getSaasSiteConfig(String siteName) throws DataServiceLoadException;
    
    public String isSiteBeingImported(String sitename) throws PSDataServiceException;

    /***
     * Finds a site by name;
     *
     * @param name  The site name, never null
     * @return the Site
     */
    public PSSiteSummary findByName(String name) throws DataServiceLoadException, PSValidationException;

    /**
     * Constant for the folder path where the site configuration is stored.
     */
    public static final String SAAS_SITE_CONFIG_FOLDER_PATH = "/var/config/saas/siteconfig";
}
