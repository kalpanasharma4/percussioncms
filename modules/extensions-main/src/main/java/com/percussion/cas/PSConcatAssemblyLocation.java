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
package com.percussion.cas;

import com.percussion.error.PSExceptionUtils;
import com.percussion.extension.IPSAssemblyLocation;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSExtensionErrors;
import com.percussion.extension.PSExtensionException;
import com.percussion.server.IPSRequestContext;

import java.io.File;

/**
 * This assembly location generator concatenates all provided parameters.
 */
public class PSConcatAssemblyLocation implements IPSAssemblyLocation
{
   // See interface for details
   public void init(IPSExtensionDef def, File codeRoot)
      throws PSExtensionException
   {
      m_def = def;
   }

   /**
    * This implementation takes as many parameters defined in the 
    * RXLOCATIONSCHEMEPARAMS table and simply concatenates them together. If
    * no parameters are specified, an empty String will be returned.
    * e.g. params[0] + params[1] + ... + params[n]. There will be no checks
    * made. All parameters provided with backslashes will be transformed to
    * forward slashes.
    */
   public String createLocation(Object[] params, IPSRequestContext request)
      throws PSExtensionException
   {
      String exitName = getClass().getName();
      request.printTraceMessage("Entering " + exitName + ".createLocation");

      StringBuilder location = new StringBuilder();
      try
      {
         for (Object param : params) {
            location.append(param.toString());
         }
         
         request.printTraceMessage("Location= " + location);
      }
      catch (Exception e)
      {
         request.printTraceMessage("Error: " + PSExceptionUtils.getMessageForLog(e));
         
         Object[] args = 
         { 
            m_def.getRef().getExtensionName(), 
            PSExceptionUtils.getMessageForLog(e)
         };
         throw new PSExtensionException(
            IPSExtensionErrors.EXT_PROCESSOR_EXCEPTION, args);
      }
      finally
      {
         request.printTraceMessage("Leaving " + exitName + ".createLocation");
      }

      return location.toString();
   }
   
   /**
    * This is the definition for this extension. You may want to use it for
    * validation purposes in the <code>createLocation</code> method.
    */
   protected IPSExtensionDef m_def = null;
}
