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
package com.percussion.services.assembly.impl.plugin;

import com.googlecode.htmlcompressor.compressor.HtmlCompressor;
import com.googlecode.htmlcompressor.compressor.XmlCompressor;
import com.googlecode.htmlcompressor.compressor.YuiCssCompressor;
import com.percussion.cms.IPSConstants;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.error.PSExceptionUtils;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.server.PSServer;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.IPSAssemblyResult;
import com.percussion.services.assembly.IPSAssemblyResult.Status;
import com.percussion.services.assembly.impl.PSTrackAssemblyError;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.services.notification.IPSNotificationListener;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.services.notification.PSNotificationServiceLocator;
import com.percussion.services.utils.jexl.PSVelocityUtils;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.utils.jexl.IPSScript;
import com.percussion.utils.jexl.PSJexlEvaluator;
import com.percussion.utils.string.PSStringUtils;
import com.percussion.utils.timing.PSStopwatchStack;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.util.ExtProperties;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static com.percussion.cms.IPSConstants.MIME_HTML;
import static com.percussion.cms.IPSConstants.MIME_XML;
import static com.percussion.cms.IPSConstants.PARAM_REINIT_TEMPLATE_ENGINE;
import static com.percussion.cms.IPSConstants.SERVER_PROP_COMPRESS_OUTPUT;
import static com.percussion.cms.IPSConstants.SERVER_PROP_NO_CACHE_TEMPLATES;
import static com.percussion.cms.IPSConstants.SYS_PARAM_CHARSET;
import static com.percussion.cms.IPSConstants.SYS_PARAM_CTX;
import static com.percussion.cms.IPSConstants.SYS_PARAM_MIMETYPE;
import static com.percussion.cms.IPSConstants.SYS_PARAM_NO_CACHE_TEMPLATE;
import static com.percussion.cms.IPSConstants.SYS_PARAM_TEMPLATE;
import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * This assembler uses a Velocity template to create HTML output text. This uses
 * the <code>RuntimeServices</code> from velocity as the velocity engine does
 * not allow access to everything required to build templates from our own
 * resource loader. The templates are cached using a <code>WeakHashMap</code>
 * which keys on the entire template text. Despite the seeming inefficiency,
 * this still takes a very small amount of time compared to the rest of the
 * process.
 * <p>
 * Note, any changes to the (system or user) Velocity macro files need to send a
 * notification to the instance of this class via
 * {@link IPSNotificationService#notifyEvent(PSNotificationEvent)}, so that the
 * the changes will be able to take effect right away.
 * <p>
 * This assembler will cache all compiled templates by default. However,
 * <b>caching compiled templates may consume too much memory</b> for some
 * templates. This behavior can be turn off with the following options:
 * <ul>
 *  <li>
 *      Specify <code>"noCacheTemplates=*"</code> as one of the server.properties,
 *      which will turn off the caching behavior for all templates.
 *  </li>
 *  <li>
 *      Specify <code>"noCacheTemplates=template-1,template-2"</code> as one of
 *      the server.properties options, which will turn off the caching behavior for only the
 *      specified templates.
 *  </li>
 *  <li>
 *      Specify a JEXL binding variable <code>"$sys.noCacheTemplate=true"</code> on
 *      a template, which will turn off the caching behavior for the template that the binding belongs to.
 *  </li>
 * </ul>
 * If a template caching is turned off, then any (snippet or child) templates used by
 * the template may inherit the none-cache behavior.
 *
 * @author dougrand
 */
@Transactional(readOnly = true,noRollbackFor = RuntimeException.class)
public class PSVelocityAssembler extends PSAssemblerBase
        implements
        IPSNotificationListener
{

   /**
    * Path to the Velocity properties file.
    */
   public static final String VELOCITY_CONFIG_PATH=  PSServer.getRxConfigDir() + File.separatorChar + "velocity.properties";

   /**
    * Start PI fragment for field and slot extraction end markup
    */
   private static final String PSX_END = "<?psx-end-";

   /**
    * Start PI fragment for field and slot extraction start markup
    */
   private static final String PSX_START = "<?psx-start-";

   /**
    * End PI fragment
    */
   private static final String END_PI = "?>";

   /**
    * Precalculated expression for JEXL
    */
   private static final IPSScript SYS_MIMETYPE = PSJexlEvaluator
           .createStaticExpression(SYS_PARAM_MIMETYPE);

   /**
    * Precalculated expression for JEXL
    */
   private static final IPSScript SYS_CHARSET = PSJexlEvaluator
           .createStaticExpression(SYS_PARAM_CHARSET);

   /**
    * Precalculated expression for JEXL
    */
   private static final IPSScript SYS_TEMPLATE = PSJexlEvaluator
           .createStaticExpression(SYS_PARAM_TEMPLATE);

   /**
    * Storage for velocity templates that have been compiled. The key is the
    * actual source of the entire template.
    */
   private static ConcurrentHashMap<String, Template> compiledTemplates = new ConcurrentHashMap<>();

   /**
    * Logger for this class
    */
   private static final Logger logger = LogManager.getLogger(PSVelocityAssembler.class);

   private static final String VELOCITY_RELOAD_PARAM="com.percussion.extension.assembly.autoReload";

   /**
    * Engine for templating
    */
   protected RuntimeServices runtimeSvc = null;

   /**
    * This is used to determine if an instance of this class has been registered
    * to listen on any file changes, see {@link #PSVelocityAssembler()} for
    * detail. Default to <code>false</code>.
    */
   private boolean listenOnFileChange = false;

   public boolean isListenOnFileChange() {
      return listenOnFileChange;
   }

   public void setListenOnFileChange(boolean listenOnFileChange) {
      this.listenOnFileChange = listenOnFileChange;
   }

   /**
    * This is used to set the {@link RuntimeConstants#VM_LIBRARY} property of
    * the template engine {@link #runtimeSvc}. Default to <code>null</code>. It
    * is set by {@link #init(IPSExtensionDef, File)}.
    *
    */
   protected String libraries = null;

   /**
    * This is used to set the {@link RuntimeConstants#VM_LIBRARY_AUTORELOAD} and
    * {@link RuntimeConstants#FILE_RESOURCE_LOADER_CACHE} properties of the
    * template engine {@link #runtimeSvc}. Default to <code>yes</code>. It is
    * set by {@link #init(IPSExtensionDef, File)}.
    * <p>
    */
   protected String reload = "yes";

   /**
    * The system template macro path. Default to <code>null</code>. It is set
    * by {@link #getSysTemplateMacroPath()} and never modified after that.
    */
   private String sysTemplatePath = null;

   /**
    * The local template macro path. Default to <code>null</code>. It is set
    * by {@link #getLocalTemplateMacroPath()} and never modified after that.
    */
   private String localTemplatePath = null;

   /**
    * Default constructor. This constructor must be called by the extended
    * classes.
    */
   public PSVelocityAssembler()
   {
      // listen to any file changes that care to send a notification
      // for invoking {@link #initVelocity()} in case changing a macro file.
      //
      // however, there is no need to add another listener if there is already
      // an instance of this class that initialized Velocity Engine (ms_rs)
      // and assumed to listen on the file changes.
      if (!listenOnFileChange)
      {
         IPSNotificationService notifyService = PSNotificationServiceLocator
                 .getNotificationService();
         notifyService.addListener(EventType.FILE, this);

         listenOnFileChange = true;
      }
   }

   @Override
   protected IPSAssemblyResult doAssembleSingle(IPSAssemblyItem item)
           throws Exception
   {
      // Discover whether this is a partial assembly
      String part = item.getParameterValue(IPSHtmlParameters.SYS_PART, null);
      IPSAssemblyResult work = (IPSAssemblyResult) item;
      PSJexlEvaluator eval = new PSJexlEvaluator(work.getBindings());

      eval.bind("$sys.part.render", true);
      eval.bind("$sys.part.end", true);

      if (StringUtils.isBlank(part))
      {
         eval.bind("$sys.part.active", false);
         return super.doAssembleSingle(item);
      }
      else
      {
         /*
          * In a partial assembly, no global template is expanded. The
          * underlying macros add PIs to the output document that help the
          * code here separate the text from the surrounding formatting. The
          * macros also use the information, if supplied, to suppress the
          * rendering of unneeded fields and slots.
          */
         if (item.hasNode())
            logger.debug("Partially Assemble item {}"
                    , item.getNode().getUUID());
         else
            logger.debug("Partially Assemble item {}"
                    , item.getParameterValue(IPSHtmlParameters.SYS_CONTENTID,
                    "unknown"));

         String[] pieces = part.split(":");

         if (pieces.length != 2)
         {
            logger.warn("Bad sys_part parameter: {}" , part);
         }
         else
         {
            eval.bind("$sys.part.type", pieces[0]);
            eval.bind("$sys.part.name", pieces[1]);
            eval.bind("$sys.part.active", true);
         }
         IPSAssemblyResult rval = assembleSingle(item);

         // Extract part
         if (pieces.length == 2)
         {
            Charset cset = PSStringUtils.getCharsetFromMimeType(rval
                    .getMimeType());
            String parttext = new String(rval.getResultData(), cset.name());
            String partstart = PSX_START + pieces[0] + " " + pieces[1]
                    + END_PI;
            String partend = PSX_END + pieces[0] + " " + pieces[1] + END_PI;
            int start = parttext.indexOf(partstart);
            if (start >= 0)
            {
               int end = parttext.indexOf(partend, start);
               if (end >= 0)
               {
                  parttext = parttext
                          .substring(start + partstart.length(), end);
                  rval.setResultData(parttext.getBytes(cset.name()));
               }
            }
         }

         logger.debug("Partial Result is of type {}",  rval.getMimeType());
         return rval;
      }
   }


   /**
    * The jexl expression object to determine if the caching binding is set.
    */
   private static final IPSScript SYS_NO_CACHE_TEMPLATE_EXP = PSJexlEvaluator
           .createStaticExpression(SYS_PARAM_NO_CACHE_TEMPLATE);

   /**
    * Contains a list of template names whose compiled templates will not be
    * cached. If the 1st element is <code>*</code>, then the system will not
    * cache any of the compiled templates.
    */
   private List<String> noCacheTemplates = Collections.synchronizedList(new ArrayList<String>());

   /**
    * Determines if the system will cache the compiled template of the
    * specified template.
    *
    * @param templateName the name of the template in question, assumed not
    * <code>null</code>.
    * @param eval the jexl evaluator, assumed not <code>null</code>.
    *
    * @return <code>true</code> if the compiled template will not be cached.
    */
   private boolean isCacheTemplateOff(String templateName, PSJexlEvaluator eval)
   {
      boolean result = false;

      getNoCacheTemplates();
      if (this.noCacheTemplates.contains("*") || this.noCacheTemplates.contains(templateName))
      {
         result = true;
      }
      else
      {
         result = getNoCacheTemplateValueFromBinding(eval);
      }

      if (result)
         logger.debug("Cache compiled velocity is off for template '{}'", templateName);
      else
         logger.debug("Cache compiled velocity is on for template '{}",templateName);


      return result;
   }

   /**
    * Gets the value of the binding variable SYS_PARAM_NO_CACHE,
    * and return its value if exists; otherwise return <code>false</code> if
    * the binding variable does not exist.
    *
    * @param eval the jex evaluator, used to retrieve and evaluate the
    * binding variable, assumed not <code>null</code>.
    *
    * @return the value described above.
    */
   private boolean getNoCacheTemplateValueFromBinding(PSJexlEvaluator eval)
   {
      try
      {
         Object isCacheObj = eval.evaluate(SYS_NO_CACHE_TEMPLATE_EXP);
         if (isCacheObj == null)
            return false;

         return (Boolean) isCacheObj;
      }
      catch (Exception e)
      {
         logger.warn("An unexpected error was encountered while processing binding: {} Error: {}", eval.bindingsToString(),
                 PSExceptionUtils.getMessageForLog(e));
         logger.debug(e);
         return false;
      }
   }

   /**
    * Gets the template names, whose compiled template objects will not
    * be cached.
    * <br>
    * The template names are specified through a server property
    * "no_cache_templates", see the class description on the possible
    * values of the property.
    *
    * Property value may be updated while the server is running, without a restart.
    */
   private void getNoCacheTemplates()
   {

      noCacheTemplates.clear();

      String nameProperty = PSServer.getProperty(SERVER_PROP_NO_CACHE_TEMPLATES,"");
      if (StringUtils.isBlank(nameProperty))
         return;

      if (nameProperty.trim().equals("*"))
      {
         this.noCacheTemplates.add("*");
         logger.warn("Caching Velocity templates is turned off for all templates based on the {} setting in server.properties.  This may affect publishing performance. Remove this property or set to an empty string to enable caching.",SERVER_PROP_NO_CACHE_TEMPLATES);
         return;
      }

      String[] names = nameProperty.split(",");
      for (String name : names)
      {
         name=name.trim();

         if (isBlank(name))
            continue;

         if (!noCacheTemplates.contains(name)) {
            logger.info("Caching of templates is turned off for the following template: {}"
                    , name);

            noCacheTemplates.add(name);
         }
      }


   }

   @Override
   public IPSAssemblyResult assembleSingle(IPSAssemblyItem item)
   {
      if (item.getParameterValue(PARAM_REINIT_TEMPLATE_ENGINE, IPSConstants.FALSE)
              .equalsIgnoreCase(IPSConstants.TRUE))
      {
         try
         {
            initVelocity();
            // Change so we don't do this twice
            item.setParameterValue(PARAM_REINIT_TEMPLATE_ENGINE, IPSConstants.FALSE);
         }
         catch (Exception e)
         {
            logger.error("Problem reinitializing velocity. Error: {}",
                    PSExceptionUtils.getMessageForLog(e));
         }
      }

      PSJexlEvaluator eval = new PSJexlEvaluator(item.getBindings());
      String template = null;

      PSStopwatchStack sws = PSStopwatchStack.getStack();
      sws.start("velocityassemble");
      try
      {
         try
         {
            template = (String) eval.evaluate(SYS_TEMPLATE);
         }
         catch (Exception e)
         {
            return getFailureResult(item, "Exception retrieving template: "
                    + PSExceptionUtils.getMessageForLog(e));
         }

         // Add currentslot map
         eval.bind("$sys.currentslot", new HashMap<String, Object>());

         if (StringUtils.isBlank(template))
         {
            return getFailureResult(item, "no velocity template present");
         }

         VelocityContext ctx = PSVelocityUtils.getContext(item
                 .getBindings());

         // Add $sys.ctx bindings so the velocity context is accessible for
         // some velocity tools
         eval.bind(SYS_PARAM_CTX, ctx);

         boolean isCacheOff = isCacheTemplateOff(item.getTemplate().getName(), eval);

         try
         {
            Template t = compiledTemplates.get(template);
            if (t == null)
            {
               try
               {
                  sws.start("parseVelocityTemplate");
                  t = PSVelocityUtils.compileTemplate(template,
                          item.getTemplate().getName(), runtimeSvc);
                  if (!isCacheOff)
                  {
                     compiledTemplates.put(template, t);
                  }
               }
               finally
               {
                  sws.stop();
               }
            }

            StringWriter writer = new StringWriter(4000);
            t.merge(ctx, writer);
            writer.close();
            String result = writer.toString();

            String mtype = (String) eval.evaluate(SYS_MIMETYPE);
            if (StringUtils.isBlank(mtype))
               mtype = IPSConstants.DEFAULT_MIMETYPE;

            if(PSServer.getProperty(SERVER_PROP_COMPRESS_OUTPUT,IPSConstants.FALSE).equalsIgnoreCase(IPSConstants.TRUE)){
               if(mtype.equals(MIME_HTML)) {
                  result = compressHtml(result);
               }else if(mtype.equals(MIME_XML)){
                  result = compressXML(result);
               }
            }

            String charset = (String) eval.evaluate(SYS_CHARSET);
            if (!StringUtils.isBlank(charset)) {
               // Canonicalize the charset
               Charset cset = Charset.forName(charset);
               charset = cset.name();

               item.setResultData(result.getBytes(charset));
            } else {
               item.setResultData(result.getBytes(StandardCharsets.UTF_8));
            }

            if (!StringUtils.isBlank(charset))
               mtype += ";charset=" + charset;
            else
               mtype += ";charset=utf-8";
            item.setMimeType(mtype);

            item.setStatus(Status.SUCCESS);
            return (IPSAssemblyResult) item;
         }
         catch (Exception ae)
         {

            logger.error("][An unexpected error occurred while assembling resource: {} with Template: {} Error: {}",
                    item.getId(),
                    item.getTemplate().getName(),
                    PSExceptionUtils.getMessageForLog(ae));

            String message = getErrorMsgForItem(item);

            PSTrackAssemblyError.addProblem(message, ae);

            // Create clone for response
            IPSAssemblyItem work = (IPSAssemblyItem) item.clone();
            work.setStatus(Status.FAILURE);
            work.setMimeType("text/html");
            String style="";
            //If not in preview - style the error to be hidden.
            if(work.getContext() != 0){
               style=" style='display:none;'";
            }

            StringBuilder results = new StringBuilder();
            results.append("<html><head></head><body>");
            results
                    .append("<div class='perc-assembly-error'").append(style).append(" \">");
            results.append("<h2>");
            results.append(message);
            results.append(" \"");
            results.append("</h2><p>");
            results.append(message).append(" : ").append(ae.getMessage());
            results.append("</p></div></body></html>");
            try{
               work.setResultData(results.toString().getBytes(StandardCharsets.UTF_8));
            }catch(IOException e){
               ms_log.error(PSExceptionUtils.getMessageForLog(e));
            }
            return (IPSAssemblyResult) work;


         }
      } finally {
         sws.stop();
      }
   }

   private String compressXML(String result) {
      XmlCompressor compressor = new XmlCompressor();

      compressor.setEnabled(true);

      String ret = result;
      try {
         ret = compressor.compress(result);
      }catch(Exception e){
         ms_log.warn(e.getMessage(),e);
      }
      return ret;

   }

   private String compressHtml(String html) {

      HtmlCompressor compressor = new HtmlCompressor();


      compressor.setEnabled(true);                   //if false all compression is off (default is true)
      compressor.setRemoveComments(true);            //if false keeps HTML comments (default is true)
      compressor.setRemoveMultiSpaces(true);         //if false keeps multiple whitespace characters (default is true)
      compressor.setRemoveIntertagSpaces(true);      //removes iter-tag whitespace characters
      compressor.setRemoveQuotes(true);              //removes unnecessary tag attribute quotes
      compressor.setSimpleDoctype(true);             //simplify existing doctype
      compressor.setRemoveScriptAttributes(true);    //remove optional attributes from script tags
      compressor.setRemoveStyleAttributes(true);     //remove optional attributes from style tags
      compressor.setRemoveLinkAttributes(true);      //remove optional attributes from link tags
      compressor.setRemoveFormAttributes(true);      //remove optional attributes from form tags
      compressor.setRemoveInputAttributes(true);     //remove optional attributes from input tags
      compressor.setSimpleBooleanAttributes(true);   //remove values from boolean tag attributes
      compressor.setRemoveJavaScriptProtocol(true);  //remove "javascript:" from inline event handlers
      compressor.setRemoveHttpProtocol(true);        //replace "http://" with "//" inside tag attributes
      compressor.setRemoveHttpsProtocol(true);       //replace "https://" with "//" inside tag attributes
      compressor.setPreserveLineBreaks(true);        //preserves original line breaks
      compressor.setRemoveSurroundingSpaces("br,p"); //remove spaces around provided tags

      compressor.setCompressCss(false);               //compress inline css
      compressor.setCompressJavaScript(false);        //compress inline javascript


      List<Pattern> preservePatterns = new ArrayList<>();
      preservePatterns.add(HtmlCompressor.PHP_TAG_PATTERN); //<?php ... ?> blocks
      preservePatterns.add(HtmlCompressor.SERVER_SCRIPT_TAG_PATTERN); //<% ... %> blocks
      preservePatterns.add(HtmlCompressor.SERVER_SIDE_INCLUDE_PATTERN); //<!--# ... --> blocks
      preservePatterns.add(Pattern.compile("<jsp:.*?>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)); //<jsp: ... > tags
      compressor.setPreservePatterns(preservePatterns);

      String ret = html;
      try {
         ret = compressor.compress(html);
      }catch(Exception e){
         ms_log.warn(e.getMessage(),e);
         return html;
      }

      YuiCssCompressor cssCompressor = new YuiCssCompressor();
      cssCompressor.setLineBreak(1); //  start from new line of every css class declaration
      compressor.setCssCompressor(cssCompressor);
      compressor.setCompressCss(true);

      try {
         ret = compressor.compress(ret);

      }catch(Exception e){
         ms_log.warn(e.getMessage(),e);
      }


      compressor.setYuiCssLineBreak(80);             //--line-break param for Yahoo YUI Compressor
      compressor.setYuiJsDisableOptimizations(true); //--disable-optimizations param for Yahoo YUI Compressor
      compressor.setYuiJsLineBreak(1);              //--line-break param for Yahoo YUI Compressor
      compressor.setYuiJsNoMunge(true);              //--nomunge param for Yahoo YUI Compressor
      compressor.setYuiJsPreserveAllSemiColons(true);//--preserve-semi param for Yahoo YUI Compressor
      compressor.setCompressJavaScript(true);        //compress inline javascript

      try {
         ret = compressor.compress(ret);

      }catch(Exception e){
         ms_log.warn(e.getMessage(),e);
      }
      return ret;
   }

   /**
    * Gets error message when failed to assemble the specified item.
    * @param item the item, assumed not <code>null</code>.
    * @return the error message, not blank.
    */
   private String getErrorMsgForItem(IPSAssemblyItem item)
   {
      IPSCmsObjectMgr cms = PSCmsObjectMgrLocator.getObjectManager();
      PSComponentSummary summary = cms.loadComponentSummary(item.getId().getUUID());

      return "Problem assembling output for item (name=\"" + summary.getName()
              + "\", id=" + item.getId().toString() + ") with template: "
              + item.getTemplate().getName() + ".";
   }

   @Override
   @SuppressWarnings("unused")
   public void init(IPSExtensionDef def, File codeRoot)
           throws PSExtensionException
   {
      try
      {

         reload = def
                 .getInitParameter(VELOCITY_RELOAD_PARAM);

         // no need to init Velocity Engine again if it has been initialized.
         if (runtimeSvc != null)
            return;

         initVelocity();
      }
      catch (Exception e)
      {
         logger.error(PSExceptionUtils.getMessageForLog(e));
         throw new PSExtensionException("Java",PSExceptionUtils.getMessageForLog(e));
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see com.percussion.services.notification.IPSNotificationListener#notifyEvent(com.percussion.services.notification.PSNotificationEvent)
    */
   public void notifyEvent(PSNotificationEvent event)
   {
      if (event.getType() == EventType.FILE
              && event.getTarget() instanceof File)
      {
         File cFile = (File) event.getTarget();
         if (isTemplateMacroPath(cFile.getAbsolutePath()))
         {
            initVelocity();
         }
      }
   }

   /**
    * Determines if the specified file path is a system or local template macro
    * path.
    *
    * @param path the file path in question. It may be <code>null</code> or
    *           empty.
    *
    * @return <code>true</code> if the specified file path is a system or
    *         local template macro path; otherwise return <code>false</code>.
    */
   private boolean isTemplateMacroPath(String path)
   {
      if (StringUtils.isBlank(path))
         return false;

      return (path.startsWith(getSysTemplateMacroPath()) || path
              .startsWith(getLocalTemplateMacroPath()));
   }

   /**
    * Lazy load the system template macro path.
    *
    * @return the system template macro path.
    */
   private String getSysTemplateMacroPath()
   {
      if (sysTemplatePath == null)
         sysTemplatePath = PSServer.getRxFile("sys_resources/vm");
      return sysTemplatePath;
   }

   /**
    * Lazy load the local template macro path.
    *
    * @return the local template macro path.
    */
   private String getLocalTemplateMacroPath()
   {
      if (localTemplatePath == null)
         localTemplatePath = PSServer.getRxFile("rx_resources/vm");
      return localTemplatePath;
   }

   /**
    * Gets all macro files from the specified directories.
    *
    * @param sysDir the system directory that contains the velocity macros from
    * the core system, assumed not <code>null</code>.
    * @param localDir the directory contains the user specific velocity macros.
    *
    * @return all the files with ".vm" extension (case insensitive) in the
    * specified directories, never <code>null</code>, may be empty.
    */
   private String getMacroFiles(String sysDir, String localDir)
   {
      StringBuilder buffer = new StringBuilder();
      getMacroFiles(buffer, sysDir);
      getMacroFiles(buffer, localDir);

      return buffer.toString();
   }


   private String validateMacros(String macros){

      StringBuilder ret = new StringBuilder("");
      String[] vmFiles = macros.split(",");


      for(String s : vmFiles){
            s = s.trim();
            if(!StringUtils.isEmpty(s)) {
               Template t = runtimeSvc.getTemplate(s);
               if (t.process()) {
                  if (ret.length() == 0)
                     ret.append(s);
                  else
                     ret.append(",").append(s);
               } else {
                  logger.warn("Skipping load of VM  file: {} due to parsing errors.", s);
               }
            }

      }

      return ret.toString();

   }
   /**
    * Gets all macro files from the specified directory
    *
    * @param buffer the buffer for collecting all macro files and separated
    * with comma.
    * @param location the directory that contains the velocity macros, assumed
    * not <code>null</code>.
    */
   private void getMacroFiles(StringBuilder buffer, String location)
   {
      FileFilter filter = new VMFileFilter();
      File dir = new File(location);
      File[] files = dir.listFiles(filter);

      if(files != null) {
         for (File f : files) {
            PSVelocityUtils.preProcessTemplateFile(f);

            if (buffer.length() > 0)
               buffer.append(",");

            buffer.append(f.getName());
         }
      }
   }

   /**
    * Filter the file names, only accept files with ".vm" extension.
    */
   private class VMFileFilter implements FileFilter
   {
      /*
       * //see base interface method for details
       */
      public boolean accept(File pathname)
      {
         String name = pathname.getName();
         if (StringUtils.isBlank(name))
            return false;

         int i = name.lastIndexOf('.');
         if ( i > 0 && (name.length() - i) == 3)
         {
            String suffix = name.substring(i);
            return suffix.equalsIgnoreCase(".vm");
         }
         return false;
      }
   }

   /**
    * Initialize velocity engine
    */
   private void initVelocity()
   {
      logger.info("Velocity initializing..");

      runtimeSvc = new RuntimeInstance();


      String sysTemplates = getSysTemplateMacroPath();
      String rxTemplates = getLocalTemplateMacroPath();

      try{
         runtimeSvc.addProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH,
                 rxTemplates);
      }catch(Exception e){
         logger.error("Error initializing macros from rx_resources/vm folder!", e);
         runtimeSvc.clearProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH);
      }

      try{
         runtimeSvc.addProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH,
                 sysTemplates);
      }catch(Exception e){
         logger.error("Error initializing macros from sys_resources/vm folder!",e);
         runtimeSvc.clearProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH);
      }

      libraries = getMacroFiles(sysTemplates, rxTemplates);

      /**
       * The below updates are set in velocity.properties.
       * They are being set here, too, as backup defaults
       * in case the file is missing.
       */
      runtimeSvc.setProperty(RuntimeConstants.VM_PERM_INLINE_LOCAL, "true");
      runtimeSvc.setProperty(RuntimeConstants.PARSER_HYPHEN_ALLOWED, "true");
      runtimeSvc.setProperty(RuntimeConstants.VM_PERM_ALLOW_INLINE_REPLACE_GLOBAL, "true");
      runtimeSvc.setProperty(RuntimeConstants.VM_PERM_ALLOW_INLINE, "true");
      runtimeSvc.setProperty(RuntimeConstants.INPUT_ENCODING, "UTF-8");
      runtimeSvc.setProperty(RuntimeConstants.CHECK_EMPTY_OBJECTS, "false");
      runtimeSvc.setProperty(RuntimeConstants.SPACE_GOBBLING, "bc");
      runtimeSvc.setProperty(RuntimeConstants.CONVERSION_HANDLER_CLASS, "none");
      runtimeSvc.setProperty(RuntimeConstants.CHECK_EMPTY_OBJECTS, "true");

      if (reload != null && reload.equalsIgnoreCase("yes"))
      {
         logger.debug("Reload is on");
         runtimeSvc.addProperty(RuntimeConstants.VM_LIBRARY_AUTORELOAD, "true");
         runtimeSvc.addProperty(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, "false");
      }
      else
      {
         logger.debug("Reload is off, caching is on");
         runtimeSvc.addProperty(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, "true");
      }

      try{
         logger.debug("Sys path: {}" , sysTemplates);
         runtimeSvc.addProperty(RuntimeConstants.VM_LIBRARY, libraries);
         logger.debug("Velocity libraries: {}" , libraries);
      }catch(Exception e){
         logger.error("Error initializing Velocity macros. Error: {}",
                 PSExceptionUtils.getMessageForLog(e));
         logger.debug(e);

         runtimeSvc.clearProperty(RuntimeConstants.VM_LIBRARY);

      }

      //get velocity properties
      Properties velProps = new Properties();

      try (InputStream input = new FileInputStream(VELOCITY_CONFIG_PATH)) {
         velProps.load(input);
         runtimeSvc.setConfiguration(new ExtProperties());
         logger.info("Velocity properties initialized from {}", VELOCITY_CONFIG_PATH);
      }
      catch (FileNotFoundException e1)
      {
         logger.error("Unable to locate velocity.properties file from: {} Error: {}",
                 VELOCITY_CONFIG_PATH,
                 PSExceptionUtils.getMessageForLog(e1));
         logger.debug(e1);
      }
      catch (IOException e)
      {
         logger.error("Unable to read properties from velocity.properties file: {}. Error: {}",
                 VELOCITY_CONFIG_PATH,
                 PSExceptionUtils.getMessageForLog(e));
         logger.debug(e);
      }


      try
      {
         runtimeSvc.init(velProps);


      }
      catch (Exception e)
      {
         logger.error("Problem initializing Velocity assembler, excluding rx_resources and attempting to re-initialize...", e);
         runtimeSvc.clearProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH);
         runtimeSvc.clearProperty(RuntimeConstants.VM_LIBRARY);
         runtimeSvc.addProperty(RuntimeConstants.VM_LIBRARY, "sys_assembly.vm");
         runtimeSvc.addProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH,sysTemplates);
         runtimeSvc.init(velProps);
      }

      logger.debug("The current Velocity configuration is: {}" , runtimeSvc.getConfiguration());

      // Remove all compiled templates
      compiledTemplates.clear();

      // Reload caching configuration
      getNoCacheTemplates();

   }

}
