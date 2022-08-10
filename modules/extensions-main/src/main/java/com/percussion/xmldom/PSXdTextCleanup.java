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
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */
package com.percussion.xmldom;

import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSField;
import com.percussion.extension.IPSRequestPreProcessor;
import com.percussion.extension.PSDefaultExtension;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.html.PSHtmlParsingException;
import com.percussion.html.PSHtmlUtils;
import com.percussion.security.PSAuthorizationException;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.PSRequestValidationException;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.util.PSPurgableTempFile;
import org.apache.commons.lang.StringUtils;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * A Rhythmyx extension used to translate a text field, parse it, tidy it,
 * scan it for inline links and then place it back into the same field.
 * <p>
 * This extension is a standard Rhythmyx pre-exit. There are 7 parameters:
 * <table border="1">
 * <tr>
 *    <th>Param #</th>
 *    <th>Name</th>
 *    <th>Description</th>
 *    <th>Required?</th>
 *    <th>default value</th>
 * <tr>
 * <tr>
 *    <td>1</td>
 *    <td>sourceFieldName</td>
 *    <td>
 *       The name of the field or file attachment that needs to be cleaned.
 *    </td>
 *    <td>yes</td>
 *    <td>&nbsp;</td>
 * </tr>
 * <tr>
 *    <td>2</td>
 *    <td>tidyPropertiesFile</td>
 *    <td>The tidy properties file used when parsing the source document.</td>
 *    <td>no</td>
 *    <td>None, tidy will be disabled if not provided.</td>
 * </tr>
 * <tr>
 *    <td>3</td>
 *    <td>serverPageTags</td>
 *    <td>
 *       The server page tags XML file used when parsing the source document.
 *    </td>
 *    <td>no</td>
 *    <td>None, server page tags are disabled if not supplied.</td>
 * </tr>
 * <tr>
 *    <td>4</td>
 *    <td>encodingOverride</td>
 *    <td>The name of the encoding to use when reading a file.</td>
 *    <td>no</td>
 *    <td>
 *       The encoding reported by the browser will be used. If this encoding is
 *       null, the default encoding for the platform will be used.
 *    </td>
 * </tr>
 * <tr>
 *    <td>5</td>
 *    <td>inlineDisable</td>
 *    <td>
 *       Disables scanning for in-line links if 'yes' (case insensitive) is
 *       supplied.
 *    </td>
 *    <td>no</td>
 *    <td>If this parameter is null, in-line scanning will occur.</td>
 * </tr>
 * <tr>
 *    <td>6</td>
 *    <td>prettyPrint</td>
 *    <td>
 *       To enable pretty printing of the output supply 'yes' (case
 *       insensitive).
 *    </td>
 *    <td>no</td>
 *    <td>Defaults to 'no'.</td>
 * </tr>
 * </table>
 * <p>
 * The source text field is always an HTML parameter. This exit will
 * automatically detect if the uploaded field is an attached file or an HTML
 * field, and process it accordingly.
 * <p>
 * The Tidy properties file is optional. If it is provided, the text will be
 * run through the HTML Tidy program before parsing. See the
 * {@link <a href="http://www.w3.org/People/Raggett/tidy" target="_blank">
 * W3C HTML Tidy</a>} page for details and properties file formats. Note that
 * this implementation uses the Java version of Tidy, which is not exactly
 * identical to the C implementation. The pathname provided must be relative
 * to the Rhythmyx server root.
 * <p>
 * The ServerPageTags.xml file is used to eliminate certain non-parsable text
 * generated by some editing programs. For details see
 * {@link ProcessServerPageTags ProcessServerPageTags}. The pathname is
 * relative to the server root.
 * <p>
 * Rhythmyx provides default files for Tidy (rxW2Ktidy.properties) and
 * ServerPageTags (rxW2KServerPageTags.xml).
 * <p>
 */

public class PSXdTextCleanup extends PSDefaultExtension
        implements IPSRequestPreProcessor
{
   /**
    * This method handles the pre-exit request.
    *
    * @param params an array of objects representing the parameters. See the
    * description under {@link PSXdTextToDom} for parameter details.
    *
    * @param request the request context for this request
    *
    * @throws PSExtensionProcessingException when a run time error is detected.
    */
   public void preProcessRequest(Object[] params, IPSRequestContext request)
           throws PSAuthorizationException, PSRequestValidationException,
           PSParameterMismatchException, PSExtensionProcessingException
   {
      String encodingDefault = StandardCharsets.UTF_8.name();
      boolean inlineDisable = false;
      Document tempDocument;

      PSXmlDomContext contxt = new PSXmlDomContext(ms_className, request);
      contxt.setRxCommentHandling(true);

      if (params.length < 1 || null == params[0] ||
              0 == params[0].toString().trim().length())
         throw new PSParameterMismatchException(params.length, 1);

      /*
       * fileParamName is also the field name
       */
      String fileParamName = PSXmlDomUtils.getParameter(params, 0, null);
      Object inputSourceObj = request.getParameterObject(fileParamName);
      if (null == inputSourceObj)
      {
         // if there is no input source, exit the extension.
         request.printTraceMessage("source is null, exiting");
         return;
      }else if(inputSourceObj instanceof String){
         if(StringUtils.isEmpty(inputSourceObj.toString())){
            // if there is no text to parse, exit the extension.
            request.printTraceMessage("source is null, exiting");
            return;
         }
      }

      //Get the cleanup file param.
      String param = PSXmlDomUtils.getParameter(params, 1, null);
      String htmlcleanupFile = param;
      if(htmlcleanupFile != null){
         //replace the legacy tidy reference with system default
         if(htmlcleanupFile.equalsIgnoreCase("rxw2ktidy.properties")){
            htmlcleanupFile = null;
         }
      }

      String contenttypeid = request.getParameter(IPSHtmlParameters.SYS_CONTENTTYPEID);
      PSField field = null;
      PSItemDefinition itemDef = null;

      if (!StringUtils.isBlank(contenttypeid))
      {
         // Null for search
         field = getField(fileParamName,contenttypeid);
         try
         {
            itemDef = PSItemDefManager.getInstance().getItemDef(
                    Long.parseLong(contenttypeid),
                    request.getSecurityToken());
         }
         catch (PSInvalidContentTypeException e)
         {
            throw new PSExtensionProcessingException(ms_className, e);
         }
      }

      param = PSXmlDomUtils.getParameter(params, 2, null);
      if (param != null && param.length() > 0)
         contxt.setServerPageTags(param);

      param = PSXmlDomUtils.getParameter(params, 3, null);
      if (param != null)
         encodingDefault = param;

      param = PSXmlDomUtils.getParameter(params, 4, null);
      if (param != null && param.toLowerCase().startsWith("y"))
         inlineDisable = true;

      param = PSXmlDomUtils.getParameter(params, 5, null);
      if (param != null && param.equalsIgnoreCase("yes"))
         contxt.setUsePrettyPrint(false);


      try
      {
         if (inputSourceObj instanceof PSPurgableTempFile)
         {
            Charset encoding;
            PSPurgableTempFile inputSourceFile =
                    (PSPurgableTempFile) inputSourceObj;
            request.printTraceMessage
                    ("Loading file " + inputSourceFile.getSourceFileName());

            encoding = PSXmlDomUtils.determineCharacterEncoding
                    (contxt, inputSourceFile, encodingDefault);

               tempDocument = PSHtmlUtils.createHTMLDocument(
                       (File) inputSourceObj
               , encoding,true,htmlcleanupFile);
         }
         else
         {
            // inputSourceObj is a String
            String inputSourceString = inputSourceObj.toString().trim();
            if (inputSourceString.length() < 1)
            {
               contxt.printTraceMessage("the source is empty");
               return;
            }

            tempDocument = PSHtmlUtils.createHTMLDocument(inputSourceString,
                    Charset.forName(encodingDefault),true,htmlcleanupFile);

         }

         String cleansedString;

         //Handle the div class='rxbodyfield' content wrapper
         Elements wrapperDivs = tempDocument.select("div[class='" + RXBODYFIELD_CLASS + "']");
         if(wrapperDivs.size()==0){
            Attributes attributes = new Attributes();
            attributes.put("class",RXBODYFIELD_CLASS);

            Element e = new Element(Tag.valueOf("div"), "", attributes);
            e.html(tempDocument.select("body").html());

            tempDocument.select("body").html(e.toString());
            cleansedString = tempDocument.body().html();
         }else if (wrapperDivs.size()>1){
            Element first = wrapperDivs.first();
            for(Element e : wrapperDivs) {

               if (first != null && ! e.equals(first)){
                  first.appendChildren(e.children());
                  e.remove();
               }
            }
            cleansedString = tempDocument.body().html();
         }else{
            cleansedString = tempDocument.body().html();
         }


         request.setParameter(fileParamName, cleansedString);
      } catch (PSHtmlParsingException e) {
         e.printStackTrace();
      }
   }

   /**
    * Lookup the field to get important properties
    * @param name the field name, not <code>null</code> or empty
    * @param contenttypeid the content type id, not <code>null</code>
    * and numeric
    * @return the field, should never be <code>null</code>
    */
   private PSField getField(String name, String contenttypeid)
   {
      if (StringUtils.isBlank(name))
      {
         throw new IllegalArgumentException("name may not be null or empty");
      }
      if (StringUtils.isBlank(contenttypeid))
      {
         throw new IllegalArgumentException("contenttypeid may not be null or empty");
      }
      if (!StringUtils.isNumeric(contenttypeid))
      {
         throw new IllegalArgumentException("contenttypeid must be numeric");
      }
      PSItemDefManager idm = PSItemDefManager.getInstance();
      long[] ctypeids = new long[1];
      ctypeids[0] = Long.parseLong(contenttypeid);
      Collection<PSField> fields = idm.getFieldsByName(ctypeids, name);
      if (fields == null || fields.size() == 0)
      {
         throw new IllegalStateException("No field found for " + name);
      }
      return fields.iterator().next();
   }

   /**
    * The function name used for error handling
    **/
   private static final String ms_className = "PSXdTextCleanup";

   /**
    * The rxbodyfield class that indicates the body div.
    */
   private static final String RXBODYFIELD_CLASS = "rxbodyfield";
}


