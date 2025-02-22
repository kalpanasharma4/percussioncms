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
package com.percussion.xml;

import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Objects;

import static org.junit.Assert.assertNotEquals;

/**
 * This is a unit test for the PSXmlTreeWalker class.
 *
 * @author      Tas Giakouminakis
 * @version    1.0
 * @since      1.0
 */
public class PSXmlTreeWalkerTest extends TestCase
{
   public void testBookWalker()
   {
      Document doc = PSXmlDocumentBuilder.createXmlDocument();
      Element root = PSXmlDocumentBuilder.createRoot(doc, "BookList");

      for (Book m_book : m_books) {
         m_book.addToDocument(doc, root);
      }

      PSXmlTreeWalker walker = new PSXmlTreeWalker(doc);
      
      assertEquals("the root should be the initial current node",
         walker.getCurrent(), doc.getDocumentElement());

      int i = 0;
      for (Element el = walker.getNextElement("Book", true, true);
         el != null;
         el = walker.getNextElement("Book", true, true))
      {
         String fromCur = walker.getElementData("title", false);
         String fromRelCur = walker.getElementData("./title", false);
         assertEquals("title should match ./title",
            fromCur, fromRelCur);

         fromCur = walker.getElementData("isbn", false);
         fromRelCur = walker.getElementData("./isbn", false);
         assertEquals("isbn should match ./isbn",
            fromCur, fromRelCur);

         fromCur = walker.getElementData("author", false);
         fromRelCur = walker.getElementData("./author", false);
         assertEquals("author should match ./author",
            fromCur, fromRelCur);

         fromCur = walker.getElementData("author/@id", false);
         fromRelCur = walker.getElementData("./author/@id", false);
         assertEquals("author/@id should match ./author/@id",
            fromCur, fromRelCur);

         // if we're on the first run, then getting the data from the
         // parent should also equal the current data
         if (i == 0) {
            fromCur = walker.getElementData("title", false);
            fromRelCur = walker.getElementData("../Book/title", false);
            assertEquals("title should match ../Book/title",
               fromCur, fromRelCur);

            fromCur = walker.getElementData("isbn", false);
            fromRelCur = walker.getElementData("../Book/isbn", false);
            assertEquals("isbn should match ../Book/isbn",
               fromCur, fromRelCur);

            fromCur = walker.getElementData("author", false);
            fromRelCur = walker.getElementData("../Book/author", false);
            assertEquals("author should match ../Book/author",
               fromCur, fromRelCur);

            fromCur = walker.getElementData("author/@id", false);
            fromRelCur = walker.getElementData("../Book/author/@id", false);
            assertEquals("author/@id should match ../Book/author/@id",
               fromCur, fromRelCur);
         }

         i++;
      }

      assertEquals("Did we get all the books?", i, m_books.size());
   }

   public void setUp()
   {
      m_books = new java.util.ArrayList<>();
      m_books.add(new Book("The Power and the Glory", "123456789", "Graham Greene", "1"));
      m_books.add(new Book("Our Man in Havana", "223456789", "Graham Greene", "1"));
      m_books.add(new Book("The Man Within", "323456789", "Graham Greene", "1"));
      m_books.add(new Book("The Inheritors", "423456789", "William Golding", "2"));
      m_books.add(new Book("The Honourable Schoolboy", "523456789", "John le Carre", "3"));
      m_books.add(new Book("All Quiet on the Western Front", "623456789",
              "Erich Maria Remarque", "4"));
   }
   
   /**
    * This tests the static method getBaseElement for the following cases:
    * <table border="1">
    * <tr><th>currentBase</th><th>xmlField</th><th>expected result</th></tr>
    * <tr><td>root/foo/bar</td><td>root/foo/bar</td><td>root/foo/bar<td></tr>
    * <tr><td>root/foo/bar</td><td>root/foo/bar_1</td><td>root/foo<td></tr>
    * <tr><td>root/foo/bar_1</td><td>root/foo/bar</td><td>root/foo<td></tr>
    * <tr><td>null</td><td>root/foo/bar</td><td>root/foo<td></tr>
    * <tr><td>root/foo/bar</td><td>null</td><td>root/foo/bar<td></tr>
    * <tr><td>null</td><td>null</td><td>null<td></tr>
    * <tr><td>root/foo1/bar</td><td>root/foo/bar</td><td>root<td></tr>
    * <tr><td>root/foo/bar</td><td>root/foo1/bar</td><td>root<td></tr>
    * <tr><td>/root/foo/bar</td><td>/root/foo1/bar</td><td>/root<td></tr>
    * <tr><td>/root1/foo/bar</td><td>/root/foo/bar</td><td>null<td></tr>
    * <tr><td>/root/foo/bar</td><td>root/foo1/bar</td><td>null<td></tr>
    * <tr><td>root/foo/bar</td><td>/root/foo1/bar</td><td>null<td></tr>
    * </table>
    */
   public void testGetBaseElement()
   {
      String currentBase = "root/foo/bar";
      String xmlField = "root/foo/bar";
      String base = PSXmlTreeWalker.getBaseElement(currentBase, xmlField);
      String oldBase = getBaseElement(currentBase, xmlField);
      assertEquals("root/foo/bar", base);
      assertEquals(base, oldBase);
      
      // this case tests the fix for bug Rx-04-04-0026
      currentBase = "root/foo/bar";
      xmlField = "root/foo/bar_1";
      base = PSXmlTreeWalker.getBaseElement(currentBase, xmlField);
      oldBase = getBaseElement(currentBase, xmlField);
      assertEquals("root/foo", base);
      assertNotEquals(base, oldBase);

      // this case tests the fix for bug Rx-04-04-0026
      currentBase = "root/foo/bar_1";
      xmlField = "root/foo/bar";
      base = PSXmlTreeWalker.getBaseElement(currentBase, xmlField);
      oldBase = getBaseElement(currentBase, xmlField);
      assertEquals("root/foo", base);
      assertNotEquals(base, oldBase);

      currentBase = null;
      xmlField = "root/foo/bar";
      base = PSXmlTreeWalker.getBaseElement(currentBase, xmlField);
      oldBase = getBaseElement(currentBase, xmlField);
      assertEquals("root/foo", base);
      assertEquals(base, oldBase);

      currentBase = "root/foo/bar";
      xmlField = null;
      base = PSXmlTreeWalker.getBaseElement(currentBase, xmlField);
      oldBase = getBaseElement(currentBase, xmlField);
      assertEquals("root/foo/bar", base);
      assertEquals(base, oldBase);

      currentBase = null;
      xmlField = null;
      base = PSXmlTreeWalker.getBaseElement(currentBase, xmlField);
      // this case throws a null pointer in the old implementation
      // oldBase = getBaseElement(currentBase, xmlField);
      assertNull(base);

      currentBase = "root/foo1/bar";
      xmlField = "root/foo/bar";
      base = PSXmlTreeWalker.getBaseElement(currentBase, xmlField);
      oldBase = getBaseElement(currentBase, xmlField);
      assertEquals("root", base);
      assertEquals(base, oldBase);

      currentBase = "root/foo/bar";
      xmlField = "root/foo1/bar";
      base = PSXmlTreeWalker.getBaseElement(currentBase, xmlField);
      oldBase = getBaseElement(currentBase, xmlField);
      assertEquals("root", base);
      assertEquals(base, oldBase);

      currentBase = "/root/foo/bar";
      xmlField = "/root/foo1/bar";
      base = PSXmlTreeWalker.getBaseElement(currentBase, xmlField);
      oldBase = getBaseElement(currentBase, xmlField);
      assertEquals("/root", base);
      assertEquals(base, oldBase);

      currentBase = "/root1/foo/bar";
      xmlField = "/root/foo/bar";
      base = PSXmlTreeWalker.getBaseElement(currentBase, xmlField);
      oldBase = getBaseElement(currentBase, xmlField);
      assertNull(base);
      assertSame(base, oldBase);

      currentBase = "/root/foo/bar";
      xmlField = "root/foo1/bar";
      base = PSXmlTreeWalker.getBaseElement(currentBase, xmlField);
      oldBase = getBaseElement(currentBase, xmlField);
      assertNull(base);
      assertSame(base, oldBase);

      currentBase = "root/foo/bar";
      xmlField = "/root/foo1/bar";
      base = PSXmlTreeWalker.getBaseElement(currentBase, xmlField);
      oldBase = getBaseElement(currentBase, xmlField);
      assertNull(base);
      assertSame(base, oldBase);
   }
   
   static String ms_expectedSer = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
        "<BookList>\n" + 
        "   <Book>\n" + 
        "      <title>The Power and the Glory</title>\n" + 
        "      <isbn>123456789</isbn>\n" + 
        "      <author id=\"1\">Graham Greene</author>\n" + 
        "   </Book>\n" + 
        "   <Book>\n" + 
        "      <title>Our Man in Havana</title>\n" + 
        "      <isbn>223456789</isbn>\n" + 
        "      <author id=\"1\">Graham Greene</author>\n" + 
        "   </Book>\n" + 
        "   <Book>\n" + 
        "      <title>The Man Within</title>\n" + 
        "      <isbn>323456789</isbn>\n" + 
        "      <author id=\"1\">Graham Greene</author>\n" + 
        "   </Book>\n" + 
        "   <Book>\n" + 
        "      <title>The Inheritors</title>\n" + 
        "      <isbn>423456789</isbn>\n" + 
        "      <author id=\"2\">William Golding</author>\n" + 
        "   </Book>\n" + 
        "   <Book>\n" + 
        "      <title>The Honourable Schoolboy</title>\n" +
        "      <isbn>523456789</isbn>\n" + 
        "      <author id=\"3\">John le Carre</author>\n" + 
        "   </Book>\n" + 
        "   <Book>\n" + 
        "      <title>All Quiet on the Western Front</title>\n" + 
        "      <isbn>623456789</isbn>\n" + 
        "      <author id=\"4\">Erich Maria Remarque</author>\n" + 
        "   </Book>\n" + 
        "</BookList>";
   
   /**
    * Test the serialization 
    * @throws IOException 
    */
   public void testSerialization() throws IOException
   {
      Document doc = PSXmlDocumentBuilder.createXmlDocument();
      Element root = PSXmlDocumentBuilder.createRoot(doc, "BookList");

      for (Book m_book : m_books) {
         m_book.addToDocument(doc, root);
      }

      PSXmlTreeWalker walker = new PSXmlTreeWalker(doc);
      
      StringWriter w = new StringWriter();
      walker.write(w);
      
      assertEquals(ms_expectedSer, w.toString());
   }

   /**
    * This was the old implementation for the getBaseElement method. This is
    * used to test the backwards compatibility.
    */
   private String getBaseElement(String curBase, String xmlField)
   {
      int baseLen = (curBase == null) ? 0 : curBase.length();
      int fldLen = (xmlField == null) ? 0 : xmlField.length();

      if ((curBase == null) || (baseLen == 0)) {
         int pos = xmlField.lastIndexOf('/');
         if (pos != -1)
            return xmlField.substring(0, pos);
         else
            return xmlField;
      }
      else if ((xmlField == null) || (fldLen == 0))
         return curBase;

      char[] chars1 = curBase.toCharArray();

      char[] chars2 = xmlField.toCharArray();
      char ch;

      int pos = 0;
      int lastElementPos = 0;
      for (; (pos < baseLen) && (pos < fldLen); pos++)
      {
         ch = chars1[pos];
         if (ch != chars2[pos])
            break;
         else if (ch == '/')
            lastElementPos = pos;
      }

      /*  No bug Id!  Removed logic which stated
          that we have a match when baseLen == fldLen, which is not
          true!  This was evident when doing a POST with xml
          with two xml fields with the bases but with
          different element names of the same length! */
      if (pos == baseLen)  // don't truncate if it's an exact match
         return curBase;
      else if (pos == fldLen)    // same here, don't truncate
         return xmlField;

      if (lastElementPos <= 0)
         return null;

      return new String(chars1, 0, lastElementPos);
   }

   private java.util.List<Book> m_books;

   protected static class Book implements Comparable
   {
      public Book(String title, String isbn, String author, String authorId)
      {
         m_title = title;
         m_isbn = isbn;
         m_author = author;
         m_authorId = authorId;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (!(o instanceof Book)) return false;
         Book book = (Book) o;
         return Objects.equals(m_title, book.m_title) &&
                 Objects.equals(m_isbn, book.m_isbn) &&
                 Objects.equals(m_author, book.m_author) &&
                 Objects.equals(m_authorId, book.m_authorId);
      }

      @Override
      public int hashCode() {
         return Objects.hash(m_title, m_isbn, m_author, m_authorId);
      }

      public int compareTo(Object o)
      {
         Book b = (Book)o;
         int compare = m_title.compareTo(b.m_title);
         if (compare != 0)
            return compare;
         compare = m_isbn.compareTo(b.m_isbn);
         if (compare != 0)
            return compare;
         compare = m_authorId.compareTo(b.m_authorId);
         if (compare != 0)
            return compare;
         compare = m_author.compareTo(b.m_author);
         return compare;
      }

      public String toXmlString()
      {
         StringBuilder buf = new StringBuilder("<Book>\n\t<title>");
         buf.append(m_title);
         buf.append("</title>\n");
         buf.append("\t<isbn>");
         buf.append(m_isbn);
         buf.append("</isbn>\n");
         buf.append("\t<author id=\"");
         buf.append(m_authorId);
         buf.append("\">");
         buf.append(m_author);
         buf.append("</author>\n</Book>");
         return buf.toString();
      }

      public Element addToDocument(Document doc, Element root)
      {
         Element book = PSXmlDocumentBuilder.addEmptyElement(
            doc, root, "Book");

         PSXmlDocumentBuilder.addElement(doc, book, "title", m_title);

         PSXmlDocumentBuilder.addElement(doc, book, "isbn", m_isbn);

         Element author = PSXmlDocumentBuilder.addElement(
            doc, book, "author", m_author);
         author.setAttribute("id", m_authorId);

         return book;
      }

      private String m_title;
      private String m_isbn;
      private String m_author;
      private String m_authorId;
   }

   public void testCovertToXMLEntities(){
      String test = PSXmlTreeWalker.convertToXmlEntities("This is a test \uD83E\uDD21");

      assertEquals("This is a test &#129313;",test);
   }
}
