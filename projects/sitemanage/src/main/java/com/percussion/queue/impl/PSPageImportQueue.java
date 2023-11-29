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

package com.percussion.queue.impl;

import com.percussion.error.PSExceptionUtils;
import com.percussion.pagemanagement.service.IPSPageCatalogService;
import com.percussion.queue.IPSPageImportQueue;
import com.percussion.queue.PSAbstractEventQueue;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.notification.IPSNotificationListener;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.IPSSystemProperties;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.error.PSSiteImportException;
import com.percussion.sitemanage.service.IPSSiteImportService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.types.PSPair;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.percussion.share.service.IPSSystemProperties.IMPORT_PAGE_MAX;

@Component("pageImportQueue")
@Lazy
@Transactional
public class PSPageImportQueue extends PSAbstractEventQueue<PSSiteQueue>
		implements IPSPageImportQueue, IPSNotificationListener,
		IPSPerformPageImport {
	private Map<Long, PSSiteQueue> m_siteCache = new ConcurrentHashMap<>();
	private PSSiteQueue m_importingSite;
	private PSSiteImportCtx m_importContext = new PSSiteImportCtx();

	private IPSSiteImportService m_importService;
	private IPSIdMapper m_idMapper;
	private IPSSystemProperties systemProps;

	private boolean m_isServerStarted = false;
	// private IPSNotificationService m_notifyService;
	private IPSPageCatalogService m_pageCatalogService;
	private IPSSiteManager m_siteMgr;
	private static Object siteQueueLock = new Object();
	

	private static final Logger log = LogManager.getLogger(PSPageImportQueue.class);

	public PSPageImportQueue(@Qualifier("pageImportService") IPSSiteImportService importService,
			IPSIdMapper idMapper, IPSNotificationService notifyService,
			IPSPageCatalogService pageCatalogService, IPSSiteManager siteMgr) {
		super();
		this.m_importService = importService;
		this.m_idMapper = idMapper;
		this.m_pageCatalogService = pageCatalogService;
		this.m_siteMgr = siteMgr;

		registerServerShutdownNotification(notifyService);
	}

	private void registerServerShutdownNotification(
			IPSNotificationService notifyService) {
		notifyService.addListener(EventType.CORE_SERVER_SHUTDOWN, this);
		notifyService.addListener(EventType.CORE_SERVER_INITIALIZED, this);
		notifyService.addListener(EventType.SITE_DELETED, this);
	}

	public void addCatalogedPageIds(PSSite s, String userAgent,
			List<Integer> ids) {
		PSSiteQueue sq = getSiteQueue(s.getSiteId());
		if (sq.getUserAgent() == null) {
			sq.setSite(s);
			sq.setUserAgent(userAgent);
			sq.setRequestInfoMap();
		}

		sq.addCatalogedIds(ids);

		if (log.isDebugEnabled()) {
			log.debug("Site[{}][addCatalogedPageIds] ids = {}", s.getSiteId(), ids.toString());
		}

		// wake up the queue if needed.
		notifyEventQueue();
	}

	public void removeImportPage(String siteName, String pageId) {
		IPSSite site = m_siteMgr.findSite(siteName);
		if (site == null) {
			return;
		}

		PSSiteQueue sq = getSiteQueue(site.getSiteId());
		Integer id = m_idMapper.getContentId(pageId);
		sq.removeImportedId(id);
	}

	public void notifyEvent(PSNotificationEvent notification) {
		if (notification.getType() == EventType.CORE_SERVER_INITIALIZED) {
			setMaxImportCountForAllSites();
			m_isServerStarted = true;
			// Start the "page import queue" thread now
			start();
		} else if (notification.getType() == EventType.CORE_SERVER_SHUTDOWN) {
			m_importContext.setCanceled(true);
			doShutdown();
		} else if (notification.getType() == EventType.SITE_DELETED) {
			deleteSiteCache(notification);
		}
	}

	/**
     * Delegate shutdown to another thread to unblock the notification service
     */
    private void doShutdown()
    {
        Thread runner = new Thread(new Runnable()
        {
            
            @Override
            public void run()
            {
                shutdown();
            }
        });
        
        runner.setDaemon(true);
        runner.start();
    }

    private int getMaxImportPage() {
		String maxVal = systemProps.getProperty(IMPORT_PAGE_MAX);
		int max = NumberUtils.toInt(maxVal, -1);
		return max;
	}

	/**
	 * Set the system properties on this service. This service will always use
	 * the the values provided by the most recently set instance of the
	 * properties.
	 * 
	 * @param systemProps
	 *            the system properties
	 */
    @Autowired
	public void setSystemProps(IPSSystemProperties systemProps) {
		this.systemProps = systemProps;
		if (m_isServerStarted) {
			setMaxImportCountForAllSites();
		}
	}

	/**
	 * Gets the system properties used by this service.
	 * 
	 * @return The properties
	 */
	public IPSSystemProperties getSystemProps() {
		return systemProps;
	}

	public List<Integer> getImportingPageIds(Long siteId) {
		PSSiteQueue sq = getSiteQueue(siteId);
		return sq.getImportingIds();
	}

	public PSSiteQueue getPageIds(Long siteId) {
		return getSiteQueue(siteId);
	}

	public List<Integer> getCatalogedPageIds(Long siteId) {
		return getSiteQueue(siteId).getCatalogedIds();
	}

	public List<Integer> getImportedPageIds(Long siteId) {
		return getSiteQueue(siteId).getImportedIds();
	}


	 public void addImportedId(Long siteId, Integer id)
	 {
		 getSiteQueue(siteId).addImportedId(id);
	 }
	
	 protected String getQueueName() {
		return "PageImportQueue";
	}

	protected void preStart() {
		// do nothing until we decide to handle auto-restart
	}

	protected boolean doRun() {
		try {
			PSPair<PSSiteQueue, Integer> nextEvent = null;
			PSSiteQueue importingSite = null;
			try {
				nextEvent = getNextQueueEvent(0);

				if (nextEvent == null) {
					return true;
				}
			} catch (InterruptedException e) {

				Thread.currentThread().interrupt();

				return false;
			} catch (Throwable t) {
				if (t instanceof ThreadDeath) {
					return false;
				}
			}
			if (nextEvent.getFirst() != null) {
				importingSite = nextEvent.getFirst();
			}
			else {
				return true;
			}
			List<Integer> ids = importingSite.getImportingIds();
			if (ids.size() == 0) {
				// The queue is empty now, nothing is waiting to be processed.
				return true;
			}

			PSSite site = importingSite.getSite();
			Integer id = null;
			if (nextEvent.getSecond() != null) {
				id = nextEvent.getSecond();
			}
			try {
				setRequestInfo(importingSite);
				m_pageImporter.performPageImport(site, id,
						importingSite.getUserAgent());
				// check to see if search indexing needs to restart
				importingSite.checkSearchIndexQueueStatus(false);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			} catch (Throwable t) {
				if (t instanceof ThreadDeath) {
					return false;
				}

				log.error("Failed to import page id={}, for site: {}, Error: {}"
						, id, site.getName(), t.getMessage());
				log.debug(t.getMessage(),t);
				importingSite.removeImportingId(id);
			}

			return true;
		} catch (Exception e) {
			return true;
		}
	}

	private void setRequestInfo(PSSiteQueue importingSite) {
		if (PSRequestInfo.isInited()) {
			PSRequestInfo.resetRequestInfo();
		}
		PSRequestInfo.initRequestInfo(importingSite.getRequestInfoMap());
	}

	/**
	 * Invoke the page import process for the specified page
	 * 
	 * @param site
	 *            the site of the page, assumed not <code>null</code>.
	 * @param id
	 *            the ID of the page, assumed not <code>null</code>.
	 * 
	 * @throws InterruptedException
	 */
	public void performPageImport(PSSite site, Integer id, String userAgent)
			throws InterruptedException, PSSiteImportException {
		String pageId = m_idMapper.getString(new PSLegacyGuid(id, -1));
		//TODO: make this a parameter
		PSSiteImportCtx siteImportContext = new PSSiteImportCtx();
		siteImportContext.setCanceled(this.m_importContext.isCanceled());
		m_importService.importCatalogedPage(site, pageId, userAgent,
				siteImportContext);
	}

	/**
	 * Defaults to the current instance, but it can be reset for the unit test
	 */
	private IPSPerformPageImport m_pageImporter = this;

	public void setPageImporter(IPSPerformPageImport pageImporter) {
		m_pageImporter = pageImporter;
	}

	public IPSPerformPageImport getPageImporter() {
		return m_pageImporter;
	}

	protected void preShutdown() {
		// signal current job to stop processing
		m_importContext.setCanceled(true);
	}

	protected PSPair<PSSiteQueue, Integer> getNextEvent() {
		PSPair<PSSiteQueue, Integer> pair = new PSPair<>();
		if (m_importingSite == null) {
			PSSiteQueue sq = getNextWaitingSite();
			if (sq == null) {
				return null;
			}
			m_importingSite = sq;
		}

		Integer id = m_importingSite.getNextId();
		if (id == null) {
			m_importingSite = getNextWaitingSite();
			if (m_importingSite != null) {
				id = m_importingSite.getNextId();
				if (id == null) {
					return null;
				}
			}
		}

		pair.setFirst(m_importingSite);
		pair.setSecond(id);
		return pair;
	}

	private void setMaxImportCountForAllSites() {
		int max = getMaxImportPage();
		for (PSSiteQueue sq : m_siteCache.values()) {
			sq.setMaxImportCount(max);
		}
	}

	private void deleteSiteCache(PSNotificationEvent notification) {
		IPSGuid siteId = (IPSGuid) notification.getTarget();
		Long id = siteId.longValue();
		m_siteCache.remove(id);
	}

	private PSSiteQueue getNextWaitingSite() {
		for (PSSiteQueue sq : m_siteCache.values()) {
			if (sq.containsPagesForImport()) {
				return sq;
			}
		}

		return null;
	}

	private PSSiteQueue getSiteQueue(Long siteId) {
		PSSiteQueue siteQueue = m_siteCache.get(siteId);
		if (siteQueue == null) {
		    synchronized(siteQueueLock)
		    {
		        if (m_siteCache.get(siteId)==null)
		        {
        			siteQueue = createSiteQueue(siteId);
        			siteQueue.setMaxImportCount(getMaxImportPage());
        			m_siteCache.put(siteId, siteQueue);
		        }
		    }
		}
		return siteQueue;
	}

	public void dirtySiteQueue(Long siteId) {
		m_siteCache.remove(siteId);
	}

	private PSSiteQueue createSiteQueue(Long siteId) {
		String siteName = getSiteName(siteId);
		try {
			if (siteName == null) {
				return new PSSiteQueue();
			}

			PSSiteQueue siteQueue = new PSSiteQueue();

			List<String> importedPages = m_pageCatalogService
					.findImportedPageIds(siteName);
			siteQueue.addImportedIds(getContentIds(importedPages));

			List<String> catalogedPages = m_pageCatalogService
					.findCatalogPages(siteName);
			siteQueue.addCatalogedIds(getContentIds(catalogedPages));

			return siteQueue;
		} catch (Exception e) {
			log.error(
					"An error occurred when getting the imported and cataloged pages for site name: {}, Error: {}"
							, siteName,PSExceptionUtils.getMessageForLog(e));
			log.debug(PSExceptionUtils.getDebugMessageForLog(e));
			return new PSSiteQueue();
		}
	}

	private String getSiteName(Long siteId) {
		try {
			IPSGuid id = new PSGuid(PSTypeEnum.SITE, siteId);
			IPSSite site = m_siteMgr.findSite(id);
			return site != null ? site.getName() : null;
		} catch (Throwable e) {
			return null;
		}
	}

	/**
	 * Builds a list of content ids from a list of complete ids.
	 * 
	 * @param ids
	 *            {@link List}<{@link String}> with the string representation of
	 *            ids. Assumed not <code>null</code>.
	 * @return {@link List}<{@link Integer}> with the content ids. Never
	 *         <code>null</code>, but may be empty.
	 */
	private List<Integer> getContentIds(List<String> ids) {
		List<Integer> contentIds = new ArrayList<>();
		for (String id : ids) {
			contentIds.add(((PSLegacyGuid) m_idMapper.getGuid(id))
					.getContentId());
		}
		return contentIds;
	}

}
