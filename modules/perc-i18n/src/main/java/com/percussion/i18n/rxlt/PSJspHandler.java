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
package com.percussion.i18n.rxlt;

import com.percussion.i18n.PSI18nUtils;
import com.percussion.i18n.tmxdom.IPSTmxDocument;
import com.percussion.i18n.tmxdom.IPSTmxDtdConstants;
import com.percussion.i18n.tmxdom.IPSTmxTranslationUnit;
import com.percussion.i18n.tmxdom.IPSTmxTranslationUnitVariant;
import com.percussion.i18n.tmxdom.PSTmxDocument;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Scans all JSP files for i18n information.  For system files, this information
 * should be added to the SystemResources.tmx file as a translation unit.  For
 * custom files, this information may be added to the ResourceBundle.tmx file or
 * directly into the jsp page as part of an rxi18n comment with the following
 * format:
 * 
 * <%-- rxi18n key="name" note="desc" tuvprops="prop=val,..." tuvseg="val" --%>
 * 
 * i.e.,
 * 
 * <%-- rxi18n key="jsp_login@Password" note="Password field label"
 *      tuvprops="mnemonic=P" tuvseg="Password" --%>
 * 
 * Where key is the i18n key, note is the description of the key's value,
 * tuvprops are the translation unit variant properties, and tuvseg is the
 * translation unit variant segment value for this key.
 * 
 * @author dougrand
 */
public class PSJspHandler extends PSIdleDotter implements IPSSectionHandler
{
   private static final String JETTY_APP_SERVER = "jetty/base/webapps";

   /**
    * Default name of section that is implemented by this class. Overridden
    * during processing by the name specified in the config element.
    * 
    * @see #process
    */
   private static String sectionName = "JSP Files";

   /**
    * Enumeration for the rxi18n comment translation unit sections that are
    * processed by the jsp section handler of the language tool.  Each section
    * is identified by a unique ordinal value and name.
    */
   private enum TuSectionEnum
   {
      /**
       * Note section -> this is a description of the translation unit segment.
       * value
       */
      NOTE(0, "note"),

      /**
       * Translation unit variant properties -> i.e., mnemonic property.
       */
      TUV_PROPS(1, "tuvprops"),

      /**
       * Translation unit variant segment -> this is the value of the
       * translation unit segment.
       */
      TUV_SEG(2, "tuvseg");

      /**
       * Ordinal value, initialized in the ctor, and never modified.
       */
      private int ordinal;

      /**
       * Name value for the action category, initialized in the ctor, never
       * modified.
       */
      private String name = null;

      /**
       * Returns the ordinal value for the enumeration.
       * 
       * @return the ordinal
       */
      private int getOrdinal()
      {
         return ordinal;
      }

      /**
       * Returns the action category name value for the enumeration.
       * 
       * @return the name, never <code>null</code> or empty.
       */
      private String getName()
      {
         return name;
      }

      /**
       * Ctor taking the ordinal value and name of the action category.
       * 
       * @param ord unique ordianl value for the action caegory.
       * @param name name of the action category, must not be <code>null</code>
       * or empty.
       */
      private TuSectionEnum(int ord, String name)
      {
         ordinal = ord;
         if (StringUtils.isBlank(name))
         {
            throw new IllegalArgumentException("name may not be null or empty");
         }
         this.name = name;
      }
   }
   
   public IPSTmxDocument process(Element cfgData)
         throws PSSectionProcessingException
   {
      if (cfgData == null)
         throw new IllegalArgumentException("cfgData element must not be null");

      String rxroot = cfgData.getOwnerDocument().getDocumentElement()
            .getAttribute(PSRxltConfigUtils.ATTR_RXROOT);
      sectionName = cfgData.getAttribute(PSRxltConfigUtils.ATTR_NAME);

      PSCommandLineProcessor.logMessage("blankLine", "");
      PSCommandLineProcessor.logMessage("processingSection", sectionName);
      PSCommandLineProcessor.logMessage("blankLine", "");

      PSTmxDocument tmxDoc = null;
      try
      {
         Map<String, String[]> tuInfo = new HashMap<>();
         handleAllJsps(rxroot, tuInfo);

         // Now the dataDoc has all the keys from all content editors.
         // Create an empty TMX Document
         tmxDoc = new PSTmxDocument();
         IPSTmxTranslationUnit tu = null;
         Set keySet = tuInfo.keySet();
         Iterator iter = keySet.iterator();
         while (iter.hasNext())
         {
            String key = (String) iter.next();
            String[] info = tuInfo.get(key);
            String desc = "";
            String note = info[TuSectionEnum.NOTE.getOrdinal()];
            String tuvprops = info[TuSectionEnum.TUV_PROPS.getOrdinal()];
            String tuvseg = info[TuSectionEnum.TUV_SEG.getOrdinal()];
            tu = tmxDoc.createTranslationUnit(key, desc);
            tu.addProperty(tmxDoc.createProperty(
                  IPSTmxDtdConstants.ATTR_VAL_SECTIONNAME,
                  PSI18nUtils.DEFAULT_LANG, sectionName));
            if (note != null)
               tu.addNote(tmxDoc.createNote(PSI18nUtils.DEFAULT_LANG,
                     note), true);
            if (tuvseg != null)
            {
               IPSTmxTranslationUnitVariant tuv =
                  tmxDoc.createTranslationUnitVariant(
                        PSI18nUtils.DEFAULT_LANG, tuvseg);
               
               if (tuvprops != null)
               {
                  String[] props = tuvprops.split(",");
                  if ( props.length > 0)
                  {
                     for (String prop : props) {
                        if (prop != null) {
                           String[] propVal = prop.split("=");
                           if (propVal.length == 2) {
                              tuv.addProperty(tmxDoc.createProperty(
                                      propVal[0], PSI18nUtils.DEFAULT_LANG,
                                      propVal[1]));
                           }
                        }
                     }
                  }
               }
               tu.addTuv(tuv, true);
            }     
            tmxDoc.merge(tu);
         }
      }
      catch (Exception e)
      {
         throw new PSSectionProcessingException(e.getMessage());
      }
      finally
      {
         endDotSession();
      }
      return tmxDoc;
   }

   /**
    * Find and process all the jsps found under the appserver directory
    * 
    * @param rxroot the root directory for the rhythmyx server
    * @param tuInfo the map of key/[note, props, seg] pairs
    */
   private void handleAllJsps(String rxroot, Map tuInfo)
   {
      File rxapp = new File(rxroot, JETTY_APP_SERVER);
      handleAllJsps(rxapp, tuInfo);
   }

   /**
    * Find and process all the jsps found in a given directory
    * 
    * @param dir directory to search
    * @param tuInfo the map of key/[note, props, seg] pairs
    */
   private void handleAllJsps(File dir, Map tuInfo)
   {
      if (dir.exists())
      {
      for (File f : Objects.requireNonNull(dir.listFiles()))
      {
         if (f.getName().endsWith(".jsp"))
         {
            handleJspFile(f, tuInfo);
         }
         else if (f.isDirectory())
         {
            handleAllJsps(f, tuInfo);
         }
      }
      }

   }

   /**
    * Find any rxi18n jsp comments in a given jsp file and stores the supplied
    * translation unit information.
    * 
    * @param file file to examine
    * @param tuInfo the map of translation unit
    * key/[note, props, seg] pairs
    */
   private void handleJspFile(File file, Map tuInfo)
   {

      try( Reader r = new FileReader(file))
      {
         showDots(true);

         String text = IOUtils.toString(r);
         getTranslationUnitInfo(text, tuInfo);
      }
      catch (Exception e)
      {
         PSCommandLineProcessor.logMessage("Problem reading file: " + file);
      }
      finally
      {
         showDots(false);
      }

   }
   
   /**
    * Find the start quote after the start index, and find the matching end
    * quote. We'll make the not unreasonable assumption that we'll find a quote
    * within a few characters
    * 
    * @param start the start pos for the prefix text
    * @param text the text to search
    * @return the quoted value
    */
   private String getQuotedValue(int start, String text)
   {
      int searchTo = Math.min(start + 128, text.length());
      char quote = 0;

      while (quote == 0 && start < searchTo)
      {
         char ch = text.charAt(start);
         if (ch == '\'')
         {
            quote = ch;
            break;
         }
         else if (ch == '"')
         {
            quote = ch;
            break;
         }
         start++;
      }

      if (quote == 0)
         return null;

      start++; // Move off quote

      int end = text.indexOf(quote, start);
      if (end < 0)
         return null;

      return text.substring(start, end);
   }
   
   /**
    * Search the text for all rxi18n comments which contain the
    * translation unit information
    * 
    * @param text the text to search
    * @param tuInfo the translation unit information as a map of 
    * translation unit -> [note, tuv props, tuv seg] pairs 
    */
   private void getTranslationUnitInfo(String text, Map tuInfo)
   {
      // Search for tag
      int i = text.indexOf("rxi18n");
      
      while (i != -1)
      {
         // Find the bounds for the comment
         int start = text.lastIndexOf("<%--", i);
         int end = text.indexOf("--%>", i);
      
         if (start < 0 || end < 0)
            break;
         
         String[] info = new String[TuSectionEnum.values().length];
         String key = getQuotedValue("key", start, end, text);
         info[TuSectionEnum.NOTE.getOrdinal()] =
            getQuotedValue(TuSectionEnum.NOTE.getName(), start,
                  end, text);
         info[TuSectionEnum.TUV_PROPS.getOrdinal()] = 
            getQuotedValue(TuSectionEnum.TUV_PROPS.getName(),
                  start, end, text);
         info[TuSectionEnum.TUV_SEG.getOrdinal()] = 
            getQuotedValue(TuSectionEnum.TUV_SEG.getName(),
                  start, end, text);
         tuInfo.put(key, info);         
         
         i = text.indexOf("rxi18n", end);
      }
   }
   
   /**
    * Searches for a quoted value after a matching string value in the given
    * string text starting at the given index.
    * 
    * @param str the string value 
    * @param start the starting index
    * @param end the end boundary index
    * @param text the string text in which to search
    * @return the first quoted string value following the string
    */
   private String getQuotedValue(String str, int start, int end, String text)
   {
      String quotedVal = null;
      
      int index = text.indexOf(str, start);
      if (index >= 0 && index <= end)
         quotedVal = getQuotedValue(index, text);
      
      return quotedVal;
   }
     
}
