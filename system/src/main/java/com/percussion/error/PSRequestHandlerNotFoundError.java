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

package com.percussion.error;

import com.percussion.log.PSLogError;
import com.percussion.log.PSLogSubMessage;

import java.util.Locale;


/**
 * The PSRequestHandlerNotFoundError class is used to report the failure
 * to locate a handler for an incoming request. This occurs when the
 * incoming request does not properly identify an application or data set.
 *
 * @author      Tas Giakouminakis
 * @version      1.0
 * @since      1.0
 */
public class PSRequestHandlerNotFoundError extends PSLogError {
   
   /**
    * Report the failure to locate a handler for an incoming request.
    * <p>
    * The application id is most commonly obtained by calling
    * {@link com.percussion.data.PSExecutionData#getId PSExecutionData.getId()} or
    * {@link com.percussion.server.PSApplicationHandler#getId PSApplicationHandler.getId()}.
    *
    * @param      applId      the id of the application that generated
    *                           the error
    *
    * @param      errorCode   the error code describing the type of error
    *
    * @param      errorParams   if the error string associated with the
    *                           error code specifies parameters, this is
    *                           an array of values to use to fill the string
    *                           appropriately. Be sure to include the
    *                           correct arguments in their correct
    *                           positions!
    */
   public PSRequestHandlerNotFoundError(   int applId,
                                          int errorCode,
                                          Object[] errorParams)
   {
      super(applId);
      m_errorCode = errorCode;
      m_errorArgs = errorParams;
   }

   /**
    * sublcasses must override this to build the messages in the
    * specified locale
    */
   protected PSLogSubMessage[] buildSubMessages(Locale loc)
   {
      PSLogSubMessage[] msgs = new PSLogSubMessage[1];

      /* the submessage containing m_errorCode/m_errorArgs */
      msgs[0]   = new PSLogSubMessage(
                                 m_errorCode,
                                 PSErrorManager.createMessage(   m_errorCode,
                                                               m_errorArgs,
                                                               loc));

      return msgs;
   }


   private int         m_errorCode;
   private Object[]   m_errorArgs;
}

