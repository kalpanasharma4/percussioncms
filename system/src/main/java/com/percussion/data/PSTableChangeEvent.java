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

package com.percussion.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the information about an action performed by an update resource
 * in a Rhythmyx application.
 */
public class PSTableChangeEvent
{
   /**
    * Constructs an event object with the supplied data.
    *
    * @param tableName The name of the table, may not be <code>null</code> or
    * empty.
    * @param actionType The action performed, must be one of the ACTION_xxx
    * types.
    * @param columns The column data used to change the specified table.  See
    * {@link #getColumns()} for more information.  May not be <code>null</code>.
    *
    * @throws IllegalArgumentException if any param is invalid.
    */
   public PSTableChangeEvent(String tableName, int actionType, Map columns)
   {
      if (tableName == null || tableName.trim().length() == 0)
         throw new IllegalArgumentException(
            "tableName may not be null or empty");

      if ( !isValidAction( actionType ) )
      {
         throw new IllegalArgumentException("Invalid actionType");
      }

      if (columns == null)
         throw new IllegalArgumentException("columns may not be null");

      m_tableName = tableName;
      m_actionType = actionType;
      m_columns = columns;
   }

   /**
    * The name of the table that was changed.
    *
    * @return The table name, never <code>null</code> or empty.
    */
   public String getTableName()
   {
      return m_tableName;
   }

   /**
    * The action that was performed, one of the ACTION_xxx types.
    *
    * @return One of the action type values, never <code>null</code>.
    */
   public int getActionType()
   {
      return m_actionType;
   }

   /**
    * Gets the data that was used to update the table.  The columns returned
    * are specified when the listener of this event is registered with
    * the publisher of this event (see IPSTableChangeListener#getColumns()}).
    *
    * @return A copy of the data map where each entry represents a column data
    * where the key is the name of the column as a <code>String</code> and the
    * value is the data that was used while performing the event converted to
    * a <code>String</code>.  Will contain all columns that were specified by
    * the listener when it was registered, but if any of these columns were
    * updated with a value other than a <code>String</code> or numeric type,
    * they will be excluded from the results.  The <code>Map</code> may contain
    * additional columns not specified by the listener as well.  Never
    * <code>null</code>. Changes to this returned <code>Map</code> does not
    * effect this event object.
    */
   public Map getColumns()
   {
      if(m_columns instanceof HashMap)
         return (Map)((HashMap)m_columns).clone();
      else
      {
         Map copy = new HashMap();
         copy.putAll(m_columns);
         return copy;
      }
   }
   
   /**
    * @param actionType  The table change action, one of the ACTION_xxx types.
    * 
    * @return <code>true</code> if the supplied action is one of the ACTION_xxx 
    * types, otherwise <code>false</code>
    */
   public static boolean isValidAction(int actionType)
   {
      return actionType == ACTION_INSERT ||
         actionType == ACTION_UPDATE ||
         actionType == ACTION_DELETE;
   }

   /**
    * Constant to indicate the row was inserted.
    */
   public static final int ACTION_INSERT = 0;

   /**
    * Constant to indicate the row was modified.
    */
   public static final int ACTION_UPDATE = 1;

   /**
    * Constant to indicate the row was deleted.
    */
   public static final int ACTION_DELETE = 2;

   /**
    * Constant to indicate undefined action.
    */
   public static final int ACTION_UNDEFINED = -1;

   /**
    * The name of the table that was modified by this event.  Set during ctor,
    * never <code>null</code>, empty, or modified after that.
    */
   private String m_tableName;

   /**
    * The action that was taken when the table was modified.  One of the
    * <code>ACTION_xxx</code> values, set during ctor, never <code>null</code>
    * or modified after that.
    */
   private int m_actionType;

   /**
    * The data used to modify a row in the the table, where the key is the
    * column name as a <code>String</code>, and the value is the object that
    * was used to set the column's value converted to a <code>String</code>.
    * Never <code>null</code> or modified after ctor.
    */
   private Map m_columns;
}
