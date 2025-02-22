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


/**
 * Dependency class that represents a top-level cms element that may be deployed
 * stand-alone. 
 */
public class PSDeployableElement extends PSDependency
{

   /**
    * Convenience ctor that calls <code>this(dependencyType, dependencyId,
    * objectType, objectTypeName, displayName, supportsIdTypes, 
    * supportsIdMapping,  supportsUserDependencies, false)</code>.
    */
   public PSDeployableElement(int dependencyType, String dependencyId,
      String objectType, String objectTypeName, String displayName,
      boolean supportsIdTypes, boolean supportsIdMapping,
         boolean supportsUserDependencies)
   {
      this(dependencyType, dependencyId, objectType, objectTypeName, 
      displayName, supportsIdTypes, supportsIdMapping, supportsUserDependencies, 
      false);
   }
   
   /**
    * Construct a deployable element with all required parameters.
    * 
    * @param dependencyType The type of dependency, must be one of the 
    * <code>TYPE_xxx</code> types.
    * @param dependencyId Combined with <code>objectType</code> uniquely 
    * identifies the object this dependency represents.  May not be 
    * <code>null</code> or empty.
    * @param displayName Name to use when displaying this dependency.  May not 
    * be <code>null</code> or empty.
    * @param objectType The type of object this dependency represents. May not
    * be <code>null</code> or empty.
    * @param objectTypeName Displayable form of the <code>objectType</code>,
    * may not be <code>null</code> or empty.
    * @param supportsIdTypes <code>true</code> if this object contains static
    * ID's whose type must be identified, <code>false</code> if not.
    * @param supportsIdMapping <code>true</code> if this object's ID can change
    * across server's and thus may be included in an ID Mapping.
    * @param supportsUserDependencies If <code>true</code>, this dependency
    * allows user defined dependencies to be added as children, 
    * <code>false</code> otherwise.
    * @param supportsParentId If <code>true</code>, supports a parent id to be
    * specified, if <code>false</code>, does not.
    * 
    * @throws IllegalArgumentException if any param is invalid.
    */
   public PSDeployableElement(int dependencyType, String dependencyId, 
      String objectType, String objectTypeName, String displayName, 
      boolean supportsIdTypes, boolean supportsIdMapping, 
         boolean supportsUserDependencies, boolean supportsParentId)
   {
      super(dependencyType, dependencyId, 
         objectType, objectTypeName, displayName, 
         supportsIdTypes, supportsIdMapping, supportsUserDependencies, 
         supportsParentId);
   }
   
   /**
    * Constructs this object from its XML representation.
    * 
    * @param src The source element.  Format expected is defined by 
    * {@link #toXml(Document)}.
    * 
    * @throws IllegalArgumentException if <code>sourceNode</code> is 
    * <code>null</code>.
    * @throws PSUnknownNodeTypeException if the XML element node does not 
    * represent a type supported by the class.
    */
   public PSDeployableElement(Element src) throws PSUnknownNodeTypeException
   {
      if (src == null)
         throw new IllegalArgumentException("src may not be null");
      
      fromXml(src);
   }
   
   /** 
    * Sets the description for this object.
    * 
    * @param description The description, may not be <code>null</code>.
    * 
    * @throws IllegalArgumentException if <code>desc</code> is invalid.
    */
   public void setDescription(String description)
   {
      if (description == null )
         throw new IllegalArgumentException(
            "description may not be null");
      
      m_description = description;
   }
   
   /**
    * Get the description for this object. 
    * 
    * @return The description, never <code>null</code>, may be empty.
    */
   public String getDescription()
   {
      return m_description;
   }
   
   /**
    * This method is called to create an XML element node with the
    * appropriate format for this object. Format is:
    * <pre><code>
    * &lt;!ELEMENT PSXDeployableElement (PSXDependency, Description)>
    * </pre></code>
    * 
    * @param doc The document to use to create the element, may not be 
    * <code>null</code>.
    * 
    * @return the newly created XML element node, never <code>null</code>.
    * 
    * @throws IllegalArgumentException if doc is <code>null</code>.
    */
   @Override
   public Element toXml(Document doc)
   {
      if (doc == null)
         throw new IllegalArgumentException("doc may not be null");
         
      Element root = doc.createElement(XML_NODE_NAME);
      root.appendChild(super.toXml(doc));      
      PSXmlDocumentBuilder.addElement(doc, root, XML_EL_DESC, m_description);
            
      return root;
   }
   
   
   /**
    * This method is called to populate this object from its XML representation.
    * 
    * @param sourceNode the XML element node to populate from, not 
    * <code>null</code>.  See {@link #toXml(Document)} for the format expected.
    * 
    * @throws IllegalArgumentException if <code>sourceNode</code> is 
    * <code>null</code>.
    * @throws PSUnknownNodeTypeException if the XML element node does not 
    * represent a type supported by the class.
    */
   @Override
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
      
      PSXmlTreeWalker tree = new PSXmlTreeWalker(sourceNode);
      Element dep = tree.getNextElement(PSDependency.XML_NODE_NAME, 
         PSXmlTreeWalker.GET_NEXT_ALLOW_CHILDREN);
      if (dep == null)
      {
         throw new PSUnknownNodeTypeException(
            IPSObjectStoreErrors.XML_ELEMENT_NULL, PSDependency.XML_NODE_NAME);
      }
      super.fromXml(dep);
      
      tree.setCurrent(sourceNode);
      String description = tree.getElementData(XML_EL_DESC);
      if (description == null)
      {
         throw new PSUnknownNodeTypeException(
            IPSObjectStoreErrors.XML_ELEMENT_NULL, XML_EL_DESC);
      }
      
      m_description = description;
   }
   
   // see IPSDeployComponent
   @Override
   public boolean equals(Object obj)
   {
      boolean isEqual = true;
      if (!(obj instanceof PSDeployableElement))
         isEqual = false;
      else
      {
         PSDeployableElement other = (PSDeployableElement)obj;
         if (!super.equals(other))
            isEqual = false;
         else if (m_description == null ^ other.m_description == null)
            isEqual = false;
         else if (m_description != null && !m_description.equals(
            other.m_description))
         {
            isEqual = false;
         }
      }
      
      return isEqual;
   }
   
   // see IPSDeployComponent
   @Override
   public void copyFrom(IPSDeployComponent obj)
   {
      if (obj == null)
         throw new IllegalArgumentException("obj may not be null");
         
      if (!(obj instanceof PSDeployableElement))
         throw new IllegalArgumentException("obj wrong type");

      PSDeployableElement dep = (PSDeployableElement)obj;
      super.copyFrom(dep);
      m_description = dep.m_description;
   }
   
   // see IPSDeployComponent interface
   @Override
   public int hashCode()
   {
      return super.hashCode() + m_description.hashCode();
   }
   
   /**
    * Constant for this object's root XML node.
    */
   public static final String XML_NODE_NAME = "PSXDeployableElement";
   
   /**
    * The description for this object, initialized to an empty string. Modified 
    * by calls to {@link #setDescription(String)}.
    */
   private String m_description = "";

   // Xml constants   
   private static final String XML_EL_DESC = "Description";

}

