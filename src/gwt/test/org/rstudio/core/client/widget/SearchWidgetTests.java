/*
 * SearchWidgetTests.java
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
import java.util.List;

import org.rstudio.core.client.command.BaseMenuBar;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.RootPanel;

public class SearchWidgetTests extends GWTTestCase
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

   private static SearchWidget createSuggestingWidget()
   {
      MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
      oracle.add("apple");
      return new SearchWidget("Search", oracle);
   }

   private static void pressKey(SearchWidget widget, int keyCode)
   {
      widget.getInputElement().dispatchEvent(Document.get().createKeyDownEvent(
            false, false, false, false, keyCode));
   }

   public void testFilterOnlyBoxIsNotCombobox()
   {
      Element input = new SearchWidget("Filter").getInputElement();
      assertEquals("", input.getAttribute("role"));
      assertEquals("", input.getAttribute("aria-expanded"));
      assertEquals("", input.getAttribute("aria-autocomplete"));
   }

   public void testSuggestingBoxIsCombobox()
   {
      Element input = createSuggestingWidget().getInputElement();
      assertEquals("combobox", input.getAttribute("role"));
      assertEquals("false", input.getAttribute("aria-expanded"));
   }

   // SearchWidget's Escape handler must run before SuggestBox's, which closes the
   // list: otherwise the first Escape would also close the search, such as Go to
   // File/Function, rather than only the list.
   public void testFirstEscapeClosesListOnly()
   {
      final SearchWidget widget = createSuggestingWidget();
      RootPanel.get().add(widget);
      final List<Object> closes = new ArrayList<>();
      widget.addCloseHandler(event -> closes.add(event.getTarget()));

      widget.setText("app");
      pressKey(widget, KeyCodes.KEY_DOWN);
      assertTrue(widget.getSuggestionDisplay().isSuggestionListShowing());

      pressKey(widget, KeyCodes.KEY_ESCAPE);
      delayTestFinish(5000);
      Scheduler.get().scheduleDeferred(() ->
      {
         assertFalse(widget.getSuggestionDisplay().isSuggestionListShowing());
         assertEquals("", widget.getText());
         assertTrue(closes.isEmpty());

         pressKey(widget, KeyCodes.KEY_ESCAPE);
         Scheduler.get().scheduleDeferred(() ->
         {
            assertEquals(1, closes.size());
            finishTest();
         });
      });
   }

   public void testControlMenuTracksHighlight()
   {
      SearchWidget widget = new SearchWidget("Search");
      BaseMenuBar menu = new BaseMenuBar(true);
      MenuItem item = menu.addItem("Item", () -> {});
      widget.controlMenu(menu.getElement(), menu);

      Element input = widget.getInputElement();
      assertEquals("combobox", input.getAttribute("role"));
      assertEquals("true", input.getAttribute("aria-expanded"));
      assertFalse(menu.getElement().getId().isEmpty());
      assertEquals(menu.getElement().getId(), input.getAttribute("aria-controls"));
      assertEquals("", input.getAttribute("aria-activedescendant"));

      menu.selectItem(item);
      assertEquals(item.getElement().getId(), input.getAttribute("aria-activedescendant"));

      menu.selectItem(null);
      assertEquals("", input.getAttribute("aria-activedescendant"));
   }
}
