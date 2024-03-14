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
package com.percussion.data.jdbc;

import com.percussion.cms.IPSConstants;
import com.percussion.error.PSExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *   Unit tests for the PSFileSystemStatementTest class
 */
public class PSFileSystemStatementTest
{
   private static final Logger log = LogManager.getLogger(IPSConstants.TEST_LOG);

   @Rule
   public TemporaryFolder tempFolder = new TemporaryFolder();

   public static void main(String[] args)
   {
      StringBuilder buf = new StringBuilder(args.length * 10);
      for (String arg : args) {
         buf.append(arg);
         buf.append(" ");
      }

      String query = buf.toString();
      log.info("Executing query: \"");
      log.info(query);
      log.info("\"");

      try
      {
         PSFileSystemStatement statement = new PSFileSystemStatement(null);
         ResultSet result = statement.executeQuery(query);
         int i = 0;
         while (result.next())
         {
            log.info(result.getLong("length") + " ");
            log.info(result.getString("modified") + " ");
            log.info("" + result.getString("fullname"));
            i++;
         }
      }
      catch (Exception t)
      {
        log.error(PSExceptionUtils.getDebugMessageForLog(t));
      }
   }

   public PSFileSystemStatementTest()
   {
   }

   /**
    *   Set up the testing directories and files
    */
   @Before
   public void setUp() throws IOException {
      // make the testing directories
      m_rootDir = tempFolder.newFolder("Testing","PSFileSystemStatementTest");
      m_2Dir = new File(m_rootDir, "Dir2");
      m_3Dir = new File(m_2Dir, "Dir3");
      m_4Dir = new File(m_3Dir, "Dir4");

      m_2Files = new String[]
      {
         "To", "him", "who", "in", "the", "love", "of", "nature", "holds",
         "Communion", "with", "her", "visible", "forms", "she", "speaks",
         "a", "various", "language"
      };

      m_3Files = new String[]
      {
         "And", "eloquence", "of", "beauty", "she", "glides", "into",
         "his", "darker", "musings", "with", "a", "mild", "healing",
         "sympathy", "that", "steals", "away", "their", "sharpness",
         "ere", "he", "is", "aware"
      };

      m_4Files = new String[] 
      {
         "When", "thoughts", "of", "the", "last", "bitter", "hour",
         "come", "like", "a", "blight", "over", "thy", "spirit",
         "and", "sad", "images", "stern", "agony", "shroud",
         "pall", "breathless", "darkness", "narrow", "house"
      };

      m_totalNumFiles = 5+ m_2Files.length + m_3Files.length + m_4Files.length ;

      try
      {
         m_4Dir.mkdirs();

         RandomAccessFile file;
         File f;
         for (int i = 0; i < m_2Files.length; i++)
         {
            f = new File(m_2Dir, m_2Files[i]);
            file = new RandomAccessFile(f, "rw");
            file.write(i);
            file.close();
            f.deleteOnExit();
         }
         for (int i = 0; i < m_3Files.length; i++)
         {
            f = new File(m_3Dir, m_3Files[i]);
            file = new RandomAccessFile(f, "rw");
            file.write(i);
            file.close();
            f.deleteOnExit();
         }
         for (int i = 0; i < m_4Files.length; i++)
         {
            f = new File(m_4Dir, m_4Files[i]);
            file = new RandomAccessFile(f, "rw");
            file.write(i);
            file.close();
            f.deleteOnExit();
         }
      } catch (IOException e)
      {
         log.error(PSExceptionUtils.getDebugMessageForLog(e));
      }
   }

   /**
    *   Test recursive file building
    */
   @Test
   @Ignore //TODO: Fix ME!
   public void testRecursive() throws Exception
   {
      String tempPath = tempFolder.getRoot().getAbsolutePath();
      PSFileSystemStatement statement = new PSFileSystemStatement(null);
      String sqlQuery =
         "SELECT name, fullname, path, modified, length FROM '"+ tempPath + "*'";

      ResultSet result = statement.executeQuery(sqlQuery);
      int i = 0;
      while (result.next())
      {
         log.info(result.getLong("length") + " ");
         log.info(result.getString("modified") + " ");
         log.info("" + result.getString("fullname"));
         i++;
      }

      // test the meta data and make sure the columns match
      ResultSetMetaData meta = result.getMetaData();
      assertNotNull(meta);

      assertEquals("Column count", 5, meta.getColumnCount());

      assertEquals("Name column type should be VARCHAR",
              java.sql.Types.VARCHAR, meta.getColumnType(1));

      assertEquals("Fullname column type should be VARCHAR",
              java.sql.Types.VARCHAR, meta.getColumnType(2));

      assertEquals("Path column type should be VARCHAR",
              java.sql.Types.VARCHAR, meta.getColumnType(3));

      assertEquals("Modified column type should be VARCHAR",
              java.sql.Types.VARCHAR, meta.getColumnType(4));

      assertEquals("Length column type should be BIGINT",
              java.sql.Types.BIGINT, meta.getColumnType(5));

      assertEquals(m_totalNumFiles, i);

   }


   File m_rootDir;   // the root of the testing directory
   File m_2Dir;   // the second tier of the testing dir
   File m_3Dir;   // the third tier
   File m_4Dir;   // the fourth tier
   String[] m_2Files;
   String[] m_3Files;
   String[] m_4Files;
   int m_totalNumFiles ;
}
