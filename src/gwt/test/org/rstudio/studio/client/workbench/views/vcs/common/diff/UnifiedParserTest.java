/*
 * UnifiedParserTest.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs.common.diff;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;

// Each diffN.out.txt is the hunk text of diffN.txt, so parsing a diff and
// re-emitting its chunk headers, column markers, and line text should
// reproduce it exactly.
public class UnifiedParserTest extends GWTTestCase
{
   interface Resources extends ClientBundle
   {
      @Source("diff1.txt")
      TextResource diff1();

      @Source("diff1.out.txt")
      TextResource diff1Out();

      @Source("diff2.txt")
      TextResource diff2();

      @Source("diff2.out.txt")
      TextResource diff2Out();
   }

   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testCombinedDiffRoundTrips()
   {
      Resources res = GWT.create(Resources.class);
      UnifiedParser parser = new UnifiedParser(res.diff1().getText());

      // nextChunk() stops at a file header, so consume it first
      DiffFileHeader header = parser.nextFilePair();
      assertNotNull(header);
      assertEquals("describe.c", header.getDescription());

      assertEquals(res.diff1Out().getText(), render(parser));
   }

   public void testBareHunkRoundTrips()
   {
      Resources res = GWT.create(Resources.class);
      UnifiedParser parser = new UnifiedParser(res.diff2().getText());

      assertEquals(res.diff2Out().getText(), render(parser));
   }

   private static String render(UnifiedParser parser)
   {
      StringBuilder output = new StringBuilder();

      DiffChunk chunk;
      while (null != (chunk = parser.nextChunk()))
      {
         output.append(UnifiedEmitter.createChunkString(chunk)).append('\n');
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

            // appliesTo says which versions contain the line, so an
            // insertion is marked against the parents that lack it, and a
            // deletion against the parents that have it
            boolean[] appliesTo = line.getAppliesTo();
            boolean insertion = line.getType() == Line.Type.Insertion;
            for (int i = 0; i < appliesTo.length - 1; i++)
               output.append(appliesTo[i] != insertion ? c : ' ');
            output.append(line.getText()).append('\n');
         }
      }

      return output.toString();
   }
}
