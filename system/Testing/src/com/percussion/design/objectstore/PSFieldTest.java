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

import com.percussion.extension.PSExtensionRef;
import com.percussion.util.PSCollection;
import com.percussion.xml.PSXmlDocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

// Test case
public class PSFieldTest extends TestCase
{
   public PSFieldTest(String name)
   {
      super(name);
   }

   public void testEquals() throws Exception
   {
   }

   public void testXml() throws Exception
   {
      Document doc = PSXmlDocumentBuilder.createXmlDocument();
      Element root = PSXmlDocumentBuilder.createRoot(doc, "Test");

      // field validation rules
      PSRule rule = new PSRule(new PSExtensionCallSet());
      PSCollection ruleCol = new PSCollection(rule.getClass());
      ruleCol.add(rule);
      ruleCol.add(rule);
      ruleCol.add(rule);
      PSCollection ruleRefs = new PSCollection("java.lang.String");
      ruleRefs.add("ref1");
      ruleRefs.add("ref2");
      ruleRefs.add("ref3");

      PSFieldValidationRules fieldValidation = new PSFieldValidationRules();
      fieldValidation.setApplyWhen(new PSApplyWhen());
      fieldValidation.setName("fieldValidation");
      fieldValidation.setRuleReferences(ruleRefs);
      fieldValidation.setRules(ruleCol);
      fieldValidation.setErrorMessage(new PSDisplayText("one"));

      // field input translation
      PSExtensionRef exitRef1 = new PSExtensionRef("handler", "context", "inputTranslation");
      PSExtensionCall exitCall1 = new PSExtensionCall(exitRef1, null);
      PSExtensionCallSet callSet1 = new PSExtensionCallSet();
      callSet1.add(exitCall1);
      callSet1.add(exitCall1);
      PSFieldTranslation inputTranslation = new PSFieldTranslation(callSet1);

      // field input translation
      PSExtensionRef exitRef2 = new PSExtensionRef("handler", "context", "outputTranslation");
      PSExtensionCall exitCall2 = new PSExtensionCall(exitRef2, null);
      PSExtensionCallSet callSet2 = new PSExtensionCallSet();
      callSet2.add(exitCall2);
      PSFieldTranslation outputTranslation = new PSFieldTranslation(callSet2);

      // create test object
      PSBackEndCredential cred = new PSBackEndCredential("credential_1");
      cred.setDataSource("rxdefault");
      PSTableLocator tl = new PSTableLocator(cred);
      PSTableRef tr = new PSTableRef("tableName_1", "tableAlias_1");
      PSTableSet ts = new PSTableSet(tl, tr);
      PSCollection tsCol = new PSCollection(ts.getClass());
      tsCol.add(ts);
      PSContainerLocator loc = new PSContainerLocator(tsCol);
      PSBackEndTable table = new PSBackEndTable("RXARTICLE");
      PSField testTo = new PSField("field_1", new PSBackEndColumn(table, "DISPLAYTITLE"));
      testTo.setDefault(new PSBackEndColumn(table, "DEFAULT"));
      testTo.setOccurrenceDimension(testTo.OCCURRENCE_DIMENSION_COUNT, null);
      testTo.setOccurrenceCount(12, null);
      testTo.setOccurrenceDimension(testTo.OCCURRENCE_DIMENSION_COUNT, new Integer(1));
      testTo.setOccurrenceCount(12, new Integer(1));
      testTo.setValidationRules(fieldValidation);
      testTo.setInputTranslation(inputTranslation);
      testTo.setOutputTranslation(outputTranslation);
      Element elem = testTo.toXml(doc);
      PSXmlDocumentBuilder.copyTree(doc, root, elem, true);

      // create a new object and populate it from our testTo element
      PSField testFrom = new PSField(elem, null, null);
      Document doc2 = PSXmlDocumentBuilder.createXmlDocument();
      Element root2 = PSXmlDocumentBuilder.createRoot(doc2, "Test");
      Element elem2 = testFrom.toXml(doc);
      PSXmlDocumentBuilder.copyTree(doc2, root2, elem2, true);
      assertTrue(testTo.equals(testFrom));
      
      testEqualMetaData(testTo, testFrom);
      
   }

   /**
    * Tests {@link PSField#equalMetaData(Object)}
    * 
    * @param testTo a field object, assumed not <code>null</code>.
    * @param testFrom a copy of the above field, assumed not <code>null</code>.
    */
   private void testEqualMetaData(PSField testTo, PSField testFrom)
   {
      assertTrue(testTo.equalMetaData(testFrom));
      
      // modify data that is not part of meta data
      assertTrue(testTo.getDefault() != null);
      testFrom.setDefault(null);
      testFrom.setForceBinary(!testTo.isForceBinary());
      assertTrue(testTo.getInputTranslation() != null);
      testFrom.setInputTranslation(null);
      assertTrue(testTo.getOutputTranslation() != null);
      testFrom.setOutputTranslation(null);
      testFrom.setShowInPreview(!testTo.isShowInPreview());
      testFrom.setShowInSummary(!testTo.isShowInSummary());
      testFrom.setShouldCleanupField(!testTo.shouldCleanupField());
      testFrom.setValidationFailed(!testTo.hasValidationFailed());
      assertTrue(testTo.getValidationRules() != null);
      testFrom.setValidationRules(null);

      // meta data should still be equal 
      assertTrue(testTo.equalMetaData(testFrom));

      // modify meta data of the field
      
      // test cleanupNamespace() property
      testTo.setCleanupNamespaces(true);
      testFrom.setCleanupNamespaces(false);
      assertFalse(testTo.equalMetaData(testFrom));
      
      testFrom.setCleanupNamespaces(true);
      assertTrue(testTo.equalMetaData(testFrom));

      // test data type
      testTo.setDataType(PSField.DT_TEXT);
      testFrom.setDataType(PSField.DT_INTEGER);
      assertFalse(testTo.equalMetaData(testFrom));

      testFrom.setDataType(PSField.DT_TEXT);
      assertTrue(testTo.equalMetaData(testFrom));
      
      // test type
      testTo.setType(PSField.TYPE_LOCAL);
      testFrom.setType(PSField.TYPE_SHARED);
      assertFalse(testTo.equalMetaData(testFrom));

      testFrom.setType(PSField.TYPE_LOCAL);
      assertTrue(testTo.equalMetaData(testFrom));

      // test field value type
      testTo.setFieldValueType(PSField.FIELD_VALUE_TYPE_CONTENT);
      testFrom.setFieldValueType(PSField.FIELD_VALUE_TYPE_META);
      assertFalse(testTo.equalMetaData(testFrom));

      testFrom.setFieldValueType(PSField.FIELD_VALUE_TYPE_CONTENT);
      assertTrue(testTo.equalMetaData(testFrom));
      
      // test data format
      testTo.setDataFormat("YYYY");
      testFrom.setDataFormat("YYYY/MM");
      assertFalse(testTo.equalMetaData(testFrom));

      testFrom.setDataFormat("YYYY");
      assertTrue(testTo.equalMetaData(testFrom));
      
      // test mime type
      testTo.setMimeType("text/xml");
      testFrom.setMimeType("text");
      assertFalse(testTo.equalMetaData(testFrom));

      testFrom.setMimeType("text/xml");
      assertTrue(testTo.equalMetaData(testFrom));
      
      // test submit name
      testTo.setSubmitName("field1");
      testFrom.setSubmitName("field1_2");
      assertFalse(testTo.equalMetaData(testFrom));

      testFrom.setSubmitName("field1");
      assertTrue(testTo.equalMetaData(testFrom));
      
      // test column locator
      PSBackEndColumn col = (PSBackEndColumn)testTo.getLocator();
      PSBackEndColumn col2 = (PSBackEndColumn)col.clone();
      testFrom.setLocator(col2);
      assertTrue(testTo.equalMetaData(testFrom));
      
      col2.setColumn(col.getColumn() + "_1");
      assertFalse(testTo.equalMetaData(testFrom));
      
      col2.setColumn(col.getColumn());
      assertTrue(testTo.equalMetaData(testFrom));
   }
   
   public static Test suite()
   {
      TestSuite suite = new TestSuite();

      suite.addTest(new PSFieldTest("testXml"));

      return suite;
   }
}
