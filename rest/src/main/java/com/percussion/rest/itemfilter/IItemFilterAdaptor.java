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

package com.percussion.rest.itemfilter;

import com.percussion.rest.Guid;
import com.percussion.services.error.PSNotFoundException;

import java.util.List;

public interface IItemFilterAdaptor {

	/***
	 * Get a list of the ItemFilters available on the system populated with rules and parameters.
	 * @return A list of item filters
	 */
	public List<ItemFilter> getItemFilters();

	/***
	 * Update or create an ItemFilter
	 * @param filter  The filter to update or create.  
	 * @return The updated ItemFilter.
	 */
	public ItemFilter updateOrCreateItemFilter(ItemFilter filter);
	
	/***
	 * Delete the specified item filter.
	 * @param itemFilterId A valid ItemFilter id.  Filter must not be associated with any ContentLists or it won't be deleted.
	 */
	public void deleteItemFilter(Guid itemFilterId) throws PSNotFoundException;
	
	/***
	 * Get a single ItemFilter by id.
	 * @param itemFilterId  A Valid ItemFilter id
	 * @return The ItemFilter
	 */
	public ItemFilter getItemFilter(Guid itemFilterId) throws PSNotFoundException;
	
}
