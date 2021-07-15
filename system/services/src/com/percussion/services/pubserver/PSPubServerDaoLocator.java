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
package com.percussion.services.pubserver;

import com.percussion.services.PSBaseServiceLocator;
import com.percussion.services.PSMissingBeanConfigurationException;
import com.percussion.services.pubserver.IPSPubServerDao;

/**
 * Get the publication server manager from the Spring configuration.
 * 
 * @author leonardohildt
 */
public class PSPubServerDaoLocator extends PSBaseServiceLocator
{
   private static volatile IPSPubServerDao psrd=null;
   /**
    * Get the publication server manager
    * 
    * @return the publication server manager, never <code>null</code>
    * @throws PSMissingBeanConfigurationException if the bean doesn't exist
    */
   public static IPSPubServerDao getPubServerManager()
         throws PSMissingBeanConfigurationException
   {
       if (psrd==null)
       {
           synchronized (PSPubServerDaoLocator.class)
           {
               if (psrd==null)
               {
                   psrd = (IPSPubServerDao) getBean("sys_pubserverdao");
               }
           }
       }
      return psrd;
   }
}
