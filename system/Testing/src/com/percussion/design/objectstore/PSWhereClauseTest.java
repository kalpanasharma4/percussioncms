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
package com.percussion.design.objectstore;

import com.percussion.xml.PSXmlDocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit tests for the PSWhereClause class.
 */
public class PSWhereClauseTest extends TestCase
{
   public PSWhereClauseTest(String name)
   {
      super(name);
   }

   public void testXml() throws Exception
   {
      PSTextLiteral foo = new PSTextLiteral("foo");
      PSTextLiteral bar = new PSTextLiteral("bar");

      PSWhereClause clause = new PSWhereClause(foo, "=", bar, true);
      PSWhereClause otherClause = new PSWhereClause();

      Document doc = PSXmlDocumentBuilder.createXmlDocument();
      Element el = clause.toXml(doc);
      otherClause.fromXml(el, null, null);
      assertEquals(clause, otherClause);
   }

   public static Test suite()
   {
      TestSuite suite = new TestSuite();
      suite.addTest(new PSWhereClauseTest("testXml"));
      return suite;
   }
}
