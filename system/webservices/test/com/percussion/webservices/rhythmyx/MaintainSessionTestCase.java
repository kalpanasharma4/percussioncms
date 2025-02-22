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

package com.percussion.webservices.rhythmyx;

import com.percussion.utils.testing.IntegrationTest;
import org.apache.axis.MessageContext;
import org.apache.axis.transport.http.HTTPConstants;

import com.percussion.webservices.PSTestBase;
import com.percussion.webservices.security.LoadCommunitiesRequest;
import com.percussion.webservices.security.LoadRolesRequest;
import com.percussion.webservices.security.LoginRequest;
import com.percussion.webservices.security.LoginResponse;
import com.percussion.webservices.security.SecuritySOAPStub;
import com.percussion.webservices.security.data.PSLogin;
import com.percussion.webservices.system.LoadWorkflowsRequest;
import com.percussion.webservices.system.SwitchCommunityRequest;
import com.percussion.webservices.system.SystemSOAPStub;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertTrue;

/**
 * Test case for maintaining JSESSION in Axis client. 
 * Note: One session can only maintained by one Stub, so we will have to
 * maintain multiple sessions when involves multiple stubs. 
 * Refer to http://wiki.apache.org/ws/FrontPage/Axis/SessionSupport for detail
 */
@Category(IntegrationTest.class)
public class MaintainSessionTestCase extends PSTestBase
{
   /**
    * Test maintaining sessions with multiple stubs.
    * 
    * @throws Exception if an error occurs.
    */
   public void testMaintainSession() throws Exception
   {
      SecuritySOAPStub binding = getSecuritySOAPStub(null);
      binding.setMaintainSession(true);

      String rxSession = login(binding);
      
      useSecurityStub(binding);
      
      useSystemStub(rxSession);
   }

   /**
    * Invokes several operations with the specified security soap stub, 
    * where the JSESSION (Cookie) is maintained across all the operations.
    * 
    * @param binding the specified security soap stub, assumed not 
    *    <code>null</code>.
    *    
    * @throws Exception if an error occurs.
    */
   private void useSecurityStub(SecuritySOAPStub binding) throws Exception
   {
      String secJsession = getJSession(binding);

      binding.loadCommunities(new LoadCommunitiesRequest());
      assertTrue(secJsession.equals(getJSession(binding)));
      
      binding.loadRoles(new LoadRolesRequest());
      assertTrue(secJsession.equals(getJSession(binding)));      
   }
   
   /**
    * Invokes several operations with a created system soap stub.
    * where the JSESSION (Cookie) is maintained across all the operations.
    * 
    * @param rxSession the Rhythmyx session used for the created system soap 
    *    stub, assumed not <code>null</code> or empty.
    *    
    * @throws Exception if an error occurs.
    */
   private void useSystemStub(String rxSession)
      throws Exception
   {
      // use the same jsession for different stub
      SystemSOAPStub binding = getSystemSOAPStub(null);
      binding.setMaintainSession(true);
      setRxSession(binding, rxSession);

      SwitchCommunityRequest req = new SwitchCommunityRequest();
      req.setName("Enterprise_Investments");
      binding.switchCommunity(req);
      
      // get the JSESSION after the 1st operation
      String sysJsession = getJSession(binding);

      binding.switchCommunity(req);
      // validate the JSESSION after the 2nd operation
      assertTrue(sysJsession.equals(getJSession(binding)));
      
      binding.loadWorkflows(new LoadWorkflowsRequest());
      // validate the JSESSION after the 3rd operation
      assertTrue(sysJsession.equals(getJSession(binding)));
   }
   
   /**
    * Login with the specified security stub.
    * 
    * @param binding the stub used to invoke the login operation, 
    *    assumed not <code>null</code>.
    * 
    * @return the Rhythmyx session, never <code>null</code> or empty.
    * 
    * @throws Exception
    */
   private String login(SecuritySOAPStub binding) throws Exception
   {
      LoginRequest loginRequest = new LoginRequest("admin1", "demo", null,
         null, null);
      LoginResponse response = binding.login(loginRequest);
      PSLogin login = response.getPSLogin();

      setRxSession(binding, login.getSessionId());
      
      return login.getSessionId();
   }
   
   /**
    * Gets the JSESSION from the specified stub.
    * 
    * @param binding the stub that contains JSESSION, assumed not 
    *    <code>null</code>.
    * 
    * @return the JSESSION of the stub, not <code>null</code> or empty.
    */
   private String getJSession(org.apache.axis.client.Stub binding)
   {
      MessageContext messageContext = binding._getCall().getMessageContext();
      Object cookieObj = messageContext
            .getProperty(HTTPConstants.HEADER_COOKIE);
      String jsession = null;
      if (cookieObj instanceof String[])
         jsession = ((String[]) cookieObj)[0];
      else if (cookieObj instanceof String)
         jsession = (String) cookieObj;
      assertTrue(jsession != null && jsession.trim().length() != 0);
      return jsession;
   }

   /**
    * Sets the specified Rhythmyx session for the given stub.
    *  
    * @param binding the stub, assumed not <code>null</code>.
    * @param rxSession the specified Rhythmyx session, assumed not 
    *    <code>null</code> or empty.
    */
   private void setRxSession(org.apache.axis.client.Stub binding,
         String rxSession)
   {
      binding.setHeader("urn:www.percussion.com/6.0.0/common", "session",
            rxSession);      
   }

}
