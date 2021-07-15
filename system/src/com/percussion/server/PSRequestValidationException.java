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

package com.percussion.server;

import com.percussion.error.PSException;
import com.percussion.xml.PSXmlDocumentBuilder;

/**
 * PSRequestValidationException is thrown when a request is deemed to
 * be invalidation. This usually occurs when an application has defined
 * selection criteria or validation rules and the conditions are not
 * met.
 *
 * @author      Tas Giakouminakis
 * @version    1.0
 * @since      1.0
 */
public class PSRequestValidationException extends PSException {
   /**
    * Construct an exception for messages taking only a single argument.
    *
    * @param msgCode       the error string to load
    *
    * @param singleArg      the argument to use as the sole argument in
    *                      the error message
    */
   public PSRequestValidationException(int msgCode, Object singleArg)
   {
      super(msgCode, singleArg);
   }
   
   /**
    * Construct an exception for messages taking an array of
    * arguments. Be sure to store the arguments in the correct order in
    * the array, where {0} in the string is array element 0, etc.
    *
    * @param msgCode       the error string to load
    *
    * @param arrayArgs      the array of arguments to use as the arguments
    *                      in the error message
    */
   public PSRequestValidationException(int msgCode, Object[] arrayArgs)
   {
      super(msgCode, arrayArgs);
   }
   
   /**
    * Construct an exception for messages taking no arguments.
    *
    * @param msgCode       the error string to load
    */
   public PSRequestValidationException(int msgCode)
   {
      super(msgCode);
   }

   /**
    * The specified validation rules were not met.
    *
    * @param validationRules      the rules which were not met
    */
   public PSRequestValidationException(
      com.percussion.util.PSCollection validationRules)
   {
      super(
         IPSServerErrors.VALIDATION_RULES_NOT_MET,
         convertRulesToXmlString(validationRules));
   }


   private static String convertRulesToXmlString(
      com.percussion.util.PSCollection validationRules)
   {
      org.w3c.dom.Document doc
         = PSXmlDocumentBuilder.createXmlDocument();
      org.w3c.dom.Element root
         = PSXmlDocumentBuilder.createRoot(doc, "ValidationRules");
      com.percussion.design.objectstore.PSCollectionComponent.appendCollectionToXml(
         doc, root, validationRules);

      java.io.StringWriter buf = new java.io.StringWriter();
      try {
         PSXmlDocumentBuilder.write(doc, buf);
         return buf.toString();
      } catch (java.io.IOException e) {
         // shouldn't really happen (out of memory?!)
         return "";
      } finally {
         if (buf!=null) try {buf.close();} catch (Exception e) { /*Ignore*/ };
      }
   }
}

