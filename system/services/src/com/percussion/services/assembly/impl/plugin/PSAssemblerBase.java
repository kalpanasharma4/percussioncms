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

import com.percussion.cms.IPSConstants;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.cms.objectstore.PSFolder;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.error.PSExceptionUtils;
import com.percussion.extension.IPSExtension;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.server.PSRequest;
import com.percussion.server.webservices.PSServerFolderProcessor;
import com.percussion.services.assembly.IPSAssembler;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.IPSAssemblyResult;
import com.percussion.services.assembly.IPSAssemblyResult.Status;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.IPSAssemblyTemplate.GlobalTemplateUsage;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.assembly.PSTemplateNotImplementedException;
import com.percussion.services.assembly.data.PSAssemblyWorkItem;
import com.percussion.services.assembly.impl.PSAssemblyJexlEvaluator;
import com.percussion.services.assembly.impl.PSTrackAssemblyError;
import com.percussion.services.assembly.jexl.PSDocumentUtils;
import com.percussion.services.filter.PSFilterException;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.utils.exceptions.PSExceptionHelper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.jexl.IPSScript;
import com.percussion.utils.jexl.PSJexlEvaluator;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Base class to implement the basic assembly pattern. Concrete implementations
 * must implement {@link #assembleSingle(IPSAssemblyItem)}.
 * 
 * @author dougrand
 * 
 */
public abstract class PSAssemblerBase implements IPSAssembler, IPSExtension
{
   /**
    * Commons logger
    */
    protected static final Logger ms_log = LogManager.getLogger(IPSConstants.PUBLISHING_LOG);

   /**
    * Assembly service
    */
   static IPSAssemblyService ms_asm = PSAssemblyServiceLocator
         .getAssemblyService();

   /**
    * Single assembly item to execute
    */
   static class CallableAssemblyItem implements Callable<IPSAssemblyResult>
   {
      /**
       * Item being assembled, set in Ctor
       */
      IPSAssemblyItem mi_item;

      /**
       * Assembler being used
       */
      PSAssemblerBase mi_assembler;

      /**
       * Constructor
       * 
       * @param item item to assemble, assumed never <code>null</code>
       * @param assm assembler being invoked, assumed never <code>null</code>
       */
      public CallableAssemblyItem(IPSAssemblyItem item, PSAssemblerBase assm)
      {
         mi_item = item;
         mi_assembler = assm;
      }

      public IPSAssemblyResult call()
      {
         long start = System.nanoTime();
         try
         {
            PSTrackAssemblyError.init();
            ms_asm.setCurrentAssemblyItem(mi_item);
            return mi_assembler.doAssembleSingle(mi_item);
         }
         catch (Exception e)
         {
            Throwable orig = PSExceptionHelper.findRootCause(e, true);
            ms_log.error("Problem during assembly: Error: {}",
                    PSExceptionUtils.getMessageForLog(e));
            ms_log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            return mi_assembler.getFailureResult(mi_item, orig.toString());
         }
         finally
         {
            PSTrackAssemblyError.handleItem(mi_item);
            long end = System.nanoTime();
            long elapsed = (end - start) / 1000000;
            mi_item.setElapsed((int) elapsed);
            ms_asm.setCurrentAssemblyItem(null);
         }
      }

   }

   /**
    * A static reference to doc utils that can be reused. Never modified after
    * class loading.
    */
   private static PSDocumentUtils ms_docutils = new PSDocumentUtils();

   /**
    * A static jexl expression that's commonly used is stored here for
    * efficiency reasons.
    */
   private static IPSScript ms_sys_site_path = PSJexlEvaluator
         .createStaticExpression("$sys.site.path");

   /**
    * A static jexl expression that's commonly used is stored here for
    * efficiency reasons.
    */
   private static IPSScript ms_sys_site_gt = PSJexlEvaluator
         .createStaticExpression("$sys.site.globalTemplate");

   /*
    * (non-Javadoc)
    * 
    * @see com.percussion.services.assembly.IPSAssembler#assemble(java.util.List)
    */
   public List<IPSAssemblyResult> assemble(List<IPSAssemblyItem> items)
   {
      List<IPSAssemblyResult> results = new ArrayList<>();

      // Spawn futures if needed
      for (IPSAssemblyItem item : items)
      {
         CallableAssemblyItem callable = new CallableAssemblyItem(item, this);
         results.add(callable.call());
      }
      return results;
   }

   /*
    * (non-Javadoc)
    * @see com.percussion.services.assembly.IPSAssembler#preProcessItemBinding(com.percussion.services.assembly.IPSAssemblyItem, com.percussion.services.assembly.impl.PSAssemblyJexlEvaluator)
    */
   public void preProcessItemBinding(IPSAssemblyItem item, PSAssemblyJexlEvaluator eval) throws PSAssemblyException
   {
      // do nothing by default
   }

   /**
    * Do single item assembly. This method first calls the regular assembly for
    * the item. Then depending on the global template settings, it may assemble
    * the global template as well.
    * 
    * @param item item to assemble, assumed not <code>null</code>
    * @return the result, never <code>null</code>
    * @throws Exception if any number of errors occur during assembly or looking
    * up a global template
    */
   protected IPSAssemblyResult doAssembleSingle(IPSAssemblyItem item)
      throws Exception
   {
      if (item.hasNode())
         ms_log.debug("Assemble item {}" , item.getNode().getUUID());
      else
         ms_log.debug("Assemble item: {}",
                 item.getParameterValue(IPSHtmlParameters.SYS_CONTENTID,
                     "unknown"));
      IPSAssemblyResult rval = assembleSingle(item);
      ms_log.debug("Result is of type: {}" , rval.getMimeType());
      // Now handle any global template
      GlobalTemplateUsage gu = item.getTemplate().getGlobalTemplateUsage();
      IPSGuid gtid = null;
      IPSAssemblyService asm = PSAssemblyServiceLocator.getAssemblyService();
      IPSAssemblyTemplate global = null;

      if (gu.equals(GlobalTemplateUsage.Defined))
      {
         gtid = item.getTemplate().getGlobalTemplate();
         if (gtid == null)
         {
            // No global template available, log an error
            ms_log.warn("No global template available for template {} with usage set to defined", item.getTemplate().getName());
         }
         else
         {
            global = asm.loadUnmodifiableTemplate(gtid);
         }
      }
      else if (gu.equals(GlobalTemplateUsage.Default))
      {
         PSJexlEvaluator e = new PSJexlEvaluator(item.getBindings());

         // Search the folders
         String folderidstr = item.getParameterValue(
               IPSHtmlParameters.SYS_FOLDERID, null);
         if (!StringUtils.isBlank(folderidstr))
         {
            try
            {
               String root = (String) e.evaluate(ms_sys_site_path);
               int folderid = Integer.parseInt(folderidstr);
               PSRequest req = PSRequest.getContextForRequest();
               PSServerFolderProcessor proc = PSServerFolderProcessor.getInstance();
               PSLocator[] folderLocators = { new PSLocator(folderid) };

               do
               {
                  PSFolder[] folders = proc.openFolder(folderLocators);
                  if (folders.length != 0)
                  {
                     PSFolder folder = folders[0];
                     String gt = folder.getGlobalTemplateProperty();
                     if (gt != null)
                     {
                        try
                        {
                           global = asm.findTemplateByName(gt);
                           break;
                        }
                        catch (PSAssemblyException ae)
                        {
                           ms_log.warn("The configured global template {} is not a global template", gt);
                        }
                     }
                     PSLocator folderl = new PSLocator(folder.getLocator()
                           .getPartAsInt());
                     PSComponentSummary[] parents = proc
                           .getParentSummaries(folderl);
                     if (parents.length == 0)
                        break;
                     // Must be only one parent per folder
                     PSComponentSummary parent = parents[0];
                     String[] paths = proc.getItemPaths(parent
                           .getCurrentLocator());
                     if (paths.length == 0)
                        break;
                     // One path per folder
                     String path = paths[0];
                     // Once we go "above" the site folder root we stop
                     // searching
                     if (StringUtils.isBlank(root) || !path.startsWith(root))
                        break;
                     folderLocators = new PSLocator[] { new PSLocator(parent
                           .getContentId()) };
                  }
                  else
                  {
                     break;
                  }
               } while (true);
            }
            catch (NumberFormatException ee)
            {
               ms_log.warn("Found illegal folder id, not searching folders for global template: {} Error: {}" , folderidstr,
                       PSExceptionUtils.getMessageForLog(ee));
            }
         }
         if (global == null)
         {
            String gt = (String) e.evaluate(ms_sys_site_gt);
            if (gt != null)
            {
               try
               {
                  global = asm.findTemplateByName(gt);
               }
               catch (PSAssemblyException ae)
               {
                  ms_log.warn("Found invalid global template specified for the site {} Error: {}" ,gt, ae);
               }
            }
         }
      }

      if (global != null)
      {
         rval = processGlobalTemplate(item, rval, asm, global);
         }
      
      return rval;
   }

   /**
    * Process the passed global template
    * 
    * @param item the original assembly item, assumed never <code>null</code>
    * @param rval the original result, assumed never <code>null</code>
    * @param asm the assembly service, assumed never <code>null</code>
    * @param global the global template, assumed never <code>null</code>
    * @return the new result, never <code>null</code> but may be the original
    * result if there's something wrong with it or the global template
    * @throws CloneNotSupportedException
    * @throws ItemNotFoundException
    * @throws RepositoryException
    * @throws PSTemplateNotImplementedException
    * @throws PSAssemblyException
    * @throws UnsupportedRepositoryOperationException
    * @throws IOException
    * @throws IllegalStateException
    * @throws PSFilterException
    */
   private IPSAssemblyResult processGlobalTemplate(IPSAssemblyItem item,
         IPSAssemblyResult rval, IPSAssemblyService asm,
         IPSAssemblyTemplate global)
      throws  RepositoryException,
      PSTemplateNotImplementedException, PSAssemblyException, IllegalStateException,
      IOException, PSFilterException
   {
      // First check that this is a valid result for a global template
      if (validGlobal(rval))
      {
         IPSGuid originalGuid = item.getOriginalTemplateGuid();
         IPSAssemblyItem globalitem = (IPSAssemblyItem) item.clone();
         globalitem.setOriginalTemplateGuid(originalGuid);
         resetAaCommands(item, globalitem);

         PSJexlEvaluator e = new PSJexlEvaluator(globalitem.getBindings());
         e.bind("$sys.innercontent", ms_docutils.extractBody(rval));
         List<IPSAssemblyItem> singleitemlist = new ArrayList<>();
         List<IPSAssemblyResult> singleitemresult;

         globalitem.setTemplate(global);
         globalitem.setBindings(e.getVars());
         singleitemlist.add(globalitem);
         singleitemresult = asm.assemble(singleitemlist);
         if (singleitemresult != null && !singleitemresult.isEmpty())
         {
            rval = singleitemresult.get(0);
            // Put the original template where the servlet can grab it. This
            // is handy for determining if the current page is a page that
            // requires AA handling
            PSJexlEvaluator eval = new PSJexlEvaluator(rval.getBindings());
            eval.bind("$sys.innertemplate", item.getTemplate());
            if (singleitemresult.size()>1)
            {
               ms_log
                     .warn("Ignoring excess results from global template for item {}",
                             item.getNode().getUUID());
            }
         }
         else
         {
            ms_log.error("No result when assembly global template for item: {} ",item.getNode().getUUID());
         }
      }
      else
      {
         ms_log.warn("Did not invoke global template on result that had a mimetype of {}. Either the mimetype was not textual or there was not result data.",
                 rval.getMimeType());
      }
      return rval;
   }

   /**
    * Removes active assembly parameters for global templates and adds a new
    * binding $sys.ltActiveAssembly, if it is active assembly to render inner
    * content.
    * 
    * @param workitem assembly workitem assumed not <code>null</code>.
    * @param globalitem assembly globalitem assumed not <code>null</code>.
    */
   private void resetAaCommands(IPSAssemblyItem workitem,
         IPSAssemblyItem globalitem)
   {
      // remove AA command param
      globalitem.removeParameter(IPSHtmlParameters.SYS_COMMAND);
      globalitem.removeParameter("sys_activeitemid");

      // remove part parameter
      globalitem.removeParameter(IPSHtmlParameters.SYS_PART);

      // reset $sys.activeAssembly
      PSAssemblyJexlEvaluator eval = new PSAssemblyJexlEvaluator(globalitem);
      eval.bind("$sys.activeAssembly", false);

      String cmd = workitem.getParameterValue(IPSHtmlParameters.SYS_COMMAND,
            null);
      if (!StringUtils.isEmpty(cmd))
      {
         eval.bind("$sys.ltActiveAssembly", true);
      }
   }

   /**
    * Check the result data to see that this result can be used in a global
    * template. If not return <code>false</code>.
    * 
    * @param rval the result, assumed not <code>null</code>
    * @return <code>true</code> if the result is a textual result
    */
   private boolean validGlobal(IPSAssemblyResult rval)
   {
      return rval.getMimeType().startsWith("text/")
            && rval.getResultData() != null;
   }

   /**
    * Base method for assembling one item, override in implementation class.
    * 
    * @param item the assembly item, never <code>null</code>
    * @return an assembled item, never <code>null</code>
    */
   public abstract IPSAssemblyResult assembleSingle(IPSAssemblyItem item) throws PSAssemblyException;

   /**
    * Get a failure result for the given assembly item. Note, the derived class
    * may override this method if the assembly item does not implement
    * {@link IPSAssemblyResult}.
    * 
    * @param work the work item, assumed not <code>null</code>
    * @param message the message string, assumed not <code>null</code> or
    * empty
    * 
    * @return the failure result, never <code>null</code>.
    */
   protected IPSAssemblyResult getFailureResult(IPSAssemblyItem work,
         String message)
   {
      return getMessageResult(work, message, Status.FAILURE);
   }

   /**
    * Handle a failure in the assembler
    * 
    * @param work the work item, assumed not <code>null</code>
    * @param message the message string, assumed not <code>null</code> or
    *           empty
    * 
    * @deprecated use {@link #getFailureResult(IPSAssemblyItem, String)} instead
    */
   @Deprecated
   protected void doFailure(PSAssemblyWorkItem work, String message)
   {
      work.setStatus(Status.FAILURE);
      work.setMimeType("text/plain");
      try{
         work.setResultData(message.getBytes(StandardCharsets.UTF_8));
      }catch(IOException e){
         ms_log.error(PSExceptionUtils.getMessageForLog(e));
      }
   }
   
   /**
    * Get a failure result for the given assembly item. Note, the derived class
    * may override this method if the assembly item does not implement
    * {@link IPSAssemblyResult}.
    * 
    * @param work the work item, assumed not <code>null</code>
    * @param message the message string, assumed not <code>null</code> or
    * empty
    * 
    * @return the failure result, never <code>null</code>.
    */
   protected IPSAssemblyResult getMessageResult(IPSAssemblyItem work,
         String message, Status status)
   {
      work.setStatus(status);
      work.setMimeType("text/plain");
      try{
         work.setResultData(message.getBytes(StandardCharsets.UTF_8));
      }catch(IOException e){
         ms_log.error(PSExceptionUtils.getMessageForLog(e));
      }
      return (IPSAssemblyResult) work;

   }


   /**
    * (non-Javadoc)
    * 
    * @see com.percussion.extension.IPSExtension#init(com.percussion.extension.IPSExtensionDef,
    * java.io.File)
    */
   @SuppressWarnings("unused")
   public void init(IPSExtensionDef def, File codeRoot)
      throws PSExtensionException
   {
   // No initialization required
   }
}
