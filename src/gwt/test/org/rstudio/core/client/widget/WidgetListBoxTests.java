/*
 * WidgetListBoxTests.java
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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

public class WidgetListBoxTests extends GWTTestCase
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

   private static WidgetListBox<Label> attachedList()
   {
      WidgetListBox<Label> list = new WidgetListBox<>();
      RootPanel.get().add(list);
      return list;
   }

   private static String describedBy(WidgetListBox<Label> list)
   {
      return list.getElement().getAttribute("aria-describedby");
   }

   // A listbox may only contain options, so the empty message must be
   // hidden from screen readers and conveyed as the list's description.
   public void testEmptyMessageDescribesList()
   {
      WidgetListBox<Label> list = attachedList();
      list.setEmptyText("No accounts connected");

      assertEquals("listbox", list.getElement().getAttribute("role"));
      String id = describedBy(list);
      assertFalse(id.isEmpty());
      Element message = Document.get().getElementById(id);
      assertNotNull(message);
      assertEquals("No accounts connected", message.getInnerText());

      Element hidden = message;
      while (hidden != null && !"true".equals(hidden.getAttribute("aria-hidden")))
         hidden = hidden.getParentElement();
      assertNotNull("empty message is not aria-hidden", hidden);
      assertTrue(list.getElement().isOrHasChild(hidden));
   }

   public void testItemsRemoveEmptyDescription()
   {
      WidgetListBox<Label> list = attachedList();
      list.setEmptyText("No accounts connected");

      list.addItem(new Label("account"));
      assertEquals("", describedBy(list));

      list.clearItems();
      assertFalse(describedBy(list).isEmpty());
   }

   public void testNoEmptyTextNoDescription()
   {
      WidgetListBox<Label> list = attachedList();
      list.addItem(new Label("account"));
      list.removeItem(0);

      assertEquals("", describedBy(list));
   }
}
