/*
 *     Percussion CMS
 *     Copyright (C) 1999-2020 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percusssion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package com.percussion.sitemanage.servlet;

import static com.percussion.pathmanagement.service.impl.PSPathUtils.ASSETS_FINDER_ROOT;
import static com.percussion.pathmanagement.service.impl.PSPathUtils.SITES_FINDER_ROOT;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.pagemanagement.data.PSInlineLinkRequest;
import com.percussion.pagemanagement.data.PSInlineRenderLink;
import com.percussion.pagemanagement.service.impl.PSRenderLinkService;
import com.percussion.pathmanagement.service.impl.PSPathUtils;
import com.percussion.server.PSRequest;
import com.percussion.server.PSRequestParsingException;
import com.percussion.server.webservices.PSServerFolderProcessor;
import com.percussion.services.assembly.impl.PSReplacementFilter;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.share.dao.impl.PSFolderHelper;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.util.PSCharSets;
import com.percussion.util.PSMutableUrl;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSWebserviceUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The servlet used to preview content of the item, where the item is specified by its folder path.
 * 
 * @author YuBingChen
 */
public class PSPreviewItemContent extends HttpServlet
{
    private static final long serialVersionUID = 1L;
    private static Log ms_log = LogFactory.getLog(PSPreviewItemContent.class);
    private static PSRenderLinkService linkService;
    private IPSiteDao siteDao;
    
    public PSPreviewItemContent()
    {
        super();
        PSSpringWebApplicationContextUtils.injectDependencies(this);
    }
    
    @Override
    protected void service(HttpServletRequest request,
          HttpServletResponse response)
    {
        String requestUri = request.getRequestURI();
        String revision = request.getParameter(IPSHtmlParameters.SYS_REVISION);
        
        try
        {
            String type = request.getParameter("type");
            String url = createAssemblyUrl(requestUri, revision, type);
            
            HttpServletRequest forwardReq = getRequestFromUrl(url, request);
            
            RequestDispatcher disp = request.getRequestDispatcher("/assembler/render");
            disp.forward(forwardReq, response);
        }
        catch (PSNotFoundException notFoundEx)
        {
            if (!requestUri.endsWith("favicon.ico"))
                ms_log.warn(notFoundEx);
            responseWithError(response, SC_NOT_FOUND, notFoundEx);
        }
        catch (UnsupportedOperationException unsupportedOpEx)
        {
            ms_log.warn(unsupportedOpEx,unsupportedOpEx);
            responseWithError(response, SC_BAD_REQUEST, unsupportedOpEx);
        }
        catch (Exception e)
        {
            ms_log.error("Preview Exception", e);
            responseWithError(response, SC_INTERNAL_SERVER_ERROR, e);
        }
    }

    private void responseWithError(HttpServletResponse response, int status, Exception e)
    {
        try
        {
            response.sendError(status, e.getMessage());
        }
        catch (IOException ioEx)
        {
            ms_log.error(ioEx);
        }
    }

    
    @SuppressWarnings("unchecked")
    private HttpServletRequest getRequestFromUrl(String url, HttpServletRequest request) throws PSRequestParsingException
    {
        PSMutableUrl mutableUrl = new PSMutableUrl(url);
        Map<String, String> params = mutableUrl.getParamMap();

        Map<String, String[]> params2 = new HashMap<String, String[]>();
        for (Map.Entry<String, String> entry : params.entrySet())
        {
            params2.put(entry.getKey(), new String[]{entry.getValue()});
        }

        PSServletRequestWrapper wrapReq = new PSServletRequestWrapper(request);
        wrapReq.setParameterMap(params2);
        
        return wrapReq;
    }
    
    /**
     * Creates the assembly URL from the specified item path.
     * @param path the path of the item, assumed not blank.
     * @param revision the id of the revision, can be blank
     * @param renderType it is "xml", "html" or "database", assumed not blank.
     * @return the assembly URL, not blank.
     */
    private String createAssemblyUrl(String path, String revision, String renderType)
    {
        IPSGuid id = getItemId(path, revision);
        if (id!=null)
        {
            IPSCmsObjectMgr objMgr = PSCmsObjectMgrLocator.getObjectManager();
            PSComponentSummary item = objMgr.loadComponentSummary(id.getUUID());
            
            if (item.isFolder())
            {
                PSSiteSummary siteSum = siteDao.findByPath("/"+path);
                if (!path.endsWith("/"))
                    path+="/";
                path += siteSum.getDefaultDocument();
                // get default page id
                id = getItemId(path, revision);
            }
        }
        if (id==null)
            throw new PSNotFoundException(path);
        
        PSInlineLinkRequest linkRequest = new PSInlineLinkRequest();
        linkRequest.setTargetId(id.toString());
        PSInlineRenderLink renderLink;
        if (path.startsWith(SITES_FINDER_ROOT))
            renderLink = linkService.renderPreviewPageLink(id.toString(), renderType);
        else
            renderLink = linkService.renderPreviewResourceLink(linkRequest);
        
        return renderLink.getUrl();
    }
    
    /**
     * Gets the ID of the item from its path.
     * @param path the path if the item in question.
     * @param revision the id of the revision, can be blank
     * @return the ID of the item, never <code>null</code>.
     * @throws PSNotFoundException if cannot find the item from the path.
     */
    private IPSGuid getItemId(String path, String revision)
    {
        path = escapeChars(path);
        //path = PSReplacementFilter.filter(path);
        path = PSPathUtils.getFolderPath(path);
       
        int revisionId = -1;
        PSServerFolderProcessor srv = PSServerFolderProcessor.getInstance();
        
        try
        {
            if(!StringUtils.isBlank(revision))
            {
                revisionId = Integer.parseInt(revision);
            }
            
            int id = srv.getIdByPath(path);
            
           
            
            if (id == -1)
                throw new PSNotFoundException("Cannot find item with path = \"" + path + "\".");
            
            PSLegacyGuid gui = new PSLegacyGuid(id, revisionId);
            return gui;
        }
        catch (PSCmsException e)
        {
            throw new RuntimeException("Failed to get item ID from path=" + path, e);
        }
        catch (NumberFormatException e)
        {
            throw new RuntimeException("Failed to parse integer from revision=" + revision, e);
        }
    }

    /**
     * Manual decode the '+' character as the browser may not do it correctly, 
     * which will cause the <code>URLDecoder.decode</code> method to mistakenly 
     * replace it with a whitespace.
     * Tested in the following browsers:
     * <ul>
     * <li>Internet Explorer 8</li>
     * <li>Firefox 3.6</li>
     * <li>Safari 4</li>
     * <li>Google Chrome</li>
     * </ul>
     * 
     * @param path the path to decode manually
     * @return
     *      the same path but with the '+' symbol replaced by '%2B'
     */
    private String escapeChars(String path)
    {
        String ret = path;
        try {
            String test = URLDecoder.decode(path, "utf8");
            if(!test.equals(path)) {
                ret = test;
            }
        }
        catch (UnsupportedEncodingException e) {
        }
        return ret;
    }

    public void setRenderLinkService(PSRenderLinkService service)
    {
        linkService = service;
    }
    
    public PSRenderLinkService getRenderLinkService()
    {
        return linkService;
    }   
    
    public IPSiteDao getSiteDao()
    {
        return siteDao;
    }

    public void setSiteDao(IPSiteDao siteDao)
    {
        this.siteDao = siteDao;
    }

}
