/*
 * ToolbarKeyboardNavigationTests.java
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

import org.rstudio.core.client.dom.DomUtils;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;

public class ToolbarKeyboardNavigationTests extends GWTTestCase
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

   private static Toolbar attach(Toolbar toolbar)
   {
      RootPanel.get().add(toolbar);
      return toolbar;
   }

   private static Button button(Toolbar toolbar, String text)
   {
      return toolbar.addLeftWidget(new Button(text));
   }

   private static void press(FocusWidget widget, int keyCode)
   {
      press(widget.getElement(), keyCode, false);
   }

   private static void press(Element el, int keyCode, boolean shift)
   {
      el.dispatchEvent(Document.get().createKeyDownEvent(
            false, false, shift, false, keyCode));
   }

   private static void assertFocused(FocusWidget widget)
   {
      assertEquals(widget.getElement(), DomUtils.getActiveElement());
   }

   private static void assertTabStops(FocusWidget[] widgets, boolean[] stops)
   {
      for (int i = 0; i < widgets.length; i++)
      {
         assertEquals("tab index of item " + i,
               stops[i] ? 0 : -1, widgets[i].getElement().getTabIndex());
      }
   }


   public void testOnlyFirstButtonIsTabStop()
   {
      Toolbar toolbar = new Toolbar("Test");
      Button a = button(toolbar, "a");
      Button b = button(toolbar, "b");
      Button c = toolbar.addRightWidget(new Button("c"));
      attach(toolbar);

      assertTabStops(new FocusWidget[] {a, b, c}, new boolean[] {true, false, false});
   }

   public void testArrowsMoveWithoutWrapping()
   {
      Toolbar toolbar = attach(new Toolbar("Test"));
      Button a = button(toolbar, "a");
      toolbar.addLeftSeparator();
      Button b = button(toolbar, "b");
      Button c = toolbar.addRightWidget(new Button("c"));

      a.setFocus(true);
      press(a, KeyCodes.KEY_LEFT);
      assertFocused(a);
      press(a, KeyCodes.KEY_RIGHT);
      assertFocused(b);
      press(b, KeyCodes.KEY_RIGHT);
      assertFocused(c);
      press(c, KeyCodes.KEY_RIGHT);
      assertFocused(c);
      press(c, KeyCodes.KEY_HOME);
      assertFocused(a);
      press(a, KeyCodes.KEY_END);
      assertFocused(c);
      press(c, KeyCodes.KEY_LEFT);
      assertFocused(b);
   }

   public void testModifiedArrowDoesNotMove()
   {
      Toolbar toolbar = attach(new Toolbar("Test"));
      Button a = button(toolbar, "a");
      button(toolbar, "b");

      a.setFocus(true);
      press(a.getElement(), KeyCodes.KEY_RIGHT, true);
      assertFocused(a);
   }

   public void testFocusedButtonBecomesTabStop()
   {
      Toolbar toolbar = attach(new Toolbar("Test"));
      Button a = button(toolbar, "a");
      Button b = button(toolbar, "b");
      Button c = button(toolbar, "c");

      // Focus from code (a menu handing focus back) counts, not only arrow keys.
      b.setFocus(true);
      assertTabStops(new FocusWidget[] {a, b, c}, new boolean[] {false, true, false});

      press(b, KeyCodes.KEY_RIGHT);
      assertTabStops(new FocusWidget[] {a, b, c}, new boolean[] {false, false, true});
   }

   public void testTextFieldSplitsTabStops()
   {
      Toolbar toolbar = new Toolbar("Test");
      Button a = button(toolbar, "a");
      Button b = button(toolbar, "b");
      TextBox field = toolbar.addLeftWidget(new TextBox());
      Button c = button(toolbar, "c");
      Button d = toolbar.addRightWidget(new Button("d"));
      attach(toolbar);

      assertTabStops(new FocusWidget[] {a, b, field, c, d},
            new boolean[] {true, false, true, true, false});

      // Arrows move into the field from either side but never out of it.
      b.setFocus(true);
      press(b, KeyCodes.KEY_RIGHT);
      assertFocused(field);
      press(field, KeyCodes.KEY_RIGHT);
      press(field, KeyCodes.KEY_LEFT);
      press(field, KeyCodes.KEY_HOME);
      press(field, KeyCodes.KEY_END);
      assertFocused(field);
      c.setFocus(true);
      press(c, KeyCodes.KEY_LEFT);
      assertFocused(field);

      // Each group keeps its own tab stop.
      d.setFocus(true);
      assertTabStops(new FocusWidget[] {a, b, field, c, d},
            new boolean[] {false, true, true, false, true});
   }

   public void testCheckBoxIsAnArrowItem()
   {
      Toolbar toolbar = attach(new Toolbar("Test"));
      Button a = button(toolbar, "a");
      CheckBox check = toolbar.addLeftWidget(new CheckBox("check"));
      Button b = button(toolbar, "b");
      Element input = check.getElement().getElementsByTagName("input").getItem(0);

      a.setFocus(true);
      press(a, KeyCodes.KEY_RIGHT);
      assertEquals(input, DomUtils.getActiveElement());
      press(input, KeyCodes.KEY_RIGHT, false);
      assertFocused(b);
      assertEquals(-1, input.getTabIndex());
   }

   public void testDisabledAndHiddenButtonsAreSkipped()
   {
      Toolbar toolbar = attach(new Toolbar("Test"));
      Button a = button(toolbar, "a");
      Button disabled = button(toolbar, "disabled");
      Button hidden = button(toolbar, "hidden");
      Button b = button(toolbar, "b");
      disabled.setEnabled(false);
      hidden.setVisible(false);

      a.setFocus(true);
      press(a, KeyCodes.KEY_RIGHT);
      assertFocused(b);
      press(b, KeyCodes.KEY_LEFT);
      assertFocused(a);
   }

   public void testFocusFirstTabStopReturnsToLastFocused()
   {
      Toolbar toolbar = attach(new Toolbar("Test"));
      button(toolbar, "a");
      Button b = button(toolbar, "b");
      Button outside = new Button("outside");
      RootPanel.get().add(outside);

      b.setFocus(true);
      outside.setFocus(true);
      assertTrue(toolbar.focusFirstTabStop());
      assertFocused(b);
   }
}
