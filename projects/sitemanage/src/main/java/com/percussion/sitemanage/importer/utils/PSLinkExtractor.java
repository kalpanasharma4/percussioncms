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
package com.percussion.sitemanage.importer.utils;

import com.percussion.queue.impl.PSSiteQueue;
import com.percussion.services.assembly.impl.PSReplacementFilter;
import com.percussion.sitemanage.dao.impl.PSSiteContentDao;
import com.percussion.sitemanage.data.PSSiteImportConfiguration;
import com.percussion.sitemanage.importer.IPSSiteImportLogger;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogEntryType;
import com.percussion.sitemanage.importer.PSLink;
import com.percussion.sitemanage.importer.PSSiteImporter;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public final class PSLinkExtractor
{

    private static final String DOUBLE_SLASH = "//";

    private static final String DASH = "-";

    private static final String BACK_SLASH = "\\";

    private static final String SLASH = "/";

    private static final String EMPTY = "";

    private static final String QUESTION_MARK = "?";

    private static final String PERIOD = ".";

    private static final String UNKNOWN = "unknown";

    private static final String ABS_HREF = "abs:href";

    public static final String A_HREF = "a[href]";

    public static final String IMG_SOURCE = "img[src]";

    public static final String HREF = "href";

    public static final String SRC = "src";

    public static final String QUERY_STRING_LINK_TEXT_TOKEN = "{{{{{{{PERCUSSION|QUERY|STRING|TOKEN}}}}}}}";

    private static final String QUERY_STRING_PAGE_NAME = "/item-";


    private PSLinkExtractor()
    {
    }

    // Wrapped for test
    /**
     * Mapped to PSSiteImporter method
     * 
     * @param siteUrl
     * @param logger
     * @param userAgent
     * @return the redirected URL
     */
    protected String getRedirectedURL(String siteUrl, IPSSiteImportLogger logger, String userAgent)
    {
        String urlReturn = siteUrl;

        try
        {
            urlReturn = PSSiteImporter.getRedirectedUrl(siteUrl, logger, userAgent);
        }
        catch (Exception e)
        {
            urlReturn = siteUrl;
        }

        return urlReturn;
    }


    /**
     * Gets a list of PSLink objects for a given Document
     * 
     * @param url the URL target
     * @return a list of PSLink objects
     */
    public static List<PSLink> getLinksForDocument(final Document doc, final IPSSiteImportLogger log,
            PSSiteQueue siteQueue, String siteUrl, PSSiteImportConfiguration config)
    {

        final ArrayList<PSLink> outList = new ArrayList<>();
        String queryParameter= config.getMapQueryParamToPageName();
        ArrayList<String> uniqueListOfLinks = new ArrayList<>();

        boolean linkCheckPassed = true;
        String paramValue = null;
        final Elements links = doc.select(A_HREF);
        for (Element link : links)
        {
            if ((!removeTrailingSlash(link.attr(ABS_HREF)).equals(getRoot(siteUrl)))
            		&& (!link.attr(HREF).startsWith("#"))
                    && (!link.attr(HREF).startsWith("tel")))
            {
                String absUrl = link.attr(ABS_HREF);
                //If query parameter check is required then need to perform new checks
                if (!StringUtils.isBlank(queryParameter)) {
                    String queryParameters = absUrl.substring(absUrl.indexOf("?") + 1);
                    String[] queryParametersArr = queryParameters.split("&");

                    for (String s: queryParametersArr) {
                        String[] parameterNameAndValueArr = s.split("=");
                        if(parameterNameAndValueArr[0].equals(queryParameter)){
                            // If duplicate link then go to next link iteration
                            paramValue = parameterNameAndValueArr[1];
                            if(paramValue.indexOf("#") != -1){
                                paramValue = paramValue.substring(0,paramValue.indexOf("#"));
                            }

                            if(uniqueListOfLinks.contains(paramValue)){
                                linkCheckPassed = false;
                            }else{
                                linkCheckPassed = true;
                                uniqueListOfLinks.add(paramValue);
                            }
                            break;
                        }
                    }

                }else{
                    //If query parameter check is not required then need to perform old check
                    linkCheckPassed =  getRoot(link.attr(ABS_HREF)).equals(getRoot(siteUrl));
                }

                //if check failed then go to next link iteration
                if(!linkCheckPassed) {
                    continue;
                }
                final String absHref = link.attr(ABS_HREF);
                final String aHref = link.attr(HREF);
                if (siteQueue != null && siteQueue.hasLinkBeenProcessed(absHref))
                {
                	PSLink cachedLink = siteQueue.getProcessedLink(absHref);
                	cachedLink.setElement(link);
                    outList.add(cachedLink);
                }
                else
                {
                    try
                    {
                        PSLink psLink = null;

                        if (absHref.equals(getRoot(doc.baseUri())) && !absHref.isEmpty())
                        {
                            psLink = createLink(link, absHref, aHref, PSSiteContentDao.HOME_PAGE_NAME,
                                    getRelativePath(absHref, aHref, log, config));
                        }
                        else
                        {
                            psLink = createLink(link, absHref, aHref, getPageName(absHref, log, config),
                                    getRelativePath(absHref, aHref, log, config));
                        }
                        link.attr(HREF, PSReplacementFilter.filter(psLink.getRelativePathWithFileName()));

                        outList.add(psLink);

                    }
                    catch (Exception e)
                    {
                        log.appendLogMessage(PSLogEntryType.ERROR, "Link Extractor", absHref
                                + " could not be retrieved.");
                        log.appendLogMessage(PSLogEntryType.STATUS, "Link Extractor", absHref
                                + " could not be retrieved due to the following error: " + e.getLocalizedMessage());
                    }
                }

            }
        }
        return outList;

    }

    /**
     * Gets a list of PSLink objects for a given Document searching for images
     * 
     * @param doc The document object to fetch
     * @param log The logger for the site importing process
     * @return a list of PSLink objects for the img tags
     */
    public static List<PSLink> getImagesForDocument(final Document doc, final IPSSiteImportLogger log)
    {
        final ArrayList<PSLink> outList = new ArrayList<>();

        final Elements images = doc.select(IMG_SOURCE);

        for (Element image : images)
        {
            final String absHref = image.attr(ABS_HREF);
            final String imgSrc = image.attr(SRC);
            try
            {
                PSLink psImage = createLink(image, "", imgSrc, "", "");
                outList.add(psImage);
            }
            catch (Exception e)
            {
                log.appendLogMessage(PSLogEntryType.ERROR, "Link Extractor", absHref + " could not be retrieved.");
                log.appendLogMessage(PSLogEntryType.STATUS, "Link Extractor", absHref
                        + " could not be retrieved due to the following error: " + e.getLocalizedMessage());
            }
        }

        return outList;
    }

    protected static PSLink createLink(Element link, final String absHref, final String aHref, final String pageName,
            final String relativePath) throws UnsupportedEncodingException
    {
        try
        {
            return PSLink.createLink(PSReplacementFilter.filter(relativePath),
                    URLDecoder.decode(getLinkText(aHref, link), "UTF-8"), absHref,
                    PSReplacementFilter.filter(pageName), link);
        }
        catch (UnsupportedEncodingException e)
        {
            throw e;
        }
    }

    /**
     * Encapsulates link text extraction
     * 
     * @param absHref the absolute HREF
     * @return the link text
     */
    protected static String getLinkText(final String absHref, Element link)
    {
        String linkText = "";
        if (absHref != null && !absHref.isEmpty() && removeTrailingSlash(absHref).equals(getRoot(absHref)))
        {
            return "Home";
        }

        if (link.attr("title") != null && !link.attr("title").isEmpty())
        {
            if (!PSLinkBadKeywords.isStringInFilterList(link.attr("title")))
                return PSLinkBadKeywords.filterLinkTextString(link.attr("title"));
        }

        if (link.text() != null && !link.text().isEmpty())
        {
            if (!PSLinkBadKeywords.isStringInFilterList(link.text()))
                return PSLinkBadKeywords.filterLinkTextString(link.text());
        }

        try
        {
            Document doc = Jsoup.connect(absHref).get();
            Elements h1 = doc.select("h1");
            if (h1.size() > 0)
            {
                String h1Text = h1.get(0).text();
                //FB: NP_NULL_PARAM_DEREF_NONVIRTUAL NC 1-16-16
                if (h1Text != null && !linkText.isEmpty())
                {
                    if (!PSLinkBadKeywords.isStringInFilterList(h1Text))
                    {
                        return h1Text;
                    }
                }
            }

            return doc.title();
        }
        catch (Exception e)
        {
            // bad link move on.
        }

        if (absHref != null)
        {
            linkText = absHref;

            if (absHref.contains(QUESTION_MARK))
            {
                linkText = QUERY_STRING_LINK_TEXT_TOKEN;
            }
            else
            {
                linkText = getLastElementInPath(linkText);

                if (linkText.contains(PERIOD))
                {
                    linkText = linkText.substring(0, linkText.indexOf(PERIOD));
                }

                if (linkText.isEmpty())
                {
                    linkText = UNKNOWN;
                }
            }
        }
        else
        {
            linkText = UNKNOWN;
        }

        return linkText;
    }

    /**
     * Gets the last item in a path. Does not handle query strings.
     * 
     * @param linkText the path for evaluation
     * @return the last item in the path
     */
    private static String getLastElementInPath(final String linkText)
    {
        String linkTextMod = removeTrailingSlash(linkText);
        // Left Cut
        if (linkTextMod.contains(SLASH))
        {
            linkTextMod = linkTextMod.substring(linkTextMod.lastIndexOf(SLASH) + 1);
        }
        return linkTextMod;
    }

    /**
     * Removes a trailing slash from link text
     * 
     * @param linkText the text to remove the slash from
     * @return a String with the trailing slash removed
     */
    private static String removeTrailingSlash(final String linkText)
    {
        String linkTextMod = linkText;
        // If the link ends in a "/" remove said "/" - must do before left cut
        if (linkTextMod.length() > 0 && hasTrailingSlash(linkTextMod))
        {
            linkTextMod = linkTextMod.substring(0, linkTextMod.length() - 1);
        }
        return linkTextMod;
    }

    /**
     * Checks text for a trailing slash
     * 
     * @param linkText the link text for evaluation
     * @return a boolean indicating the presence or lack of a trailing slash
     */
    public static boolean hasTrailingSlash(final String linkText)
    {
        boolean hasTrailingSlash = false;
        if (!linkText.isEmpty())
        {
            hasTrailingSlash = linkText.substring(linkText.length() - 1).equals(SLASH);
        }
        return hasTrailingSlash;
    }

    /**
     * Handles query string
     * 
     * @param stringForStrip the string to be stripped
     * @return a String stripped of query elements
     */
    private static String handleQueryString(final String stringForStrip)
    {
        String strippedString = stringForStrip;
        // Handles Word Press URLs
        strippedString = stringForStrip.replace("/?", QUERY_STRING_PAGE_NAME);
        return strippedString;
    }

    /**
     * Shared functionality basic path without root and query string
     * 
     * @param absHref url for evaluation
     * @return the base path
     */
    private static String getBasePath(final String absHref)
    {
        String relativePath = absHref.replace(BACK_SLASH, SLASH);
        relativePath = absHref.replace(getRoot(absHref), "");
        relativePath = handleQueryString(relativePath);
        return relativePath;
    }

    /**
     * Encapsulates relative path extraction
     * 
     * @param absHref the absolute HREF
     * @return the relative path
     */
    protected static String getRelativePath(final String absHref, String aHref, final IPSSiteImportLogger log, PSSiteImportConfiguration config)
    {
        String relativePath = getBasePath(absHref.replace(BACK_SLASH, SLASH));
        if (relativePath.isEmpty())
        {
            if (getRoot(absHref).equals(absHref))
            {
                return SLASH;
            }
            String drPath = aHref.replace(BACK_SLASH, SLASH);
            relativePath = drPath.replace(getPageName(drPath, log, config), EMPTY);
        }
        else
        {
            if (!hasTrailingSlash(relativePath))
            {
                relativePath = relativePath.substring(0, relativePath.lastIndexOf(SLASH) + 1);
            }
        }
        if (log != null)
        {
            log.appendLogMessage(PSLogEntryType.STATUS, "Link Extractor", "Changed Relative Path : " + relativePath
                    + " to " + PSReplacementFilter.filter(relativePath));
        }
        return PSReplacementFilter.filter(relativePath);
    }

    /**
     * Gets the page name (file name from an absHref)
     * 
     * @param absHref
     * @return the page name
     */
    protected static String getPageName(final String absHref, final IPSSiteImportLogger log, PSSiteImportConfiguration config) {
        String queryParameter = null;
        if(config !=null){
            queryParameter = config.getMapQueryParamToPageName();
        }

        if (!StringUtils.isBlank(queryParameter) && absHref.indexOf("?") != -1) {
            String queryParameters = absHref.substring(absHref.indexOf("?") + 1);
            String[] queryParametersArr = queryParameters.split("&");
            String pageName = null;
            String paramValue = null;
            for (String s: queryParametersArr) {
                String[] parameterNameAndValueArr = s.split("=");
                if(parameterNameAndValueArr[0].equals(queryParameter)){
                    paramValue = parameterNameAndValueArr[1];
                    if(paramValue.indexOf("#") != -1){
                        paramValue = paramValue.substring(0,paramValue.indexOf("#"));
                    }
                    pageName = paramValue+"."+config.getSite().getDefaultFileExtention();
                    break;
                }
            }
            return pageName;
        }else{
            String endPartString = PSSiteContentDao.HOME_PAGE_NAME;
            String cleanAbsHref = absHref.replace(BACK_SLASH, SLASH);
            if (!(cleanAbsHref != null && !cleanAbsHref.isEmpty() && removeTrailingSlash(cleanAbsHref).equals(
                    getRoot(cleanAbsHref)))) {
                final String basePath = getBasePath(cleanAbsHref);
                if (!hasTrailingSlash(cleanAbsHref) || getLastElementInPath(basePath).contains(PERIOD)) {
                    endPartString = getLastElementInPath(basePath);
                }
            }
            if (!PSReplacementFilter.filter(endPartString).equals(endPartString)) {
                if (log != null) {
                    log.appendLogMessage(PSLogEntryType.STATUS, "Link Extractor", "Changed Page Name: " + endPartString
                            + " to " + PSReplacementFilter.filter(endPartString));
                }
            }
            if (endPartString.contains(PERIOD) && endPartString.contains(QUESTION_MARK)) {
                endPartString = endPartString.replace(PERIOD, DASH);
            }
            return PSReplacementFilter.filter(endPartString).replace("#", "-");
        }
    }

    /**
     * Gets the site root for a path
     * 
     * @param path
     * @return a string that maps to the site root
     */
    protected static String getRoot(final String path)
    {
        String builtRoot = "";

        if (path != null && path.contains(DOUBLE_SLASH))
        {
            final String leftPart = path.substring(0, path.indexOf(DOUBLE_SLASH) + 2);
            String rightPart = path.replace(leftPart, EMPTY);
            if (rightPart.contains(SLASH))
            {
                rightPart = rightPart.substring(0, rightPart.indexOf(SLASH));
                builtRoot = leftPart + rightPart;
            }
            else
            {
                // we were at the root.
                builtRoot = path;
            }

        }
        return builtRoot;
    }
}
