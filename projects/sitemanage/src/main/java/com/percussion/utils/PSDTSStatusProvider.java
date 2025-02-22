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

/**
 * 
 */
package com.percussion.utils;

import com.percussion.delivery.client.IPSDeliveryClient.HttpMethodType;
import com.percussion.delivery.client.IPSDeliveryClient.PSDeliveryActionOptions;
import com.percussion.delivery.client.PSDeliveryClient;
import com.percussion.delivery.data.PSDeliveryInfo;
import com.percussion.delivery.service.IPSDeliveryInfoService;
import com.percussion.error.PSExceptionUtils;
import com.percussion.integritymanagement.data.PSIntegrityTask.TaskStatus;
import com.percussion.utils.types.PSPair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
/**
 * Check and report on the health status of the DTS and all of its services
 * 
 * @author robertjohansen
 * 
 */
@Component("dtsStatusProvider")
public class PSDTSStatusProvider implements IPSDTSStatusProvider {
    //Root of server
    private String serverRoot;

    //external services - services external from CM1 (List - in case more need to be added)
    
    private final Map<String,String> externalServices = new HashMap<String,String>(){{
        put("perc-polls-services","/perc-polls-services/polls/version");
    }};
    
    private final Map<String,String> services = new HashMap<String,String>() {{
        put(PSDeliveryInfo.SERVICE_FEEDS, "/feeds/rss/version");
        put(PSDeliveryInfo.SERVICE_INDEXER, "/perc-metadata-services/metadata/version");
        put(PSDeliveryInfo.SERVICE_COMMENTS,"/perc-comments-services/comment/version");
        put(PSDeliveryInfo.SERVICE_FORMS,"perc-form-processor/form/version");
        put(PSDeliveryInfo.SERVICE_MEMBERSHIP,"/perc-membership-services/membership/version");
    }};

    private IPSDeliveryInfoService deliveryService;

    private PSDeliveryClient deliveryClient;

    /**
     * Default constructor
     */
    @Autowired
    public PSDTSStatusProvider(IPSDeliveryInfoService service)
    {
        deliveryService = service;
        deliveryClient = new PSDeliveryClient();
        serverRoot = deliveryService.findBaseByServerType(null);
    }

    /**
     * Returns Health status of DTS and all services - No Services are
     * represented if DTS is not running Services are represented as key values
     * in a map with a PSPair representing Status (success or failed) in the
     * first element and the response message in the second element. The first
     * element of the PSPair will always represent Status.
     */
    @Override
    public Map<String, PSPair<TaskStatus, String>> getDTSStatusReport()
    {
        Map<String, PSPair<TaskStatus, String>> statusReport = new HashMap<>();
        
        //Check the status of the DTS - if down return status of dts else continue checking services
        PSPair<Boolean, String> dtsPair = getExternalTomcatServiceStatus(serverRoot);
        if (!dtsPair.getFirst())
        {
            statusReport.put("dts", new PSPair<>(TaskStatus.FAILED, dtsPair.getSecond()));
            return statusReport;
        }
        statusReport.put("dts", new PSPair<>(TaskStatus.SUCCESS, dtsPair.getSecond()));
        
        //check the external services and add the status to the report
        for(Map.Entry<String, String> externalService : externalServices.entrySet())
        {
            if (!getExternalTomcatServiceStatus(serverRoot + externalService.getValue()).getFirst()) {
                statusReport.put(externalService.getKey(), new PSPair<>(TaskStatus.FAILED, dtsPair.getSecond()));
            }
            else {
                statusReport.put(externalService.getKey(), new PSPair<>(TaskStatus.SUCCESS, dtsPair.getSecond()));
            }
        }
        
        for (Map.Entry<String, String> entry : services.entrySet())
        {
            PSPair<TaskStatus,String> pair = getServiceStatus(entry.getKey(),entry.getValue());
            statusReport.put(entry.getKey(),pair);
        }
        
        return statusReport;
    }
     
    /**
     * Get the status of the specified service
     * @param service Name of service
     * @param serviceURL url from service to /version (ping url)
     * @return PSPair where first represents status and second is response message
     */
    private PSPair<TaskStatus,String> getServiceStatus(String service, String serviceURL)
    {
        try
        {
            PSDeliveryInfo server = deliveryService.findByService(service);
            String message = deliveryClient.getString(new PSDeliveryActionOptions(server, serviceURL,
                    HttpMethodType.GET, false));
            return new PSPair<>(TaskStatus.SUCCESS, message);
        }
        catch (RuntimeException e)
        {
            return new PSPair<>(TaskStatus.FAILED, PSExceptionUtils.getMessageForLog(e));
        }
    }

    /**
     * Get the status of the external serivce
     * such as polls and or just root tomcat
     * @return PSPair<Status,Message>
     */
    private PSPair<Boolean, String> getExternalTomcatServiceStatus(String surl)
    {
        try
        {
            boolean alive = false;
            String response = "";
            URL url = new URL(surl);
            if(url.getProtocol().equalsIgnoreCase("https")){
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "*/*");
    
                response = conn.getResponseMessage();
                if(response.contains("OK")) {
                    alive = true;
                }
            }
            else{
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "*/*");
    
                response = conn.getResponseMessage();
                if(response.contains("OK")) {
                    alive = true;
                }
                
            }
            return new PSPair<>(alive, response);
        }
        catch (ConnectException e)
        {
            return new PSPair<>(false,PSExceptionUtils.getMessageForLog(e));
        }
        catch (IOException e)
        {
            return new PSPair<>(false,PSExceptionUtils.getMessageForLog(e));
        }
    }
}
