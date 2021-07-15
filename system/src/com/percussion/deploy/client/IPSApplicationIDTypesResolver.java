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

package com.percussion.deploy.client;

import java.util.Iterator;

/**
 * A pluggable class for PSFolderContentsDescriptorBuilder to delegate the
 * typing of undefined application ID types. In the standard MSM client,
 * undefined application ID types are typed through user-interaction with a
 * dialog.
 * 
 * @author James Schultz
 */
public interface IPSApplicationIDTypesResolver
{
   /**
    * Defines each undefined ID type mapping in the supplied iterator, by
    * calling the mapping's <code>setType</code> method.
    * 
    * @param undefinedMappings an iterator of
    *           <code>PSApplicationIDTypeMapping</code>, each representing an
    *           undefined ID type
    */
   public void defineIdTypes(Iterator undefinedMappings);

}
