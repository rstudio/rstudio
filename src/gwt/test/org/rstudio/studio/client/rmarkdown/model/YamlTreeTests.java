/*
 * YamlTreeTests.java
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
package org.rstudio.studio.client.rmarkdown.model;

import java.util.List;

import com.google.gwt.junit.client.GWTTestCase;

public class YamlTreeTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testRootLevelCommentPreserved()
   {
      String yaml = "# This is a comment\ntitle: Test\n";
      YamlTree tree = new YamlTree(yaml);
      String output = tree.toString();

      assertTrue("Comment should be preserved",
            output.contains("# This is a comment"));
      assertEquals("Test", tree.getKeyValue("title"));
   }

   public void testRootLevelCommentWithColonPreserved()
   {
      String yaml = "# Note: important info\ntitle: Test\n";
      YamlTree tree = new YamlTree(yaml);
      String output = tree.toString();

      assertTrue("Comment with colon should be preserved",
            output.contains("# Note: important info"));
      assertFalse("Comment should not create a key",
            tree.containsKey("# Note"));
      assertEquals("Test", tree.getKeyValue("title"));
   }

   public void testCommentBetweenSiblings()
   {
      String yaml = "title: Test\n# Comment\nauthor: Author\n";
      YamlTree tree = new YamlTree(yaml);
      String output = tree.toString();

      assertTrue("Comment should be preserved",
            output.contains("# Comment"));

      int titlePos = output.indexOf("title:");
      int commentPos = output.indexOf("# Comment");
      int authorPos = output.indexOf("author:");
      assertTrue("Comment should be between title and author",
            titlePos < commentPos && commentPos < authorPos);
   }

   public void testIndentedCommentPreserved()
   {
      String yaml =
            "output:\n" +
            "  # Comment about format\n" +
            "  html_document:\n" +
            "    toc: true\n";
      YamlTree tree = new YamlTree(yaml);
      String output = tree.toString();

      assertTrue("Indented comment should be preserved",
            output.contains("# Comment about format"));
   }

   public void testIndentedCommentPositionPreserved()
   {
      String yaml =
            "output:\n" +
            "  html_document:\n" +
            "    toc: true\n" +
            "    # Comment about theme\n" +
            "    theme: united\n";
      YamlTree tree = new YamlTree(yaml);
      String output = tree.toString();

      assertTrue("Comment should be preserved",
            output.contains("# Comment about theme"));

      int tocPos = output.indexOf("toc:");
      int commentPos = output.indexOf("# Comment about theme");
      int themePos = output.indexOf("theme:");
      assertTrue("Comment should be between toc and theme",
            tocPos < commentPos && commentPos < themePos);
   }

   public void testEditorOptionsPreservesComments()
   {
      String yaml =
            "title: Test\n" +
            "# Important comment\n" +
            "output: html_document\n";
      String result = RmdEditorOptions.set(
            yaml, "chunk_output_type", "console", false);

      assertTrue("Comment should be preserved after adding editor options",
            result.contains("# Important comment"));
      assertTrue("New option should be added",
            result.contains("chunk_output_type: console"));
   }

   public void testMultilineValuesStillWork()
   {
      String yaml =
            "description: >\n" +
            "  This is a long\n" +
            "  description\n" +
            "title: Test\n";
      YamlTree tree = new YamlTree(yaml);
      String output = tree.toString();

      assertTrue("Multi-line value should be preserved",
            output.contains("This is a long"));
      assertEquals("Test", tree.getKeyValue("title"));
   }

   public void testMultipleComments()
   {
      String yaml =
            "# First comment\n" +
            "title: Test\n" +
            "# Second comment\n" +
            "author: Author\n" +
            "# Third comment\n";
      YamlTree tree = new YamlTree(yaml);
      String output = tree.toString();

      assertTrue("First comment should be preserved",
            output.contains("# First comment"));
      assertTrue("Second comment should be preserved",
            output.contains("# Second comment"));
      assertTrue("Third comment should be preserved",
            output.contains("# Third comment"));
   }

   public void testKeyValuesUnaffectedByComments()
   {
      String yaml =
            "# Comment\n" +
            "title: Test\n" +
            "# Another comment\n" +
            "author: Author\n";
      YamlTree tree = new YamlTree(yaml);

      assertEquals("Test", tree.getKeyValue("title"));
      assertEquals("Author", tree.getKeyValue("author"));
      assertTrue(tree.containsKey("title"));
      assertTrue(tree.containsKey("author"));
   }

   public void testBlankLineBetweenSiblings()
   {
      String yaml =
            "title: Test\n" +
            "\n" +
            "author: Author\n";
      YamlTree tree = new YamlTree(yaml);
      String output = tree.toString();

      assertEquals("Test", tree.getKeyValue("title"));
      assertEquals("Author", tree.getKeyValue("author"));

      int titlePos = output.indexOf("title:");
      int blankPos = output.indexOf("\n\n");
      int authorPos = output.indexOf("author:");
      assertTrue("Blank line should be between title and author",
            titlePos < blankPos && blankPos < authorPos);
   }

   public void testBlankLineAndCommentPreserved()
   {
      String yaml =
            "title: Test\n" +
            "\n" +
            "# Comment after blank line\n" +
            "author: Author\n";
      YamlTree tree = new YamlTree(yaml);
      String output = tree.toString();

      assertTrue("Blank line should be preserved",
            output.contains("\n\n"));
      assertTrue("Comment should be preserved",
            output.contains("# Comment after blank line"));

      int titlePos = output.indexOf("title:");
      int commentPos = output.indexOf("# Comment after blank line");
      int authorPos = output.indexOf("author:");
      assertTrue("Ordering should be preserved",
            titlePos < commentPos && commentPos < authorPos);
   }

   public void testNestedBlankLinePreservesTreeStructure()
   {
      String yaml =
            "output:\n" +
            "  html_document:\n" +
            "    toc: true\n" +
            "\n" +
            "    theme: united\n";
      YamlTree tree = new YamlTree(yaml);
      String output = tree.toString();

      assertTrue("toc should be preserved",
            output.contains("toc: true"));
      assertTrue("theme should be preserved",
            output.contains("theme: united"));

      // theme should still be a child of html_document, not displaced
      List<String> htmlChildren = tree.getChildKeys("html_document");
      assertNotNull("html_document should have children", htmlChildren);
      assertTrue("toc should be a child of html_document",
            htmlChildren.contains("toc"));
      assertTrue("theme should be a child of html_document",
            htmlChildren.contains("theme"));
   }

   public void testCommentAsFirstChildPreserved()
   {
      String yaml =
            "params:\n" +
            "  # P1\n" +
            "  p1: 1\n";
      YamlTree tree = new YamlTree(yaml);
      String output = tree.toString();

      int paramsPos = output.indexOf("params:");
      int commentPos = output.indexOf("# P1");
      int p1Pos = output.indexOf("p1:");
      assertTrue("Comment should be between params and p1",
            paramsPos < commentPos && commentPos < p1Pos);

      assertEquals("1", tree.getKeyValue("p1"));
   }
}
