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
package com.percussion.server.cache;

import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.error.PSExceptionUtils;
import com.percussion.services.error.PSNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class is used to 'plot' graphs for a specific relationship type, so
 * that we can retrieve the parent and/or child graph paths of a specified item.
 * <p>
 * Note: it is necessary to plot all relationships, otherwise it only contains
 * partial graths. The current implementation is for folder relationship only.
 */
class PSRelationshipGraph
{
   /**
    * Reference to Log4j singleton object used to log any errors or debug info.
    */
   private static final Logger log = LogManager.getLogger("PSRelationshipGraph");

   public void addRelationship(Integer relationshipId, PSLocator parent, PSLocator child)
   {
      addRelationship(relationshipId,parent,child,-1);
   }
   
   /**
    * Adds (or plots) the supplied parent & child relationship into the graph.
    *
    * @param relationshipId the related object, never <code>null</code>.
    * @param parent the parent object, never <code>null</code>.
    * @param child the child object, never <code>null</code>.
    */
   public void addRelationship(Integer relationshipId, PSLocator parent, PSLocator child, int sortRank)
   {
      if (relationshipId == null)
         throw new IllegalArgumentException("relatedObj may not be null");
      if (parent == null)
         throw new IllegalArgumentException("parent may not be null");
      if (child == null)
         throw new IllegalArgumentException("child may not be null");

      // store the relationship into m_childMap first
      Map<Integer, PSGraphEntry> children = m_parentMapToChildren.get(parent);
      PSGraphEntry childObj = new PSGraphEntry(child, relationshipId, sortRank);
      if (children == null)
      {
         children = new ConcurrentHashMap<>();
         children.put(childObj.getrelationshipId(),childObj);
         m_parentMapToChildren.put(parent, children);
      }
      else
      {
         children.put(childObj.getrelationshipId(),childObj);
      }

      // store the relationship into m_parentMap first
      Map<Integer,PSGraphEntry> parents = m_childMapToParent.get(child);
      PSGraphEntry parentObj = new PSGraphEntry(parent, relationshipId, sortRank);
      if (parents == null)
      {
         parents = new ConcurrentHashMap<>();
         parents.put(parentObj.getrelationshipId(),parentObj);
         m_childMapToParent.put(child, parents);
      }
      else
      {
         parents.put(parentObj.getrelationshipId(),parentObj);
      }
   }

   /**
    * Removes the supplied relationship from the graph.
    *
    * @param relatedObj
    *           the related object, never <code>null</code>.
    * @param parent
    *           the parent object, never <code>null</code>.
    * @param child
    *           the child object, never <code>null</code>.
    */
   public void removeRelationship(Integer relatedObj, PSLocator parent, PSLocator child)
   {
      if (relatedObj == null)
         throw new IllegalArgumentException("relatedObj may not be null");
      if (parent == null)
         throw new IllegalArgumentException("parent may not be null");
      if (child == null)
         throw new IllegalArgumentException("child may not be null");

      PSGraphEntry childEntry = new PSGraphEntry(child, relatedObj);
      PSGraphEntry parentEntry = new PSGraphEntry(parent, relatedObj);

      // update the m_childMap
      Map<Integer,PSGraphEntry> children = m_parentMapToChildren.get(parent);
      if (children != null)
      {
         if (children.remove(childEntry.getrelationshipId(),childEntry) && children.isEmpty())
            m_parentMapToChildren.remove(parent);
      }

      // update the m_parentMap
      Map<Integer,PSGraphEntry> parents = m_childMapToParent.get(child);
      if (parents != null)
      {
         if (parents.remove(parentEntry.getrelationshipId(),parentEntry) && parents.isEmpty())
         m_childMapToParent.remove(childEntry.getrelationshipId(),childEntry);
      }
   }

   /**
    * Returns the paths to the root for the supplied child.
    * <p>
    * Note: this is implemented for folder relationships. It needs to be
    * overridden for other relationships.
    *
    * @param child
    *           the child object, never <code>null</code>.
    *
    * @return a 2 dimension array. The 1st dimension is a list of paths; the
    *         2nd dimension contains the actual paths, which is an array of
    *         <code>PSGraphEntry[]</code> objects. Within each path, the 1st
    *         element is the root, followed by its immediate child, ...etc.
    *         Never <code>null</code>, but may be empty if the supplied
    *         object is the root, which does not have parent.
    */
   public List<List<PSGraphEntry>> getPathsToRoot(PSLocator child, String relationshipTypeName) {
      if (child == null)
         throw new IllegalArgumentException("child may not be null");

      List<List<PSGraphEntry>> ret = new ArrayList<>();
      // For folder need to check revisionless
      PSLocator childLoc = child.getRevision() == -1 ? child : new PSLocator(child.getId());

      Map<Integer,PSGraphEntry> parents = m_childMapToParent.get(childLoc);
      if (parents != null && !parents.isEmpty()) {

         for (PSGraphEntry parent : parents.values()) {

            PSRelationship rel = null;
            try {
               rel = PSFolderRelationshipCache.getInstance().getRelationship(parent.getrelationshipId());
               if (rel == null || !rel.getConfig().getName().equalsIgnoreCase(relationshipTypeName)) {
                  continue;
               }
            } catch (PSNotFoundException e) {
               log.warn("Relationship Not Found : {} : Error : {} " ,parent.getrelationshipId(), PSExceptionUtils.getMessageForLog(e));
            }

            List<PSGraphEntry> path = new ArrayList<>();
            getFolderPath(path, parent, relationshipTypeName);
            // reverse the path
            Collections.reverse(path);
            ret.add(path);
         }
      }

      return ret;
   }

   /**
    * This is the same as , but it returns a list
    * instead of array.
    * 
    * @param parent the parent, never <code>null</code>.
    * 
    * @return a list of immediate child objects, never <code>null</code>
    *   
    */
   public List<PSGraphEntry> getChildrenList(PSLocator parent)
   {

      if (parent == null)
         throw new IllegalArgumentException("parent may not be null");
      Map<Integer,PSGraphEntry> children = m_parentMapToChildren.get(parent);
      if(children == null){
         return new CopyOnWriteArrayList<>();
      }else{
         List<PSGraphEntry> psGraphEntry = new CopyOnWriteArrayList( children.values());
         psGraphEntry.sort(null);
         return psGraphEntry;
      }
   }

   /**
    * Returns the immediate parent objects for the supplied child.
    *
    * @param child the child object, never <code>null</code>.
    *
    * @return the parent objects, never <code>null</code>, but may be empty.
    */
   public List<PSGraphEntry> getParents(PSLocator child)
   {
      if (child == null)
         throw new IllegalArgumentException("child may not be null");

      Map<Integer,PSGraphEntry> parentList = m_childMapToParent.get(child);
      if (parentList == null)
      {
         parentList = new ConcurrentHashMap<>();
      }

      return new ArrayList<PSGraphEntry>(parentList.values());
   }

   /**
    * Gets the folder path for the supplied child item.
    *
    * @param path
    *           the folder path that is returned to the caller. It is a list of
    *           {@link PSGraphEntry}objects. Assume never <code>null</code>,
    *           but may be empty.
    * @param childFolder
    *           the child folder object. Assume not <code>null</code>.
    */
   private void getFolderPath(List<PSGraphEntry> path, PSGraphEntry childFolder, String relationshipTypeName)
   {
      IPSFolderRelationshipCache cache = PSFolderRelationshipCache.getInstance();
      PSRelationship childRel = null;
      if (cache != null) {
         try {
            childRel = cache.getRelationship(childFolder.getrelationshipId());
         } catch (PSNotFoundException e) {
            log.warn("Relationship Not Found : {} : Error : {} " ,childFolder.getrelationshipId(), PSExceptionUtils.getMessageForLog(e));
            return;
         }
      }
      if (childRel != null && childRel.getConfig().getName().equalsIgnoreCase(relationshipTypeName)) {
          path.add(childFolder);
      } else if (!childRel.getConfig().getName().equalsIgnoreCase(relationshipTypeName)) {
         // we are going down the path of a relationship that doesn't match the relationshipType
         return;
      }
      Map<Integer,PSGraphEntry> parents = m_childMapToParent.get(childFolder
            .getValue());

      if (parents != null && !parents.isEmpty())
      {
          List<PSGraphEntry> filteredParents = new ArrayList<>();
          for (PSGraphEntry entry : parents.values()) {
              if (cache != null) {
                 PSRelationship rel = null;
                 try {
                    rel = cache.getRelationship(entry.getrelationshipId());
                    if (rel != null && rel.getConfig().getName().equalsIgnoreCase(relationshipTypeName)) {
                       filteredParents.add(entry);
                    }
                 } catch (PSNotFoundException e) {
                    log.warn("Relationship Not Found : {} : Error : {} " ,entry.getrelationshipId(), PSExceptionUtils.getMessageForLog(e));
                 }

              }
          }

         Iterator<PSGraphEntry> it = filteredParents.iterator();
         // there may be a situation where the above code would remove
          // any items in the iterator.  This is when the parents.size() == 1
          // and that 1 item is not of the type of relationship being searched for.
          if (it.hasNext()) {
              getFolderPath(path, (PSGraphEntry) it.next(), relationshipTypeName);
          }
      }
   }

   /**
    * Returns all parents from the current graph.
    * 
    * @return a set of <code>Object</code>, which is the <code>parent</code>
    *    object that was passed in 
    *    , never
    *    <code>null</code>, may be empty. It cannot be modified by the caller.
    */
   Set<PSLocator> getAllParents()
   {
      return Collections.unmodifiableSet(m_parentMapToChildren.keySet());
   }
   
   /**
    * Returns the total number of parents in the graph.
    * 
    * @return total number of parents.
    */
   int getTotalParent()
   {
      return m_parentMapToChildren.size();
   }
   
   /**
    * Returns the total number of children in the graph.
    * 
    * @return total number of children.
    */
   int getTotalChildren()
   {
      return m_childMapToParent.size();
   }
   
   /**
    * It maps the child object to its parent. The map keys are child in
    * <code>Object</code>; the map values are collections of parents in
    * <code>Collection</code> objects. Each value (the <code>Collection</code>
    * object) is a collection of {@link PSGraphEntry} object.
    * <p>
    * It may be empty, but never <code>null</code>.
    */
   private  Map<PSLocator,Map<Integer,PSGraphEntry>> m_childMapToParent = new ConcurrentHashMap<>();

   /**
    * It maps the parent object to its children. The map keys are parents in
    * <code>Object</code> objects; the map values are lists of child in
    * <code>Collection</code> objects. Each value (the <code>Collection</code>
    * object) is a collection of {@link PSGraphEntry} object.
    * <p>
    * It may be empty, but never
    * <code>null</code>.
    */
   private Map<PSLocator,Map<Integer,PSGraphEntry>> m_parentMapToChildren = new ConcurrentHashMap<>();

}
