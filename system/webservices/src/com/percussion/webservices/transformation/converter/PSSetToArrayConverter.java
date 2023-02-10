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
package com.percussion.webservices.transformation.converter;

import java.util.Set;

import org.apache.commons.beanutils.BeanUtilsBean;

/**
 * Converts inputs of type Set to outputs of type Array for the type 
 * mappings registered.
 */
public class PSSetToArrayConverter extends PSConverter
{
   /* (non-Javadoc)
    * @see PSConverter#PSConvert(BeanUtilsUtil)
    */
   public PSSetToArrayConverter(BeanUtilsBean beanUtils)
   {
      super(beanUtils);
   }

   /* (non-Javadoc)
    * @see Converter#convert(Class, Object)
    */
   public Object convert(Class arrayType, Object value)
   {
      if (value == null)
         return null;
      
      if (arrayType == null)
         throw new IllegalArgumentException("arrayType cannot be null");
      
      if (!(value instanceof Set))
         throw new IllegalArgumentException("value must be of type Set");
      
      Set valueSet = (Set) value;
      return super.convert(arrayType, valueSet.toArray());
   }
}
