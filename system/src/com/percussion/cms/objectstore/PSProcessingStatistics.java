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
package com.percussion.cms.objectstore;

import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.util.PSXMLDomUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class contains the results from a processing request. It is immutable.
 *
 * @author Paul Howard
 * @version 1.0
 */
public class PSProcessingStatistics implements IPSCmsComponent
{
   /**
    * Creates a new instance with the supplied values.
    *
    * @param inserts How many rows were inserted in the db. >= 0.
    *
    * @param updates How many rows were updated in the db. >= 0.
    *
    * @param deletes How many rows were deleted from the db. >= 0.
    *
    * @param skips How many elements were not processed (probably because they
    *    didn't hava a dbaction parameter). >= 0.
    *
    * @param errors How many errors occurred. If the transaction level is set
    *    to row level, a non-zero value could be set here. >= 0.
    */
   public PSProcessingStatistics(int inserts, int updates, int deletes,
         int skips, int errors)
   {
      if (inserts < 0 || updates < 0 || deletes < 0 || skips < 0 || errors < 0)
      {
         throw new IllegalArgumentException(
               "Invalid value supplied for statistic, must be >= 0");
      }

      m_inserts = inserts;
      m_updates = updates;
      m_deletes = deletes;
      m_skips = skips;
      m_errors = errors;
   }

   /**
    * Creates stats from the PSXExecStatistics element returned by the server
    * as the result of an update request.
    *
    * @param doc the XML representation of the object. Never <code>null</code>.
    */
   public PSProcessingStatistics(Document doc)
      throws PSUnknownNodeTypeException
   {
      if (null == doc)
         throw new IllegalArgumentException("Source document cannot be null.");

      fromXml(doc);
   }


   /**
    * Creates stats from the PSXExecStatistics element returned by the server
    * as the result of an update request.
    *
    * @param src Never <code>null</code>.
    */
   public PSProcessingStatistics(Element src)
      throws PSUnknownNodeTypeException
   {
      if (null == src)
         throw new IllegalArgumentException("Source element cannot be null.");

      fromXml(src);
   }


   /**
    * Convenience method that calls {@link #PSProcessingStatistics(int,int,int,
    * int,int) this(0, 0, deletes, 0, errors)}.
    */
   public PSProcessingStatistics(int deletes, int errors)
   {
      this(0, 0, deletes, 0, errors);
   }

   /**
    * The number of rows inserted during the processing of this request. A row
    * is considered inserted if a new row is created in the db.
    *
    * @return >= 0
    */
   public int getInsertedCount()
   {
      return m_inserts;
   }


   /**
    * The number of rows updated during the processing of this request. A row
    * is considered updated if it was already in the db.
    *
    * @return >= 0
    */
   public int getUpdatedCount()
   {
      return m_updates;
   }


   /**
    * The number of rows deleted during the processing of this request.
    *
    * @return >= 0
    */
   public int getDeletedCount()
   {
      return m_deletes;
   }


   /**
    * The number of elements skipped during the processing of this request.
    * An element may be skipped if the db action for that element was not set.
    *
    * @return >= 0
    */
   public int getSkippedCount()
   {
      return m_skips;
   }


   /**
    * The number of rows failed during the processing of this request.
    *
    * @return >= 0
    */
   public int getErroredCount()
   {
      return m_errors;
   }



   /**
    * See {@link IPSCmsComponent#toXml(Document) interface} for description.
    * The document generated by this class conforms to the following dtd:
    * <pre><code>
    *    &lt;!ELEMENT getNodeName() EMPTY&gt;
    *    &lt;!ATTLIST getNodeName()
    *       inserts  #CDATA #REQUIRED
    *       updates  #CDATA #REQUIRED
    *       deletes  #CDATA #REQUIRED
    *       skips    #CDATA #REQUIRED
    *       errors   #CDATA #REQUIRED
    *       &gt;
    * </code></pre>
    */
   public Element toXml(Document doc)
   {
      if (null == doc)
         throw new IllegalArgumentException("Document cannot be null.");

      Element root = doc.createElement(getNodeName());
      root.setAttribute(XML_ATTR_INSERTS, "" + m_inserts);
      root.setAttribute(XML_ATTR_UPDATES, "" + m_updates);
      root.setAttribute(XML_ATTR_DELETES, "" + m_deletes);
      root.setAttribute(XML_ATTR_SKIPS, "" + m_skips);
      root.setAttribute(XML_ATTR_ERRORS, "" + m_errors);
      return root;
   }

   //see interface for description
   public String getNodeName()
   {
      String name = getClass().getName().substring(
            getClass().getName().lastIndexOf('.')+1);
      if ( name.startsWith("PS"))
         name = "PSX" + name.substring(2);
      return name;
   }


   //see interface for description
   public void fromXml(Element src)
      throws PSUnknownNodeTypeException
   {
      if (src == null)
         throw new IllegalArgumentException("Source element cannot be null.");

      PSXMLDomUtil.checkNode(src, getNodeName());

      m_inserts = PSXMLDomUtil.checkAttributeInt(src, XML_ATTR_INSERTS, false);
      if (m_inserts == -1)
         m_inserts = 0;

      m_updates = PSXMLDomUtil.checkAttributeInt(src, XML_ATTR_UPDATES, false);
      if (m_updates == -1)
         m_updates = 0;

      m_deletes = PSXMLDomUtil.checkAttributeInt(src, XML_ATTR_DELETES, false);
      if (m_deletes == -1)
         m_deletes = 0;

      m_skips = PSXMLDomUtil.checkAttributeInt(src, XML_ATTR_SKIPS, false);
      if (m_skips == -1)
         m_skips = 0;

      m_errors = PSXMLDomUtil.checkAttributeInt(src, XML_ATTR_ERRORS, false);
      if (m_errors == -1)
         m_errors = 0;
   }



   /**
    * Creates stats from the PSXExecStatistics element returned by the server
    * as the result of an update request.
    *
    * @param doc Never <code>null</code>.
    */
   public void fromXml(Document doc)
      throws PSUnknownNodeTypeException
   {
      if (doc == null)
         throw new IllegalArgumentException("Source document cannot be null.");

      Element src = doc.getDocumentElement();
      String nodeName = "PSXExecStatistics";
      PSXMLDomUtil.checkNode(src, nodeName);

      m_inserts = PSXMLDomUtil.checkAttributeInt(src, XML_ATTR_INSERTS, false);
      if (m_inserts == -1)
         m_inserts = 0;

      m_updates = PSXMLDomUtil.checkAttributeInt(src, XML_ATTR_UPDATES, false);
      if (m_updates == -1)
         m_updates = 0;

      m_deletes = PSXMLDomUtil.checkAttributeInt(src, XML_ATTR_DELETES, false);
      if (m_deletes == -1)
         m_deletes = 0;

      m_skips = PSXMLDomUtil.checkAttributeInt(src, XML_ATTR_SKIPS, false);
      if (m_skips == -1)
         m_skips = 0;

      m_errors = PSXMLDomUtil.checkAttributeInt(src, XML_ATTR_ERRORS, false);
      if (m_errors == -1)
         m_errors = 0;
   }


   //see interface for description
   public Object clone()
   {
      /**@todo: Implement this com.percussion.cms.objectstore.IPSCmsComponent method*/
      throw new java.lang.UnsupportedOperationException("Method clone() not yet implemented.");
   }


   //see interface for description
   @Override
   public boolean equals(Object obj)
   {
      /**@todo: Implement this com.percussion.cms.objectstore.IPSCmsComponent method*/
      throw new UnsupportedOperationException("Method equals() not yet implemented.");
   }

   /**
    * Generates code of the object. Overrides {@link Object#hashCode()}.
    */
   @Override
   public int hashCode()
   {
      throw new UnsupportedOperationException("Not Implemented");
   }

   /**
    * The number of rows inserted into the db by the resource that generated
    * these results.
    */
   private int m_inserts = 0;

   /**
    * The number of rows updated in the db by the resource that generated
    * these results.
    */
   private int m_updates = 0;

   /**
    * The number of rows removed from the db by the resource that generated
    * these results.
    */
   private int m_deletes = 0;

   /**
    * The number of elements skipped while walking the supplied xml document
    * during processing. This can happen if the db action isn't set properly.
    */
   private int m_skips = 0;

   /**
    * The number of errors that occurred. This may be non-zero if the
    * transaction level is set to row-level on the resource.
    */
   private int m_errors = 0;

   //Attribute names
   public static final String XML_ATTR_INSERTS = "inserts";
   public static final String XML_ATTR_UPDATES = "updates";
   public static final String XML_ATTR_DELETES = "deletes";
   public static final String XML_ATTR_SKIPS = "skips";
   public static final String XML_ATTR_ERRORS = "errors";

   public static final String XML_NODE_NAME =
         PSProcessingStatistics.class.getName().substring(
         PSProcessingStatistics.class.getName().lastIndexOf('.')+1);
}
