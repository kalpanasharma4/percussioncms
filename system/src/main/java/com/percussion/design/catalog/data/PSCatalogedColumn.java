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

package com.percussion.design.catalog.data;

import com.percussion.design.objectstore.PSBackEndTable;
import com.percussion.xml.PSXmlTreeWalker;


/**
 * The PSCatalogedColumn is used to store a cataloged column's details.
 * 
 * @author     Tas Giakouminakis
 * @version    1.0
 * @since      1.0
 */
public class PSCatalogedColumn
{
   /** the column allows NULL values */
   public static final int      NULLABLE_UNKNOWN   = 0;
   public static final int      NULLABLE            = 1;
   public static final int      NOT_NULLABLE      = 2;

   /**
    * Construct a column meta data object with some basic info.
    */
   public PSCatalogedColumn(
      com.percussion.design.objectstore.PSBackEndTable table,
      org.w3c.dom.Element node)
   {
      super();

      if (!"Column".equals(node.getTagName()))
         throw new IllegalArgumentException("Column node required!");

      m_table = table;

      PSXmlTreeWalker w = new PSXmlTreeWalker(node);
      m_name = w.getElementData("name", true);
      m_beType = w.getElementData("backEndDataType", true);
      m_jdbcType = Integer.parseInt(w.getElementData("jdbcDataType", true));

      String sTemp = w.getElementData("size", true);
      int pos = sTemp.indexOf(".");
      if (pos == -1)
      {
         m_precision = Integer.parseInt(sTemp);
         m_scale = 0;
      }
      else
      {
         m_precision = Integer.parseInt(sTemp.substring(0, pos));
         m_scale = Integer.parseInt(sTemp.substring(pos+1));
      }

      sTemp = w.getElementData("allowsNull", true);
      if ("yes".equals(sTemp))
         m_allowsNull = NULLABLE;
      else if ("no".equals(sTemp))
         m_allowsNull = NOT_NULLABLE;
      else   // unknown
         m_allowsNull = NULLABLE_UNKNOWN;
   }

   /**
    * Get the name of this column.
    */
   public String getName()
   {
      return m_name;
   }

   /**
    * Get the name of this column.
    */
   public PSBackEndTable getTable()
   {
      return m_table;
   }

   /**
    * Get the back-end specific type name.
    */
   public String getBackEndType()
   {
      return m_beType;
   }

   /**
    * Get the JDBC type (from java.sql.Types).
    */
   public int getJdbcType()
   {
      return m_jdbcType;
   }

   /**
    * Get the precision of the column. For numeric and date/time types,
    * this is the max size (including fractions). For other types, it
    * is usually the column size.
    */
   public int getPrecision()
   {
      return m_precision;
   }

   /**
    * Get the scale of the column. For numeric types this is the fraction.
    * For time types this is the number of fractional seconds
    * (eg, 1 = tenths, 2 = hundredths, etc.). For most other types
    * this is 0.
    */
   public int getScale()
   {
      return m_scale;
   }

   /**
    * Get the nullability setting which may be NULLABLE,
    * NOT_NULLABLE or NULLABLE_UNKNOWN.
    */
   public int getNullability()
   {
      return m_allowsNull;
   }

   /**
    * Get a string representing the column info.
    */
   public String toString()
   {
      StringBuilder buf = new StringBuilder();
      buf.append("Column[");
      buf.append(m_name);
      buf.append(" type=");
      buf.append(m_beType);
      buf.append("(");
      buf.append(String.valueOf(m_precision));
      buf.append(".");
      buf.append(String.valueOf(m_scale));
      buf.append(")]");
      return buf.toString();
   }


   private PSBackEndTable m_table;
   private String m_name;
   private String m_beType;
   private int m_jdbcType;
   private int m_precision;
   private int m_scale;
   private int m_allowsNull;
}

