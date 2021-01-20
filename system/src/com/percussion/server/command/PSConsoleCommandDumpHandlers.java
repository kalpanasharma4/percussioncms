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
package com.percussion.server.command;

import com.percussion.server.PSRequest;
import com.percussion.server.PSServer;
import com.percussion.xml.PSXmlDocumentBuilder;

import java.util.Locale;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class dumps the currently used server resources to the server console.
 */
public class PSConsoleCommandDumpHandlers extends PSConsoleCommand
{
   /**
    * The constructor for this class.
    *
    * @param cmdArgs the argument string to use when executing this command, 
    *    may be <code>null</code>.
    */
   public PSConsoleCommandDumpHandlers(String cmdArgs)
   {
      super(cmdArgs);
   }

   /**
    * Execute the command specified by this object. The results are returned
    * as an XML document of the appropriate structure for the command.
    *   <P>
    * The execution of this command results in the following XML document
    * structure:
    *   
    * @see IPSConsolCommand
    */
   public Document execute(PSRequest request) throws PSConsoleCommandException
   {
      Document doc = PSXmlDocumentBuilder.createXmlDocument();
      Element root = PSXmlDocumentBuilder.createRoot(doc, 
         "PSXConsoleCommandResults");
      PSXmlDocumentBuilder.addElement(doc, root, "command", ms_cmdName + 
         " " + m_cmdArgs);

      Locale loc;
      if (request != null)
         loc = request.getPreferredLocale();
      else
         loc = Locale.getDefault();
      
      return doc;
   }
   
   /**
    * The command entered in the server console to get the information produced
    * in this class.
    */
   final static String ms_cmdName = "dump handlers";
}

