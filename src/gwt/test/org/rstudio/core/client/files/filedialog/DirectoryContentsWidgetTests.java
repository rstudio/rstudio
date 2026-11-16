/*
 * DirectoryContentsWidgetTests.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.core.client.files.filedialog;

import java.util.ArrayList;

import com.google.gwt.junit.client.GWTTestCase;

import org.rstudio.core.client.files.FileSystemItem;

/**
 * Lists /home/sub, whose rows are `..` (row 0) and file.R (row 1).
 */
public class DirectoryContentsWidgetTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   @Override
   protected void gwtSetUp()
   {
      // the widget does not use its context
      widget_ = new DirectoryContentsWidget(null);
      widget_.setContents(
            new FileSystemItem[] { FileSystemItem.createFile("/home/sub/file.R") },
            FileSystemItem.createDir("/home"));

      // after the listing, whose redraw reports a cleared selection
      selected_ = new ArrayList<>();
      widget_.addSelectionHandler(event -> selected_.add(event.getSelectedItem()));
   }

   // the `..` row is the parent directory to hosts, not an item whose path is
   // literally `..` that each dialog must translate (#19478)
   public void testParentRowSelectionIsParentDirectory()
   {
      widget_.setSelectedRow(0);

      assertEquals(1, selected_.size());
      assertDirectory("/home", selected_.get(0));
      assertDirectory("/home", widget_.getSelectedItem());
      assertDirectory("/home", widget_.getSelectedTarget());
   }

   // the selected row's name stays relative to the listed directory, where
   // `..` names the parent; the parent's own name would name a child
   public void testParentRowValueIsRelativeName()
   {
      widget_.setSelectedRow(0);

      assertEquals("..", widget_.getSelectedValue());
   }

   public void testFileRowSelectionIsFile()
   {
      widget_.setSelectedRow(1);

      assertEquals(1, selected_.size());
      assertEquals("/home/sub/file.R", selected_.get(0).getPath());
      assertEquals("/home/sub/file.R", widget_.getSelectedItem().getPath());
      assertEquals("file.R", widget_.getSelectedValue());
   }

   public void testNoSelection()
   {
      assertNull(widget_.getSelectedItem());
      assertNull(widget_.getSelectedTarget());
      assertEquals("", widget_.getSelectedValue());
   }

   private static void assertDirectory(String path, FileSystemItem item)
   {
      assertNotNull(item);
      assertEquals(path, item.getPath());
      assertTrue(item.isDirectory());
   }

   private DirectoryContentsWidget widget_;
   private ArrayList<FileSystemItem> selected_;
}
