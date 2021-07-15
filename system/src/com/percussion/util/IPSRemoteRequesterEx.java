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
package com.percussion.util;

import java.io.IOException;
import java.util.Map;

import org.xml.sax.SAXException;

import com.percussion.design.objectstore.PSLocator;

import com.percussion.HTTPClient.PSBinaryFileData;


/**
 * This interface simplifies making a request to a Rhythmyx application or
 * resource that returns an XML document. It allows the user to set all the
 * information to make a request to the server, it will construct the XML
 * document out of the response from the server. Also includes methods
 * that handle binary data and can return bytes instead of an xml document.
 */
public interface IPSRemoteRequesterEx extends IPSRemoteRequester
{

   /**
    * Makes an http/s request to the specified binary resource, providing
    * the key-value pairs in the params map as html parameters. Expects that a
    * byte array will be returned.
    *
    * @param resource Never <code>null</code> or empty. Must be a full path
    *    to the target resource without the root path, e.g. app/res.xml. (assume
    *    the full path including the root is, /Rhythmyx/app/res.xml)
    *
    * @param params A set of name/value pairs. Each key is a String, while
    *    each value is either a String or a List of Strings. If a list
    *    is supplied, then an htlm param with the name of the key will
    *    be created for each entry.
    *
    * @return The byte array representing the returned data, may be empty if
    * no data was returned
    *
    * @throws IOException If any problems occur while communicating with the
    *    server.
    */
   public byte[] getBinary(String resource, Map params)
      throws IOException;

   /**
    * Makes an http/s request to the specified binary update resource,
    * providing the key-value pairs in the params map as html parameters.
    *
    * @param the BinaryFileData array data that represents the binary being sent.
    *
    * @param resource Never <code>null</code> or empty. Must be a full path
    *    to the target resource without the root path, e.g. app/res.xml. (assume
    *    the full path including the root is, /Rhythmyx/app/res.xml)
    *
    * @param params A set of name/value pairs. Each key is a String, while
    *    each value is either a String or a List of Strings. If a list
    *    is supplied, then an htlm param with the name of the key will
    *    be created for each entry.
    *
    * @return the <code>PSLocator</code> for this content item. May be <code>
    * null</code> if the locator could not be retrieved.
    *
    * @throws IOException If any problems occur while communicating with the
    *    server.
    */
   public PSLocator updateBinary(
      PSBinaryFileData[] files,
      String resource,
      Map params)
      throws IOException, SAXException;




}
