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

package com.percussion.HTTPClient;


/**
 * This interface represents the read-only interface of an http request.
 * It is the compile-time type passed to various handlers which might
 * need the request info but musn't modify the request.
 *
 * @version	0.3-3  06/05/2001
 * @author	Ronald Tschalär
 */
@Deprecated
public interface RoRequest
{
    /**
     * @return the HTTPConnection this request is associated with
     */
    public HTTPConnection getConnection();

    /**
     * @return the request method
     */
    public String getMethod();

    /**
     * @return the request-uri
     */
    public String getRequestURI();

    /**
     * @return the headers making up this request
     */
    public NVPair[] getHeaders();

    /**
     * @return the body of this request
     */
    public byte[] getData();

    /**
     * @return the output stream on which the body is written
     */
    public HttpOutputStream getStream();

    /**
     * @return true if the modules or handlers for this request may popup
     *         windows or otherwise interact with the user
     */
    public boolean allowUI();
}
