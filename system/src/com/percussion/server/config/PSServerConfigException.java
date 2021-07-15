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
package com.percussion.server.config;

import com.percussion.error.PSException;

/**
 * Generic exception class to be used for server configurations errors. More 
 * specific server configuration errors should be derived from this class.
 */
public class PSServerConfigException extends PSException
{
   /**
    * Pass-through constructor to super class.
    * 
    * @see PSException#PSException(int,Object)
    */ 
   public PSServerConfigException(int msgCode, Object singleArg)
   {
      super(msgCode, singleArg);
   }

   /**
    * Pass-through constructor to super class.
    * 
    * @see PSException#PSException(int,Object[])
    */ 
   public PSServerConfigException(int msgCode, Object[] arrayArgs)
   {
      super(msgCode, arrayArgs);
   }

   /**
    * Pass-through constructor to super class.
    * 
    * @see PSException#PSException(int)
    */ 
   public PSServerConfigException(int msgCode)
   {
      super(msgCode);
   }
}
