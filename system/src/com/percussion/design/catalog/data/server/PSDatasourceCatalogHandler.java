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

package com.percussion.design.catalog.data.server;

import com.percussion.design.catalog.IPSCatalogErrors;
import com.percussion.design.catalog.IPSCatalogRequestHandler;
import com.percussion.design.catalog.PSCatalogRequestHandler;
import com.percussion.error.PSIllegalArgumentException;
import com.percussion.server.PSRequest;
import com.percussion.services.datasource.PSDatasourceMgrLocator;
import com.percussion.utils.jdbc.IPSDatasourceManager;
import com.percussion.utils.jdbc.PSConnectionDetail;
import com.percussion.utils.jdbc.PSConnectionInfo;
import com.percussion.utils.jdbc.PSJdbcUtils;
import com.percussion.xml.PSXmlDocumentBuilder;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class is the server-side implementation to handle requests generated by 
 * the {@link com.percussion.design.catalog.data.PSDatasourceCatalogHandler}.  
 * See that class for more info.
 */
public class PSDatasourceCatalogHandler extends PSCatalogRequestHandler
   implements
      IPSCatalogRequestHandler
{
   private static final Logger logger = LogManager.getLogger(PSDatasourceCatalogHandler.class);

   /**
    * Default ctor.
    */
   public PSDatasourceCatalogHandler()
   {
      super();
   }

   /**
    * Get the request type(s) (XML document types) supported by this handler.
    * 
    * @return the supported request type(s)
    */
   public String[] getSupportedRequestTypes()
   {
      return new String[] { REQUEST_DTD };
   }

   /**
    * Process a data related catalog request. This uses the input context
    * information and data. The results are written to the specified output
    * stream using the appropriate XML document format.  See 
    * {@link com.percussion.design.catalog.data.PSDatasourceCatalogHandler} for
    * the format.
    * 
    * @param request the request object containing all context data associated
    * with the request, may not be <code>null</code>.
    */
   @Override
   public void processRequest(PSRequest request)
   {
      Document doc = request.getInputDocument();
      Element root = null;
      if ((doc == null) || ((root = doc.getDocumentElement()) == null))
      {
         Object[] args = {REQUEST_CATEGORY, REQUEST_TYPE, REQUEST_DTD};
         createErrorResponse(request, new PSIllegalArgumentException(
            IPSCatalogErrors.REQ_DOC_MISSING, args));
         return;
      }
      String tAttrib = root.getAttribute("includeDbPubOnlySources");
      boolean inclDbPubSources = 
          tAttrib != null && tAttrib.equalsIgnoreCase("true");

      /* verify this is the appropriate request type */
      if (!REQUEST_DTD.equals(root.getTagName()))
      {
         Object[] args = {REQUEST_DTD, root.getTagName()};
         createErrorResponse(request, new PSIllegalArgumentException(
            IPSCatalogErrors.REQ_DOC_INVALID_TYPE, args));
         return;
      }

      Document retDoc = PSXmlDocumentBuilder.createXmlDocument();

      root = PSXmlDocumentBuilder.createRoot(retDoc, REQUEST_DTD + "Results");

      IPSDatasourceManager mgr = PSDatasourceMgrLocator.getDatasourceMgr();
      String defaultDs = mgr.getRepositoryDatasource();
      
      try
      {
         List<String> dsList = mgr.getDatasources();
         for (String dsName : dsList)
         {
            PSConnectionDetail detail = null;
            try
            {
               detail = mgr.getConnectionDetail(new PSConnectionInfo(dsName));
            }
            catch (Exception e)
            {
               // error occurred loading connection information
               logger.warn("Error occurred loading connection info for: " +
                     dsName, e);
               continue;
            }
            
            // detail should not be null since the mgr gave us the name
            if (detail == null)
            {
               throw new RuntimeException("No conn detail found for: " + 
                  dsName);
            }
            
            boolean isDbPubOnly = 
               PSJdbcUtils.getDbPubOnlyDrivers().contains(detail.getDriver());
            if(isDbPubOnly && !inclDbPubSources)
               continue;
            
            Element dsEl = PSXmlDocumentBuilder.addEmptyElement(doc, root, 
               "datasource");           
                        
            PSXmlDocumentBuilder.addElement(doc, dsEl, "name", dsName);
            PSXmlDocumentBuilder.addElement(doc, dsEl, "jndiDatasource", 
               detail.getDatasourceName());
            PSXmlDocumentBuilder.addElement(doc, dsEl, "jdbcUrl", 
               detail.getJdbcUrl());
            PSXmlDocumentBuilder.addElement(doc, dsEl, "database", 
               detail.getDatabase());
            PSXmlDocumentBuilder.addElement(doc, dsEl, "origin", 
               detail.getOrigin());
            PSXmlDocumentBuilder.addElement(doc, dsEl, "isRepository", 
               dsName.equals(defaultDs) ? "yes" : "no");
            PSXmlDocumentBuilder.addElement(doc, dsEl, "driver",
               detail.getDriver());
            PSXmlDocumentBuilder.addElement(doc, dsEl, "isDbPubOnly", 
               isDbPubOnly ? "yes" : "no");
         }

         /* send the result to the caller */
         sendXmlData(request, retDoc);
      }
      catch (Exception e)
      {
         createErrorResponse(request, e);
         return;
      }      
   }

   /**
    * Shutdown the request handler, freeing any associated resources.
    */
   @Override
   public void shutdown()
   {

   }
    
   /**
    * Constant for the request category of this handler
    */
   private static final String REQUEST_CATEGORY = "data";

   /**
    * Constant for the request type serviced by this handler
    */
   private static final String REQUEST_TYPE = "Datasource";

   /**
    * Constant for the root element name of the catalog request handled by this
    * cataloger.
    */
   private static final String REQUEST_DTD = "PSXDatasourceCatalog";
}
