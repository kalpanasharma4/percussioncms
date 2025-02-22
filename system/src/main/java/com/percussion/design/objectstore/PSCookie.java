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

import org.w3c.dom.Element;

import java.util.List;

/**
 * The PSCookie class is used to define a replacement value is a 
 * cookie value.
 *
 * @see        IPSReplacementValue
 *
 * @author     Tas Giakouminakis
 * @version    1.0
 * @since      1.0
 */
public class PSCookie extends PSNamedReplacementValue
{
   /**
    * Construct a Java object from its XML representation.
    *
    * @param sourceNode the XML element node to construct this object from
    * @param parentDoc the Java object which is the parent of this object
    * @param parentComponents   the parent objects of this object
    *
    * @throws PSUnknownNodeTypeException if the XML element node is not of the
    *   appropriate type
    */
   public PSCookie(Element sourceNode, IPSDocument parentDoc,
                   List parentComponents)
         throws PSUnknownNodeTypeException
   {
      super( sourceNode, parentDoc, parentComponents );
   }


   /**
    * Constructs a cookie replacement value.
    *
    * @param name the name of the cookie
    */
   public PSCookie(String name)
   {
      super( name );
   }


   // see base class for description
   protected int getErrorCode()
   {
      return IPSObjectStoreErrors.COOKIE_NAME_EMPTY;
   }


   /**
    * Gets the type of replacement value this object represents.
    * @return {@link #VALUE_TYPE}
    */
   public String getValueType()
   {
      return VALUE_TYPE;
   }


   // see base class for description
   protected String getNodeName()
   {
      return ms_NodeType;
   }


   /**
    * The value type associated with this instances of this class.
    */
   public static final String VALUE_TYPE = "Cookie";

   /* package access on this so they may reference each other in fromXml */
   static final String ms_NodeType = "PSXCookie";
}

