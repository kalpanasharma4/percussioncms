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

package com.percussion.design.objectstore;

import com.percussion.xml.PSXmlDocumentBuilder;

import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Basic attribute testing, including simple to/from Xml
 * checking w/ simple attribute classes.  Also includes 
 * testing of accessor methods
 */
public class PSAttributeTest extends TestCase
{
   /**
    * @see TestCase#TestCase(String)
    */
   public PSAttributeTest(String name)
   {
      super(name);
   }

   // See super class for more info
   public void setUp()
   {
   }

   /**
    * Test if the two supplied attributes are equal.
    *
    * @param att1 attribute to compare
    *
    * @param att2 attribute to compare
    */
   public static boolean testAttributeEquals(
      PSAttribute att1, PSAttribute att2)
   {
      if ((att1 == null) || (att2 == null))
      {
         if ((att2 != null) || (att1 != null))
            return false;

         return true; // null == null
      }

      // Check if the names are equal
      if (!att1.getName().equals(att2.getName()))
      {
         return false;
      }

      if (att1.size() != att2.size())
      {
         return false;
      }

      for (int i = 0; i < att1.size(); i++)
      {
         PSAttributeValue val1 = (PSAttributeValue) att1.get(i);
         PSAttributeValue val2 = (PSAttributeValue) att2.get(i);

         if (!val1.getValueText().equals(val2.getValueText()))
         {
            return false;
         }
      }

      return true;      // equal!
   }

   /**
    * Assert that the two supplied atts are equal.
    *
    * @param att1 att to compare
    *
    * @param att2 att to compare
    *
    * @throws Exception If any exceptions occur or assertions fail.
    */
   public void assertAttributeEquals(
      PSAttribute att1, PSAttribute att2) throws Exception
   {
      assertTrue(testAttributeEquals(att1, att2));
   }

   /**
    * Test if two newly constructed atts (empty ctor) are equal
    *
    * @throws Exception If any exceptions occur or assertions fail.
    */
   public void testEmptyEquals() throws Exception
   {
      PSAttribute att = new PSAttribute();
      PSAttribute otherAtt = new PSAttribute();
      assertAttributeEquals(att, otherAtt);
   }

   /**
    * Test if two newly constructed atts (name ctor) are equal
    *
    * @throws Exception If any exceptions occur or assertions fail.
    */
   public void testNameTypeConstructor() throws Exception
   {
      PSAttribute att =
         new PSAttribute("foo");

      assertEquals(att.getName(), "foo");

      PSAttribute otherAtt =
         new PSAttribute("foo");

      assertAttributeEquals(att, otherAtt);

      boolean didThrow = false;
      try
      {
         att = new PSAttribute(null);
      }
      catch (IllegalArgumentException e)
      {
         didThrow = true;
      }
      assertTrue(didThrow);

      didThrow = false;
      try
      {
         att = new PSAttribute("");
      }
      catch (IllegalArgumentException e)
      {
         didThrow = true;
      }
      assertTrue(didThrow);
   }

   /**
    * Test to and from xml methods (round trip) of this os object.
    *
    * @throws Exception If any exceptions occur or assertions fail.
    */
   public void testXml() throws Exception
   {
      PSAttribute att = new PSAttribute("foobar");
      PSAttribute otherAtt = new PSAttribute();

      assertTrue(!testAttributeEquals(att,otherAtt));

      Document doc = PSXmlDocumentBuilder.createXmlDocument();
      Element el = att.toXml(doc);
      doc.appendChild(el);

      otherAtt.fromXml(el, null, null);
      assertAttributeEquals(att, otherAtt);

      // use different name and verify to/from loop
      att = new PSAttribute("taebo");
      assertTrue(!testAttributeEquals(att,otherAtt));

      doc = PSXmlDocumentBuilder.createXmlDocument();
      el = att.toXml(doc);
      doc.appendChild(el);

      otherAtt.fromXml(el, null, null);
      assertAttributeEquals(att, otherAtt);

      ArrayList val = new ArrayList();
      val.add("one");
      val.add("two");
      val.add("three");

      // add a value and verify to/from loop
      att.setValues(val);
      assertTrue(!testAttributeEquals(att,otherAtt));

      doc = PSXmlDocumentBuilder.createXmlDocument();
      el = att.toXml(doc);
      doc.appendChild(el);

      otherAtt.fromXml(el, null, null);
      assertAttributeEquals(att, otherAtt);

      // add a different value (one item list) and verify to/from loop
      ArrayList l = new ArrayList();
      l.add("one");
      att.setValues(l);

      assertTrue(!testAttributeEquals(att,otherAtt));

      doc = PSXmlDocumentBuilder.createXmlDocument();
      el = att.toXml(doc);
      doc.appendChild(el);
      otherAtt.fromXml(el, null, null);
      assertAttributeEquals(att, otherAtt);

      // add a different value (including a null value) and verify to/from loop
      l.add(null);
      l.add("three");
      att.setValues(l);
      assertTrue(!testAttributeEquals(att,otherAtt));

      doc = PSXmlDocumentBuilder.createXmlDocument();
      el = att.toXml(doc);
      doc.appendChild(el);

      otherAtt.fromXml(el, null, null);
      assertAttributeEquals(att, otherAtt);

      // Now try a null value and verify to/from loop
      att.setValues(null);
      assertTrue(!testAttributeEquals(att,otherAtt));

      doc = PSXmlDocumentBuilder.createXmlDocument();
      el = att.toXml(doc);
      doc.appendChild(el);

      otherAtt.fromXml(el, null, null);
      assertAttributeEquals(att, otherAtt);
   }

   /**
    * Collect all tests into a TestSuite and return it.
    *
    * @return the suite of all tests for this class. Not <code>null</code>.
    */
   public static Test suite()
   {
      TestSuite suite = new TestSuite();
      suite.addTest(new PSAttributeTest("testEmptyEquals"));
      suite.addTest(new PSAttributeTest("testNameTypeConstructor"));
      suite.addTest(new PSAttributeTest("testXml"));
      return suite;
   }
}

