/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.HTTPClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;


/**
 * This interface represents read-only interface of an intermediate http
 * response. It is the compile-time type passed to various handlers which
 * might the response info but musn't modify the response.
 *
 * @version	0.3-3  06/05/2001
 * @author	Ronald Tschalär
 */
@Deprecated
public interface RoResponse
{
    /**
     * give the status code for this request. These are grouped as follows:
     * <UL>
     *   <LI> 1xx - Informational (new in HTTP/1.1)
     *   <LI> 2xx - Success
     *   <LI> 3xx - Redirection
     *   <LI> 4xx - Client Error
     *   <LI> 5xx - Server Error
     * </UL>
     *
     * @return the status code
     * @exception IOException If any exception occurs on the socket.
     */
    public int getStatusCode()  throws IOException;

    /**
     * @return the reason line associated with the status code.
     * @exception IOException If any exception occurs on the socket.
     */
    public String getReasonLine()  throws IOException;

    /**
     * @return the HTTP version returned by the server.
     * @exception IOException If any exception occurs on the socket.
     */
    public String getVersion()  throws IOException;

    /**
     * retrieves the field for a given header.
     *
     * @param  hdr the header name.
     * @return the value for the header, or null if non-existent.
     * @exception IOException If any exception occurs on the socket.
     */
    public String getHeader(String hdr)  throws IOException;

    /**
     * retrieves the field for a given header. The value is parsed as an
     * int.
     *
     * @param  hdr the header name.
     * @return the value for the header if the header exists
     * @exception NumberFormatException if the header's value is not a number
     *                                  or if the header does not exist.
     * @exception IOException if any exception occurs on the socket.
     */
    public int getHeaderAsInt(String hdr)
		throws IOException, NumberFormatException;

    /**
     * retrieves the field for a given header. The value is parsed as a
     * date; if this fails it is parsed as a long representing the number
     * of seconds since 12:00 AM, Jan 1st, 1970. If this also fails an
     * IllegalArgumentException is thrown.
     *
     * @param  hdr the header name.
     * @return the value for the header, or null if non-existent.
     * @exception IOException If any exception occurs on the socket.
     * @exception IllegalArgumentException If the header cannot be parsed
     *            as a date or time.
     */
    public Date getHeaderAsDate(String hdr)
	    throws IOException, IllegalArgumentException;

    /**
     * Retrieves the field for a given trailer. Note that this should not
     * be invoked until all the response data has been read. If invoked
     * before, it will force the data to be read via <code>getData()</code>.
     *
     * @param  trailer the trailer name.
     * @return the value for the trailer, or null if non-existent.
     * @exception IOException If any exception occurs on the socket.
     */
    public String getTrailer(String trailer)  throws IOException;

    /**
     * Retrieves the field for a given tailer. The value is parsed as an
     * int.
     *
     * @param  trailer the tailer name.
     * @return the value for the trailer if the trailer exists
     * @exception NumberFormatException if the trailer's value is not a number
     *                                  or if the trailer does not exist.
     * @exception IOException if any exception occurs on the socket.
     */
    public int getTrailerAsInt(String trailer)
		throws IOException, NumberFormatException;


    /**
     * Retrieves the field for a given trailer. The value is parsed as a
     * date; if this fails it is parsed as a long representing the number
     * of seconds since 12:00 AM, Jan 1st, 1970. If this also fails an
     * IllegalArgumentException is thrown.
     * <br>Note: When sending dates use Util.httpDate().
     *
     * @param  trailer the trailer name.
     * @return the value for the trailer, or null if non-existent.
     * @exception IllegalArgumentException if the trailer's value is neither a
     *            legal date nor a number.
     * @exception IOException if any exception occurs on the socket.
     * @exception IllegalArgumentException If the header cannot be parsed
     *            as a date or time.
     */
    public Date getTrailerAsDate(String trailer)
		throws IOException, IllegalArgumentException;

    /**
     * Reads all the response data into a byte array. Note that this method
     * won't return until <em>all</em> the data has been received (so for
     * instance don't invoke this method if the server is doing a server
     * push). If getInputStream() had been previously called then this method
     * only returns any unread data remaining on the stream and then closes
     * it.
     *
     * @see #getInputStream()
     * @return an array containing the data (body) returned. If no data
     *         was returned then it's set to a zero-length array.
     * @exception IOException If any io exception occurred while reading
     *			      the data
     */
    public byte[] getData()  throws IOException;

    /**
     * Gets an input stream from which the returned data can be read. Note
     * that if getData() had been previously called it will actually return
     * a ByteArrayInputStream created from that data.
     *
     * @see #getData()
     * @return the InputStream.
     * @exception IOException If any exception occurs on the socket.
     */
    public InputStream getInputStream()  throws IOException;
}
