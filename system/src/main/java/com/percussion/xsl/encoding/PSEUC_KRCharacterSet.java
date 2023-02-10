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
package com.percussion.xsl.encoding;

import java.io.IOException;

/**
 * Defines the EUC_KR character encoding for the Saxon XSLT processor.
 */
public final class PSEUC_KRCharacterSet extends PSGenericCharacterSet
{
   /**
    * Initializes a newly created <code>PSEUC_KRCharacterSet</code> object by
    * delegating to {@link PSGenericCharacterSet#PSGenericCharacterSet(String,
    * String) <code>super("EUC_KR", "java-EUC_KR.xml")</code>}
    * 
    * @throws IOException if there are problems reading the resource file.
    */
   public PSEUC_KRCharacterSet() throws IOException
   {
      super("EUC_KR", "java-EUC_KR.xml");
   }
}
