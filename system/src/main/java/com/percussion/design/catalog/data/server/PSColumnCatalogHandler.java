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

package com.percussion.design.catalog.data.server;

import com.percussion.design.catalog.IPSCatalogErrors;
import com.percussion.design.catalog.IPSCatalogRequestHandler;
import com.percussion.error.PSIllegalArgumentException;
import com.percussion.server.PSRequest;
import com.percussion.utils.jdbc.IPSConnectionInfo;
import com.percussion.utils.jdbc.PSConnectionDetail;
import com.percussion.utils.jdbc.PSConnectionHelper;
import com.percussion.utils.jdbc.PSConnectionInfo;
import com.percussion.xml.PSXmlDocumentBuilder;
import com.percussion.xml.PSXmlTreeWalker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * The PSColumnCatalogHandler class implements cataloging of 
 * columns. This request type is used to locate the columns 
 * defined in a specific back-end table.
 * <p>
 * The request format is defined in the
 * {@link com.percussion.design.catalog.data.PSColumnCatalogHandler} class.
 */
public class PSColumnCatalogHandler
   extends com.percussion.design.catalog.PSCatalogRequestHandler
   implements IPSCatalogRequestHandler
{
   /**
    * Constructs an instance of this handler.
    */
   public PSColumnCatalogHandler()
   {
      super();
   }


   /* ********  IPSCatalogRequestHandler Interface Implementation ******** */

   /**
    * Get the request type(s) (XML document types) supported by this
    * handler.
    * 
    * @return      the supported request type(s)
    */
   public String[] getSupportedRequestTypes()
   {
      return new String[] { "PSXColumnCatalog" };
   }


   /* ************ IPSRequestHandler Interface Implementation ************ */

   /**
    * Process the catalog request. This uses the XML document sent as the input
    * data. The results are written to the specified output stream using the
    * appropriate XML document format.
    * 
    * @param request the request object containing all context data associated
    * with the request
    */
   public void processRequest(PSRequest request)
   {
      Document doc = request.getInputDocument();
      Element root = null;
      if ((doc == null) || ((root = doc.getDocumentElement()) == null))
      {
         Object[] args = {ms_RequestCategory, ms_RequestType, ms_RequestDTD};
         createErrorResponse(request, new PSIllegalArgumentException(
            IPSCatalogErrors.REQ_DOC_MISSING, args));
         return;
      }

      /* verify this is the appropriate request type */
      if (!ms_RequestDTD.equals(root.getTagName()))
      {
         Object[] args = {ms_RequestDTD, root.getTagName()};
         createErrorResponse(request, new PSIllegalArgumentException(
            IPSCatalogErrors.REQ_DOC_INVALID_TYPE, args));
         return;
      }

      PSXmlTreeWalker tree = new PSXmlTreeWalker(doc);

      String datasource = tree.getElementData("datasource");
      String table = tree.getElementData("tableName");

      Document retDoc = PSXmlDocumentBuilder.createXmlDocument();

      root = PSXmlDocumentBuilder.createRoot(retDoc, "PSXColumnCatalogResults");

      PSXmlDocumentBuilder.addElement(retDoc, root, "driverName", 
         datasource == null ? "" : datasource);

      if (table != null)
         PSXmlDocumentBuilder.addElement(retDoc, root, "tableName", table);

      Element colNode;
      String colName, colBeType, colJdbcType, colSize, colFraction, colNull;

      Connection conn;
      IPSConnectionInfo connInfo = new PSConnectionInfo(datasource);
      PSConnectionDetail detail;
      try
      {
         conn = PSConnectionHelper.getDbConnection(connInfo);
         detail = PSConnectionHelper.getConnectionDetail(connInfo);
      }
      catch (Exception e)
      {
         createErrorResponse(request, e);
         return;
      }

      try
      {
         DatabaseMetaData meta = conn.getMetaData();
         ResultSet rs = meta.getColumns(detail.getDatabase(), 
            detail.getOrigin(), table, "%");
         if (rs != null)
         {
            while (rs.next())
            {
               /* THESE MUST BE READ IN THE CORRECT SEQUENCE !!! */
               colName = rs.getString(COLNO_COLUMN_NAME);
               colJdbcType = rs.getString(COLNO_JDBC_TYPE);
               colBeType = rs.getString(COLNO_BE_TYPE);
               colSize = rs.getString(COLNO_COLUMN_SIZE);
               colFraction = rs.getString(COLNO_COLUMN_FRACTION);
               colNull = rs.getString(COLNO_ALLOWS_NULL);

               colNode = PSXmlDocumentBuilder.addEmptyElement(retDoc, root,
                  "Column");

               PSXmlDocumentBuilder
                  .addElement(retDoc, colNode, "name", colName);

               PSXmlDocumentBuilder.addElement(retDoc, colNode,
                  "backEndDataType", colBeType);

               PSXmlDocumentBuilder.addElement(retDoc, colNode, "jdbcDataType",
                  colJdbcType);

               if ((colFraction != null) && !colFraction.equals("0"))
                  colSize += "." + colFraction;
               PSXmlDocumentBuilder
                  .addElement(retDoc, colNode, "size", colSize);

               colNull = colNull.toLowerCase();
               if (!colNull.equals("yes") && !colNull.equals("no"))
                  colNull = "unknown";
               PSXmlDocumentBuilder.addElement(retDoc, colNode, "allowsNull",
                  colNull);
            }

            rs.close();
         }

         /* and send the result to the caller */
         sendXmlData(request, retDoc);
      }
      catch (SQLException e)
      {
         // if this feature is not supported, we really can't use the driver
         // in this case, we will always treat it as an error
         createErrorResponse(request, e);
      }
      finally
      {
         try
         {
            conn.close();
         }
         catch (SQLException e)
         {
         }
      }
   }

   /**
    * Shutdown the request handler, freeing any associated resources.
    */
   public void shutdown()
   {
      /* nothing to do here */
   }

   private static final String   ms_RequestCategory   = "data";
   private static final String   ms_RequestType         = "Column";
   private static final String   ms_RequestDTD         = "PSXColumnCatalog";

   private static final int COLNO_COLUMN_NAME      = 4;
   private static final int COLNO_JDBC_TYPE         = 5;
   private static final int COLNO_BE_TYPE            = 6;
   private static final int COLNO_COLUMN_SIZE      = 7;
   private static final int COLNO_COLUMN_FRACTION   = 9;
   private static final int COLNO_ALLOWS_NULL      = 18;
}

