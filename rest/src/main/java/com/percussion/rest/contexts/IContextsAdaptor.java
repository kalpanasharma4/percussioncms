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

package com.percussion.rest.contexts;

import java.net.URI;
import java.util.List;

/***
 * Defines the adaptor interface for publishing Contexts
 */
public interface IContextsAdaptor {

    /***
     * Delete a publishing Context by id
     * @param baseURI referring url
     * @param id A string guid id
     */
    public void deleteContext(URI baseURI, String id);

    /***
     * Get a publishing context by it's ID
     * @param baseUri referring uri
     * @param id A string guid id
     * @return The publishing Conext
     */
    public Context getContextById(URI baseUri, String id);

    /***
     * List all publishing contexts configured on the system
     * @param baseURI
     * @return a list of publishing contexts
     */
    public List<Context> listContexts(URI baseURI);

    /***
     * Create or update a publishing context
     * @param baseURI referring url
     * @param context a fully initialized Context
     * @return The updated context
     */
    public Context createOrUpdateContext(URI baseURI, Context context);
}
