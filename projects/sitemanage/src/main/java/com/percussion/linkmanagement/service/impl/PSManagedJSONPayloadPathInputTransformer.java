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
package com.percussion.linkmanagement.service.impl;

import com.percussion.data.PSConversionException;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.error.PSExceptionUtils;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSFieldInputTransformer;
import com.percussion.extension.PSDefaultExtension;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionParams;
import com.percussion.linkmanagement.service.IPSManagedLinkService;
import com.percussion.server.IPSRequestContext;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.util.IPSHtmlParameters;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.File;

/**
 * A field input transformer to process/update an item path. Expects a JSON object with an array named percJSONConfigas an input, calls the managedlink service to manage any links in the payload,
 * the payload is updated and returned with the resulting link id's updated.
 *
 * For new items the item id is not yet created, so PSManagedItemPathPreProcessor should be added to initialized new item links in the service
 * to track that and set a request private object as a marker. The managed link post processor exit checks the private object and if it is there
 * and if its value is true then updates the parent id.
 *
 * @author NateChadwick
 *
 */
public class PSManagedJSONPayloadPathInputTransformer extends PSDefaultExtension implements IPSFieldInputTransformer
{

	private static final Logger log = LogManager.getLogger(PSManagedJSONPayloadPathInputTransformer.class);

    private IPSManagedLinkService service;

    /* (non-Javadoc)
     * @see com.percussion.extension.IPSExtension#init(com.percussion.extension.IPSExtensionDef, java.io.File)
     */
    @Override
    public void init(IPSExtensionDef def, File codeRoot) throws PSExtensionException
    {
        super.init(def, codeRoot);
        //This is for wiring the services
        PSSpringWebApplicationContextUtils.injectDependencies(this);

    }

    @Override
    public Object processUdf(Object[] params, IPSRequestContext request) throws PSConversionException
    {
        PSExtensionParams ep = new PSExtensionParams(params);
        String jsonPayload = ep.getStringParam(0, null, true);

        if(StringUtils.isBlank(jsonPayload)) {
			return "";
		}

		//Fix Old Data for Image Slider
		if(request != null && "percImageSlider.xml".equalsIgnoreCase(request.getRequestPage())){
			if(jsonPayload != null){
				jsonPayload = jsonPayload.replaceAll(IPSManagedLinkService.PERC_OLD_IMAGE_SLIDER_CONFIG_ATTR,IPSManagedLinkService.PERC_CONFIG);
				jsonPayload = jsonPayload.replaceAll(IPSManagedLinkService.PERC_OLD_IMAGE_SLIDER_IMAGEPATH_ATTR,IPSManagedLinkService.PERC_IMAGEPATH);
				log.info("Updated Old data in ImageSlider");
			}
		}

        JSONObject object = null;

		try {
			log.debug("Parsing JSONPayload: " + jsonPayload);
			object = new JSONObject(jsonPayload);
			log.debug("Returned from parsing JSONPayload.");
			log.debug("Parsing for " + IPSManagedLinkService.PERC_CONFIG);
			JSONArray objectArray;
			try {
				objectArray = object.getJSONArray(IPSManagedLinkService.PERC_CONFIG);
				log.debug("Found " + IPSManagedLinkService.PERC_CONFIG + " array.");
			}catch (JSONException e){
				//Unable to get the array so log an error that it is missing
				log.error("An error occurred while trying to manage links in a JSONPayload field.", PSExceptionUtils.getMessageForLog(e));
				log.debug("An error occurred while trying to manage links in a JSONPayload field.", e);
				return null;
			}

	        String newLinkId = "";

	        for (int i = 0; i < objectArray.length(); i++) {
	            JSONObject entry = objectArray.getJSONObject(i);
				log.debug("Processing array entry " + i);

	            //Images
	            if(entry.has(IPSManagedLinkService.PERC_IMAGEPATH)){
	            	if(entry.has(IPSManagedLinkService.PERC_IMAGEPATH_LINKID)){
						 log.debug("Processing ImagePath entry with path " + entry.getString(IPSManagedLinkService.PERC_IMAGEPATH) + " and Link Id" + entry.getString(IPSManagedLinkService.PERC_IMAGEPATH_LINKID));

	            		newLinkId = manageLinks(request, entry.getString(IPSManagedLinkService.PERC_IMAGEPATH), entry.getString(IPSManagedLinkService.PERC_IMAGEPATH_LINKID));

	            		log.debug("Updating Image JSONPayload entry:" + entry.getString(IPSManagedLinkService.PERC_IMAGEPATH) + " with Link Id" + newLinkId);


	            		entry.put(IPSManagedLinkService.PERC_IMAGEPATH_LINKID, newLinkId);
	            		objectArray.put(i,entry);

						log.debug("Done updating.");

	            	}
	            }

	            //Files
	            if(entry.has(IPSManagedLinkService.PERC_FILEPATH)){
	            	if(entry.has(IPSManagedLinkService.PERC_FILEPATH_LINKID)){

						log.debug("Processing filePath entry with path " + entry.getString(IPSManagedLinkService.PERC_FILEPATH) + " and Link Id" + entry.getString(IPSManagedLinkService.PERC_FILEPATH_LINKID));
						newLinkId = manageLinks(request, entry.getString(IPSManagedLinkService.PERC_FILEPATH), entry.getString(IPSManagedLinkService.PERC_FILEPATH_LINKID));
						log.debug("Updating File JSONPayload entry:" + entry.getString(IPSManagedLinkService.PERC_FILEPATH) + " with Link Id" + newLinkId);
						entry.put(IPSManagedLinkService.PERC_FILEPATH_LINKID, newLinkId);
						objectArray.put(i,entry);
						log.debug("Done updating.");

	            	}
	            }

	            //Pages
	            if(entry.has(IPSManagedLinkService.PERC_PAGEPATH)){
	            	if(entry.has(IPSManagedLinkService.PERC_PAGEPATH_LINKID)){
						log.debug("Processing pagePath entry with path " + entry.getString(IPSManagedLinkService.PERC_PAGEPATH) + " and Link Id" + entry.getString(IPSManagedLinkService.PERC_PAGEPATH_LINKID));
						newLinkId = manageLinks(request, entry.getString(IPSManagedLinkService.PERC_PAGEPATH), entry.getString(IPSManagedLinkService.PERC_PAGEPATH_LINKID));

						log.debug("Updating Page JSONPayload entry:" + entry.getString(IPSManagedLinkService.PERC_PAGEPATH) + " with Link Id" + newLinkId);

						entry.put(IPSManagedLinkService.PERC_PAGEPATH_LINKID, newLinkId);
						objectArray.put(i,entry);

						log.debug("Done updating.");

	            	}
	            }
	        }
			 log.debug("Updating JSONPayload to use updated array");
	   		 object.put(IPSManagedLinkService.PERC_CONFIG, objectArray);
			 log.debug("Done updating.");

		} catch (JSONException ex) {
			log.error("An error occurred while trying to manage links in a JSONPayload field.", PSExceptionUtils.getMessageForLog(ex));
			log.debug("Error occurred. Returning original payload:" + jsonPayload, ex);
			return jsonPayload;
		}

		log.debug("Returning updated payload:" + object.toString());
		return object.toString();
    }

    private String manageLinks(IPSRequestContext request, String path, String linkId){

    	String result = "";
        String cid = request.getParameter(IPSHtmlParameters.SYS_CONTENTID);
        if(StringUtils.isBlank(cid) || !StringUtils.isNumeric(cid))
        {
            service.initNewItemLinks();
            result = service.manageItemPath(null, path, linkId);
            request.setPrivateObject(PSManagedLinksPostProcessor.PERC_UPDATE_NEW_MANAGED_LINKS, true);
        }
        else
        {
            cid = PSGuidManagerLocator.getGuidMgr().makeGuid(new PSLocator(cid)).toString();
            result = service.manageItemPath(cid, path, linkId);
        }
        return result;
    }

    /**
     * Setter for dependency injection
     *
     * @param service the service to set
     */
    public void setService(IPSManagedLinkService service)
    {
        this.service = service;
    }

}
