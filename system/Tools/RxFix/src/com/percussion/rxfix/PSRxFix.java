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
package com.percussion.rxfix;

import com.percussion.error.PSExceptionUtils;
import com.percussion.rx.ui.jsf.beans.PSHelpTopicMapping;
import com.percussion.rxfix.dbfixes.*;
import com.percussion.server.IPSStartupProcessManager;
import com.percussion.server.PSStartupProcessManager;
import com.percussion.server.cache.PSCacheManager;
import com.percussion.server.cache.PSCacheProxy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * A framework program that runs a series of fixup modules for a Rhythmyx
 * installation. The modules are run in order, when you add a module, please
 * consider its position in the overall list. Each module implements
 * {@link IPSFix}.
 */
public class PSRxFix
{

   private static final Logger log = LogManager.getLogger(PSRxFix.class);

   /**
    * Represents and entry in the ui model
    */
   public class Entry
   {
      boolean mi_dofix;

      String mi_fixname;

      Class mi_fix;
      
      List<PSFixResult> mi_results;

      /**
       * Ctor
       * @param fixclass fix class must impleemnt {@link IPSFix}
       * @throws InstantiationException
       * @throws IllegalAccessException
       */
      public Entry(Class fixclass) throws InstantiationException,
            IllegalAccessException {
         // Instantiate to get the descriptive info
         mi_fix = fixclass;
         IPSFix fix = (IPSFix) mi_fix.newInstance();
         mi_fixname = fix.getOperation();
         mi_dofix = true;
      }

      /**
       * @return Returns the dofix.
       */
      public boolean isDofix()
      {
         return mi_dofix;
      }

      /**
       * @param dofix The dofix to set.
       */
      public void setDofix(boolean dofix)
      {
         mi_dofix = dofix;
      }

      /**
       * @return Returns the fix.
       */
      public Class getFix()
      {
         return mi_fix;
      }

      /**
       * @param fix The fix to set.
       */
      public void setFix(Class fix)
      {
         mi_fix = fix;
      }

      /**
       * @return Returns the fixname.
       */
      public String getFixname()
      {
         return mi_fixname;
      }

      /**
       * @param fixname The fixname to set.
       */
      public void setFixname(String fixname)
      {
         mi_fixname = fixname;
      }

      /**
       * @return Returns the results.
       */
      public List<PSFixResult> getResults()
      {
         return mi_results;
      }

      /**
       * @param results The results to set.
       */
      public void setResults(List<PSFixResult> results)
      {
         mi_results = results;
      }
   }

   /**
    * Set after the preview has been done, guards the page flow
    */
   private boolean m_previewDone = false;

   /**
    * Set after the fix run has been done, used in page flow
    */
   private boolean m_fixDone = false;

   /**
    * The array of fixes that exist. The order of these fixes is important.
    */
   private Class m_fixes[] = new Class[]
   {
      PSFixNextNumberTable.class, 
      PSFixContentStatusHistory.class,
      //PSFixContentStatusHistoryWFInfo.class, 
      PSFixOrphanedSlots.class, 
      PSFixBrokenRelationships.class,
      // PSFixOrphanedData.class, omitted since the data is missing 
      PSFixInvalidFolders.class,
      PSFixOrphanedFolders.class,
      PSFixInvalidFolderRelationships.class,
      PSFixDanglingAssociations.class,
      PSFixCommunityVisibilityForViews.class,
      PSFixTranslationRelationships.class,
      PSFixInvalidSysTitle.class,
      PSFixAllowedSitePropertiesWithBadSites.class,
      PSFixOrphanedContentChangeEvents.class,
      PSFixZerosInRelationshipProperties.class,
      PSFixOrphanedManagedLinks.class,
      PSFixStaleDataForContentTypes.class,
      PSFixPageCatalog.class,
           PSFixAcls.class,
           PSFixFormUrl.class,
           PSFixWidgetVisibility.class
   };

   /**
    * These entries dictate what do to for each fix. The data is presented and
    * modified in the UI as the model, and is used directly in the doFix call.
    * Initialized on reset or construction, and never <code>null</code> after.
    */
   private List<Entry> m_entries = null;

   /**
    * Ctor
    * @throws Exception 
    */
   public PSRxFix() throws Exception {
      init();
   }

   /**
    * Initialize state
    */
   private void init() throws Exception
   {
      m_previewDone = false;
      m_fixDone = false;
      m_entries = new ArrayList<Entry>();
      for(int i = 0; i < m_fixes.length; i++)
      {
         Entry e = new Entry(m_fixes[i]);
         e.setResults(null);
         m_entries.add(e);
      }
   }

   /**
    * @return Returns the previewDone.
    */
   public boolean isPreviewDone()
   {
      return m_previewDone;
   }

   /**
    * @return Returns the fixDone.
    */
   public boolean isFixDone()
   {
      return m_fixDone;
   }

   /**
    * @param fixDone The fixDone to set.
    */
   public void setFixDone(boolean fixDone)
   {
      m_fixDone = fixDone;
   }

   /**
    * @param previewDone The previewDone to set.
    */
   public void setPreviewDone(boolean previewDone)
   {
      m_previewDone = previewDone;
   }
   
   /**
    * Get entries, which include result data
    * @return the entries, never <code>null</code>
    */
   public List<Entry> getEntries()
   {
      return m_entries;
   }
   
   /**
    * Get only those entries that were actually run
    * @return the entries, might be empty
    */
   public List<Entry> getRunentries()
   {
      List<Entry> rval = new ArrayList<Entry>();
      
      for(Entry e : m_entries)
      {
         if (e.isDofix())
         {
            rval.add(e);
         }
      }
      
      return rval;
   }

   /**
    * Preview action
    * 
    * @return the outcome
    */
   public String preview()
   {
      try
      {
         doFix(true);
      }
      catch (Exception e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
      return "admin-rxfix-preview";
   }

   /**
    * Fix action
    * 
    * @return the outcome
    */
   public String next()
   {
      if (m_fixDone)
      {
         return "admin-rxfix";
      }
      
      try
      {
         doFix(false);
      }
      catch (Exception e)
      {
         log.error("PSRXFix Failed: " ,PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
      return "admin-rxfix-preview";
   }
   
   /**
    * UI label for next action
    * @return the label for the "next" button on the results page
    */
   public String getFixnextlabel()
   {
      return m_fixDone ? "Done" : "Fix";
   }

   /**
    * Reset action
    * 
    * @return outcome
    * @throws Exception 
    */
   public String reset() throws Exception
   {
      init();
      return "reset";
   }

   /**
    * Startup and do one or more fixes
    * 
    * @param preview Run the fixups in preview mode
    * @throws Exception if there is a problem setting up to perform the fixes
    * 
    */
   public void doFix(boolean preview) throws Exception{
      doFix(preview,null);
   }

   public void doFix(boolean preview, IPSStartupProcessManager startupProcessManager) throws Exception
   {
      boolean clearCache = false;
      for (Entry e : m_entries)
      {
         if (! e.isDofix())
            continue;

         // Instantiate
         IPSFix f = null;
         f = (IPSFix) e.getFix().newInstance();
         log.info("Executing update {} in Preview mode: {}", f.toString(),Boolean.toString(preview));
         f.fix(preview);

         if(startupProcessManager instanceof PSStartupProcessManager && f.removeStartupOnSuccess()){
            ((PSStartupProcessManager)startupProcessManager).removeStartupProcess(f.getClass().getSimpleName());
         }

         // Get results
         if(!clearCache) {
            clearCache = f.getResults().size() > 0;
         }
         e.setResults(f.getResults());
      }

      //Only Clear the cache if there was data changed.
      if (PSCacheManager.isAvailable() && clearCache && !preview) {
         PSCacheManager cacheManager = PSCacheManager.getInstance();
         cacheManager.flush();
         PSCacheProxy.flushFolderCache();
      }

      m_previewDone = preview;
      m_fixDone = !preview;
   }
   
   /**
    * Get the help file name for the RxFix page.
    * 
    * @return  the help file name, never <code>null</code> or empty.
    */
   public String getHelpFile()
   {
      return PSHelpTopicMapping.getFileName("RxFix");      
   }
   
}
