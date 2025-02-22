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

package com.percussion.cms.objectstore.ws;

import com.percussion.cms.PSCmsException;
import com.percussion.util.IPSRemoteRequester;
import com.percussion.util.PSHttpConnection;
import com.percussion.util.PSRemoteAppletRequester;
import org.w3c.dom.Element;

import java.net.URL;


/**
 * This class is used to handle the communications between the Remote Server
 * and a applet client for all folder specific operations.
 */
public class PSRemoteFolderAgent
{
   /**
    * Constructs an object with a base URL
    * @param psContentExplorerApplet 
    *
    * @param url the base URL to be used to communicate to the remote server.
    *    When this is used in an applet, this should be the document base of
    *    the applet, <code>Applet.getRhythmyxCodeBase()</code>. It may not be
    *    <code>null</code>.
    */
   public PSRemoteFolderAgent(PSHttpConnection psHttpConnection, URL url)
   {
      // ctor of PSRemoteAppletRequester(URL) will validate if url == null
      this(new PSRemoteAppletRequester(psHttpConnection, url));
   }

   /**
    * Constructs an instance from a remote requester.
    * 
    * @param rmRequester The remote requester used to communicate with 
    *    Rhythmyx Server. It may not be <code>null</code>.
    */
   public PSRemoteFolderAgent(IPSRemoteRequester rmRequester)
   {
      if (rmRequester == null)
         throw new IllegalArgumentException("rmRequester may not be null");
      
      m_requester = new PSRemoteWsRequester(rmRequester);
   }
   
   /**
    * Send the specified message to the remote server.
    *
    * @param action The action of the message is intended for. It may not
    *    be <code>null</code> or empty.
    *
    * @param message The to be send message. It may not be <code>null</code>.
    *
    * @param responseNodeName The expected node name of the responsed message.
    *    It may not be <code>null</code> or empty.
    *
    * @return The response from the server, never <code>null</code>.
    *
    * @throws PSCmsException if an error occurs.
    */
   public Element sendMessage(String action, Element message,
      String responseNodeName) throws PSCmsException
   {
      return m_requester.sendRequest(action, "Folder", message, 
         responseNodeName);
   }
   
   // The remote webservice requester, used to communicate with webservices
   // handler on the remote server. It is initialized by the constructor,
   // never <code>null</code> or modified after that.
   private PSRemoteWsRequester m_requester;
}
