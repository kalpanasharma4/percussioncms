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
package test.percussion.pso.imageedit.services;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.pso.imageedit.services.AbstractTemplateExpanderAdaptor;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.IPSContentPropertyConstants;
import com.percussion.services.contentmgr.IPSNode;
import com.percussion.services.contentmgr.PSContentMgrConfig;
import com.percussion.services.contentmgr.data.PSQueryResult;
import com.percussion.services.contentmgr.data.PSRow;
import com.percussion.services.contentmgr.data.PSRowComparator;
import com.percussion.services.contentmgr.impl.jsrdata.PSRowIterator;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.publisher.data.PSContentListItem;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.types.PSPair;

/**
 * 
 *
 * @author DavidBenua
 *
 */
//TODO: Fix Me
@Ignore()
public class AbstractTemplateExpanderAdaptorTest
{
   private static final Logger log = LogManager.getLogger(AbstractTemplateExpanderAdaptorTest.class);
   
   Mockery context; 
   TestableTemplateExpanderAdaptor cut; 
   List<IPSGuid> templateList; 
   
   IPSGuidManager gmgr;
   IPSContentMgr cmgr; 
   
   /**
    * @throws Exception
    */
   @Before
   public void setUp() throws Exception
   {
      context = new Mockery(){{  setImposteriser(ClassImposteriser.INSTANCE);}};
      templateList = new ArrayList<IPSGuid>();
      
      cut = new TestableTemplateExpanderAdaptor(); 
      gmgr = context.mock(IPSGuidManager.class);
      AbstractTemplateExpanderAdaptor.setGmgr(gmgr);
      cmgr = context.mock(IPSContentMgr.class);
      AbstractTemplateExpanderAdaptor.setCmgr(cmgr); 
      
      final IPSGuid tguid1 = context.mock(IPSGuid.class,"tguid1");
      final IPSGuid tguid2 = context.mock(IPSGuid.class, "tguid2");
      
      context.checking(new Expectations(){{
         allowing(tguid1).getType();
         will(returnValue(PSTypeEnum.TEMPLATE));
         allowing(tguid1).getUUID(); 
         will(returnValue(1));
         allowing(tguid2).getType();
         will(returnValue(PSTypeEnum.TEMPLATE));
         allowing(tguid2).getUUID(); 
         will(returnValue(2));
      }});
      templateList.add(tguid1);
      templateList.add(tguid2);
   }
   /**
    * Test method for {@link AbstractTemplateExpanderAdaptor#expand(QueryResult, Map, Map)}.
    */
   @Test
   public final void testExpand()
   {
      
      Map<String,String> params = new HashMap<String, String>();
      params.put(IPSHtmlParameters.SYS_CONTEXT, "101");
      params.put("siteid" , "301"); 
      
 
      Map<Integer, PSComponentSummary> summaryMap = buildSummaryMapExpectations();
      cut.setNeedsContentNode(false);
     
      QueryResult qr = buildQueryResultExpectations();

      final IPSGuid siteGuid = context.mock(IPSGuid.class,"siteguid"); 

      try
      {
         context.checking(new Expectations(){{
            one(gmgr).makeGuid("301",PSTypeEnum.SITE);
            will(returnValue(siteGuid));   
         }});  
         List<PSContentListItem> items = cut.expand(qr, params, summaryMap);
         assertNotNull(items);
         log.debug("items returned " + items.size());
         assertEquals(4,items.size()); 
         log.info(items);
         
         context.assertIsSatisfied();
      } catch (Exception ex)
      {
         log.error("Unexpected Exception " + ex,ex);
         fail("Exception");
      }
   }

   @Test
   @SuppressWarnings("unchecked")
   public final void testBuildNodeMap()
   {
      QueryResult qr = buildQueryResultExpectations();
      Map<Integer,PSComponentSummary> summaryMap = buildSummaryMapExpectations();
      
      try
      {
         final IPSNode node1 = context.mock(IPSNode.class, "node1");
         final IPSGuid guid1 = context.mock(IPSGuid.class, "guid1");
         
         final IPSNode node2 = context.mock(IPSNode.class, "node2");
         final IPSGuid guid2 = context.mock(IPSGuid.class, "guid2"); 
         
         final List<IPSNode> nodelist = new ArrayList<IPSNode>(){{
            add(node1);
            add(node2);
         }};
         
         context.checking(new Expectations(){{
            one(cmgr).findItemsByGUID(with(any(List.class)), with(any(PSContentMgrConfig.class)));
            will(returnValue(nodelist));
            one(node1).getGuid();
            will(returnValue(guid1));
            one(node2).getGuid();
            will(returnValue(guid2));
         }});
         
         Map<IPSGuid, Node> nodeMap = cut.buildNodeMap(qr, summaryMap);
         assertNotNull(nodeMap); 
         log.debug("nodeMap " + nodeMap);
         assertTrue(nodeMap.containsKey(guid1)); 
         assertTrue(nodeMap.containsKey(guid2));
         context.assertIsSatisfied();
         
      } catch (RepositoryException ex)
      {
         log.error("Unexpected Exception " + ex,ex);
         fail("Exception");
      }
   }
   
   private Map<Integer,PSComponentSummary> buildSummaryMapExpectations()
   {
      final PSComponentSummary sum302 = context.mock(PSComponentSummary.class, "sum302");
      final PSComponentSummary sum303 = context.mock(PSComponentSummary.class, "sum303");
      
      context.checking(new Expectations(){{
         allowing(sum302).getContentId();
         will(returnValue(302));
         allowing(sum303).getContentId();
         will(returnValue(303)); 
      }});
      
          
      final Map<Integer, PSComponentSummary> summaryMap = 
          new HashMap<Integer, PSComponentSummary>(){{
             put(302, sum302);
             put(303, sum303); 
          }};
     return summaryMap;
   }
   
   private QueryResult buildQueryResultExpectations()
   {
      final PSRow row1 = new PSRow(new HashMap<String, Object>(){{
         put(IPSContentPropertyConstants.RX_SYS_CONTENTID, "302");
         put(IPSContentPropertyConstants.RX_SYS_REVISION, "2");
         put(IPSContentPropertyConstants.RX_SYS_CONTENTTYPEID, "47"); 
         put(IPSContentPropertyConstants.RX_SYS_FOLDERID, "201");
      }});
      final PSRow row2 = new PSRow(new HashMap<String, Object>(){{
         put(IPSContentPropertyConstants.RX_SYS_CONTENTID, "303");
         put(IPSContentPropertyConstants.RX_SYS_REVISION, "4");
         put(IPSContentPropertyConstants.RX_SYS_CONTENTTYPEID, "48"); 
         put(IPSContentPropertyConstants.RX_SYS_FOLDERID, "201");
      }});
      final RowIterator rows = new PSRowIterator(new ArrayList<PSRow>(){{
         add(row1);
         add(row2);
      }});
      
      
      final IPSGuid guid302 = context.mock(IPSGuid.class,"guid302");
      final IPSGuid guid303 = context.mock(IPSGuid.class,"guid303");
      
      final IPSGuid folderGuid = context.mock(IPSGuid.class,"folderGuid");
      
      final PSRowComparator rowcomp = new PSRowComparator(new ArrayList<PSPair<String,Boolean>>(){{
         add(new PSPair<String, Boolean>(IPSContentPropertyConstants.RX_SYS_CONTENTID, true));   
      }});

      final PSQueryResult qr = new PSQueryResult(new String[]{
            IPSContentPropertyConstants.RX_SYS_CONTENTID,
            IPSContentPropertyConstants.RX_SYS_REVISION,
            IPSContentPropertyConstants.RX_SYS_CONTENTTYPEID,
            IPSContentPropertyConstants.RX_SYS_FOLDERID},
            rowcomp);
      
      qr.addRow(row1);
      qr.addRow(row2);
      
      context.checking(new Expectations(){{
         one(gmgr).makeGuid(new PSLocator(302,2));
         will(returnValue(guid302));
         one(gmgr).makeGuid(new PSLocator(303,4)); 
         will(returnValue(guid303)); 
         allowing(gmgr).makeGuid(new PSLocator(201,0));
         will(returnValue(folderGuid));
      }});

      return qr; 
   }
   
   private class TestableTemplateExpanderAdaptor extends AbstractTemplateExpanderAdaptor
   {

      @Override
      protected List<IPSGuid> findTemplates(IPSGuid itemGuid,
            IPSGuid folderGuid, IPSGuid siteGuid, int context,
            PSComponentSummary summary, Node contentNode,
            Map<String, String> parameters)
      {
         log.info("Item Guid " + itemGuid );
         
         return templateList;
      }

      /**
       * @see AbstractTemplateExpanderAdaptor#buildNodeMap(QueryResult, Map)
       */
      @Override
      public Map<IPSGuid, Node> buildNodeMap(QueryResult result,
            Map<Integer, PSComponentSummary> summaryMap)
            throws RepositoryException
      {
         return super.buildNodeMap(result, summaryMap);
      }

      /**
       * @see AbstractTemplateExpanderAdaptor#isNeedsContentNode()
       */
      @Override
      public boolean isNeedsContentNode()
      {
         return super.isNeedsContentNode();
      }

      /**
       * @see AbstractTemplateExpanderAdaptor#setNeedsContentNode(boolean)
       */
      @Override
      public void setNeedsContentNode(boolean needsContentNode)
      {
         super.setNeedsContentNode(needsContentNode);
      }
      
      
   }
   
}
