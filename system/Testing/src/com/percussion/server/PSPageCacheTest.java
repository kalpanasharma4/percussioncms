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

package com.percussion.server;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.percussion.security.xml.PSSecureXMLUtils;
import junit.framework.TestCase;
import org.w3c.dom.Document;
import static org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace;

public class PSPageCacheTest extends TestCase
{

   public void testPSPageCache()
   {
      PSPageCache cache = new PSPageCache();

      DocumentBuilderFactory factory = PSSecureXMLUtils.getSecuredDocumentBuilderFactory(
              false
      );

      DocumentBuilder builder;
      try
      {
         // cache timeout in miliseconds but we use Date to compare which uses seconds.
         cache.setCacheTimeout(2000);
         builder = factory.newDocumentBuilder();
         cache.addPage(builder.newDocument());
         assertEquals(1L, cache.getCacheSize());
         Thread.sleep(3000);
         cache.cleanCache();
         assertEquals(0L, cache.getCacheSize());

         // Test ceiling
         int i = 1;
         Document prototypeDoc = builder.newDocument();
         while (i < 2000)
         {
            cache.addPage(prototypeDoc);
            i++;
         }
         assertEquals(1000L, cache.getCacheSize());
         
         // wait 3 seconds cache items should expire.
         Thread.sleep(3000);

         cache.cleanCache();

         assertEquals(0L, cache.getCacheSize());

      }
      catch (Exception e)
      {
         assertEquals("Exception caught" + getFullStackTrace(e), 0, 1);
      }

   }
}
