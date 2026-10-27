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

   public void testRedrawWithoutFocusLeavesNoActiveOption()
   {
      StringTable table = createTable("a", "b", "c");

      table.clear();
      table.draw(new ArrayList<>(Arrays.asList("x", "y")));

      assertEquals("", getActiveDescendant(table));
   }
}
