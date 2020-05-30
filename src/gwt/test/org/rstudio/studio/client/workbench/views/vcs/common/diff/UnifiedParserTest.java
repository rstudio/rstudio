/*
 * UnifiedParserTest.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs.common.diff;

import junit.framework.TestCase;

import java.io.*;
import java.net.URL;

public class UnifiedParserTest extends TestCase
{
   public void setUp() throws Exception
   {

   }

   public void tearDown() throws Exception
   {

   }

   private String readFileResource(String name) throws Exception
   {
      FileInputStream fileInputStream = null;
      try
      {
         URL url = getClass().getResource(name);
         fileInputStream = new FileInputStream(url.getFile());
         StringWriter sw = new StringWriter();
         for (int c; -1 != (c = fileInputStream.read()); )
         {
            sw.append((char) c);
         }
         return sw.toString();
      }
      finally
      {
         if (fileInputStream != null)
            fileInputStream.close();
      }
   }

   public void testNextChunk() throws Exception
   {
      testFile("diff1");
   }

   public void testNextChunk2() throws Exception
   {
      testFile("diff2");
   }

   private void testFile(String testName) throws Exception
   {
      StringWriter stringWriter = new StringWriter();
      PrintWriter output = new PrintWriter(stringWriter);
      UnifiedParser parser = new UnifiedParser(readFileResource(testName + ".txt"));
      DiffChunk chunk;
      while (null != (chunk = parser.nextChunk()))
      {
         output.println(UnifiedEmitter.createChunkString(chunk));
         for (Line line : chunk.getLines())
         {
            char c;
            switch (line.getType())
            {
               case Insertion:
                  c = '+';
                  break;
               case Same:
                  c = ' ';
                  break;
               case Deletion:
                  c = '-';
                  break;
               case Comment:
                  c = '/';
                  break;
               default:
                  throw new IllegalArgumentException();
            }
            boolean[] appliesTo = line.getAppliesTo();
            for (int i = 0; i < appliesTo.length-1; i++)
               output.print(appliesTo[i] ? c : ' ');
            output.println(line.getText());
         }
      }
      assertEquals(readFileResource(testName + ".out.txt"), stringWriter.toString());
   }
}
