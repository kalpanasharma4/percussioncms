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

package com.percussion.cookieconsent.service.impl;

import com.percussion.cookieconsent.service.IPSCookieConsentService;
import com.percussion.delivery.client.IPSDeliveryClient.HttpMethodType;
import com.percussion.delivery.client.IPSDeliveryClient.PSDeliveryActionOptions;
import com.percussion.delivery.client.PSDeliveryClient;
import com.percussion.delivery.data.PSDeliveryInfo;
import com.percussion.delivery.service.IPSDeliveryInfoService;
import com.percussion.error.PSExceptionUtils;
import com.percussion.pubserver.IPSPubServerService;
import com.percussion.security.SecureStringUtils;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.util.PSSiteManageBean;
import com.percussion.utils.request.PSRequestInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import static com.percussion.share.service.exception.PSParameterValidationUtils.rejectIfBlank;

/**
 * Service which interfaces with DTS meta data
 * cookie consent service.
 * 
 * @author chriswright
 *
 */
@Path("/consent")
@PSSiteManageBean("cookieConsentService")
public class PSCookieConsentService implements IPSCookieConsentService {
    
    private static final Logger log = LogManager.getLogger(PSCookieConsentService.class);
    
    private static final String DTS_URL = "/perc-metadata-services/metadata/consent/log";
    
    private static final String TOTAL_ENTRIES_URL = DTS_URL + "/totals";
    @Autowired
    @Lazy
    private IPSPubServerService pubServerService;
    /**
     * The delivery service initialized by constructor, never <code>null</code>.
     */
    IPSDeliveryInfoService deliveryService;

    @Autowired
    public PSCookieConsentService(IPSDeliveryInfoService deliveryService) {
        this.deliveryService = deliveryService;
    }
    
    @Override
    @GET
    @Path("log/{csvFileName}")
    @Produces({"text/csv"})
    public String exportCookieConsentData(@PathParam("csvFileName") String csvFileName) {
        return exportCookieConsentData(null, csvFileName);
    }
    
    @Override
    @GET
    @Path("log/{siteName}/{csvFileName}")
    @Produces({"text/csv"})
    public String exportCookieConsentData(@PathParam("siteName") String siteName, 
            @PathParam("csvFileName") String csvFileName) {
       try {
           rejectIfBlank("exportCookieConsentData", "csvFileName", csvFileName);

           String fullPath = null;

           if (siteName == null) {
               fullPath = DTS_URL + "/" + csvFileName;
           } else {
               fullPath = DTS_URL + "/" + siteName + "/" + csvFileName;
           }

           PSDeliveryInfo deliveryServer = findServer(siteName);

           if (deliveryServer == null) {
               throw new WebApplicationException("Cannot find service of: " + PSDeliveryInfo.SERVICE_INDEXER);
           }

           PSDeliveryClient deliveryClient = new PSDeliveryClient();

           String response = deliveryClient.getString(new PSDeliveryActionOptions(deliveryServer,
                   fullPath, HttpMethodType.GET, true));

           if (response != null) {
               log.debug(response);
           }

           return response;
       } catch (PSValidationException | IPSPubServerService.PSPubServerServiceException | PSNotFoundException e) {
           log.error(PSExceptionUtils.getMessageForLog(e));
           throw new WebApplicationException(e.getMessage());
       }
    }
    
    @Override
    @GET
    @Path("log/totals/{siteName}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getCookieConsentForSite(@PathParam("siteName") String siteName) {
        try {
            rejectIfBlank("getcookieConsentForSite", "siteName", siteName);

            PSDeliveryInfo deliveryServer = findServer(siteName);

            if (deliveryServer == null) {
                throw new WebApplicationException("Cannot find service of: " + PSDeliveryInfo.SERVICE_INDEXER);
            }

            PSDeliveryClient deliveryClient = new PSDeliveryClient();

            String response = deliveryClient.getString(new PSDeliveryActionOptions(deliveryServer,
                    TOTAL_ENTRIES_URL + "/" + siteName, HttpMethodType.GET, true));

            if (response != null) {
                log.debug(SecureStringUtils.stripAllLineBreaks(response));
            }

            return response;
        } catch (PSValidationException | IPSPubServerService.PSPubServerServiceException | PSNotFoundException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e.getMessage());
        }
    }
    
    @Override
    @GET
    @Path("/log/totals")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAllCookieConsentTotals() {
        PSDeliveryInfo deliveryServer = findServer();
        
        if (deliveryServer == null) {
            throw new WebApplicationException("Cannot find service of: " + PSDeliveryInfo.SERVICE_INDEXER);
        }
        
        PSDeliveryClient deliveryClient = new PSDeliveryClient();
        
        String response = deliveryClient.getString(new PSDeliveryActionOptions(deliveryServer,
                TOTAL_ENTRIES_URL, HttpMethodType.GET, true));
        
        if (response != null) {
            log.debug(SecureStringUtils.stripAllLineBreaks(response));
        }
        
        return response;
    }
    

    @Override
    @DELETE
    @Path("/log")
    @Consumes(MediaType.APPLICATION_JSON)
    public void deleteAllCookieConsentEntries() {
        String currentUser = (String)PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
        log.info("All cookie consent entries are being deleted by: {}", currentUser);
        
        PSDeliveryInfo deliveryServer = findServer();
        
        if (deliveryServer == null) {
            throw new WebApplicationException("Cannot find service of: " + PSDeliveryInfo.SERVICE_INDEXER);
        }
        
        PSDeliveryClient deliveryClient = new PSDeliveryClient();
        
        String response = deliveryClient.getString(new PSDeliveryActionOptions(deliveryServer,
                DTS_URL, HttpMethodType.DELETE, true));
        
        if (response != null) {
            log.debug(SecureStringUtils.stripAllLineBreaks(response));
        }
    }
    
    @Override
    @DELETE
    @Path("/log/{siteName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void deleteCookieConsentEntriesForSite(@PathParam("siteName") String siteName) {
      try {
          String currentUser = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);

          log.info("Cookie consent entries for site: {} are being deleted by: {}",
                  siteName, currentUser);

          PSDeliveryInfo deliveryServer = findServer(siteName);

          if (deliveryServer == null) {
              throw new WebApplicationException("Cannot find service of: " + PSDeliveryInfo.SERVICE_INDEXER);
          }

          PSDeliveryClient deliveryClient = new PSDeliveryClient();

          String response = deliveryClient.getString(new PSDeliveryActionOptions(deliveryServer,
                  DTS_URL + "/" + siteName, HttpMethodType.DELETE, true));

          if (response != null) {
              log.debug(SecureStringUtils.stripAllLineBreaks(response));
          }
      } catch (IPSPubServerService.PSPubServerServiceException | PSNotFoundException e) {
         throw new WebApplicationException(e);
      }
    }
    
    /**
     * Finds a server with the meta data service.
     * 
     * @return the server, it may be <code>null</code> if cannot find the
     *         server.
     */
    private PSDeliveryInfo findServer(String site) throws IPSPubServerService.PSPubServerServiceException, PSNotFoundException {
        String adminURl= pubServerService.getDefaultAdminURL(site);
        PSDeliveryInfo server = deliveryService.findByService(PSDeliveryInfo.SERVICE_INDEXER,null,adminURl);

        if (server == null) {
            log.debug("Cannot find server with service of: {}" , PSDeliveryInfo.SERVICE_INDEXER);
        }

        return server;
    }
    private PSDeliveryInfo findServer()
    {
        //String adminURl= pubServerService.getDefaultAdminURL(site);
        //PSDeliveryInfo server = deliveryService.findByService(PSDeliveryInfo.SERVICE_INDEXER,null,adminURl);
        PSDeliveryInfo server = deliveryService.findByService(PSDeliveryInfo.SERVICE_INDEXER);
        if (server == null) {
            log.debug("Cannot find server with service of: {}" , PSDeliveryInfo.SERVICE_INDEXER);
        }

        return server;
    }
    
}
