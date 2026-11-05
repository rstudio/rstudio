/*
 * WarningBarListTests.java
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
package org.rstudio.studio.client.application.ui;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.RootPanel;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.WarningBarClosedEvent;

public class WarningBarListTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   @Override
   protected void gwtSetUp()
   {
      bars_ = new ArrayList<>();
      closed_ = new ArrayList<>();
      dismissals_ = 0;
      events_ = new EventBus(null, null);
      events_.addHandler(WarningBarClosedEvent.TYPE, event -> closed_.add(event.getId()));
      list_ = createList();
      RootPanel.get().add(list_);
   }

   @Override
   protected void gwtTearDown()
   {
      list_.removeFromParent();
   }

   private WarningBarList createList()
   {
      AriaLiveService aria = new AriaLiveService(events_, null)
      {
         @Override
         public boolean isDisabled(String id)
         {
            return true;
         }
      };
      return new WarningBarList(() ->
      {
         WarningBar bar = new WarningBar(events_, aria);
         bars_.add(bar);
         return bar;
      }, () -> {}, () -> dismissals_++);
   }

   private void show(String id, String message)
   {
      list_.show(id, false, message, false, null, null);
   }

   private static void click(Element element)
   {
      element.dispatchEvent(Document.get().createClickEvent(1, 0, 0, 0, 0, false, false, false, false));
   }

   public void testMessagesKeepTheirOwnActionsAndDismissButtons()
   {
      int[] actions = {0, 0};
      list_.show("first", false, "First warning", false, "First action", () -> actions[0]++);
      list_.show("second", true, "Second warning", false, "Second action", () -> actions[1]++);
      assertEquals(56, list_.getPreferredHeight());
      assertEquals("First warning", bars_.get(0).label_.getInnerText());
      assertEquals("Second warning", bars_.get(1).label_.getInnerText());
      click(bars_.get(0).actionLink_.getElement());
      click(bars_.get(1).actionLink_.getElement());
      assertEquals(1, actions[0]);
      assertEquals(1, actions[1]);

      click(bars_.get(0).dismiss_.getElement());
      assertFalse(list_.contains("first"));
      assertTrue(list_.contains("second"));
      assertEquals(28, list_.getPreferredHeight());
      assertEquals(1, dismissals_);
      assertEquals("first", closed_.get(0));
      click(bars_.get(1).actionLink_.getElement());
      assertEquals(2, actions[1]);
   }

   public void testRepeatedAndUpdatedWarningsReuseTheirRow()
   {
      int[] actions = {0};
      list_.show("quota", false, "Near quota", false, "Old action", () -> actions[0] += 100);
      list_.show("quota", true, "Over quota", false, "New action", () -> actions[0]++);
      list_.show("quota", true, "Over quota", false, "New action", () -> actions[0]++);
      assertEquals(1, bars_.size());
      assertEquals("Over quota", bars_.get(0).label_.getInnerText());
      click(bars_.get(0).actionLink_.getElement());
      assertEquals(1, actions[0]);
      show("quota", "No action");
      assertFalse(bars_.get(0).actionLink_.isVisible());
   }

   public void testDismissalSurvivesReconnectButChangedWarningAppears()
   {
      show("first", "First warning");
      show("second", "Second warning");
      list_.dismiss("first");
      String state = list_.getDismissedState();
      list_.removeFromParent();
      list_ = createList();
      RootPanel.get().add(list_);
      list_.restoreDismissedState(state);
      show("first", "First warning");
      show("second", "Second warning");
      assertFalse(list_.contains("first"));
      assertTrue(list_.contains("second"));
      show("first", "Changed warning");
      assertTrue(list_.contains("first"));
      list_.dismiss("first");
      list_.show("first", true, "Changed warning", false, null, null);
      assertTrue(list_.contains("first"));
   }

   public void testLateStateRestoreRemovesOnlyMatchingMessage()
   {
      show("first", "First warning");
      list_.dismiss("first");
      String state = list_.getDismissedState();
      list_.clear();
      show("first", "First warning");
      show("second", "Second warning");
      list_.restoreDismissedState(state);
      assertFalse(list_.contains("first"));
      assertTrue(list_.contains("second"));
   }

   public void testResolveOnlyRemovesItsOwnWarningAndAllowsRecurrence()
   {
      show("license", "License warning");
      show("quota", "Quota warning");
      list_.dismiss("license");
      list_.hide("license");
      show("license", "License warning");
      assertTrue(list_.contains("license"));
      assertTrue(list_.contains("quota"));
      list_.hide("license");
      assertFalse(list_.contains("license"));
      assertTrue(list_.contains("quota"));
      assertEquals(1, dismissals_);
   }

   public void testHeightIsBoundedAndReturnsToZero()
   {
      for (int i = 0; i < 10; i++)
         show("warning-" + i, "Warning " + i);
      assertEquals(84, list_.getPreferredHeight());
      for (int i = 0; i < 10; i++)
         assertTrue(list_.contains("warning-" + i));
      list_.clear();
      assertEquals(0, list_.getPreferredHeight());
   }

   public void testInvalidSavedStateDoesNotBreakWarnings()
   {
      list_.restoreDismissedState("invalid json");
      list_.restoreDismissedState("[]");
      list_.restoreDismissedState("{\"first\": 3}");
      show("first", "First warning");
      assertTrue(list_.contains("first"));
      assertFalse(WarningBarList.messageId("ab", "c").equals(WarningBarList.messageId("a", "bc")));
   }

   private WarningBarList list_;
   private EventBus events_;
   private List<WarningBar> bars_;
   private List<String> closed_;
   private int dismissals_;
}
