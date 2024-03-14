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
package com.percussion.services.assembly.impl.nav;

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.cms.objectstore.PSRelationshipProcessorProxy;
import com.percussion.cms.objectstore.server.PSRelationshipProcessor;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.design.objectstore.PSRelationshipSet;
import com.percussion.error.PSExceptionUtils;
import com.percussion.server.cache.IPSFolderRelationshipCache;
import com.percussion.server.cache.PSFolderRelationshipCache;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSProxyNode;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.assembly.jexl.PSManagedNavUtils;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.IPSNode;
import com.percussion.services.contentmgr.PSContentMgrConfig;
import com.percussion.services.contentmgr.PSContentMgrLocator;
import com.percussion.services.contentmgr.PSContentMgrOption;
import com.percussion.services.contentmgr.data.PSContentNode;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.filter.IPSFilterItem;
import com.percussion.services.filter.PSFilterException;
import com.percussion.services.filter.data.PSFilterItem;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.services.sitemgr.PSSiteHelper;
import com.percussion.services.sitemgr.PSSiteManagerException;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.jexl.IPSScript;
import com.percussion.utils.jexl.PSJexlEvaluator;
import com.percussion.utils.jsr170.PSProperty;
import com.percussion.utils.timing.PSStopwatchStack;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.net.nntp.NNTPConnectionClosedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang.Validate.notNull;



/**
 * Methods to aid in locating and loading the proxy navon node. This object
 * should be created once per assembly request. It caches some information
 * during the current request.
 * As relationship processor is run as the system user, community filtering is ignored.
 * 
 * @author dougrand
 */
public class PSNavHelper
{
   /**
    * Jexl constant for self
    */
   private static final IPSScript NAV_SELF = PSJexlEvaluator
         .createStaticExpression("$nav.self");

   /**
    * Jexl constant for variables
    */
   private static final IPSScript VARIABLES = PSJexlEvaluator
         .createStaticExpression("$sys.variables");

   /**
    * Logger for this class
    */
   private static final Logger log = LogManager.getLogger(PSNavHelper.class);

   /**
    * Static configuration used for loading navon nodes
    */
   private static PSContentMgrConfig ms_config = new PSContentMgrConfig();

   {
      ms_config.addOption(PSContentMgrOption.LOAD_MINIMAL);
   }

   /**
    * Interfaces for JSR-170 node
    */
   public static Class ms_interfaces[] = new Class[]
   {IPSProxyNode.class};

   /**
    * Managed nav utilities
    */
   private static volatile PSManagedNavUtils ms_utils = new PSManagedNavUtils();

   /**
    * Landing page slot id, inited in ctor
    */
   private static volatile List<String> landingPageSlots = new ArrayList<>();

   /**
    * Sub menu slot id, inited in ctor
    */
   private static volatile List<String> submenuSlots = new ArrayList<>();

   /**
    * Image slot id, inited in ctor
    */
   private static volatile List<String> imageSlots = new ArrayList<>();

   /**
    * Cms object Manager initialized in ctor
    */
   private static volatile IPSCmsObjectMgr m_cmsObjMgr;

   /**
    * Content manager
    */
   private static volatile IPSContentMgr m_contentMgr;

   private static volatile IPSGuidManager m_gmgr;
   /**
    * Relationship processor object initialized in ctor
    */
   private static PSRelationshipProcessor m_relProc;

   /**
    * Instance of Navigation Configuration object initialized in ctor
    */
   private static volatile PSNavConfig navConfig;

   /**
    * Holds onto nav nodes already found. Used to reconstruct children
    * information. The map goes from contentid to a node that's been loaded.
    */
   private Map<Integer, Node> m_foundProxies = new HashMap<>();

   /**
    * Store the template information for landing pages as the pages are loaded.
    * This maps from a content guid to a template guid.
    */
   private Map<IPSGuid, IPSGuid> m_cidToTemplate = new HashMap<>();

   /**
    * Original assembly item, used when creating landing pages. Never
    * <code>null</code> after construction.
    */
   private IPSAssemblyItem m_assemblyItem;

   /**
    * Parameters passed via the assembly item, flattened into a single value
    * set. Never <code>null</code> after ctor
    */
   private Map<String, String> m_params;

   private static AtomicInteger landingPageWarnCount = new AtomicInteger(100);

   private static ArrayList<Long> m_navCtypes;
   
   private static volatile boolean m_isInited = false;
   
   /**
    * Ctor
    * 
    * @param assemblyItem assembly item, never <code>null</code>
    */
   public PSNavHelper(IPSAssemblyItem assemblyItem) {
      if (assemblyItem == null)
      {
         throw new IllegalArgumentException("assemblyItem may not be null");
      }
      m_assemblyItem = assemblyItem;
      m_params = getParams(assemblyItem);
      m_relProc = PSRelationshipProcessor.getInstance();
     
      init();
      
     
   }
   
   public static Map<String,String> getParams(IPSAssemblyItem assemblyItem)
   {
      Map<String,String> params = new HashMap<>();
      for (String name : assemblyItem.getParameters().keySet())
      {
         String value = assemblyItem.getParameterValue(name, null);
         if (value != null)
            params.put(name, value);
      }
      if (StringUtils.isNotBlank(assemblyItem.getUserName()))
      {
         params.put(IPSHtmlParameters.SYS_USER, assemblyItem.getUserName());
      }
      return params;
   }

   public static void init()
   {
      if (!m_isInited)
      {
         synchronized (PSNavHelper.class)
         {
            
            if (!m_isInited)
            {
            navConfig = PSNavConfig.getInstance();
            m_cmsObjMgr = PSCmsObjectMgrLocator.getObjectManager();
            m_contentMgr = PSContentMgrLocator.getContentMgr();
            m_gmgr = PSGuidManagerLocator.getGuidMgr();
            
            // Initialize the navconfig
            if (navConfig.getNavonTypes() == null)
            {
               throw new IllegalArgumentException("Navon content type configuration missing!");
            }
            if (navConfig.getNavTreeTypes() == null)
            {
               throw new IllegalArgumentException("NavTree content type configuration missing!");
            }

           
            
            m_navCtypes = new ArrayList<>();
            for(IPSGuid g: navConfig.getNavonTypes()){
               m_navCtypes.add(g.longValue());
            }
           for(IPSGuid g : navConfig.getNavTreeTypes()){
              m_navCtypes.add(g.longValue());
           }
            
      IPSAssemblyService asm = PSAssemblyServiceLocator.getAssemblyService();
      List<String> slotnames = new ArrayList<>();

      List<String> lSubMenuList = navConfig.getNavSubMenuSlotNames();
      slotnames.addAll(lSubMenuList);

      List<String> lNavImageList = navConfig.getNavImageSlotNames();
      slotnames.addAll(lNavImageList);

      List<String> lNavLandingList = navConfig.getNavLandingPageSlotNames();
      slotnames.addAll(lNavLandingList);

      try
      {
         List<IPSTemplateSlot> slots = asm.findSlotsByNames(slotnames);
         for (IPSTemplateSlot s : slots)
         {
            if(lSubMenuList.contains(s.getName())) {
               submenuSlots.add(String.valueOf(
                       s.getGUID().getUUID()));
            }
            else if (lNavImageList.contains(s.getName())){
               imageSlots.add(String.valueOf(
                       s.getGUID().getUUID()));
            }else if(lNavLandingList.contains(s.getName())){
               landingPageSlots.add(String.valueOf(
                       s.getGUID().getUUID()));
            }

         }

         m_isInited = true;
      }
      catch (Exception e)
      {
         log.warn("Could not load one or more nav slots. Error: {}",
                 PSExceptionUtils.getMessageForLog(e));
      }
            
            }
         }
      
      }
   }

   private List<String> getSlotIdsAsList(List<IPSTemplateSlot> slots){
      List<String> ret = new ArrayList<>();

      for(IPSTemplateSlot ts : slots){
         ret.add(String.valueOf(ts.getGUID().getUUID()));
      }

      return ret;
   }

   public static ArrayList<Long> geNavCtypes()
   {
      init();
      return m_navCtypes;
   }

   /**
    * Find the nav node object. The main work is done in
    * {@link #getNavNode(PSLocator)} where the parent axis is loaded and linked
    * together.
    * <p>
    * This method stores data into the assembly item. The data uses the $nav
    * binding, and the presence of $nav.self indicates to the method that this
    * data is initialized.
    * 
    * @param sourceItem the source assembly item, never <code>null</code>
    * @return the self navon node, <code>null</code> if there is no visible
    *         navon node
    * @throws RepositoryException
    * @throws PSCmsException
    * @throws PSFilterException
    */
   public Node findNavNode(IPSAssemblyItem sourceItem)
         throws RepositoryException, PSCmsException, PSFilterException
   {
      if (sourceItem == null)
      {
         throw new IllegalArgumentException("sourceItem may not be null");
      }
      PSStopwatchStack sws = PSStopwatchStack.getStack();
      sws.start("PSNavHelper.findNavNode");
      try
      {
         PSJexlEvaluator eval = new PSJexlEvaluator(sourceItem.getBindings());
         Node self;
         try
         {
            self = (Node) eval.evaluate(NAV_SELF);
         }
         catch (Exception e1)
         {
            throw new RepositoryException("Unexpected cms problem: "
                  + e1.getLocalizedMessage());
         }

         if (self != null)
            return self;

         // Find the folder corresponding to the sourceitem
         int folder=0;
         if(!isInRecycler(sourceItem.getId().toString())) {
            folder = findFolder(sourceItem);
         }else{
            //This item is in the Recycler so should be ignored in the NavTree.
            log.debug("Skipping Navigation for Recycled item:" + sourceItem.getId().toStringUntyped());
            return null;
         }

         if (folder == 0)
         {
            throw new RepositoryException("Problem finding the folder.");
         }


         // Find the navon of the folder we found
         PSLocator navonLoc = findNavon(new PSLocator(folder));
         if (navonLoc == null)
         {
            throw new RepositoryException("Problem finding the navon.");
         }

         // Create and return proxy node(s)
         self = getNavNode(navonLoc);
         setupNavValues(sourceItem, eval, self);
         return self;
      }
      finally
      {
         sws.stop();
      }
   }

   /**
    * Gets the root node of the given node.
    * 
    * @param self the node in question, it may be <code>null</code>.
    * 
    * @return the root node, it can never be <code>null</code>, but It may be 
    * the node itself if the given node does not have a parent.
    * 
    * @throws ItemNotFoundException
    * @throws AccessDeniedException
    * @throws RepositoryException
    */
   public Node getRoot(Node self) throws ItemNotFoundException,
         AccessDeniedException, RepositoryException
   {
      notNull(self);
      
      Node root = self;
      while (root.getParent() != null)
      {
         root = root.getParent();
      }
      return root;
   }
   
   /**
    * Setup navigation predefined values for assembly, $nav.self, $nav.base
    * and $nav.root
    * 
    * @param sourceItem source assembly item, never <code>null</code>
    * @param eval the jexl bindings to setup, never <code>null</code>
    * @param self the self navon, if <code>null</code> this method has no
    *           effect
    * @throws ItemNotFoundException
    * @throws AccessDeniedException
    * @throws RepositoryException
    * @throws ValueFormatException
    */
   @SuppressWarnings("unchecked")
   public void setupNavValues(IPSAssemblyItem sourceItem,
         PSJexlEvaluator eval, Node self) throws ItemNotFoundException, AccessDeniedException,
         RepositoryException, ValueFormatException
   {
      if (sourceItem == null)
      {
         throw new IllegalArgumentException("sourceItem may not be null");
      }
      if (eval == null)
      {
         throw new IllegalArgumentException("eval may not be null");
      }
      
      if (self == null)
         return;

      Node root = getRoot(self);

      // Calculate base
      Map<String, Object> navmap = new HashMap<>();
      Property basevar = ms_utils.findProperty(self, navConfig
            .getNavonVariableName());
      if (basevar == null || StringUtils.isBlank(basevar.getString()))
      {
         basevar = ms_utils.findProperty(root, navConfig.getNavtreeVarName());
      }
      // Bind
      if (basevar != null && basevar.getString() != null)
      {
         try
         {
            Map<String, String> variables = (Map<String, String>) eval
                  .evaluate(VARIABLES);
            if (variables != null)
            {
               String value = variables.get(basevar.getString());
               if (value != null)
                  navmap.put("base", value);
            }
         }
         catch (Exception e)
         {
            // Ignore
         }

      }

      navmap.put("self", self);
      navmap.put("root", root);
      eval.bind("$nav.self", self);
      eval.bind("$nav.root", root);

      sourceItem.getBindings().put("$nav", navmap);
   }
   
   /**
    * Build parent axis and load the parents. This is done by recursively
    * calling the relationship processor on the configured submenu relationship.
    * The resulting locators are converted to guids and loaded in one call to
    * the content manager.
    * <p>
    * The final loop reconstructs the partial tree (going up, but not down). The
    * resulting nodes thus are setup so calls to get the children will force all
    * the children to be loaded.
    * <p>
    * This method does add the proxies constructed to the found proxies map.
    * This enables the proxy to be reused when constructing the child axis.
    * <p><b>Note: The implementation now is revisionless this will always get the Tip Revision
    * of the parent.  Any changes while the navons are checked out will have immediate effect.</b></p>
    * 
    * @param self the self navon's locator, assumed not <code>null</code>
    * @return the self navon's proxy node
    * @throws PSCmsException
    * @throws RepositoryException
    * @throws PSFilterException
    */
   public Node getNavNode(PSLocator self) throws PSCmsException,
         RepositoryException, PSFilterException
   {
      init();
      
      IPSFolderRelationshipCache folderCache = PSFolderRelationshipCache
            .getInstance();
      
      
      if (self == null)
      {
         throw new IllegalArgumentException("self may not be null");
      }
      
      if (folderCache == null)
      {
         throw new IllegalArgumentException("FolderCache is not initialized check for previous errors in startup");
      }
      List<PSLocator> folders = folderCache.getOwnerLocators(self, PSRelationshipConfig.TYPE_FOLDER_CONTENT);
      List<IPSFilterItem> filterset = new ArrayList<>();
      List<IPSGuid> guids = new ArrayList<>();
      Map<Integer,IPSGuid> mapToGuid = new HashMap<>();
      IPSGuid siteid = m_assemblyItem.getSiteId();
      boolean found = false;
      for (int i = folders.size() - 1; i >= 0; i--)
      {
            final PSLocator folder = (PSLocator) folders.get(i);
            PSLocator item = folderCache.findChildOfType(folder, m_navCtypes);
            if (item!=null)
            {
               IPSGuid folderid = new PSLegacyGuid(folder.getId());
               IPSGuid itemid = new PSLegacyGuid(item.getId());
               guids.add(itemid);
               filterset.add(new PSFilterItem(itemid, folderid, siteid));
            }

      }
      if (guids.size()==0) return null;
      
      
      List<IPSFilterItem> filteredset = m_assemblyItem.getFilter().filter(
            filterset, m_params);
      
      if (filteredset.size()==0) return null;
      
      for (IPSFilterItem g : filteredset)
      {
         PSLegacyGuid lg = (PSLegacyGuid) g.getItemId();
         mapToGuid.put(lg.getContentId(), lg);
      }

      // Only keep guids up to the first non-visible, update the guids with
      // filtered guids
      for (int i = guids.size() - 1; i >= 0; i--)
      {
         IPSGuid guid = guids.get(i);

         if(isInRecycler(guid.toString())){
             continue;
         }
         int cid = guid.getUUID();
         IPSGuid fixedGuid = mapToGuid.get(cid);
         if (fixedGuid == null)
         {
            // Truncate list and break
            if (i == guids.size() - 1)
            {
               guids.clear();

            }
            else
            {
               guids = guids.subList(i + 1, guids.size());
            }
            break;
         }
         else
         {
            // update the element with the filtered (or fixed) GUID
            guids.remove(i);
            guids.add(i,fixedGuid);
         }
      }

      List<Node> nodes = m_contentMgr.findItemsByGUID(guids, ms_config);
      if (nodes.size() == 0)
         return null;

      // walk the list of results, create the proxies, store in the map and
      // connect the nodes through the parent
      IPSNode last = null;
      int depth = nodes.size() - 1;
      int count = 0;
      Node rval = null;
      for (Node n : nodes)
      {
         IPSNode cur = (IPSNode) n;
         cur.setDepth(depth--);
         // Create the proxy and store it
         PSNavAxisEnum axis = PSNavAxisEnum.ANCESTOR;
         if (count == 0)
         {
            axis = PSNavAxisEnum.SELF;
         }
         else if (count == 1)
         {
            axis = PSNavAxisEnum.PARENT;
         }
         Node pnode = createProxyNode(cur, axis, PSSectionTypeEnum.section);
         
         if (last != null)
         {
            last.setParent(pnode);
         }
         last = cur;
         PSLegacyGuid guid = (PSLegacyGuid) cur.getGuid();
         m_foundProxies.put(guid.getContentId(), pnode);
         if (count == 0)
         {
            rval = pnode;
         }
         count++;
      }

      return rval;
   }

   /**
    * Navons must be filtered according to the item filter. Here we first find
    * the associated folders and create filter items. Then we filter them, and
    * finally extract the guids of the items that remain
    * 
    * @param guids the navon guids, assumed not <code>null</code>
    * @return a list of guids that has been filtered, {@link NNTPConnectionClosedException}
    * @throws PSCmsException
    * @throws PSFilterException
    */
   private List<IPSGuid> filterNavons(List<IPSGuid> guids)
         throws PSCmsException, PSFilterException
   {
      if (guids.isEmpty())
         return guids;

      Collection<Integer> ids = new ArrayList<>();
      IPSGuid siteid = m_assemblyItem.getSiteId();
      Map<Integer, IPSGuid> mapToGuid = new HashMap<>();
      for (IPSGuid g : guids)
      {
         PSLegacyGuid lg = (PSLegacyGuid) g;
         ids.add(lg.getContentId());
         mapToGuid.put(lg.getContentId(), lg);
      }

      // Get the folder parents of the passed guids
      PSRelationshipFilter filter = new PSRelationshipFilter();
      filter.setDependentIds(ids);
      filter.setCommunityFiltering(false);
      filter.setName(PSRelationshipFilter.FILTER_NAME_FOLDER_CONTENT);
      PSRelationshipSet relSet = m_relProc.getRelationships(filter);
      List<IPSFilterItem> filterset = new ArrayList<>();
      for (int i = 0; i < relSet.size(); i++)
      {
         PSRelationship rel = (PSRelationship) relSet.get(i);
         IPSGuid folderid = new PSLegacyGuid(rel.getOwner());
         IPSGuid itemid = mapToGuid.get(rel.getDependent().getId());
         filterset.add(new PSFilterItem(itemid, folderid, siteid));
      }
      // Filter
      List<IPSFilterItem> filteredset = m_assemblyItem.getFilter().filter(
            filterset, m_params);

      // Create return list
      Map<Integer,PSLegacyGuid> filteredItemToId = 
         new HashMap<>();
      for (IPSFilterItem fitem : filteredset)
      {
         PSLegacyGuid lg = (PSLegacyGuid) fitem.getItemId();
         filteredItemToId.put(lg.getContentId(), lg);
      }
      Iterator<IPSGuid> giter = guids.iterator();
      List<IPSGuid> rval = new ArrayList<>();
      while (giter.hasNext())
      {
         PSLegacyGuid itemid = (PSLegacyGuid) giter.next();
         PSLegacyGuid filteredItemid = filteredItemToId.get(itemid
               .getContentId());
         if (filteredItemid != null)
         {
            rval.add(filteredItemid);
         }
      }
      return rval;
   }

   /**
    * Finds the parent folder id of the source item. If sys_folderid argument is
    * present then that folder id is returned. If the source item exists in only
    * one folder then that folder is returned. If the source item exists in more
    * than one folder then uses the supplied siteid to determine, which of the
    * folders exists under the given site. If more than one folder exist under
    * the given site then alphbatically first folder is returned.
    * 
    * @param sourceItem IPSAssemblyItem assumed not <code>null</code>.
    * @return PSComponentSummary of the sourceItem's folder or <code>null</code>
    *         if not found.
    */
   private int findFolder(IPSAssemblyItem sourceItem)
   {
      int originalFolderId = NumberUtils.toInt(sourceItem.getParameterValue(IPSHtmlParameters.SYS_ORIGINALFOLDERID, "-1"));
      if (sourceItem.getFolderId() != 0)
      {
         return sourceItem.getFolderId();
      }
      else if (originalFolderId>0)
      {
         return originalFolderId;
      }

      int folderid = 0;

      log.debug("The parent folderid (sys_folderid) is not specified, "
            + "finding the navon folder from item relationships for id=" + sourceItem.getId().getUUID());
      // Find the items folder from relationships
      String siteidstr = sourceItem.getParameterValue(
            IPSHtmlParameters.SYS_SITEID, "");
      PSRelationshipSet relationships = null;
      try
      {
         PSRelationshipFilter filter = new PSRelationshipFilter();
         PSLegacyGuid guid = (PSLegacyGuid) sourceItem.getId();
         PSLocator dependent = new PSLocator(guid.getContentId(), guid
               .getRevision());
         filter.setDependent(dependent);
         filter.setName(PSRelationshipFilter.FILTER_NAME_FOLDER_CONTENT);

         relationships = m_relProc.getRelationships(filter);
         // If the item is not in any folder return null
         if (relationships.isEmpty())
         {
            log.error("The supplied source item(" + sourceItem.getId()
                  + ") does not exist in any folder");
         }
         // If the item is in only one folder, that is the folder we want
         else if (relationships.size() == 1)
         {
            PSRelationship rel = (PSRelationship) relationships.get(0);
            folderid = rel.getOwner().getId();
         }
         // If the item is in more than one folder then find the right folder
         // using the supllied siteid
         else if (relationships.size() > 1)
         {
            if (!StringUtils.isEmpty(siteidstr))
            {
               int siteid = PSSiteHelper.getSiteFolderId(siteidstr);
               if (siteid > 1)
               {
                  // Now loop through all relstionships and keep only the
                  // site descendent rel
                  PSLocator siteLoc = new PSLocator(siteid);
                  Set<PSRelationship> rels = new HashSet<>();
                  for (int i = 0; i < relationships.size(); i++)
                  {
                     boolean isdesc = m_relProc.isDescendent(
                           PSRelationshipProcessorProxy.RELATIONSHIP_COMPTYPE,
                           siteLoc, ((PSRelationship) relationships.get(i))
                                 .getOwner(),
                           PSRelationshipConfig.TYPE_FOLDER_CONTENT);
                     if (!isdesc)
                        rels.add((PSRelationship)relationships.get(i));
                  }
                  relationships.removeAll(rels);
               }
            }
            // After filtering if we are left with 0 items return null
            if (relationships.isEmpty())
            {
               log
                     .error("The supplied source item("
                           + sourceItem.getId()
                           + ") does not exist in any folder under the supplied site("
                           + siteidstr + ")");

            }
            // If we are left with only one folder, that is the folder we
            // want
            else if (relationships.size() == 1)
            {
               PSRelationship rel = (PSRelationship) relationships.get(0);
               folderid = rel.getOwner().getId();
            }
            else if (relationships.size() > 0)
            {
               // Now we have the filtered relationships.
               // Create a folder summaries and return alphabatically first
               // folder
               List<Integer> folderids = new ArrayList<>();
               for (int i = 0; i < relationships.size(); i++)
               {
                  PSRelationship rel = (PSRelationship) relationships.get(i);
                  folderids.add(rel.getOwner().getId());
               }
               List<PSComponentSummary> folderSummaries = m_cmsObjMgr
                     .loadComponentSummaries(folderids);
               PSComponentSummary folderSummary = (PSComponentSummary) folderSummaries
                     .get(0);
               for (int j = 0; j < folderSummaries.size(); j++)
               {
                  PSComponentSummary tmp = (PSComponentSummary) folderSummaries
                        .get(j);
                  if (tmp.getName().compareTo(folderSummary.getName()) < 0)
                     folderSummary = tmp;
               }
               folderid = folderSummary.getContentId();
            }
         }
      }
      catch (PSNotFoundException | PSCmsException e)
      {
         log.error("problem finding the parent folder", e);
      }
      catch (PSSiteManagerException e)
      {
         log.error("problem finding the parent folder", e);
      }

      return folderid;
   }

   /**
    * Finds the navon/navTree content item for the given folder locator.
    * Recurses if the given folder does not have a content item of navon or
    * navTree content types.
    * 
    * @param folderLoc locator of the folder for which the navon or navTree
    *           content item needs to find.
    * @return PSLocator of navon or navTree contenttype item. May be
    *         <code>null</code> if supplied folder id is null or unable to
    *         find the navon or navTree content item.
    */
   public PSLocator findNavon(PSLocator folderLoc)
   {
      if (folderLoc == null)
      {
         log.error("Could not find navon/navTree content item."
               + " The supplied folderLoc is null or empty");
         return null;
      }

      if (folderLoc.getId() == 1) {
         log.warn("Previewing item using navigation outside of a site");
         return null;
      }
      PSRelationshipFilter filter = new PSRelationshipFilter();
      filter.setOwner(folderLoc);
      filter.setName(PSRelationshipFilter.FILTER_NAME_FOLDER_CONTENT);
      filter.setDependentContentTypeIds(m_navCtypes);
      filter.setCommunityFiltering(false);

      PSRelationshipSet relSet = null;
      try
      {
         relSet = m_relProc.getRelationships(filter);
         if (relSet.isEmpty())
         {
            PSRelationshipFilter filter1 = new PSRelationshipFilter();
            filter1.setDependent(folderLoc);
            filter1.setName(PSRelationshipFilter.FILTER_NAME_FOLDER_CONTENT);
            filter1.setCommunityFiltering(false);
            relSet = m_relProc.getRelationships(filter1);
            //The PSRelationshipSet relset above was returning two rows one for the folder associated with page and the other for folder associated to recycler.
            //Thus failing returning null and breaking the navon on the page.
            //Removed the recycled association from the PSRelationshipSet for the folder thus giving correct parent folder and single result.
            //This preserves the folder in recycle bin avoiding the need to delete the duplicate relation in PSX_OBJECTRELATIONSHIP table.
            for(int i = 0; i<relSet.size();i++){
               PSRelationship r = (PSRelationship) relSet.get(i);
               if(r.getConfig().getName()!=null && r.getConfig().getName().equalsIgnoreCase(PSRelationshipConfig.TYPE_RECYCLED_CONTENT)){
                  relSet.remove(i);
               }
            }
            if (relSet.size() != 1)
            {
               log.debug("Invalid folder structure."
                     + " Failed to get proper parent folder for folder id:"
                     + folderLoc.getId());
            return null;
            }

         return findNavon(((PSRelationship) relSet.get(0)).getOwner());
         }
         else if (relSet.size() == 1)
         {
            return ((PSRelationship) relSet.get(0)).getDependent();
         }
         else
         {
            log.debug("The folder with id(" + folderLoc.getId()
                  + ") in the hierarchy has multiple items of"
                  + " navon/navTree content types");

            return null;
         }
      }
      catch (PSCmsException e)
      {
         log.error(e);
         return null;
      }
   }

   /**
    * Find and return all the children of the given node. This method will find
    * and load the image, landing page and submenu related nodes, all in one go.
    * <p>
    * The axis on the submenu nodes will be determined by the parent. If the
    * parent is the self node, then the axis is set to CHILD
    * 
    * @param axis the axis of the parent, never <code>null</code>
    * @param parentNode the parent node being manipulated, never
    *           <code>null</code>
    * @return the children of the given node as a multi map. The key is the
    *         child name, and the value of the child is a collection
    * @throws PSCmsException
    * @throws RepositoryException
    * @throws PSFilterException
    */
   @SuppressWarnings("unchecked")
   public MultiValuedMap findNavChildren(PSNavAxisEnum axis, IPSNode parentNode)
         throws PSCmsException, RepositoryException, PSFilterException
   {
      if (axis == null) 
      {
         throw new IllegalArgumentException("axis may not be null");
      }
      if (parentNode == null)
      {
         throw new IllegalArgumentException("containedNode may not be null");
      }

      PSLegacyGuid curGuid = (PSLegacyGuid) parentNode.getGuid();
      PSLocator curLocator = curGuid.getLocator();
      PSRelationshipFilter filter = new PSRelationshipFilter();
      filter.setName(PSRelationshipFilter.FILTER_NAME_ACTIVE_ASSEMBLY);
      filter.setOwner(curLocator);
      filter.limitToOwnerRevision(true);
      PSRelationshipSet dependents = m_relProc.getRelationships(filter);
      // Create sorted sets to keep the children in the right order
      PSRelationshipSortOrderComparator c = new PSRelationshipSortOrderComparator();
      Set<PSRelationship> sortedNavImages = new TreeSet<>(c);
      Set<PSRelationship> sortedSubMenues = new TreeSet<>(c);

      List<IPSGuid> nguids = new ArrayList<>();
      List<IPSGuid> iguids = new ArrayList<>();

      for (PSRelationship r : ((Collection<PSRelationship>) dependents))
      {
         String slotid = r.getProperty("sys_slotid");
         if (slotid != null && (submenuSlots.contains(slotid)))
         {
            sortedSubMenues.add(r);
            nguids.add(new PSLegacyGuid(r.getDependent().getId()));
         }
         else if (slotid != null && imageSlots.contains(slotid))
         {
            sortedNavImages.add(r);
            iguids.add(new PSLegacyGuid(r.getDependent().getId()));
         }
      }

      // Filter
      nguids = filterNavons(nguids);
      iguids = filterFolderContent(parentNode, iguids);
      List<IPSGuid> guids = new ArrayList<>();
      guids.addAll(nguids);
      guids.addAll(iguids);

      if (log.isDebugEnabled())
      {
         log.debug("findNavChildren({}, {}), depends = {}",
                 axis,
                 curLocator.getId(),
                 guids);
      }

      MultiValuedMap rval = new ArrayListValuedHashMap<>();
      if (guids.isEmpty())
         return rval;

      // Load all the nodes
      List<Node> nodes = m_contentMgr.findItemsByGUID(guids, ms_config);

      // Put them all in a Map so they can be pulled out in order
      Map<Integer, Node> nodeMap = new HashMap<>();
      for (Node n : nodes)
      {
         IPSNode cn = (IPSNode) n;
         PSLegacyGuid lg = (PSLegacyGuid) cn.getGuid();
         nodeMap.put(lg.getContentId(), cn);
      }

      // Handle image children
      for (PSRelationship r : sortedNavImages)
      {
         Integer contentId = r.getDependent().getId();
         Node n = nodeMap.get(contentId);
         if (n == null)
         {
            log.warn("Didn't load node for image id {}" , contentId);
         }
         else
         {
            rval.put("nav:image", n);
         }
      }

      // Handle navon children
      PSNavAxisEnum newaxis;

      if (axis.equals(PSNavAxisEnum.SELF)
            || axis.equals(PSNavAxisEnum.DESCENDANT))
         newaxis = PSNavAxisEnum.DESCENDANT;
      else if (axis.equals(PSNavAxisEnum.PARENT))
         newaxis = PSNavAxisEnum.SIBLING;
      else
         newaxis = PSNavAxisEnum.NONE;

      boolean found;
      
      PSLocator parentFolder = getParentFolder(new PSLocator(parentNode.getGuid().getUUID()));
      List<PSLocator> childFolders = getChildFolders(parentFolder);
      
      for (PSRelationship r : sortedSubMenues)
      {
         Integer contentId = r.getDependent().getId();
         PSLocator nodeFolder = getParentFolder(r.getDependent());
         
         boolean isSectionLink = !childFolders.contains(nodeFolder);
         Node n = null;
         if(!isSectionLink)
         {
            n = m_foundProxies.get(contentId);
         }
         if (n == null)
         {
            n = nodeMap.get(contentId);
            found = false;
         }
         else
         {
            found = true;
         }

         if (n == null)
         {
            // Ignore, we may have filtered the navon
         }
         else
         {
            if (!found)
            {
               IPSNode cn = (IPSNode) n;
               PSSectionTypeEnum type = isSectionLink?PSSectionTypeEnum.sectionlink:PSSectionTypeEnum.section;
               n = createProxyNode(cn, newaxis, type);
               cn.setParent(parentNode);
               if(!isSectionLink)
                  m_foundProxies.put(contentId, n);
            }
            rval.put("nav:submenu", n);
         }
      }

      return rval;
   }


   
   /**
    * Helper method to get the parent folder of the supplied item content id.
    * @param psLocator
    * @return Parent folder guid, may be <code>null</code> if not found.
    * @throws PSCmsException
    */
   private PSLocator getParentFolder(PSLocator psLocator) throws PSCmsException
   {
      IPSFolderRelationshipCache folderCache = PSFolderRelationshipCache
            .getInstance();
      
      List<PSLocator> parents = folderCache.getParentLocators(psLocator);
      if (parents.isEmpty())
         log.warn("Navon with id {} is not in any folder", psLocator.getId());
      else if (parents.size()>1)
         log.warn("Navon with id {} is in multiple folders {}", psLocator.getId(),
                 parents);
      
      return !parents.isEmpty() ? parents.get(0): null;
   }

   /**
    * Helper method to get the child folders of the supplied parent guid.
    * @param locator parent locator, assumed not <code>null</code>.
    * @return never <code>null</code> may be empty.
    * @throws PSCmsException 
    */
   private List<PSLocator> getChildFolders(PSLocator locator) throws PSCmsException
   {
      IPSFolderRelationshipCache folderCache =  PSFolderRelationshipCache
            .getInstance();
      
      return folderCache.getChildLocators(locator);
      
   }

   /**
    * Creates a proxy node from the specified "contained" node.
    * 
    * @param node the contained node of the created proxy node, assumed not
    * <code>null</code>.
    * @param axis the axis of the node.
    * 
    * @return the created proxy node, never <code>null</code>.
    */
   private Node createProxyNode(IPSNode node, PSNavAxisEnum axis, PSSectionTypeEnum type)
   {
      PSNavonNodeInvocationHandler handler = 
         new PSNavonNodeInvocationHandler(this, node, axis, type);
      IPSProxyNode proxyNode = (IPSProxyNode) Proxy.newProxyInstance(getClass()
            .getClassLoader(), ms_interfaces, handler);
      handler.setProxyNode(proxyNode);
      
      return proxyNode;
   }
   
   /**
    * Filter the image items in a folder using the item filter from the 
    * assembly
    * @param parentNode the parent node, assumed never <code>null</code>
    * @param iguids the image item guids, assumed never <code>null</code>
    * @return those image item guids that pass the item filter
    * @throws PSCmsException
    * @throws PSFilterException
    */
   private List<IPSGuid> filterFolderContent(IPSNode parentNode,
         List<IPSGuid> iguids) throws PSCmsException, PSFilterException
   {
      // Find associated folder and filter guids
      PSRelationshipFilter filter = new PSRelationshipFilter();
      filter.setDependentId(((PSLegacyGuid) parentNode.getGuid())
            .getContentId());
      filter.setName(PSRelationshipFilter.FILTER_NAME_FOLDER_CONTENT);
      filter.setCommunityFiltering(false);
      PSRelationshipSet relSet = m_relProc.getRelationships(filter);
      if (relSet.size() == 0)
      {
         throw new RuntimeException("Navon node without associated folder: "
               + parentNode.getGuid());
      }
      if (relSet.size() > 1)
      {
         throw new RuntimeException("Navon node in too many folders: "
               + parentNode.getGuid());
      }
      PSRelationship rel = (PSRelationship) relSet.get(0);
      IPSGuid folderId = new PSLegacyGuid(rel.getOwner());
      IPSGuid siteId = m_assemblyItem.getSiteId();

      List<IPSFilterItem> items = new ArrayList<>();
      for (IPSGuid item : iguids)
      {
         items.add(new PSFilterItem(item, folderId, siteId));
      }
      List<IPSFilterItem> filtereditems = m_assemblyItem.getFilter().filter(
            items, m_params);
      List<IPSGuid> filteredids = new ArrayList<>();
      for (IPSFilterItem fitem : filtereditems)
      {
         filteredids.add(fitem.getItemId());
      }
      return filteredids;
   }


   private static PSRelationshipSet filterForSlotDependants(IPSNode node, String slotId) throws PSCmsException {
      PSLegacyGuid lg = (PSLegacyGuid) node.getGuid();
      PSRelationshipFilter filter = new PSRelationshipFilter();
      filter.setName(PSRelationshipFilter.FILTER_NAME_ACTIVE_ASSEMBLY);
      filter.setOwner(lg.getLocator());

      // without this it all revisions may be pulled from database regardless of locator revision then filtered in java.
      filter.limitToOwnerRevision(true);
      filter.setProperty("sys_slotid", slotId);
      return m_relProc.getRelationships(filter);
   }
   /**
    * Find the landing page node associated with the given navon guid
    * 
    * @param node the navon's node, never <code>null</code>
    * @return the landing page, or <code>null</code> if no page is defined
    * @throws PSCmsException
    * @throws RepositoryException
    */
   public Property findLandingPage(IPSNode node) throws PSCmsException,
         RepositoryException
   {
      if (node == null)
      {
         throw new IllegalArgumentException("node may not be null");
      }

      PSRelationshipSet dependents = new PSRelationshipSet();
      for(String s : landingPageSlots) {
         PSRelationshipSet rs =filterForSlotDependants(node, s);
         if(!rs.isEmpty()){
            dependents.addAll(rs);
         }
      }


      // Use the relationships and filter out any that are not visible
      IPSGuid landingPageItem = null, 
               landingPageFolder = null,
               landingPageSite = null;
      
      if (!dependents.isEmpty())
      {
         List<IPSFilterItem> items = new ArrayList<>();
         for (Object dependent : dependents) {
            PSRelationship dep = (PSRelationship) dependent;
            String fid = dep.getProperty(PSRelationshipConfig.PDU_FOLDERID);
            String sid = dep.getProperty(PSRelationshipConfig.PDU_SITEID);
            IPSGuid item = m_gmgr.makeGuid(dep.getDependent());
            IPSGuid folderId = StringUtils.isNotBlank(fid) ? m_gmgr
                    .makeGuid(new PSLocator(fid)) : null;
            IPSGuid siteId = StringUtils.isNotBlank(sid) ? m_gmgr
                    .makeGuid(sid, PSTypeEnum.SITE) : null;
            items.add(new PSFilterItem(item, folderId, siteId));
         }
         try
         {
            List<IPSFilterItem> filtereditems = m_assemblyItem.getFilter().filter(
                  items, m_params);
            if (!filtereditems.isEmpty())
            {
               IPSFilterItem first = filtereditems.get(0);
               landingPageItem = first.getItemId();
               landingPageFolder = first.getFolderId();
               landingPageSite = first.getSiteId();
            }
         }
         catch (PSFilterException e)
         {
            log.error("Problem filtering landing pages.  Error: {}",
                    PSExceptionUtils.getMessageForLog(e));
            return null;
         }
      }
      
      if (landingPageItem == null)
      {
         if (navConfig.isNavonLandingPageRequired())
         {
            int count = landingPageWarnCount.get();
            if (count > 0)
            {
               
               log.warn("No landing page relationship found for content id {}"
               , node.getGuid().getUUID());
               if (landingPageWarnCount.decrementAndGet()==0)
               {
                  log.warn("Suppressing further landing page warnings.  To turn off warnings set navon.landingpage.required=false in rxconfig/Server/Navigation.properties");
               }
            }
         }
         return null;
      }
      List<Node> nodes = m_contentMgr.findItemsByGUID(
            Collections.singletonList(landingPageItem), ms_config);
      if (nodes.isEmpty())
      {
         log.error("No landing page node found for landing page id {}"
               , landingPageItem);
      }

      // Setup the template information
      PSRelationship landingPageRel = null;
      PSLocator landingPageLoc = m_gmgr.makeLocator(landingPageItem);
      for (Object dependent : dependents) {
         PSRelationship dep = (PSRelationship) dependent;
         if (dep.getDependent().getId() == landingPageLoc.getId()) {
            landingPageRel = dep;
            break;
         }
      }
      String vid = landingPageRel != null ? 
            landingPageRel.getProperty("sys_variantid") : null;
      if (StringUtils.isBlank(vid))
      {
         log.error("No template registered for landing page, id: {}",
                 landingPageItem);
      }
      else
      {
         m_cidToTemplate.put(landingPageItem,
               new PSGuid(PSTypeEnum.TEMPLATE, vid));
      }
      PSContentNode lnode = (PSContentNode)nodes.get(0);
      //If we have properties like folderid and siteid add them as 
      //nav:landingPageFolderId and nav:landingPageSiteId
      if (landingPageFolder != null)
      {
         PSLocator folder = m_gmgr.makeLocator(landingPageFolder);
         lnode.addProperty(new PSProperty(
               PROP_NAV_LANDINPAGE_FOLDERID, node, 
               folder.getId()));
      }
      if (landingPageSite != null)
      {
         lnode.addProperty(new PSProperty(
               PROP_NAV_LANDINPAGE_SITEID, node, 
               landingPageSite.longValue()));
      }
         
      return new PSProperty("nav:landingPage", node, lnode);
   }
   
   /**
    * Find the landing page node associated with the given navon guid
    * 
    * @param node the navon's node, never <code>null</code>
    * @return the landing page, or <code>null</code> if no page is defined
    * @throws PSCmsException
    * @throws RepositoryException
    */
   public static PSRelationship findLandingPageRel(IPSNode node) throws PSCmsException,
         RepositoryException
   {
      if (node == null)
      {
         throw new IllegalArgumentException("node may not be null");
      }

      PSRelationshipSet dependents = new PSRelationshipSet();
      for(String s : landingPageSlots) {
         PSRelationshipSet rs =filterForSlotDependants(node, s);
         if(!rs.isEmpty()){
            dependents.addAll(rs);
         }
      }

      // Use the relationships and filter out any that are not visible
      if (dependents.size()>0)
         return (PSRelationship)dependents.get(0);
      else
         return null;
   }
   
   

   /**
    * Get template guid for specified content item guid
    * 
    * @param cid content item guid, never <code>null</code>
    * @return corresponding template, may be <code>null</code> for a broken
    *         relationship
    */
   public IPSGuid getTemplateForContent(IPSGuid cid)
   {
      if (cid == null)
      {
         throw new IllegalArgumentException("cid may not be null");
      }
      return m_cidToTemplate.get(cid);
   }

   /**
    * Calculate and return the "right" image child
    * 
    * @param navon the navon proxy, never <code>null</code>
    * @return the image node, could be <code>null</code> if there's no match
    * @throws RepositoryException
    * @throws AccessDeniedException
    * @throws ItemNotFoundException
    */
   @SuppressWarnings("unchecked")
   public Node findSelectedImage(Node navon) throws ItemNotFoundException,
         AccessDeniedException, RepositoryException
   {
      if (navon == null)
      {
         throw new IllegalArgumentException("navon may not be null");
      }
      String sel_prop = navConfig.getImageSelector();
      Property color = ms_utils.findProperty(navon, sel_prop);

      NodeIterator niter = navon.getNodes("nav:image");

      while (niter.hasNext())
      {
         Node image = niter.nextNode();
         Property selval = image.getProperty(sel_prop);
         // If color is null then we just take the first - no selector
         if (color == null || color.equals(selval))
         {
            return image;
         }
      }

      return null;
   }

   /**
    * @return Returns the assemblyItem.
    */
   public IPSAssemblyItem getAssemblyItem()
   {
      return m_assemblyItem;
   }

   /**
    * Constant for property name nav landing page folder id. This property is
    * meant to hold the folderid that is stored in the navon and landing page
    * relationship. If no folderid is stored in the relationship this property
    * should be set to empty string.
    */
   public static String PROP_NAV_LANDINPAGE_FOLDERID = "nav:landingPageFolderId";

   /**
    * Constant for property name nav landing page site id. This property is
    * meant to hold the siteid that is stored in the navon and landing page
    * relationship. If no siteid is stored in the relationship this property
    * should be set to empty string.
    */
   public static String PROP_NAV_LANDINPAGE_SITEID = "nav:landingPageSiteId";
   
   /**
    * Describes the type of section. 
    */
   public enum PSSectionTypeEnum {
       /**
        * regular section.
        */
       section,
       
       /**
        * link to a regular section.
        */
       sectionlink,
       
       /**
        * external link type section.
        */
       externallink
       
   }

   public static boolean isNavSlot(String slot)
   {
      init();
      return isSubmenuSlot(slot) || isLandingSlot(slot) || isNavImageSlot(slot);
   }
       
   public static boolean isSubmenuSlot(String slot)
   {
      init();
      return submenuSlots.contains(slot);
   }
       
   public static boolean isLandingSlot(String slot)
   {
      init();
      return landingPageSlots.contains(slot);
   }
       
   public static boolean isNavImageSlot(String slot)
   {
      init();
      return imageSlots.contains(slot);
   }


   /***
    * Method to filter out any items that may be in the recycler, awaiting recovery or deletion
    * @param guid
    * @return
    */
   public boolean isInRecycler(String guid){
      boolean ret = false;

      try {
         int id = ((PSLegacyGuid)m_gmgr.makeGuid(guid)).getContentId();
         Collection<Integer> ids = new ArrayList<>();
         ids.add(id);

         PSRelationshipFilter filter = new PSRelationshipFilter();

         filter.setDependentIds(ids);
         filter.setCommunityFiltering(false);
         filter.setCategory(PSRelationshipFilter.FILTER_CATEGORY_RECYCLED);

         PSRelationshipSet relSet = m_relProc.getRelationships(filter);

         if(relSet.size()>0){
            ret = true;
         }
      } catch (PSCmsException e) {
         //NOTE: We may want to flip this logic and return true on error depending on behavior we see - NC
         log.warn("Unable to confirm if item: {} is in Recycler, assuming not. Error: {}",
                 guid,
                 PSExceptionUtils.getMessageForLog(e));
         log.debug("Recycler check failed with:",e);
      }

      return ret;
   }
}
