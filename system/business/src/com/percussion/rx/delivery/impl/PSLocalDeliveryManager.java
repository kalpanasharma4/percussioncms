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
package com.percussion.rx.delivery.impl;

import com.percussion.cms.IPSConstants;
import com.percussion.error.PSExceptionUtils;
import com.percussion.rx.delivery.IPSDeliveryErrors;
import com.percussion.rx.delivery.IPSDeliveryHandler;
import com.percussion.rx.delivery.IPSDeliveryItem;
import com.percussion.rx.delivery.IPSDeliveryManager;
import com.percussion.rx.delivery.IPSDeliveryResult;
import com.percussion.rx.delivery.IPSDeliveryResult.Outcome;
import com.percussion.rx.delivery.PSDeliveryException;
import com.percussion.rx.delivery.data.PSDeliveryResult;
import com.percussion.rx.publisher.IPSPublisherJobStatus.ItemState;
import com.percussion.rx.publisher.IPSRxPublisherServiceInternal;
import com.percussion.rx.publisher.data.PSPubItemStatus;
import com.percussion.rx.publisher.impl.PSPublishHandler;
import com.percussion.server.PSServer;
import com.percussion.services.PSBaseServiceLocator;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.publisher.IPSDeliveryType;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.publisher.PSPublisherServiceLocator;
import com.percussion.services.pubserver.IPSPubServer;
import com.percussion.services.pubserver.IPSPubServerDao;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.sitemgr.PSSiteManagerLocator;
import com.percussion.util.PSStopwatch;
import com.percussion.utils.types.PSPair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The local delivery manager simply looks up the appropriate publisher plugin,
 * prepares the data for delivery and calls the plugin.
 * <p>
 * For each new plugin for a given job, the manager initializes the plugin. It
 * records this information so that when the {@link #commit(long)} or
 * {@link #rollback(long)} methods are called it can delegate to all the 
 * handlers involved for the given job.
 * 
 * @author dougrand
 * 
 */
public class PSLocalDeliveryManager implements IPSDeliveryManager
{
   /**
    * Logger used for delivery manager.
    */
   private static final Logger log = LogManager.getLogger(IPSConstants.PUBLISHING_LOG);

   /**
    * A map joining the job id to a map of activated delivery handlers. Used to
    * deliver content and do abort and commit behavior.
    */
   private Map<Long, Map<String, IPSDeliveryHandler>> jobToHandler =
      new HashMap<>();

   /**
    * A map recording the site used for each job.
    * 
    * TODO: Consider caching delivery type beans in memory
    * in the publisher service
    */
   private Map<Long, PSPair<IPSSite,IPSPubServer>> m_jobToSiteServer = new HashMap<>();

   /**
    * The temporary directory for the assembled content (before deliver to
    * the target location). It can be set by Spring bean's property.
    * 
    * Default to {@link #DEFAULT_TMP_DIR} under the Rhythmyx install root.
    */
   private volatile File m_tempDir = null;
   
   /**
    * The default temporary directory relative to the Rhythmyx installation root.
    */
   public static final String DEFAULT_TMP_DIR = "temp/publish";
   
   private final Object tempDirLock = new Object();

   /**
    * The rx publish service, (auto) wired by spring
    */
   @Autowired
   @java.lang.SuppressWarnings("java:S1170")
   private final IPSRxPublisherServiceInternal m_rxPubService = null;
   
   
   public IPSDeliveryResult process(IPSDeliveryItem result)
   {
      IPSPublisherService svc = PSPublisherServiceLocator.getPublisherService();
      try
      {
         IPSDeliveryType loc = svc.loadDeliveryType(result
               .getDeliveryType());
         IPSDeliveryHandler bean = getHandlerForJob(result.getJobId(), loc, result);
         if (bean == null)
         {
            return new PSDeliveryResult(Outcome.FAILED,
                  "Cannot find delivery handler", result.getId(), result
                        .getJobId(), result.getReferenceId(), result.getDeliveryContext(), null);            
         }
         
         result.setTempDir(getTempDirFile());
         
         // Decide whether we're calling deliver or remove. We need to do this
         // if we're unpublishing, but the deliver type require assembly.
         boolean callDeliver = true;
         if (!result.isPublish())
         {
            callDeliver = loc.isUnpublishingRequiresAssembly();
         }
         if (callDeliver)
         {
            log.debug("Deliver: {}" , result.getReferenceId());
            return bean.deliver(result);
         }
         else
         {
            log.debug("Remove: {}" , result.getReferenceId());
            return bean.remove(result);
         }
      }
      catch (Exception e)
      {
         log.error("Failed delivery: {} Error: {}" , result.getReferenceId(), PSExceptionUtils.getMessageForLog(e));
         return new PSDeliveryResult(Outcome.FAILED, PSExceptionUtils.getMessageForLog(e),
               result.getId(), result.getJobId(), result.getReferenceId(), result.getDeliveryContext(),
               null);
      }
   }

   /**
    * Sets the temporary directory for storing assembled content before
    * deliver to the target location. This is typically called by Spring bean
    * framework.
    * Note, this is expected to be called once at server startup. It will clean
    * up any files under the temporary directory at the 1st call.
    * 
    * @param tmpDirPath the temporary directory path. It is ignored if
    *    <code>null</code> or empty.
    *    
    * @throws UnsupportedOperationException if attempt to set the temporary 
    *    directory more than once.
    */
   public void setTempDir(String tmpDirPath)
   {
      if (StringUtils.isBlank(tmpDirPath))
      {
         log.warn("Ignore empty value of the tempDir property");
         return;
      }
      
      if (m_tempDir != null)
         throw new UnsupportedOperationException(
               "The temporary directory cannot be reset.");
      
      synchronized(tempDirLock)
      {
         m_tempDir = new File(tmpDirPath);
         initTempDir(m_tempDir);
      }
   }

   /**
    * Initialize the temporary directory. 
    * 
    * @param tempDir the temporary directory, assumed not <code>null</code>.
    */
   private void initTempDir(File tempDir)
   {
      tempDir.mkdirs();
      if ((!tempDir.exists()) || (!tempDir.isDirectory()))
      {
         log.error("Failed to create temp directory, {}"
               , tempDir.getAbsolutePath());
         return;
      }

      // clean up the temporary directory.
      try
      {
         FileUtils.cleanDirectory(tempDir);
      }
      catch (IOException e)
      {
         log.error(
               "Failed to clean temporary directory: {} Error: {}"
                     , tempDir.getAbsolutePath(), PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
   }
   
   /**
    * Gets the temporary directory path.
    *  
    * @return the temporary directory path, it may be <code>null</code> if
    *    uses the default temporary directory.
    */
   public String getTempDir()
   {
      return getTempDirFile().getAbsolutePath();
   }
  
   /**
    * Get the temporary directory object. If the temporary directory has not 
    * been set (or specified in the Spring bean), then the temporary directory 
    * will be set to {@link #DEFAULT_TMP_DIR} under the Rhythmyx install root.
    * 
    * @return the temporary directory object, never <code>null</code>.
    */
   private File getTempDirFile()
   {
      File result = m_tempDir;
      if (result != null) // First check (no locking)
         return result;
      synchronized(this) {
         if (m_tempDir == null) // Second check (with locking)
            m_tempDir = new File(PSServer.getRxDir(), DEFAULT_TMP_DIR);
         initTempDir(m_tempDir);
         return m_tempDir;
      }

   }

   /**
    * Get the delivery handler given the job and type. If the handler hasn't
    * been loaded yet for the job, the init method is called.
    * 
    * @param jobId the job id
    * @param loc the delivery type, assumed never <code>null</code>
    * @return the handler, may be <code>null</code> if the job does not exist
    * @throws PSDeliveryException 
    */
   private IPSDeliveryHandler getHandlerForJob(long jobId, IPSDeliveryType loc, 
         IPSDeliveryItem result) throws PSDeliveryException, PSNotFoundException {
      IPSDeliveryHandler handler = null;
      synchronized (jobToHandler)
      {
         Map<String, IPSDeliveryHandler> hmap = jobToHandler.computeIfAbsent(jobId, k -> new HashMap<>());
         handler = hmap.get(loc.getBeanName());
         if (handler == null)
         {
            IPSSite site = null;
            IPSPubServer server = null;
            PSPair<IPSSite,IPSPubServer> siteServer = m_jobToSiteServer.get(jobId);
            if (siteServer != null)
            {
               site = siteServer.getFirst();
               server = siteServer.getSecond();
            }
            if (site == null)
            {
               IPSSiteManager siteMgr = PSSiteManagerLocator.getSiteManager();
               site = siteMgr.loadSite(result.getSiteId());
               
               if (site == null)
                  return null; // site does not exist
            }
            
            if(server == null)
            {
               IPSPubServerDao pubSrvMgr = PSSiteManagerLocator.getPubServerDao();
               if (result.getPubServerId() != null)
                  server = pubSrvMgr.findPubServer(result.getPubServerId());
            }
            
            handler = (IPSDeliveryHandler) PSBaseServiceLocator.getBean(loc
                  .getBeanName());
            if (handler == null)
            {
               log.error("Couldn't load bean for delivery type: {}"
                     ,loc.getName());
               throw new PSDeliveryException(IPSDeliveryErrors.CANNOT_DELIVER_NO_DELIVERYTYPE,
                       loc.getName());
            }
            handler.init(jobId, site, server);
            hmap.put(loc.getBeanName(), handler);
         }
      }

      return handler;
   }

   public void rollback(long jobId) throws PSDeliveryException
   {
      Map<String, IPSDeliveryHandler> hmap;
      synchronized (jobToHandler)
      {
         hmap = jobToHandler.get(jobId);
      }
      
      if (hmap != null)
      {
         for(IPSDeliveryHandler handler : hmap.values())
         {
            handler.rollback(jobId);
         }
      }
      
      synchronized(jobToHandler)
      {
         jobToHandler.remove(jobId);
         m_jobToSiteServer.remove(jobId);
      }
   }

   /*
    * The commit method looks up the data associated with the job. It then
    * calls each associated delivery handler and asks it to commit the 
    * data being held.
    */
   public Collection<IPSDeliveryResult> commit(long jobId)
         throws PSDeliveryException
   {
      PSStopwatch watch = new PSStopwatch();
      watch.start();
      log.debug("Commit for job {} ...",jobId);

      Map<String, IPSDeliveryHandler> hmap;
      synchronized (jobToHandler)
      {
         hmap = jobToHandler.get(jobId);
      }
      Collection<IPSDeliveryResult> results = 
         new ArrayList<>();
      
      if (hmap != null)
      {
         for (IPSDeliveryHandler handler : hmap.values())
         {
            results.addAll(handler.commit(jobId));
         }

         synchronized (jobToHandler)
         {
            jobToHandler.remove(jobId);
            m_jobToSiteServer.remove(jobId);
         }
         log.debug("Commit: {}" , jobId);
      }
      else
      {
         log.debug("Job cancelled: {}" , jobId);
      }
      
      watch.stop();
      log.debug("Committed buffered {} files for job {}. Elapsed time: {}",results.size(),jobId,
             watch);

      return results;
   }

   /*
    * Initialize just associates site information with the job id to allow 
    * the delivery manager to later initialize the handlers.
    */
   public void init(long jobid, IPSSite site, IPSPubServer server)
   {
      synchronized (jobToHandler)
      {
         PSPair<IPSSite, IPSPubServer> siteServer = new PSPair<>();
         siteServer.setFirst(site);
         siteServer.setSecond(server);
         m_jobToSiteServer.put(jobid, siteServer);
      }
   }

   @java.lang.SuppressWarnings("java:S2583")
   public void updateItemState(IPSDeliveryResult result)
   {
      PSPair<IPSSite, IPSPubServer> jobId = m_jobToSiteServer.get(result.getJobId());
      IPSPubServer server = jobId.getSecond();
      final ItemState state = PSPublishHandler.OUTCOME_STATE.get(result.getOutcome());
      PSPubItemStatus status = new PSPubItemStatus(
            result.getReferenceId(), result.getJobId(), server.getServerId(), result.getDeliveryContext(), state);
      if (result.getUnpublishData() != null)
      {
         status.setUnpublishingInformation(result.getUnpublishData());
      }
      if (StringUtils.isNotBlank(result.getFailureMessage()))
      {
         status.addMessage(result.getFailureMessage());
      }
      status.setSiteId(jobId.getFirst().getGUID());

      if(m_rxPubService != null)
         m_rxPubService.updateItemState(status);
   }
   
   
}
