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
package com.percussion.services.filter.impl;

import com.percussion.cms.PSCmsException;
import com.percussion.server.PSRequest;
import com.percussion.server.webservices.PSServerFolderProcessor;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.filter.IPSFilterItem;
import com.percussion.services.filter.PSFilterException;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.sitemgr.PSSiteManagerException;
import com.percussion.services.sitemgr.PSSiteManagerLocator;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.string.PSStringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A item filter that mimics the behavior of authtype 101 in part. This filter
 * passes items that exist on the destination site, and that can be published on
 * the destination site by virtue of having a publishable template available.
 * 
 * @author dougrand
 */
public class PSSiteFolderFilter extends PSBaseFilter
{
   /**
    * Site folder filter logger
    */
   private static final Logger ms_log = LogManager.getLogger(PSSiteFolderFilter.class);

   /** (non-Javadoc)
    * @see com.percussion.services.filter.impl.PSBaseFilter#filter(java.util.List, java.util.Map)
    */
   @Override
   public List<IPSFilterItem> filter(List<IPSFilterItem> ids,
         Map<String, String> params) throws PSFilterException
   {
      // Get default site id from parameters
      String siteidstr = params.get(IPSHtmlParameters.SYS_SITEID);
         
      List<IPSFilterItem> removals = new ArrayList<>();
      PSServerFolderProcessor fproc = PSServerFolderProcessor.getInstance();
      IPSSiteManager smgr = PSSiteManagerLocator.getSiteManager();
      
      IPSGuid defaultsiteid = null;
      if (StringUtils.isNotBlank(siteidstr))
      {
         defaultsiteid = new PSGuid(PSTypeEnum.SITE, siteidstr);
      }

      for (IPSFilterItem id : ids)
      {
         IPSGuid siteid = id.getSiteId();
         if (siteid == null)
         {
            if (defaultsiteid == null)
            {
               ms_log.warn("No default siteid available, removing item "
                     + id.getItemId());
               removals.add(id);
               continue;
            }
            siteid = defaultsiteid;
         }
         IPSSite site = null;
         try
         {
            site = smgr.loadUnmodifiableSite(siteid);
         }
         catch (PSNotFoundException e)
         {
            ms_log.warn("Specified site " + siteid
                  + " doesn't exist, removing item " + id.getItemId());
            removals.add(id);
            continue;
         }
         try
         {
            String paths[];
            if (id.getFolderId() != null)
            {
               PSLegacyGuid flg = (PSLegacyGuid) id.getFolderId();
               paths = fproc.getItemPaths(flg.getLocator());
            }
            else
            {
               paths = fproc.getFolderPaths(((PSLegacyGuid) id.getItemId())
                     .getLocator());
            }
            String match = PSStringUtils.findMatchingLeftSubstring(site
                  .getFolderRoot(), paths);
            if (match == null)
            {
               removals.add(id);
               continue;
            }
         }
         catch (PSCmsException | PSNotFoundException e)
         {
            ms_log.warn("Problem getting paths for folder " + id.getFolderId()
                  + " removing item");
            removals.add(id);
            continue;
         }
      }
      ids.removeAll(removals);
      return ids;
   }

}
