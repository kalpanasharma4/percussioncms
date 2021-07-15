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
package com.percussion.share.service;

import java.util.List;

import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.service.IPSDataService.DataServiceLoadException;

/**
 * An extremely low level wrapper to 
 * CM System for relatively fast item retrieval.
 * <p>
 * Be aware: Although this is public API it should probably not be used externally
 * and maybe removed in the future.
 * 
 * @author adamgent
 *
 * @param <S> item summary type.
 */
public interface IPSItemSummaryService<S extends IPSItemSummary> extends IPSCatalogService<S, String>
{
    /**
     * Returns the id for the given path.
     * The path could be to an asset, page, or folder.
     * <strong>
     * NOTICE that the return value maybe <code>null</code>.
     * </strong>
     * Higher level layers and API should deal with the null return value.
     * @param path never <code>null</code> or empty.
     * @return maybe <code>null</code> if there is not item at the given path.
     */
    public String pathToId(String path) throws IPSDataService.DataServiceNotFoundException;
    /**
     * Returns the id for the given path.
     * The path could be to an asset, page, or folder.
     * <strong>
     * NOTICE that the return value maybe <code>null</code>.
     * </strong>
     * Higher level layers and API should deal with the null return value.
     * @param path never <code>null</code> or empty.
     * @return maybe <code>null</code> if there is not item at the given path.
     */
    public String pathToId(String path, String relationshipTypeName) throws IPSDataService.DataServiceNotFoundException;
    
    /**
     * Returns the items that are children to the given id.
     * The id should probably be an item that is a folder.
     * @param id never <code>null</code> or empty.
     * @return never <code>null</code>, maybe empty.
     * @throws DataServiceLoadException if the item is not valid to have children or does not exist.
     */
    public List<S> findFolderChildren(String id) throws DataServiceLoadException;
}
