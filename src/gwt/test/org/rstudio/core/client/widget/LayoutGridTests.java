/*
 * LayoutGridTests.java
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

import org.rstudio.core.client.widget.LayoutGrid.TwoColumnLayoutGridBuilder;

import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class LayoutGridTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   // Build a one-row grid and return its label's for= attribute.
   private static String labelFor(String text, Widget widget)
   {
      TwoColumnLayoutGridBuilder builder = new TwoColumnLayoutGridBuilder();
      builder.add(text, widget);
      LayoutGrid grid = builder.get();
      return grid.getWidget(0, 0).getElement().getAttribute("for");
   }

   private static Element onlyChild(Widget widget, String tagName)
   {
      return widget.getElement().getElementsByTagName(tagName).getItem(0);
   }

   public void testLabelTargetsNumericValueWidgetInput()
   {
      NumericValueWidget numeric = new NumericValueWidget();
      String forId = labelFor("Maximum lines", numeric);

      Element input = onlyChild(numeric, "input");
      assertFalse(input.getId().isEmpty());
      assertEquals(input.getId(), forId);
   }

   public void testLabelTargetsSelectWidgetList()
   {
      SelectWidget select = new SelectWidget(
            SelectWidget.ExternalLabel, new String[] { "a", "b" }, null, false);
      String forId = labelFor("ANSI escape codes", select);

      Element list = onlyChild(select, "select");
      assertFalse(list.getId().isEmpty());
      assertEquals(list.getId(), forId);
   }

   public void testLabelKeepsExistingControlId()
   {
      SelectWidget select = new SelectWidget(
            SelectWidget.ExternalLabel, new String[] { "a", "b" }, null, false);
      select.getListBox().getElement().setId("existing_control_id");

      assertEquals("existing_control_id", labelFor("Mode", select));
      assertEquals("existing_control_id", select.getListBox().getElement().getId());
   }

   public void testLabelTargetsPlainControl()
   {
      TextBox textBox = new TextBox();
      String forId = labelFor("Name", textBox);

      assertFalse(textBox.getElement().getId().isEmpty());
      assertEquals(textBox.getElement().getId(), forId);
   }
}
