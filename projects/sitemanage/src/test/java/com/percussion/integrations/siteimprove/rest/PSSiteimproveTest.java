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

package com.percussion.integrations.siteimprove.rest;

import com.percussion.integrations.siteimprove.data.PSSiteImproveCredentials;
import com.percussion.integrations.siteimprove.data.PSSiteImproveSiteConfigurations;
import com.percussion.share.test.PSRestClient.RestClientException;
import com.percussion.share.test.PSRestTestCase;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.UUID;

@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PSSiteimproveTest extends PSRestTestCase<PSSiteimproveRestClient> {


	// Testing Resources
	private static final String TESTING_USER = "percussionbot@gmail.com";
	private static final String TESTING_TOKEN = "388c3dc6a18b9582baada754b1408b7e";
	private static final String TESTING_SITENAME = "unitTest";

	/**
	 * @param baseUrl base url for unit testing
	 * @return Construct the rest client for testing purposes.
	 */
	@Override
	protected PSSiteimproveRestClient getRestClient(String baseUrl) {
		return new PSSiteimproveRestClient(baseUrl);
	}

	@Test
	public void a_storeCredentialsTest() throws Exception {
		PSSiteImproveCredentials credentials = new PSSiteImproveCredentials(TESTING_SITENAME, TESTING_TOKEN);
		restClient.storeCredentials(credentials);
	}

	@Test
	public void b_storebadCredentialsTest() throws Exception {
		try {
			PSSiteImproveCredentials credentials = new PSSiteImproveCredentials(UUID.randomUUID().toString(),
					UUID.randomUUID().toString());
			restClient.storeCredentials(credentials);
		} catch (Exception exception) {
			Assert.assertTrue(exception instanceof RestClientException);
			RestClientException restClientException = (RestClientException) exception;
			Assert.assertTrue(restClientException.getStatus() == 401);
		}
	}

	@Test
	public void c_storeSiteConfigurationTest() throws Exception {
		PSSiteImproveSiteConfigurations configurations = new PSSiteImproveSiteConfigurations(TESTING_SITENAME, true, false, false, true, null);
		restClient.storeSiteConfig(configurations);
	}

	@Test
	public void d_storeBadSiteConfigurationTest() throws Exception {
		try {
			PSSiteImproveSiteConfigurations configurations = new PSSiteImproveSiteConfigurations(null, null, null, null, null, null);
			restClient.storeSiteConfig(configurations);
		} catch (Exception exception) {
			Assert.assertTrue(exception instanceof RestClientException);
			RestClientException restClientException = (RestClientException) exception;
			Assert.assertTrue(restClientException.getStatus() == 500);
		}
	}

	@Test
	public void retrieveSiteCredentialsTest() throws Exception {
		String results = restClient.retrieveCredentials(TESTING_SITENAME);
		Assert.assertNotNull(results);
		Assert.assertTrue(results.contains("perc.siteimprove.credentials." + TESTING_SITENAME));
		Assert.assertTrue(results.contains(TESTING_USER));
		Assert.assertTrue(results.contains(TESTING_TOKEN));
	}

	@Test
	public void retrieveAllCredentialsTest() throws Exception {
		String results = restClient.retrieveAllCredentials();

		Assert.assertNotNull(results);
		Assert.assertTrue(results.length() > 0);
		Assert.assertTrue(results.contains(TESTING_SITENAME));
		Assert.assertTrue(results.contains(TESTING_TOKEN));
		Assert.assertTrue(results.contains(TESTING_USER));
	}

	@Test
	public void retrieveBadCredentialsTest() throws Exception {
		String results = null;
		try {
			results = restClient.retrieveCredentials(UUID.randomUUID().toString());
		} catch (Exception exception) {
			Assert.assertTrue(exception instanceof RestClientException);
		}
		Assert.assertNull(results);
	}

	@Test
	public void retrieveSiteConfigurationTest() throws Exception {
		String results = restClient.retrieveSiteConfig(TESTING_SITENAME);
		Assert.assertNotNull(results);
		Assert.assertTrue(results.contains("doStaging"));
		Assert.assertTrue(results.contains("false"));
	}

	@Test
	public void retrieveAllSiteConfigurationsTest() throws Exception {
		String results = restClient.retrieveAllSiteConfig();

		Assert.assertNotNull(results);
		Assert.assertTrue(results.length() > 0);
		Assert.assertTrue(results.contains(TESTING_SITENAME));
		Assert.assertTrue(results.contains("doPreview"));
		Assert.assertTrue(results.contains("doProduction"));
		Assert.assertTrue(results.contains("true"));
	}

	@Test
	public void retrievebadSiteConfigTest() throws Exception {
		String results = null;
		try {
			results = restClient.retrieveSiteConfig(UUID.randomUUID().toString());
		} catch (Exception exception) {
			Assert.assertTrue(exception instanceof RestClientException);
		}
		Assert.assertNull(results);
	}

}
