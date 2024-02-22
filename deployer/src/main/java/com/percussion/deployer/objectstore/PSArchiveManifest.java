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

package com.percussion.deployer.objectstore;


import com.percussion.design.objectstore.IPSObjectStoreErrors;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.xml.PSXmlDocumentBuilder;
import com.percussion.xml.PSXmlTreeWalker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.*;

/**
 * Encapsulates a list of <code>PSDependencyFile</code> and
 * <code>PSApplicationIDTypes</code> objects (in pairs), and its related
 * <code>PSDependency</code> object (via its key).
 */
public class PSArchiveManifest  implements IPSDeployComponent
{
   /**
    * Default constructor.
    */
   public PSArchiveManifest()
   {
   }

   /**
    * Create this object from its XML representation
    *
    * @param source The source element.  See {@link #toXml(Document)} for
    * the expected format.  May not be <code>null</code>.
    *
    * @throws IllegalArgumentException If <code>source</code> is
    * <code>null</code>.
    * @throws PSUnknownNodeTypeException <code>source</code> is malformed.
    */
   public PSArchiveManifest(Element source) throws PSUnknownNodeTypeException
   {
      if (source == null)
         throw new IllegalArgumentException("source may not be null");

      fromXml(source);
   }

   /**
    * Adds a list of <code>PSDependencyFile</code> objects for the given
    * <code>PSDependency</code>. If the given <code>PSDependency</code>
    * already has a list of <code>PSDependencyFile</code> objects in the
    * current object, then do nothing.
    *
    * @param    dep The <code>PSDependency</code> object which relates to
    * the list of <code>PSDependencyFile</code> objects. It may not be
    * <code>null</code>.
    * @param    depfiles    An iterator over one or more
    * <code>PSDependencyFile</code> objects, it may not be <code>null</code>
    * or empty.
    *
    * @throws IllegalArgumentException If any param is invalid.
    */
   public void addFiles(PSDependency dep, Iterator<PSDependencyFile> depfiles)
   {
      if (dep == null)
         throw new IllegalArgumentException(DEP_NOT_NULL_MSG);
      if (depfiles == null || (!depfiles.hasNext()))
         throw new IllegalArgumentException(
            "depfiles may not be null or empty");

      // make a new list from the Iterator.
      List<PSDependencyFile> depFileList = new ArrayList<>();
      while (depfiles.hasNext())
         depFileList.add(depfiles.next());

      DepFilesIdTypes value = m_depMap.get(dep.getKey());
      if ( value == null )
      {
         value = new DepFilesIdTypes(dep.getKey(), depFileList, null, null);
         m_depMap.put(value.m_key, value);
      }
      else if ( value.m_depFiles == null )
      {
         value.m_depFiles = depFileList;
      }
   }

   /**
    * Get a list of <code>PSDependencyFile</code> objects for a given
    * <code>PSDependency</code> object.
    *
    * @param    dep The <code>PSDependency</code> object. It may not be
    * <code>null</code>.
    *
    * @return an Iterator over zero or more <code>PSDependencyFile</code>
    * objects. It will never be <code>null</code>.
    *
    * @throws IllegalArgumentException If any param is invalid.
    */
   public Iterator<PSDependencyFile> getFiles(PSDependency dep)
   {
      if (dep == null)
         throw new IllegalArgumentException(DEP_NOT_NULL_MSG);

      DepFilesIdTypes value = m_depMap.get(dep.getKey());
      if (value == null || value.m_depFiles == null )
         return Collections.emptyIterator();
      else
         return value.m_depFiles.iterator();
   }

   /**
    * Get a list of all <code>PSDependencyFile</code> objects in this
    * manifest.
    *
    * @return an Iterator over zero or more <code>PSDependencyFile</code>
    * objects. It will never be <code>null</code>.
    */
   public Iterator<PSDependencyFile> getFiles()
   {
      List<PSDependencyFile> files = new ArrayList<>();

      for (Map.Entry<String, DepFilesIdTypes> entry : m_depMap.entrySet()) {
         DepFilesIdTypes value = m_depMap.get(entry.getKey());
         if (value != null && value.m_depFiles != null) {
            files.addAll(value.m_depFiles);
         }
      }
      
      return files.iterator();
   }
   
   /**
    * Determines if the object contains a given <code>PSDependency</code>
    * and its related <code>PSDependencyFile</code>.
    *
    * @param    dep The <code>PSDependency</code> object. It may not be
    * <code>null</code>.
    *
    * @return <code>true</code> if the given <code>PSDependency</code> has its
    * related <code>PSDependencyFile</code> object; <code>false</code>
    * otherwise.
    *
    * @throws IllegalArgumentException If any param is invalid.
    */
   public boolean hasDependencyFiles(PSDependency dep)
   {
      if (dep == null)
         throw new IllegalArgumentException(DEP_NOT_NULL_MSG);

      DepFilesIdTypes value =  m_depMap.get(dep.getKey());
      return value != null && value.m_depFiles != null;
   }

   /**
    * Adds <code>PSApplicationIDTypes</code> for its related
    * <code>PSDependency</code> object. If this pair relationship already
    * exist, do nothing.
    *
    * @param    dep The <code>PSDependency</code> object. It may not be
    * <code>null</code>.
    * @param    idTypes The to be added <code>PSApplicationIDTypes</code>.
    * It may not be <code>null</code>.
    *
    * @throws IllegalArgumentException If any param is invalid.
    */
   public void addIdTypes(PSDependency dep, PSApplicationIDTypes idTypes)
   {
      if (dep == null)
         throw new IllegalArgumentException(DEP_NOT_NULL_MSG);
      if (idTypes == null)
        return;

      DepFilesIdTypes value =  m_depMap.get(dep.getKey());
      if ( value == null )
      {
         value = new DepFilesIdTypes(dep.getKey(), null, idTypes, null);
         m_depMap.put(value.m_key, value);
      }
      else if ( value.m_idtypes == null )
      {
         value.m_idtypes = idTypes;
      }
   }
   
   /**
    * Adds the external dbms info for the specified dependency.  
    * 
    * @param dep The dependency to add it for, may not be <code>null</code>.  
    * @param infoList A list of zero or more <code>PSDbmsInfo</code> 
    * objects, may not be <code>null</code>.
    * 
    * @throws IllegalArgumentException if any param is invalid.
    */
   public void addDbmsInfoList(PSDependency dep, List<PSDatasourceMap> infoList)
   {
      if (dep == null)
         throw new IllegalArgumentException("pkg may not be null");
         
      if (infoList == null)
         throw new IllegalArgumentException("infoList may not be null");
         
      DepFilesIdTypes value = m_depMap.get(dep.getKey());
      if ( value == null )
      {
         value = new DepFilesIdTypes(dep.getKey(), null, null, infoList);
         m_depMap.put(value.m_key, value);
      }
      value.m_dbmsInfoList = infoList;
   }

   

   /**
    * Get the <code>PSApplicationIDTypes</code> object for the given
    * <code>PSDependency</code>.
    *
    * @param    dep The <code>PSDependency</code> object. It may not be
    * <code>null</code>.
    *
    * @return The related <code>PSApplicationIDTypes</code> object which
    * relate to <code>dep</code> (via its key) if it exists; otherwise,
    * return <code>null</code>.
    *
    * @throws IllegalArgumentException If any param is invalid.
    */
   public PSApplicationIDTypes getIdTypes(PSDependency dep)
   {
      if (dep == null)
         throw new IllegalArgumentException(DEP_NOT_NULL_MSG);

      DepFilesIdTypes value = m_depMap.get(dep.getKey());
      if (value == null || value.m_idtypes == null )
         return null;
      else
         return value.m_idtypes;
   }
   
   /**
    * Get the list of external dbmsinfo objects for the given
    * <code>PSDependency</code>.
    *
    * @param dep The <code>PSDependency</code> object. It may not be
    * <code>null</code>.
    *
    * @return The list of <code>PSDbmsInfo</code> objects for the specified 
    * dependency, may be <code>null</code> or empty.
    *
    * @throws IllegalArgumentException If any param is invalid.
    */
   public List<PSDatasourceMap> getDbmsInfoList(PSDependency dep)
   {
      if (dep == null)
         throw new IllegalArgumentException(DEP_NOT_NULL_MSG);
      
      List<PSDatasourceMap> infoList = null;
      DepFilesIdTypes value = m_depMap.get(dep.getKey());
      if (value != null)
         infoList = value.m_dbmsInfoList;
         
      return infoList;
   }
   

   /**
    * Serializes this object's state to its XML representation.  The format is:
    * <pre><code>
    * &lt;!ELEMENT PSXArchiveManifest (PSXDepFilesIdTypes*)>
    * &lt;!ELEMENT PSXDepFilesIdTypes (PSXApplicationIDTypes |
    *    PSXDependencyFile+ | (PSXApplicationIDTypes, PSXDependencyFile+) )>
    * &lt;!ATTLIST PSXDepFilesIdTypes
    *    DependencyKey CDATA #REQUIRED
    * </code></pre>
    *
    * See {@link IPSDeployComponent#toXml(Document)} for more info.
    */
   public Element toXml(Document doc)
   {
      if (doc == null)
         throw new IllegalArgumentException("doc may not be null");

      Element root = doc.createElement(XML_NODE_NAME);

      for (DepFilesIdTypes depChild : m_depMap.values()) {
         Element depChildXml = depChild.toXml(doc);
         root.appendChild(depChildXml);
      }
      return root;
   }

   // see IPSDeployComponent interface
   public void fromXml(Element sourceNode) throws PSUnknownNodeTypeException
   {
      if (sourceNode == null)
         throw new IllegalArgumentException("sourceNode may not be null");

      if (!XML_NODE_NAME.equals(sourceNode.getNodeName()))
      {
         Object[] args = { XML_NODE_NAME, sourceNode.getNodeName() };
         throw new PSUnknownNodeTypeException(
            IPSObjectStoreErrors.XML_ELEMENT_WRONG_TYPE, args);
      }

      m_depMap.clear(); // initialize internal data.

      PSXmlTreeWalker tree = new PSXmlTreeWalker(sourceNode);
      Element childEl = tree.getNextElement(XML_CHILD_NODE, FIRST_FLAGS);
      while (childEl != null)
      {
         DepFilesIdTypes fileIdType = new DepFilesIdTypes(childEl);
         m_depMap.put(fileIdType.m_key, fileIdType);

         childEl = tree.getNextElement(XML_CHILD_NODE, NEXT_FLAGS);
      }

   }

   // see IPSDeployComponent interface
   public void copyFrom(IPSDeployComponent obj)
   {
      if ( obj == null )
         throw new IllegalArgumentException("obj parameter should not be null");

      if (!(obj instanceof PSArchiveManifest))
         throw new IllegalArgumentException(
            "obj wrong type, expecting PSArchiveManifest");

      PSArchiveManifest objSrc = (PSArchiveManifest) obj;

      m_depMap.clear();
      m_depMap.putAll(objSrc.m_depMap);
   }

   // see IPSDeployComponent interface
   public int hashCode()
   {
      return m_depMap.hashCode();
   }

   // see IPSDeployComponent interface
   public boolean equals(Object obj)
   {
      boolean bEqual;

      if (!(obj instanceof PSArchiveManifest))
      {
         bEqual = false;  // not the same type of object
      }
      else if (obj == this) // compare to itself
      {
         bEqual = true;
      }
      else
      {
         PSArchiveManifest obj2 = (PSArchiveManifest) obj;
         bEqual = m_depMap.equals(obj2.m_depMap);
      }
      return bEqual;
   }

   /**
    * Root node name of this object's XML representation.
    */
   public static final String XML_NODE_NAME = "PSXArchiveManifest";
   /**
    * Child node name of this object's XML representation.
    */
   private static final String XML_CHILD_NODE = "PSXDepFilesIdTypes";
   /**
    * The attribute of the child node.
    */
   private static final String XML_CHILD_NODE_ATTR = "DependencyKey";

   /**
    * It maps a list of <code>DepFilesIdTypes</code> objects (as the value
    * of the Map) to its related Dependency's key (as the key of the Map in
    * <code>String</code>). It will never <code>null</code>, but may be empty.
    */
   private Map<String, DepFilesIdTypes> m_depMap = new HashMap<>();

   /**
    * flags to walk to a child node of an XML tree
    */
   private static final int FIRST_FLAGS =
      PSXmlTreeWalker.GET_NEXT_ALLOW_CHILDREN |
      PSXmlTreeWalker.GET_NEXT_RESET_CURRENT;

   /**
    * flags to walk to a sibling node of an XML tree
    */
   private static final int NEXT_FLAGS =
      PSXmlTreeWalker.GET_NEXT_ALLOW_SIBLINGS |
      PSXmlTreeWalker.GET_NEXT_RESET_CURRENT;

   /**
    * Encapsulates a pair of <code>PSDependencyFile</code> and
    * <code>PSApplicationIDTypes</code> objects for a specific
    * <code>PSDependency</code> (via its key), as well as a list of external
    * <code>PSDbmsInfo</code> objects.
    */
   private static class DepFilesIdTypes implements IPSDeployComponent
   {
      /**
       * Constructing the object with given parameters.
       *
       * @param key The key of the related Dependency object, assume it is
       * not <code>null</code> or empty.
       * @param files The list of <code>PSDependencyFile</code> objects, it
       * may be <code>null</code>.
       * @param idtypes The <code>PSApplicationIdTypes</code> object, it may be
       * <code>null</code>.
       * @param dbmsInfoList The list of external dbmsinfo objects for this dependency, it
       * may be <code>null</code>
       * <br/>
       * NOTE: Assuming any of the parameters may be <code>null</code>, but not 
       * all are <code>null</code>.
       */
      public DepFilesIdTypes(String key, List<PSDependencyFile> files,
         PSApplicationIDTypes idtypes, List<PSDatasourceMap> dbmsInfoList)
      {
         m_key = key;
         m_depFiles = files;
         m_idtypes = idtypes;
         m_dbmsInfoList = dbmsInfoList;
      }

      /**
       * Create this object from its XML representation
       *
       * @param source The source element.  See {@link #toXml(Document)} for
       * the expected format.  May not be <code>null</code>.
       *
       * @throws IllegalArgumentException If <code>source</code> is
       * <code>null</code>.
       * @throws PSUnknownNodeTypeException <code>source</code> is malformed.
       */
      public DepFilesIdTypes(Element source) throws PSUnknownNodeTypeException
      {
         if (source == null)
            throw new IllegalArgumentException("source may not be null");

         fromXml(source);
      }

      /**
       * Serializes this object's state to its XML representation. Format is:
       * <pre><code>
       * &lt;!ELEMENT PSXDepFilesIdTypes (PSXApplicationIDTypes?, DBMSInfoList?,
       *    PSXDependencyFile*)>
       * &lt;!ATTLIST PSXDepFilesIdTypes
       *    DependencyKey CDATA #REQUIRED
       * &lt;!ELEMENT DBMSInfoList (PSXDbmsInfo*)>
       * </code></pre>
       *
       * @param doc The Document for creating XML Element. Assumed not
       * <code>null</code>.
       *
       * @return The serialized XML Element.
       */
      public Element toXml(Document doc)
      {
         if (doc == null)
            throw new IllegalArgumentException("doc should not be null");

         Element root = doc.createElement(XML_CHILD_NODE);
         root.setAttribute(XML_CHILD_NODE_ATTR, m_key);

         if ( m_idtypes != null )
            root.appendChild( m_idtypes.toXml(doc) );
            
         if (m_dbmsInfoList != null)
         {
            Element dbmsListEl = PSXmlDocumentBuilder.addEmptyElement(doc, root, 
               XML_DBMS_LIST_EL);
            for (PSDatasourceMap psDbmsInfo : m_dbmsInfoList) {
               Element dsElem =  psDbmsInfo.toXml(doc);
               dbmsListEl.appendChild(dsElem);
            }
         }
         
         if ( m_depFiles != null )
         {
            for (PSDependencyFile depFile : m_depFiles) {
               root.appendChild(depFile.toXml(doc));
            }
         }
         return root;
      }


      // see IPSDeployComponent interface
      public void fromXml(Element sourceNode) throws PSUnknownNodeTypeException
      {
         if (sourceNode == null)
            throw new IllegalArgumentException("sourceNode may not be null");

         if (!XML_CHILD_NODE.equals(sourceNode.getNodeName()))
         {
            Object[] args = { XML_CHILD_NODE, sourceNode.getNodeName() };
            throw new PSUnknownNodeTypeException(
               IPSObjectStoreErrors.XML_ELEMENT_WRONG_TYPE, args);
         }
         m_key = PSDeployComponentUtils.getRequiredAttribute(
            sourceNode, XML_CHILD_NODE_ATTR);
            
         m_depFiles = null;
         m_dbmsInfoList = null;
         m_idtypes = null;
         
         PSXmlTreeWalker tree = new PSXmlTreeWalker(sourceNode);

         Element childEl = tree.getNextElement(FIRST_FLAGS);

         if ( childEl == null ) // no child element
         {
            Object[] args = { XML_CHILD_NODE + " DependencyKey=\"" + m_key +"\"", 
                  "first", "null" };
            throw new PSUnknownNodeTypeException(
               IPSObjectStoreErrors.XML_ELEMENT_INVALID_CHILD, args);

         }
         while (childEl != null)
         {
            if (childEl.getNodeName().equals(
               PSApplicationIDTypes.XML_NODE_NAME))
            {
               m_idtypes = new PSApplicationIDTypes(childEl);
            }
            else if (childEl.getNodeName().equals(XML_DBMS_LIST_EL))
            {
               m_dbmsInfoList = getInfoListFromXml(tree);
               tree.setCurrent(childEl);
            }
            else
            {
               m_depFiles = getDepFilesFromXml(childEl, tree);
            }
            
            childEl = tree.getNextElement(NEXT_FLAGS);
         }
      }

      // see IPSDeployComponent interface
      public int hashCode()
      {
         return (m_key != null ? m_key.hashCode() : 0) + (m_depFiles != null ? 
            m_depFiles.hashCode() : 0) + (m_idtypes != null ? 
            m_idtypes.hashCode() : 0) + (m_dbmsInfoList != null ? 
            m_dbmsInfoList.hashCode() : 0);
      }

      // see IPSDeployComponent interface
      public boolean equals(Object obj)
      {
         boolean isEqual = true;

         if (!(obj instanceof DepFilesIdTypes))
         {
            isEqual = false;  // not the same type of object
         }
         else
         {
            DepFilesIdTypes other = (DepFilesIdTypes) obj;
            if (m_idtypes == null ^ other.m_idtypes == null)
               isEqual = false;
            else if (m_idtypes != null && !m_idtypes.equals(other.m_idtypes))
               isEqual = false;
            else if (m_dbmsInfoList == null ^ other.m_dbmsInfoList == null)
               isEqual = false;
            else if (m_dbmsInfoList != null && !m_dbmsInfoList.equals(
               other.m_dbmsInfoList))
            {
               isEqual = false;
            }
            else if (m_depFiles == null ^ other.m_depFiles == null)
               isEqual = false;
            else if (m_depFiles != null && !m_depFiles.equals(
               other.m_depFiles))
            {
               isEqual = false;
            }
         }
         return isEqual;
      }

      /**
       * Get the list of <code>PSDependencyFile</code> objects from the
       * given parameters.
       *
       * @param depfileEl XML Element, assume not <code>null</code>.
       * @param tree The XML tree, assume not <code>null</code>.
       *
       * @return The list of <code>PSDependencyFile</code> objects, it will
       * never be <code>null</code>.
       *
       * @throws PSUnknownNodeTypeException if MALFORMED XML occurs.
       */
      private List<PSDependencyFile> getDepFilesFromXml(Element depfileEl, PSXmlTreeWalker tree)
         throws PSUnknownNodeTypeException
      {
         List<PSDependencyFile> depFiles = new ArrayList<>();

         while (depfileEl != null)
         {
            PSDependencyFile depfile = new PSDependencyFile(depfileEl);
            depFiles.add(depfile);

            depfileEl = tree.getNextElement(PSDependencyFile.XML_NODE_NAME,
               NEXT_FLAGS);
         }
         
         return depFiles;
      }


      /**
       * Get the list of <code>PSDbmsInfo</code> objects from the
       * given parameters.
       *
       * @param tree The XML tree, assumed not <code>null</code> and to be 
       * positioned on a <code>XML_DBMS_LIST_EL</code> element.
       *
       * @return The list of <code>PSDbmsInfo</code> objects, it will
       * never be <code>null</code>, may be empty.
       *
       * @throws PSUnknownNodeTypeException if MALFORMED XML occurs.
       */
      private List<PSDatasourceMap> getInfoListFromXml(PSXmlTreeWalker tree)
         throws PSUnknownNodeTypeException
      {
         List<PSDatasourceMap> infoList = new ArrayList<>();
         Element infoEl = tree.getNextElement(PSDatasourceMap.XML_NODE_NAME, FIRST_FLAGS);
         while (infoEl != null)
         {
            PSDatasourceMap dsMap = new PSDatasourceMap(infoEl);

            infoList.add(dsMap);

            infoEl = tree.getNextElement(PSDbmsInfo.DATASOURCE_XML_ELEMENT,
               NEXT_FLAGS);
         }         return infoList;
      }

      // see IPSDeployComponent interface
      public void copyFrom(IPSDeployComponent obj)
      {
         if ( obj == null )
            throw new IllegalArgumentException(
               "obj parameter should not be null");

         if (!(obj instanceof PSArchiveManifest))
            throw new IllegalArgumentException(
               "obj wrong type, expecting PSArchiveManifest");

         //TODO: Not sure how this has been working
         DepFilesIdTypes obj2 = (DepFilesIdTypes) obj;

         m_key = obj2.m_key;
         m_idtypes = obj2.m_idtypes;

         if (obj2.m_depFiles != null)
         {
            m_depFiles = new ArrayList<>();
            m_depFiles.addAll(obj2.m_depFiles);
         }
         else
            m_depFiles = null;

         if (obj2.m_dbmsInfoList != null)
         {
            m_dbmsInfoList = new ArrayList<>();
            m_dbmsInfoList.addAll(obj2.m_dbmsInfoList);
         }
         else
            m_dbmsInfoList = null;
      }

      /**
       * The key of the related Dependency object. Initialized by the
       * constructor, it may not be <code>null</code> or empty after that.
       */
      protected String m_key;
      /**
       * A list of <code>PSDependencyFile</code> objects, it may be
       * <code>null</code>, never empty if not null.
       */
      protected List<PSDependencyFile> m_depFiles;
      /**
       * The <code>PSApplicationIdTypes</code> object, it may be
       * <code>null</code>
       */
      protected PSApplicationIDTypes m_idtypes;
      
      /**
       * A <code>List</code> of <code>PSDbmsInfo</code> objects, it may be 
       * <code>null</code> or empty.
       */
      protected List<PSDatasourceMap> m_dbmsInfoList = null;
      
      // Private xml constants   
      private static final String XML_DBMS_LIST_EL = "DBMSInfoList";


   }

   private static final String DEP_NOT_NULL_MSG = "dep may not be null";
}
