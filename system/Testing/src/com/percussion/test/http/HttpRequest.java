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
package com.percussion.test.http;

import com.percussion.test.io.IOTools;
import com.percussion.test.io.LogSink;
import com.percussion.util.PSInputStreamReader;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

/**
 * Encapsulates an HTTP GET or POST request and the results of the request.
 * We use this class because we want more flexibility over the behavior
 * of the HTTP transaction than classes like java.net.HttpURLConnection
 * gives us. For example, we may want to send garbage or unencoded URLs.
 *
 * @deprecated use com.percussion.test.http.PSHttpRequest instead.
 */
public class HttpRequest
{

   public HttpRequest(URL url)
   {
      this(url.toString(), "GET", null);
      setRequestPort(url.getPort());
   }

   /**
    * Construct a request to the given URL with the given method. If request
    * content is non-null, then the request content will be after the request
    * and the request headers.
    * <p>
    * If you happen to know the length (in bytes) of the content, you may want
    * to set the "Content-length" request header to that value. We will not do
    * it automatically, and we will <B>not</B> consider the setting of this
    * header when reading the content stream.
    * <p>
    * Also, if you happen to know the character encoding that the content uses,
    * you may want to set the "Char-encoding" request header. Again, we will
    * not consider this value when sending the content.
    *
    * @param   URL
    * @param   reqMethod
    * @param   content May be null if no POST data is needed.
    * 
    */
   public HttpRequest(
      String URL,
      String reqMethod,
      InputStream reqContent)
   {
      init(URL, reqMethod, reqContent);
   }

   private void init(String URL, String reqMethod, InputStream reqContent)
   {
      m_reqURL = URL;
      m_reqMethod = reqMethod;
      m_reqContent = reqContent;
   }

   /**
    * Sets the outgoing request content for this request. If an existing
    * content had been specified (and it is not the same content stream
    * as the argument to this method), the existing content will be
    * closed first.
    *
    * @param   content
    * 
    */
   public void setRequestContent(InputStream content)
   {
      if (m_reqContent != null && content != m_reqContent)
      {
         try
         {
            m_reqContent.close();
         }
         catch (IOException e)
         {
            /* ignore */
         }
      }
      m_reqContent = content;
   }

   /**
    * Sets the request's hostname. Usually this value will be extracted from
    * the URL, but if the host is not available in the URL, then you will to
    * set the host using this method.
    * <p>
    * If this value is set, then it will override any setting in the URL.
    *
    * @param   hostName
    * 
    */
   public void setRequestHost(String hostName)
   {
      m_reqHost = hostName;
   }

   /**
    * Sets the request's port. Usually this value will be extracted from
    * the URL, but if the port is not available in the URL, then you will need
    * to set the port using this method.
    * <p>
    * If this value is set, then it will override any setting in the URL.
    *
    * @param   port
    * 
    */
   public void setRequestPort(int port)
   {
      m_reqPort = port;
   }

   /**
    * Gets the request method, usually "GET" or "POST".
    *
    * @return  String
    */
   public String getRequestMethod()
   {
      return m_reqMethod;
   }

   /**
    * Enables tracing status to the given PrintWriter.
    *
    * @param   logger
    * 
    */
   public void enableTrace(LogSink logger)
   {
      m_logger = logger;
   }

   /**
    * Sets the HTTP version to declare when making a request. The
    * default is 1.0
    *
    * @param   major
    * @param   minor
    * 
    */
   public void setRequestHttpVersion(int major, int minor)
   {
      m_reqHttpVersion = "" + major + "." + minor;
   }

   /**
    * Sets the request URL.
    *
    * @param   URL
    * 
    */
   public void setRequestURL(String URL)
   {
      m_reqURL = URL;
   }

   public void addRequestHeaders(HttpHeaders headers)
   {
      m_reqHeaders.addAll(headers);
   }

   /**
    * Adds a header that will be sent along with the request.
    *
    * @param   headerName
    * @param   headerValue
    * 
    */
   public void addRequestHeader(String headerName, String headerValue)
   {
      m_reqHeaders.addHeader(headerName, headerValue);
   }

   /**
    * Adds a response header that was present in the response.
    *
    * @param   headerName
    * @param   headerValue
    * 
    */
   protected void addResponseHeader(String headerName, String headerValue)
   {
      m_respHeaders.addHeader(headerName, headerValue);
   }

   /**
    * Returns the response headers.
    *
    * @return  Iterator
    */
   public HttpHeaders getResponseHeaders()
   {
      return m_respHeaders;
   }

   /**
    * Sends the request and parses the response. If request content was
    * supplied in the constructor, it will be sent.
    * <P>
    * The request content stream (if specified in the constructor) is
    * guaranteed to be closed after this method is called, even if
    * exceptions are thrown from this method.
    *
    * @throws  IOException
    * @throws HttpConnectException if all atempts failed to establish an HTTP
    *    connection.
    */
   public void sendRequest() throws IOException, HttpConnectException
   {
      HttpRequestTimings timings = new HttpRequestTimings();
      sendRequest(timings);
      m_timings = timings;
   }

   /**
    * Sends the request and parses the response. If request content was
    * supplied in the constructor, it will be sent.
    * <P>
    * The request content stream (if specified in the constructor) is
    * guaranteed to be closed after this method is called, even if
    * exceptions are thrown from this method.
    *
    * @param timer Can be <CODE>null</CODE>.
    *
    * @throws IOException
    * @throws HttpConnectException if all atempts failed to establish an HTTP
    *    connection.
    */
   private void sendRequest(HttpRequestTimings timings) throws IOException, 
      HttpConnectException
   {
      try
      {
         // reset the counter tracking total bytes sent
         m_bytesSent = 0;

         // TODO: don't do this if we support keep-alive connections
         if (m_sock != null)
         {
            disconnect();
         }
         // reset response headers in case this request is being re-used
         m_respHeaders = new HttpHeaders();

         String host = m_reqHost;
         if (host == null)
         {
            URL u = new URL(m_reqURL);
            host = u.getHost();
         }

         int port = m_reqPort;
         if (port <= 0)
         {
            URL u = new URL(m_reqURL);
            port = u.getPort();
         }

         if (port <= 0)
         {
            port = 80;
         }

         timings.beforeConnect(System.currentTimeMillis());

         // connect the socket
         connect(host, port);

         timings.afterConnect(System.currentTimeMillis());

         OutputStream out = m_sock.getOutputStream();

         m_reqWriter = new BufferedWriter(
            new OutputStreamWriter(out));

         // send the request line
         sendRequestLine(m_reqWriter);

         // send the request headers
         sendRequestHeaders(m_reqWriter);

         m_reqWriter.flush();

         // if applicable, send the additional content
         if (m_reqContent != null)
         {
            sendReqContent(m_reqContent, out);
            m_reqContent.close();
            m_reqContent = null;
         }

         timings.afterRequest(System.currentTimeMillis());

         InputStream is = m_sock.getInputStream();
         int b = is.read();
         timings.setTimeAfterFirstByte( System.currentTimeMillis());
         ByteArrayOutputStream bos = new ByteArrayOutputStream( 5000 );
         bos.write( b );
         long totalBytes = IOTools.copyStream( is, bos );
         ByteArrayInputStream bis =
               new ByteArrayInputStream(bos.toByteArray());
         timings.afterContent( System.currentTimeMillis());

         // prepare to read the response
         m_respIn = new PSInputStreamReader(bis);

         // read the response header
         long hdrBytes = parseResponse(m_respIn);

         timings.afterHeaders(System.currentTimeMillis());
         timings.headerBytes(hdrBytes);
         timings.contentBytes( totalBytes - hdrBytes );
      }
      finally
      {
         if (m_reqContent != null)
         {
            m_reqContent.close();
            m_reqContent = null;
         }
      }  
   }

   protected void connect(String host, int port) throws IOException, 
      HttpConnectException
   {
      log("Connecting to " + host + ":" + port + "...");
      
      // todo: remove this once we support keeping connections alive
      if (m_sock != null)
         throw new HttpConnectException(
            "Socket must be closed befor creating a new connection!");
      
      int MAX_CONNECT_ATTEMPTS = 3;
      int tryCount = 0;
      while (tryCount++ < MAX_CONNECT_ATTEMPTS)
      {
         try
         {
            m_sock = new Socket(host, port);
            m_sock.setSoTimeout(0);
            
            // if we made it here, we are done
            return;
         }
         catch (ConnectException e)
         {
            String reason = e.getLocalizedMessage();
            log("Try #" + tryCount + " failed: " + reason);
            try
            {
               if (tryCount < MAX_CONNECT_ATTEMPTS)
                  Thread.sleep(100);
               else
                  throw new HttpConnectException("Connection failed: " + reason);
            }
            catch (InterruptedException ie)
            {
               Thread.currentThread().interrupt();
            }
         }
         catch (Throwable t)
         {
            throw new HttpConnectException("Connection failed: " + 
               t.getLocalizedMessage());
         }
      }
   }

   protected void sendReqContent(InputStream in, OutputStream out)
      throws IOException
   {
      long bytesSent = IOTools.copyStream(in, out);
      m_bytesSent += bytesSent;
      log("Sent " + bytesSent + " bytes of content");
   }

   /**
    * Gets the approximate number of milliseconds we had to wait
    * for the first byte of the response to become available from the
    * server.
    *
    * @return  long
    */
   public long getResponseLatency()
   {
      return m_respLatencyMs;
   }

   public HttpRequestTimings getTimings() throws CloneNotSupportedException
   {
      return (HttpRequestTimings)m_timings.clone();
   }

   /**
    * Gets the response content stream, which may be null
    * or empty if getResponseCode() returns anything other
    * than 2xx. If we are currently waiting for data to
    * become available over the connection, this method
    * will block until either we have timed out or until
    * data becomes available.
    *
    * @return  InputStream
    */
   public InputStream getResponseContent()
   {
      return m_respIn;
   }

   /**
    * Gets the HTTP response code.
    *
    * @return  int
    */
   public int getResponseCode()
   {
      return m_respHttpCode;
   }

   /**
    * Closes the request. Any pending results are discarded,
    * and the response content is no longer valid.
    *
    * @throws  Exception;
    * 
    */
   public void disconnect() throws IOException
   {
      if (m_respIn != null || m_sock != null)
      {
         log("Disconnecting...");
      }

      if (m_respIn != null)
         m_respIn.close();

      if (m_sock != null)
         m_sock.close();

      m_respIn = null;
      m_sock = null;
   }

   protected void finalize() throws Throwable
   {
      disconnect();
      super.finalize();
   }

   /**
    * Gets the response message, that is any text following the status code on
    * the response header line.
    *
    * @return  String
    */
   public String getResponseMessage()
   {
      return m_respMsg;
   }

   /** send the HTTP request line to the given writer */
   protected void sendRequestLine(Writer writer) throws IOException
   {
      // this logic allows us to send invalid URLs
      String sendURL = m_reqURL;
      try
      {
         URL u = new URL(m_reqURL);
         sendURL = u.getFile();
      }
      catch (MalformedURLException e)
      {
         // ignrore, just send the invalid URL as is
      }

      String reqLine = m_reqMethod + " " + sendURL + " HTTP/"
            + m_reqHttpVersion;
      writer.write(reqLine + "\r\n");
      m_bytesSent += reqLine.length() + 2;   // +2 for CRLF

      log("Sent request line " + reqLine);
   }

   /* sends the request headers to the given writer, followed by a blank line */
   protected void sendRequestHeaders(Writer writer) throws IOException
   {
      log("Sending request headers...");
      Collection keySet = m_reqHeaders.getHeaderNames();
      for (Iterator i = keySet.iterator(); i.hasNext(); )
      {
         String headerName = i.next().toString();
         for (Iterator j = m_reqHeaders.getHeaders(headerName); j.hasNext(); )
         {
            String val = headerName + ": " + j.next().toString();
            writer.write(val + "\r\n");
            m_bytesSent += val.length() + 2;    // +2 for CRLF
            log("\tSent header " + val);
         }
      }

      writer.write("\r\n"); // blank line to terminate the headers
      m_bytesSent += 2;
      
      log("Finished sending headers");
   }

   protected long parseResponse(PSInputStreamReader reader)
      throws IOException
   {
      long bytes = parseResponseStatus(reader);
      bytes += parseResponseHeaders(reader);
      return bytes;
   }

   /**
    * Read the HTTP status code, which looks like "HTTP/1.1 nnn:blah"
    * where nnn is the code
    */
   protected long parseResponseStatus(PSInputStreamReader reader)
      throws IOException
   {
      String statusLine = reader.readLine();
      
      log("Server status: " + statusLine);
      
      {
         if (statusLine == null || statusLine.length() < 8)
         {
            throw new IOException("Malformed HTTP status line \""
                  + statusLine + "\"");
         }

         int spacePos = statusLine.indexOf(' ');
         if (spacePos < 5)
         {
            throw new IOException("Malformed HTTP status line \""
                  + statusLine + "\"");
         }

         int startCode = spacePos + 1; // points at first char of code
         int endCode = startCode + 3; // points one past the code
         m_respHttpCode =
               Integer.parseInt(statusLine.substring(startCode, endCode));
         m_respMsg = statusLine.substring(endCode).trim();
      }

      // status line is ASCII bytes (don't forget 2 bytes for CR+LF)
      return statusLine.length() + 2;
   }

   /**
    * Parses the headers, and positions the reader
    * on first byte of actual data.
    *
    */
   protected long parseResponseHeaders(PSInputStreamReader reader)
         throws IOException
   {
      long bytes = 0L;

      log("Parsing response headers...");

      // now read each header
      for (String line = reader.readLine(); line != null;
            line = reader.readLine())
      {
         // line is ASCII bytes (don't forget 2 bytes for CR+LF)
         bytes += line.length() + 2;

         if (line.length() == 0)
         {
            // this is the last (empty) line in the headers
            break;
         }

         int pos = line.indexOf(':');
         if (pos < 1 || pos == line.length())
         {
            throw new IOException("Malformed result header line \"" + line
                  + "\"");
         }

         log("\tParsed header " + line);

         String name = line.substring(0, pos).trim();
         String val = line.substring(pos + 1, line.length()).trim();

         addResponseHeader(name, val);
      }

      log("Finished parsing response headers");

      return bytes;
   }

   public void logException(Throwable t)
   {
      if (m_logger != null)
         m_logger.log(t);
   }

   public void log(String message)
   {
      if (m_logger != null)
         m_logger.log(message);
   }

   /**
    * Returns the approximate number of bytes sent when the request was made.
    * Until {@link #sendRequest} is called once, 0 is returned. The bytes sent
    * is recalculated each time a request is sent.
    *
    * @return A value >= 0.
    */
   public int getBytesSent()
   {
      return m_bytesSent;
   }

   /**
    * The number of bytes sent in a request. Calculated during the processing
    * of <code>sendRequest</code>. 0 until this method called at least once.
    * Use {@link #getBytesSent} to retrieve.
    */
   protected int m_bytesSent = 0;
   protected Socket m_sock;
   protected LogSink m_logger;

   /* response */
   protected long m_respLatencyMs;
   protected String m_respMsg;
   protected PSInputStreamReader m_respIn;
   protected int m_respHttpCode = -1;
   protected HttpHeaders m_respHeaders = new HttpHeaders();

   /* request */
   protected String m_reqHost;
   protected int m_reqPort = -1;
   protected String m_reqMethod;
   protected InputStream m_reqContent;
   protected Writer m_reqWriter;
   protected HttpHeaders m_reqHeaders = new HttpHeaders();
   protected String m_reqHttpVersion = "1.0";
   protected String m_reqURL;

   /* statistics */
   protected HttpRequestTimings m_timings;
}
