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

package com.percussion.cms;

import com.percussion.design.objectstore.PSSystemValidationException;
import com.percussion.share.service.exception.PSValidationException;

/**
 * Interface to allow classes to listen for changes to content items.
 */
public interface IPSEditorChangeListener 
{
   /**
    * Called to notify listeners when a content item has changed by the 
    * content editor.
    * 
    * @param e The change event object, never <code>null</code>.
    */
   void editorChanged(PSEditorChangeEvent e) throws PSSystemValidationException, PSValidationException;

}
