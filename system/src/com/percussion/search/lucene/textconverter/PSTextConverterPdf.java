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
package com.percussion.search.lucene.textconverter;

import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionProcessingException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;


import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Extracts the text from a supplied input stream corresponding to a PDF file.
 * It uses PDFBox to extract the text from the PDF Document. The following are
 * the limitations.
 * <ul>
 * <li>Extracts only text and no meta data like author or created date etc.</li>
 * <li>If the document is password protected, it tries to decrypt using empty
 * password. If succeeds extracts the text and returns, otherwise throws
 * appropritae exception wrapped in PSExtensionProcessingException.</li>
 * </ul>
 * 
 */
public class PSTextConverterPdf implements IPSLuceneTextConverter
{

   /*
    * (non-Javadoc)
    * @see com.percussion.search.lucene.textconverter.IPSLuceneTextConverter#getConvertedText(java.io.InputStream, java.lang.String)
    */
   public String getConvertedText(InputStream is, String mimetype)
      throws PSExtensionProcessingException
   {
      if (is == null)
         throw new IllegalArgumentException("is must not be null");

      String resultText = "";
      PDDocument pdfDocument = null;
      try
      {
         pdfDocument = PDDocument.load(is);
         if (pdfDocument.isEncrypted())
         {
            //Just try using the default password and move on

         }
         PDFTextStripper stripper = new PDFTextStripper();
         StringWriter writer = new StringWriter();
         stripper.writeText(pdfDocument, writer);
         resultText = writer.getBuffer().toString();
      }
      catch (Exception e)
      {
         throw new PSExtensionProcessingException(m_className, e);
      }
      finally
      {
         if (pdfDocument != null)
         {
            try
            {
               pdfDocument.close();
            }
            catch (IOException e)
            {
               throw new PSExtensionProcessingException(m_className, e);
            }
         }

      }
      return resultText;
   }

   /*
    * (non-Javadoc)
    * @see com.percussion.extension.IPSExtension#init(com.percussion.extension.IPSExtensionDef, java.io.File)
    */
   public void init(IPSExtensionDef def, File codeRoot)
      throws PSExtensionException
   {

   }

   /**
    * A member variable to hold the name of this class.
    */
   private String m_className = getClass().getName();
}
