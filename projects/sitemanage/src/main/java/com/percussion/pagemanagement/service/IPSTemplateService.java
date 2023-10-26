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
package com.percussion.pagemanagement.service;

import com.percussion.pagemanagement.data.PSHtmlMetadata;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.data.PSTemplate.PSTemplateTypeEnum;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.exception.PSBeanValidationException;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.utils.guid.IPSGuid;

import java.util.Collection;
import java.util.List;

/**
 * Provides various CRUD operations for template objects.
 *
 * @author YuBingChen
 */
public interface IPSTemplateService extends IPSDataService<PSTemplate, PSTemplateSummary, String>
{
    
    public PSTemplateSummary createNewTemplate(String plainBaseTemplateName, String templateName, String id) throws PSAssemblyException, PSDataServiceException;

    
    
   /**
        * The content type name of the page template.
        */
    public static final String TPL_CONTENT_TYPE = "percPageTemplate";


    /**
    * Finds all templates, which includes base and user created templates.
    * 
    * @return the templates, never <code>null</code>, but may be empty.
    */
   public List<PSTemplateSummary> findAll() throws IPSGenericDao.LoadException, PSTemplateException;

    String getTemplateEditUrl(String id);
    /**
    * Finds all templates from selected site, which includes base and user created templates.
    * 
    * @return the templates, never <code>null</code>, but may be empty.
    * @param siteName accepts the site name from PercSiteTemplatesController.js
    */
   public List<PSTemplateSummary> findAll(String siteName) throws IPSGenericDao.LoadException, PSTemplateException;
   
   /**
    * Finds all user created templates.
    * 
    * @return the template summaries, never <code>null</code>, may be empty.
    */
   public List<PSTemplateSummary> findAllUserTemplates() throws PSTemplateException;
   
   /**
    * Loads a list of user template summaries.
    *  
    * @param ids a list of IDs of the user templates, not <code>null</code>, but may be empty.
    * 
    * @return the loaded template summaries, not <code>null</code>, but may be empty
    */
   public List<PSTemplateSummary> loadUserTemplateSummaries(List<String> ids, String siteName) throws PSTemplateException;
   
   /**
    * Finds all base Templates based on the supplied type.
    * @param type The type of the base templates, may be null or empty. @see IPSTemplateDao for details
    * 
    * @return a list of requested Templates. It can never be <code>null</code>,
    * but may be empty. The order of the list is undetermined.
    */
   List<PSTemplateSummary> findBaseTemplates(String type);
   
   /**
    * Creates a template from a name and a specified source template.
    * 
    * @param name the name of the created template, may not be blank.
    * @param srcId the ID of the source template, may not be <code>null</code>
    * or empty.
    * 
    * @return the ID of the created template, never <code>null</code>.
    * 
    * @deprecated use {@link #createTemplate(String, String, String)} instead
    */
   PSTemplateSummary createTemplate(String name, String srcId) throws PSDataServiceException;

   /**
    * Save the specified template to the specified site.
    * 
    * @param template the to be saved template, not <code>null</code>.
    * @param siteId the ID of the site, it may be <code>null</code> assumed 
    * the template has already attached to a site.
    * 
    * @return the saved template, never <code>null</code>.
    * 
    * @throws PSBeanValidationException if there is any invalid properties in the template.
    * @throws com.percussion.share.service.IPSDataService.DataServiceSaveException if there is any unexpected error.
    */
   public PSTemplate save(PSTemplate template, String siteId) throws PSDataServiceException;
   
   /**
    * Save the specified template to the specified site.
    * 
    * @param template the to be saved template, not <code>null</code>.
    * @param siteId the ID of the site, it may be <code>null</code> assumed 
    * the template has already attached to a site.
    * @param pageId the ID of a page if the template being saved has been edited with a page and
    * content migration has been executed between the page and template, which requires the page to be 
    * validated to exist and to be checked out.
    * 
    * @return the saved template, never <code>null</code>.
    * 
    * @throws PSBeanValidationException if there is any invalid properties in the template.
    * @throws com.percussion.share.service.IPSDataService.DataServiceSaveException if there is any unexpected error.
    */
   public PSTemplate save(PSTemplate template, String siteId, String pageId) throws PSDataServiceException;
   
   /**
    * Creates a template from a name and a specified source template with no specific type
    * 
    * @param name the name of the created template, may not be blank.
    * @param srcId the ID of the source template, may not be <code>null</code>
    * or empty.
    * @param siteId the ID of the site that template belongs to, not blank.
    * 
    * @return the ID of the created template, never <code>null</code>.
    */
   PSTemplateSummary createTemplate(String name, String srcId, String siteId) throws PSDataServiceException;

   /**
    * Creates a template from a name and a specified source template using an specific type.
    * 
    * @param name the name of the created template, may not be blank.
    * @param srcId the ID of the source template, may not be <code>null</code>
    * or empty.
    * @param siteId the ID of the site that template belongs to, not blank.
    * @param type - the type of template to create
    * 
    * @return the ID of the created template, never <code>null</code>.
    */
   PSTemplateSummary createTemplate(String name, String srcId, String siteId, PSTemplateTypeEnum type) throws PSDataServiceException;

   /**
    * Finds the specified template.
    * 
    * @param id the ID of the template item, never <code>null</code>.
    * 
    * @return the template item. It may be <code>null</code> if cannot find one.
    */
   PSTemplateSummary find(String id) throws PSDataServiceException;
   

   /**
    * Loads the specified template.
    * 
    * @param id the ID of the specified template, never <code>null</code>.
    * 
    * @return the template with the specified ID, never <code>null</code>.
    */
   PSTemplate load(String id) throws PSDataServiceException;
   
   /**
    * Deletes the specified template if it is not used by any pages.
    * 
    * @param id the ID of the specified template, never blank.
    */
   void delete(String id) throws PSDataServiceException, PSNotFoundException;
   
   /**
    * This method is a wrapper to expose as a service the following method:
    * {@link com.percussion.pagemanagement.dao.IPSTemplateDao#getTemplateThumbPath(PSTemplateSummary, String)}
    * 
    * For additional information please refer to it.
    * 
    */
   public String getTemplateThumbPath(PSTemplateSummary summary, String siteName);

   /**
    * Returns PSTemplateMetadata object with the following Data set:
    * additional_head_content
    * code_insert_before_body_close
    * code_insert_after_body_start
    * 
    * @param id - long string version of template content id.
    * @return PSTemplateMetadata with the fields from template.  Fields may be empty.
    */
   public PSHtmlMetadata loadHtmlMetadata(String id) throws PSDataServiceException;
   
   /**
    * Saves/Replaces The following metadata field of a template
    * additional_head_content
    * code_insert_before_body_close
    * code_insert_after_body_start
    *  
    * @param metadata - Object must have id set.
    */
   public void saveHtmlMetadata(PSHtmlMetadata metadata) throws PSDataServiceException;
   
   
   /**
    * Deletes the specified template.
    * 
    * @param id the ID of the specified template, never blank.
    * @param force <code>true</code> to delete the template even if it is in use, <code>false</code> otherwise.
    */
   void delete(String id, boolean force) throws PSDataServiceException, PSNotFoundException;
   
   /**
    * Finds the user template with the specified name.
    * 
    * @param name never blank.
    * 
    * @return the template, never <code>null</code>.
    * @throws DataServiceLoadException if the template cannot be found.
    * 
    * @deprecated This is used by unit test only. It cannot be used by production code
    */
   public PSTemplateSummary findUserTemplateByName_UsedByUnitTestOnly(String name) throws PSDataServiceException;
   
   /**
    * Finds the user template for the specified name and site.
    * 
    * @param templateName the name of the template in question, not blank.
    * @param siteName the name of the site that template belongs to, not blank.
    * 
    * @return the ID of the template. It may be <code>null</code> if such template does not exist.
    */
   public IPSGuid findUserTemplateIdByName(String templateName, String siteName) throws PSValidationException, DataServiceLoadException;

   /**
    * Determines if the specified template is currently associated to any pages.
    * 
    * @param templateId never blank.
    * 
    * @return <code>true</code> if the template is being used by one or more pages, <code>false</code> otherwise.
    */
   public boolean isAssociatedToPages(String templateId) throws PSValidationException;

    /**
     * Returns List of ids of pages associated with template.
     *
     * @param templateId never blank.
     *
     * @return List of pageIds if the template is being used by one or more pages.
     */
    public Collection<Integer> getPageIdsForTemplate(String templateId);
   
   /**
    * Returns the template with the specified id and without the content
    * 
    * @param id the ID of the site that template belongs to, not blank
    * @param name the name of the template never blank.
    * 
    * @return the template, never <code>null</code>.
    * @throws DataServiceLoadException if the template cannot be found.
    */
   public PSTemplate exportTemplate(String id, String name) throws PSValidationException, PSTemplateException;
   
   
  
   
   /**
    * Import the specified template to the specified site.
    * 
    * @param template the to be saved template, not <code>null</code>.
    * @param siteId the ID of the site, it may be <code>null</code> assumed 
    * the template has already attached to a site.
    * 
    * @return the imported template, never <code>null</code>.
    * 
    * @throws PSBeanValidationException if there is any invalid properties in the template.
    * @throws com.percussion.share.service.IPSDataService.DataServiceSaveException if there is any unexpected error.
    */
   public PSTemplate importTemplate(PSTemplate template, String siteId) throws PSDataServiceException, IPSPathService.PSPathNotFoundServiceException;
   
   /**
    * (Runtime) Exception is thrown when an unexpected error occurs in this
    * service.
    */
   public static class PSTemplateException extends PSDataServiceException {

       /**
        * 
        */
       private static final long serialVersionUID = 1L;

       /**
        * Default constructor.
        */
       public PSTemplateException() {
           super();
       }

       /**
        * Constructs an exception with the specified detail message and the
        * cause.
        * 
        * @param message
        *            the specified detail message.
        * @param cause
        *            the cause of the exception.
        */
       public PSTemplateException(String message, Throwable cause) {
           super(message, cause);
       }

       /**
        * Constructs an exception with the specified detail message.
        * 
        * @param message
        *            the specified detail message.
        */
       public PSTemplateException(String message) {
           super(message);
       }

       /**
        * Constructs an exception with the specified cause.
        * 
        * @param cause
        *            the cause of the exception.
        */
       public PSTemplateException(Throwable cause) {
           super(cause);
       }
   }

}
