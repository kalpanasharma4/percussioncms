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
package com.percussion.cms.objectstore;

import com.percussion.design.objectstore.PSUnknownNodeTypeException;

import org.w3c.dom.Element;

public class PSActionProperty
   extends PSCmsProperty
{
   /**
    * no-args constructor
    */
   
   public PSActionProperty()
   {
   }
   
   /**
    * Convenience method that calls {@link #PSActionProperty(String,
    * String,String) PSActionProperty(name, value, null)}.
    */
   public PSActionProperty(String name, String value)
   {
      this(name, value, null);
   }


   /**
    *
    * @param name Never <code>null</code> or empty.
    *
    * @param value May be <code>null</code>. If so, "" is used.
    *
    * @param desc May be <code>null</code>. If so, "" is used.
    */
   public PSActionProperty(String name, String value, String desc)
   {
      super(getKeyDef(), name, value, desc, KEYASSIGN_NAME_AS_KEYPART);
   }


   /**
    * Create an object from a previously serialized one.
    *
    * @param src Never <code>null</code>.
    *
    * @throws PSUnknownNodeTypeException
    */
   public PSActionProperty(Element src)
      throws PSUnknownNodeTypeException
   {
      super(getKeyDef(), "dummy");
      fromXml(src);
   }


   /**
    * Creates a key containing the proper definition for this object.
    *
    * @return Never <code>null</code>.
    */
   private static PSKey getKeyDef()
   {
      return new PSKey(new String [] {"PROPNAME", "ACTIONID"}, false);
   }

   //see base class for description
   protected String[] getKeyPartValues(IPSKeyGenerator gen)
   {
      return new String[] {getName()};
   }
}