/*
 * RowTableTests.java
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
package org.rstudio.core.client.widget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.RootPanel;

public class RowTableTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   @Override
   protected void gwtTearDown()
   {
      RootPanel.get().clear();
   }

   private static class StringTable extends RowTable<String>
   {
      StringTable()
      {
         super("test");
      }

      @Override
      public void drawRowImpl(String object, TableRowElement rowEl)
      {
         rowEl.appendChild(Document.get().createTDElement()).setInnerText(object);
      }

      @Override
      public double getRowHeight()
      {
         return 20;
      }

      @Override
      public int[] getColumnWidths()
      {
         return new int[] { 100 };
      }

      @Override
      public String getKey(String object)
      {
         return object;
      }
   }

   private StringTable createTable(String... items)
   {
      StringTable table = new StringTable();
      table.setSize("200px", "200px");
      RootPanel.get().add(table);
      table.draw(new ArrayList<>(Arrays.asList(items)));
      return table;
   }

   private static Element getTableElement(RowTable<String> table)
   {
      return table.getElement().getElementsByTagName("table").getItem(0);
   }

   private static List<Element> getOptions(RowTable<String> table)
   {
      List<Element> options = new ArrayList<>();
      NodeList<Element> rows = getTableElement(table).getElementsByTagName("tr");
      for (int i = 0; i < rows.getLength(); i++)
      {
         if ("option".equals(rows.getItem(i).getAttribute("role")))
            options.add(rows.getItem(i));
      }
      return options;
   }

   private static String getActiveDescendant(RowTable<String> table)
   {
      return getTableElement(table).getAttribute("aria-activedescendant");
   }

   private static void pressKey(RowTable<String> table, String key, int keyCode)
   {
      NativeEvent event = Document.get().createKeyDownEvent(
            false, false, false, false, keyCode);
      setKey(event, key);
      getTableElement(table).dispatchEvent(event);
   }

   // createKeyDownEvent() leaves 'key' unset, but RowTable reads it
   private static native void setKey(NativeEvent event, String key)
   /*-{
      Object.defineProperty(event, "key", { value: key });
   }-*/;

   public void testFocusMakesFirstOptionActiveWithoutSelectingIt()
   {
      StringTable table = createTable("a", "b", "c");
      int[] selectionEvents = { 0 };
      table.addSelectionHandler(event -> selectionEvents[0]++);

      table.setFocus(true);

      Element first = getOptions(table).get(0);
      assertEquals(first.getId(), getActiveDescendant(table));
      assertFalse(first.hasAttribute("aria-selected"));
      assertNull(table.getSelectedItem());
      assertEquals(0, selectionEvents[0]);
   }

   public void testFocusKeepsExistingSelectionActive()
   {
      StringTable table = createTable("a", "b", "c");
      table.selectRow(2);

      table.setFocus(true);

      assertEquals(getOptions(table).get(2).getId(), getActiveDescendant(table));
      assertEquals("c", table.getSelectedItem());
   }

   public void testFocusOnEmptyTableHasNoActiveOption()
   {
      StringTable table = createTable();

      table.setFocus(true);

      assertEquals("", getActiveDescendant(table));
      assertNull(table.getSelectedItem());
   }

   public void testArrowDownMovesFromActiveOption()
   {
      StringTable table = createTable("a", "b", "c");
      table.setFocus(true);

      pressKey(table, "ArrowDown", KeyCodes.KEY_DOWN);

      assertEquals("b", table.getSelectedItem());
      assertEquals(getOptions(table).get(1).getId(), getActiveDescendant(table));
   }

   public void testArrowUpSelectsActiveFirstOption()
   {
      StringTable table = createTable("a", "b", "c");
      table.setFocus(true);

      pressKey(table, "ArrowUp", KeyCodes.KEY_UP);

      assertEquals("a", table.getSelectedItem());
      assertEquals("true", getOptions(table).get(0).getAttribute("aria-selected"));
   }

   public void testArrowDownWithoutFocusSelectsFirstOption()
   {
      // with no active option, the first arrow press still lands on row 0
      StringTable table = createTable("a", "b", "c");

      pressKey(table, "ArrowDown", KeyCodes.KEY_DOWN);

      assertEquals("a", table.getSelectedItem());
   }

   public void testRedrawWhileFocusedMakesFirstOptionActive()
   {
      // navigating to another directory redraws the table while it keeps focus
      StringTable table = createTable("a", "b", "c");
      table.setFocus(true);
      table.selectRow(1);

      table.clear();
      table.draw(new ArrayList<>(Arrays.asList("x", "y")));

      assertEquals(getOptions(table).get(0).getId(), getActiveDescendant(table));
      assertNull(table.getSelectedItem());
   }

   public void testClearingToEmptyDropsActiveOption()
   {
      // e.g. a file dialog navigating into an empty directory
      StringTable table = createTable("a", "b", "c");
      table.setFocus(true);

      table.clear();
      table.draw(new ArrayList<>());

      assertEquals("", getActiveDescendant(table));
   }

   public void testMouseFocusDoesNotScrollToActiveOption()
   {
      // scrolling while a click is in progress would move the clicked row
      String[] items = new String[50];
      for (int i = 0; i < items.length; i++)
         items[i] = "item" + i;
      StringTable table = createTable(items);
      table.setVerticalScrollPosition(400);
      assertEquals(400, table.getVerticalScrollPosition());

      table.setFocus(true);

      assertEquals(getOptions(table).get(0).getId(), getActiveDescendant(table));
      assertEquals(400, table.getVerticalScrollPosition());
   }

   // More rows than RowTable renders at once (MAX_VISIBLE_ROWS = 500).
   private static final int LONG_TABLE_ROWS = 600;

   private StringTable createLongTable()
   {
      String[] items = new String[LONG_TABLE_ROWS];
      for (int i = 0; i < items.length; i++)
         items[i] = "item" + i;
      return createTable(items);
   }

   // Scrolls to the given row and redraws at once, as the scroll timer would.
   private static void scrollToRow(StringTable table, int row)
   {
      table.setVerticalScrollPosition((int) (row * table.getRowHeight()));
      table.redraw();
   }

   private static List<Element> getAriaSelectedRows(RowTable<String> table)
   {
      List<Element> selected = new ArrayList<>();
      NodeList<Element> rows = getTableElement(table).getElementsByTagName("tr");
      for (int i = 0; i < rows.getLength(); i++)
      {
         if (rows.getItem(i).hasAttribute("aria-selected"))
            selected.add(rows.getItem(i));
      }
      return selected;
   }

   private static void assertActiveDescendantRendered(RowTable<String> table, int row)
   {
      String id = getActiveDescendant(table);
      Element el = Document.get().getElementById(id);
      assertNotNull("aria-activedescendant '" + id + "' names no element", el);
      assertEquals(String.valueOf(row), el.getAttribute("__row"));
   }

   public void testActiveOptionVirtualizedAwayIsNotReferenced()
   {
      StringTable table = createLongTable();
      table.setFocus(true);
      assertActiveDescendantRendered(table, 0);

      scrollToRow(table, 400);
      assertFalse(getTableElement(table).hasAttribute("aria-activedescendant"));

      scrollToRow(table, 0);
      assertActiveDescendantRendered(table, 0);
   }

   public void testSelectedOptionVirtualizedAwayIsNotReferenced()
   {
      StringTable table = createLongTable();
      table.selectRow(0);
      assertActiveDescendantRendered(table, 0);

      scrollToRow(table, 400);
      assertFalse(getTableElement(table).hasAttribute("aria-activedescendant"));
      assertEquals("item0", table.getSelectedItem());

      scrollToRow(table, 0);
      assertActiveDescendantRendered(table, 0);
   }

   public void testRedrawKeepsOneOptionAriaSelected()
   {
      // scrolling shifts which row element shows the selected row
      StringTable table = createLongTable();
      table.selectRow(120, RowTable.ScrollType.NONE);

      scrollToRow(table, 400);

      List<Element> selected = getAriaSelectedRows(table);
      assertEquals(1, selected.size());
      assertEquals("120", selected.get(0).getAttribute("__row"));
      assertActiveDescendantRendered(table, 120);
   }

   public void testSelectingRowPastRenderedWindowMarksNoElement()
   {
      // row 500 is the first row outside the initial window of 0..499
      StringTable table = createLongTable();

      table.selectRow(500, RowTable.ScrollType.NONE);

      assertEquals("item500", table.getSelectedItem());
      assertEquals(0, getAriaSelectedRows(table).size());
      assertFalse(getTableElement(table).hasAttribute("aria-activedescendant"));
   }

   private static void doubleClick(Element el)
   {
      el.dispatchEvent(Document.get().createDblClickEvent(
            0, 0, 0, 0, 0, false, false, false, false));
   }

   private static List<String> recordCommits(RowTable<String> table)
   {
      List<String> commits = new ArrayList<>();
      table.addSelectionCommitHandler(event -> commits.add(event.getSelectedItem()));
      return commits;
   }

   public void testDoubleClickOnRowCommitsIt()
   {
      StringTable table = createTable("a", "b", "c");
      List<String> commits = recordCommits(table);
      // a real click lands on a cell, not the row itself
      Element cell = getOptions(table).get(1).getFirstChildElement();

      cell.dispatchEvent(Document.get().createClickEvent(
            1, 0, 0, 0, 0, false, false, false, false));
      doubleClick(cell);

      assertEquals(Arrays.asList("b"), commits);
   }

   public void testDoubleClickOffTheRowsCommitsNothing()
   {
      // e.g. blank space below a short list's rows
      StringTable table = createTable("a", "b", "c");
      table.selectRow(1);
      List<String> commits = recordCommits(table);

      doubleClick(table.getElement());

      assertTrue(commits.isEmpty());
   }

   public void testDoubleClickOffTheRowsWithNoSelectionCommitsNothing()
   {
      StringTable table = createTable("a", "b", "c");
      List<String> commits = recordCommits(table);

      doubleClick(table.getElement());

      assertTrue(commits.isEmpty());
   }

   public void testEmptyStateFollowsRowsAppendedToTheSharedList()
   {
      // DirectoryContentsWidget.addDirectory appends to the list it passed to
      // draw(), then selects the new row, without redrawing
      List<String> data = new ArrayList<>();
      StringTable table = new StringTable();
      RootPanel.get().add(table);
      table.draw(data);
      String empty = GWT.<RowTable.Resources>create(RowTable.Resources.class).styles().empty();
      assertTrue(table.getElement().hasClassName(empty));

      data.add("new folder");
      table.selectRow(0);

      assertFalse(table.getElement().hasClassName(empty));
   }

   public void testRedrawWithoutFocusLeavesNoActiveOption()
   {
      StringTable table = createTable("a", "b", "c");

      table.clear();
      table.draw(new ArrayList<>(Arrays.asList("x", "y")));

      assertEquals("", getActiveDescendant(table));
   }
}
