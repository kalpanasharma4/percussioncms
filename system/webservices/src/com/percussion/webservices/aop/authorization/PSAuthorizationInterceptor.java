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
package com.percussion.webservices.aop.authorization;

import com.percussion.design.objectstore.PSAclEntry;
import com.percussion.security.PSAuthorizationException;
import com.percussion.security.PSSecurityToken;
import com.percussion.server.PSRequest;
import com.percussion.server.PSServer;
import com.percussion.server.PSUserSession;
import com.percussion.server.PSUserSessionManager;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.aop.security.IPSWsMethod;
import com.percussion.webservices.aop.security.IPSWsParameter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Interceptor to verify that the method invoker has design access set for the 
 * rhythmyx server ACL. Only acts on these design methods (see 
 * <code>ear/config/spring/beans.xml</code>):
 * <ul>
 *    <li>create*</li>
 *    <li>load*, only if the objects are loaded as locked</li>
 *    <li>delete*</li>
 *    <li>save*</li>
 * </ul>
 * Use {@link IPSWsMethod#ignoreAuthorization()} to ignore specific methods
 * from processing.
 */
public class PSAuthorizationInterceptor implements MethodInterceptor
{
   /**
    * If the invoked method is not ignored explicitly and belongs to one of
    * the webservice java API design interfaces this will test if the user for
    * the current session has server design access rights. It throws a new
    * <code>RuntimeException</code> if the user does not have design access.
    * The process expects that one of the method arguments is a valid 
    * session id. If no session id is supplied or if it is invalid it will
    * also throw a new <code>RuntimeException</code>.
    * 
    * @see org.aopalliance.intercept.MethodInterceptor#invoke(
    *    org.aopalliance.intercept.MethodInvocation)
    */
   public Object invoke(MethodInvocation invocation) throws Throwable
   {
      if (!ignore(invocation) && !skipLoadUnlocked(invocation))
      {
         PSUserSession session = null;
         
         Object[] args = invocation.getArguments();
         if (args != null)
         {
            for (int i=args.length-1; i>=0; i--)
            {
               Object arg = args[i];
               if (arg instanceof String)
               {
                  session = PSUserSessionManager.getUserSession(arg.toString());
                  if (session != null)
                  {
                     try
                     {
                        PSServer.checkAccessLevel(new PSSecurityToken(session), 
                           PSAclEntry.SACE_ACCESS_DESIGN);
                     }
                     catch (PSAuthorizationException e)
                     {
                        throw new RuntimeException(e.getLocalizedMessage(), e);
                     }
                     
                     break;
                  }
               }
            }
         }
         // If the session is not passed part of the arguments get it from
         // request info.
         if(session == null)
         {
            session = ((PSRequest)PSRequestInfo
            .getRequestInfo(PSRequestInfo.KEY_PSREQUEST)).getUserSession();
         }
         
         /*
          * Invalid usage if we have not found a valid session, should not
          * happen.
          */
         if (session == null)
         {
            throw new RuntimeException(
               "No valid session was found for invocation of method: " + 
               invocation.getMethod().getName());
         }
      }
      
      return invocation.proceed();
   }

   /**
    * Determine if the specified method should be processed at all. Methods
    * annotated as <code>@IPSWsMethod(ignoreAuthorization=true)</code> will 
    * not be processed.
    * 
    * @param invocation the invocation specifying the method, assumed not 
    *    <code>null</code>.
    * @return <code>true</code> to ignore, <code>false</code> otherwise.
    */
   private boolean ignore(MethodInvocation invocation)
   {
      IPSWsMethod ws = invocation.getMethod().getAnnotation(IPSWsMethod.class);
      if (ws != null)
         return ws.ignoreAuthorization();
      else
         return false;
   }
   
   /**
    * First tests if the invoked method is a <code>load*</code>. If that is
    * the case it is determined whether or not the requested objects are to be
    * loaded unlocked.
    * 
    * @param invocation the invocation specifying the method, assumed not 
    *    <code>null</code>.
    * @return <code>true</code> if the invoked method is a <code>load*</code> 
    *    and the requested objects are requested as unlocked, 
    *    <code>false</code> otherwise.
    */
   private boolean skipLoadUnlocked(MethodInvocation invocation)
   {
      Method method = invocation.getMethod();
      if (method.getName().startsWith("load"))
      {
         boolean found = false;
         int lockParameterIndex = 0;
         
         Annotation[][] parameters = method.getParameterAnnotations();
         for (Annotation[] annotations : parameters)
         {
            for (Annotation annotation : annotations)
            {
               if (annotation instanceof IPSWsParameter)
               {
                  IPSWsParameter ws = (IPSWsParameter) annotation;
                  if (ws.isLockParameter())
                  {
                     found = true;
                     break;
                  }
               }
            }
            
            if (found)
               break;
            
            lockParameterIndex++;
         }
         
         if (found)
         {
            Object[] args = invocation.getArguments();
            return !((Boolean) args[lockParameterIndex]);
         }
      }
      
      return false;
   }
}

