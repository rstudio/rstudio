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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.RootPanel;

import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.command.ShortcutManager.Handle;
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
      // as the file dialogs (modal dialogs) do while shown; otherwise its key
      // preview reaches Ace, which these tests don't load
      shortcutsDisabled_ = ShortcutManager.INSTANCE.disable();

      // the widget does not use its context; attached so that DOM events
      // reach the table's handlers
      widget_ = new DirectoryContentsWidget(null);
      RootPanel.get().add(widget_);
      listSub();

      // after the listing, whose clear() reports a cleared selection
      selected_ = new ArrayList<>();
      widget_.addSelectionHandler(event -> selected_.add(event.getSelectedItem()));
      committed_ = new ArrayList<>();
      widget_.addSelectionCommitHandler(event -> committed_.add(event.getSelectedItem()));
   }

   @Override
   protected void gwtTearDown()
   {
      RootPanel.get().clear();
      shortcutsDisabled_.close();
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

   // FileSystemDialog.onSelectionCommit cd()s into a committed directory, so
   // the `..` placeholder must not reach it
   public void testDoubleClickOnParentRowCommitsParentDirectory()
   {
      Element cell = getRows().get(0).getFirstChildElement();

      cell.dispatchEvent(Document.get().createDblClickEvent(
            0, 0, 0, 0, 0, false, false, false, false));

      assertEquals(1, committed_.size());
      assertDirectory("/home", committed_.get(0));
   }

   public void testEnterOnParentRowCommitsParentDirectory()
   {
      widget_.setSelectedRow(0);

      pressKey("Enter", KeyCodes.KEY_ENTER);

      assertEquals(1, committed_.size());
      assertDirectory("/home", committed_.get(0));
   }

   public void testBackspaceCommitsParentDirectory()
   {
      widget_.setSelectedRow(1);

      pressKey("Backspace", KeyCodes.KEY_BACKSPACE);

      assertEquals(1, committed_.size());
      assertDirectory("/home", committed_.get(0));
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

   // ChooseFolderDialog2.onSelection resets its folder field on the null
   // event a new listing reports (#18938)
   public void testRelistingReportsNullSelection()
   {
      widget_.setSelectedRow(0);
      selected_.clear();

      listSub();

      assertEquals(1, selected_.size());
      assertNull(selected_.get(0));
   }

   private void listSub()
   {
      widget_.setContents(
            new FileSystemItem[] { FileSystemItem.createFile("/home/sub/file.R") },
            FileSystemItem.createDir("/home"));
   }

   private Element getTableElement()
   {
      return widget_.getElement().getElementsByTagName("table").getItem(0);
   }

   private ArrayList<Element> getRows()
   {
      ArrayList<Element> rows = new ArrayList<>();
      NodeList<Element> trs = getTableElement().getElementsByTagName("tr");
      for (int i = 0; i < trs.getLength(); i++)
      {
         if ("option".equals(trs.getItem(i).getAttribute("role")))
            rows.add(trs.getItem(i));
      }
      return rows;
   }

   private void pressKey(String key, int keyCode)
   {
      NativeEvent event = Document.get().createKeyDownEvent(
            false, false, false, false, keyCode);
      setKey(event, key);
      getTableElement().dispatchEvent(event);
   }

   // createKeyDownEvent() leaves 'key' unset, but RowTable reads it
   private static native void setKey(NativeEvent event, String key)
   /*-{
      Object.defineProperty(event, "key", { value: key });
   }-*/;

   private static void assertDirectory(String path, FileSystemItem item)
   {
      assertNotNull(item);
      assertEquals(path, item.getPath());
      assertTrue(item.isDirectory());
   }

   private Handle shortcutsDisabled_;
   private DirectoryContentsWidget widget_;
   private ArrayList<FileSystemItem> selected_;
   private ArrayList<FileSystemItem> committed_;
}
