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

package com.percussion.delivery.feeds;

import com.percussion.error.PSExceptionUtils;
import com.percussion.junitrunners.Concurrent;
import com.percussion.junitrunners.ConcurrentJunitRunner;
import com.percussion.junitrunners.Server;
import com.percussion.junitrunners.Threshold;
import com.rometools.rome.io.FeedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertTrue;

@Ignore("Remove this if you want to run the test. Remember the webservice must be up and running and you should configure the Server annotation with the correct params")
@RunWith(ConcurrentJunitRunner.class)
@Concurrent(threads = 10)
@Threshold(value = 2000)
@Server(url="http://localhost:9980/",siteName="Site1",feedName="feeds1")
public class PSFeedServicePerformanceTest
{
    private static final Logger log = LogManager.getLogger(PSFeedServicePerformanceTest.class);
    private static int sum = 0;
    private static int Threshold = 0;
    private static String URL = "";
    private static String SITENAME = "";
    private static String FEEDNAME = "";
    
    @Before
    public void setUp() throws Throwable
    {
        Threshold = this.getClass().getAnnotation(Threshold.class).value();
        URL = this.getClass().getAnnotation(Server.class).url();
        SITENAME = this.getClass().getAnnotation(Server.class).siteName();
        FEEDNAME = this.getClass().getAnnotation(Server.class).feedName();
    }
    
    @AfterClass
    public static void average() throws Throwable
    {
        log.info("Average: {}", (sum / 100));
        assertTrue((sum / 100) < Threshold);
    }
    
    @Test public void test1() throws Throwable { requestService(); }
    @Test public void test2() throws Throwable { requestService(); }
    @Test public void test3() throws Throwable { requestService(); }
    @Test public void test4() throws Throwable { requestService(); }
    @Test public void test5() throws Throwable { requestService(); }
    @Test public void test6() throws Throwable { requestService(); }
    @Test public void test7() throws Throwable { requestService(); }
    @Test public void test8() throws Throwable { requestService(); }
    @Test public void test9() throws Throwable { requestService(); }
    @Test public void test10() throws Throwable { requestService(); }
    @Test public void test11() throws Throwable { requestService(); }
    @Test public void test12() throws Throwable { requestService(); }
    @Test public void test13() throws Throwable { requestService(); }
    @Test public void test14() throws Throwable { requestService(); }
    @Test public void test15() throws Throwable { requestService(); }
    @Test public void test16() throws Throwable { requestService(); }
    @Test public void test17() throws Throwable { requestService(); }
    @Test public void test18() throws Throwable { requestService(); }
    @Test public void test19() throws Throwable { requestService(); }
    @Test public void test20() throws Throwable { requestService(); }
    @Test public void test21() throws Throwable { requestService(); }
    @Test public void test22() throws Throwable { requestService(); }
    @Test public void test23() throws Throwable { requestService(); }
    @Test public void test24() throws Throwable { requestService(); }
    @Test public void test25() throws Throwable { requestService(); }
    @Test public void test26() throws Throwable { requestService(); }
    @Test public void test27() throws Throwable { requestService(); }
    @Test public void test28() throws Throwable { requestService(); }
    @Test public void test29() throws Throwable { requestService(); }
    @Test public void test30() throws Throwable { requestService(); }
    @Test public void test31() throws Throwable { requestService(); }
    @Test public void test32() throws Throwable { requestService(); }
    @Test public void test33() throws Throwable { requestService(); }
    @Test public void test34() throws Throwable { requestService(); }
    @Test public void test35() throws Throwable { requestService(); }
    @Test public void test36() throws Throwable { requestService(); }
    @Test public void test37() throws Throwable { requestService(); }
    @Test public void test38() throws Throwable { requestService(); }
    @Test public void test39() throws Throwable { requestService(); }
    @Test public void test40() throws Throwable { requestService(); }
    @Test public void test41() throws Throwable { requestService(); }
    @Test public void test42() throws Throwable { requestService(); }
    @Test public void test43() throws Throwable { requestService(); }
    @Test public void test44() throws Throwable { requestService(); }
    @Test public void test45() throws Throwable { requestService(); }
    @Test public void test46() throws Throwable { requestService(); }
    @Test public void test47() throws Throwable { requestService(); }
    @Test public void test48() throws Throwable { requestService(); }
    @Test public void test49() throws Throwable { requestService(); }
    @Test public void test50() throws Throwable { requestService(); }
    @Test public void test51() throws Throwable { requestService(); }
    @Test public void test52() throws Throwable { requestService(); }
    @Test public void test53() throws Throwable { requestService(); }
    @Test public void test54() throws Throwable { requestService(); }
    @Test public void test55() throws Throwable { requestService(); }
    @Test public void test56() throws Throwable { requestService(); }
    @Test public void test57() throws Throwable { requestService(); }
    @Test public void test58() throws Throwable { requestService(); }
    @Test public void test59() throws Throwable { requestService(); }
    @Test public void test60() throws Throwable { requestService(); }
    @Test public void test61() throws Throwable { requestService(); }
    @Test public void test62() throws Throwable { requestService(); }
    @Test public void test63() throws Throwable { requestService(); }
    @Test public void test64() throws Throwable { requestService(); }
    @Test public void test65() throws Throwable { requestService(); }
    @Test public void test66() throws Throwable { requestService(); }
    @Test public void test67() throws Throwable { requestService(); }
    @Test public void test68() throws Throwable { requestService(); }
    @Test public void test69() throws Throwable { requestService(); }
    @Test public void test70() throws Throwable { requestService(); }
    @Test public void test71() throws Throwable { requestService(); }
    @Test public void test72() throws Throwable { requestService(); }
    @Test public void test73() throws Throwable { requestService(); }
    @Test public void test74() throws Throwable { requestService(); }
    @Test public void test75() throws Throwable { requestService(); }
    @Test public void test76() throws Throwable { requestService(); }
    @Test public void test77() throws Throwable { requestService(); }
    @Test public void test78() throws Throwable { requestService(); }
    @Test public void test79() throws Throwable { requestService(); }
    @Test public void test80() throws Throwable { requestService(); }
    @Test public void test81() throws Throwable { requestService(); }
    @Test public void test82() throws Throwable { requestService(); }
    @Test public void test83() throws Throwable { requestService(); }
    @Test public void test84() throws Throwable { requestService(); }
    @Test public void test85() throws Throwable { requestService(); }
    @Test public void test86() throws Throwable { requestService(); }
    @Test public void test87() throws Throwable { requestService(); }
    @Test public void test88() throws Throwable { requestService(); }
    @Test public void test89() throws Throwable { requestService(); }
    @Test public void test90() throws Throwable { requestService(); }
    @Test public void test91() throws Throwable { requestService(); }
    @Test public void test92() throws Throwable { requestService(); }
    @Test public void test93() throws Throwable { requestService(); }
    @Test public void test94() throws Throwable { requestService(); }
    @Test public void test95() throws Throwable { requestService(); }
    @Test public void test96() throws Throwable { requestService(); }
    @Test public void test97() throws Throwable { requestService(); }
    @Test public void test98() throws Throwable { requestService(); }
    @Test public void test99() throws Throwable { requestService(); }
    @Test public void test100() throws Throwable { requestService(); }
    
    private void requestService () throws Throwable
    {
        try {
            Client client = ClientBuilder.newClient();

            WebTarget webTarget = client.target("this.URL + \"feeds/rss/\" + this.SITENAME + \"/\" + this.FEEDNAME");
            GregorianCalendar initialDate = new GregorianCalendar();
            Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_XML);
            Response response = invocationBuilder.get();
            GregorianCalendar responseDate = new GregorianCalendar();
            sum += (responseDate.getTimeInMillis() - initialDate.getTimeInMillis());

        }catch (Exception e)
            {
                log.error(PSExceptionUtils.getMessageForLog(e));
                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                throw new FeedException(e.getLocalizedMessage(), e);
            }

    }
}
