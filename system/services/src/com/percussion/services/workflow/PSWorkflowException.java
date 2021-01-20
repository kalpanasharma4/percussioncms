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
package com.percussion.services.workflow;

import com.percussion.utils.exceptions.PSBaseException;

/**
 * Exceptions generated by the workflow service
 * 
 * @author dougrand
 */
public class PSWorkflowException extends PSBaseException
{
   /**
    * Serial id required for serializable classes 
    */
   private static final long serialVersionUID = 4181855115204766647L;

   /**
    * Construct a workflow exception for messages taking an array of
    * arguments. Be sure to store the arguments in the correct order in
    * the array, where {0} in the string is array element 0, etc.
    *
    * @param msgCode The code of the error string to load.
    *
    * @param arrayArgs The array of arguments to use as the arguments
    *    in the error message.  May be <code>null</code>, and may contain
    *    <code>null</code> elements.
    */
   public PSWorkflowException(int msgCode, Object... arrayArgs) {
      super(msgCode, arrayArgs);
   }

   /**
    * Same as {@link #PSWorkflowException(int, Object...)} but takes one
    * additional parameter to indicate the exception that caused this exception.
    * 
    * @param msgCode The code of the error string to load.
    * @param cause The original exception that caused this exception to be
    *           thrown, may be <code>null</code>.
    * @param arrayArgs The array of arguments to use as the arguments in the
    *           error message. May be <code>null</code>, and may contain
    *           <code>null</code> elements.
    */
   public PSWorkflowException(int msgCode, Throwable cause, Object... arrayArgs) {
      super(msgCode, cause, arrayArgs);
   }

   /**
    * Construct a workflow exception for messages taking no arguments.
    *
    * @param msgCode The error string to load.
    */
   public PSWorkflowException(int msgCode) {
      super(msgCode);
   }

   @Override
   protected String getResourceBundleBaseName()
   {
      return "com.percussion.services.workflow.PSWorkflowErrorStringBundle";
   }

}
