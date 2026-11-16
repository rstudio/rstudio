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
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.Timer;
import org.rstudio.core.client.dom.DomUtils;
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
      changes_ = 0;
      dismissals_ = 0;
      focusReturns_ = 0;
      announcementsDisabled_ = true;
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
            return announcementsDisabled_;
         }
      };
      return new WarningBarList(() ->
      {
         WarningBar bar = new WarningBar(events_);
         bars_.add(bar);
         return bar;
      }, aria, () -> changes_++, () -> dismissals_++, () -> focusReturns_++);
   }

   private void reload()
   {
      String state = list_.getDismissedState();
      list_.removeFromParent();
      list_ = createList();
      RootPanel.get().add(list_);
      list_.restoreDismissedState(state);
   }

   private String announced(String role)
   {
      Element container = list_.getAnnouncementWidget().getElement();
      for (Element child = container.getFirstChildElement(); child != null; child = child.getNextSiblingElement())
      {
         if (role.equals(child.getAttribute("role")))
            return child.getInnerText();
      }
      fail("No " + role + " region");
      return null;
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

   public void testRefreshingARowDoesNotRelayout()
   {
      show("first", "First");
      show("first", "First, updated");
      list_.show("first", false, "First, updated", false, "Action", () -> {});
      assertEquals(1, changes_);
      show("second", "Second");
      assertEquals(2, changes_);
      list_.hide("second");
      list_.hide("second");
      assertEquals(3, changes_);
   }

   public void testDismissalSurvivesReconnectButChangedWarningAppears()
   {
      show("first", "First warning");
      show("second", "Second warning");
      list_.dismiss("first");
      reload();
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

   public void testStateKeyedDismissalIgnoresLiveFigures()
   {
      list_.show("memory", "approaching", true, "Approaching: 90 MB", false, null, null);
      list_.show("memory", "approaching", true, "Approaching: 91 MB", false, null, null);
      assertEquals(1, bars_.size());
      assertEquals("Approaching: 91 MB", bars_.get(0).getText());
      list_.dismiss("memory");
      reload();
      list_.show("memory", "approaching", true, "Approaching: 95 MB", false, null, null);
      assertFalse(list_.contains("memory"));
      list_.show("memory", "over", true, "Over: 110 MB", false, null, null);
      assertTrue(list_.contains("memory"));
   }

   public void testUndismissAllowsAnExplicitRequestOnly()
   {
      show("first", "First warning");
      list_.dismiss("first");
      show("first", "First warning");
      assertFalse(list_.contains("first"));
      list_.undismiss("first");
      show("first", "First warning");
      assertTrue(list_.contains("first"));

      // Undismissing a showing row leaves it in place.
      list_.undismiss("first");
      show("first", "First warning");
      assertEquals(2, bars_.size());
      assertEquals(1, closed_.size());
   }

   public void testLateStateRestoreRemovesOnlyMatchingMessage()
   {
      show("first", "First warning");
      list_.dismiss("first");
      String state = list_.getDismissedState();
      list_.hide("first");
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
      for (int i = 0; i < 10; i++)
         list_.hide("warning-" + i);
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

   public void testRemovingFocusedRowsSelectsNextThenPreviousThenWorkbench()
   {
      show("first", "First");
      show("middle", "Middle");
      show("last", "Last");
      bars_.get(1).focusDismissButton();
      list_.dismiss("middle");
      assertEquals(bars_.get(2).dismiss_.getElement(), DomUtils.getActiveElement());
      list_.hide("last");
      assertEquals(bars_.get(0).dismiss_.getElement(), DomUtils.getActiveElement());
      list_.dismiss("first");
      assertEquals(1, focusReturns_);
   }

   public void testBackgroundRemovalDoesNotMoveFocus()
   {
      TextBox editor = new TextBox();
      RootPanel.get().add(editor);
      try
      {
         show("first", "First");
         editor.setFocus(true);
         list_.hide("first");
         assertEquals(editor.getElement(), DomUtils.getActiveElement());
         assertEquals(0, focusReturns_);
      }
      finally
      {
         editor.removeFromParent();
      }
   }

   public void testLabelsFollowRowPosition()
   {
      show("first", "First <b>warning</b>");
      show("second", "Second warning");
      WarningBar first = bars_.get(0);
      WarningBar second = bars_.get(1);
      assertEquals("Warning 1", first.getElement().getAttribute("aria-label"));
      assertEquals("Warning 2", second.getElement().getAttribute("aria-label"));
      assertEquals("Dismiss warning 1", first.dismiss_.getElement().getInnerText());
      assertEquals("Dismiss warning 2", second.dismiss_.getElement().getInnerText());
      assertEquals(first.label_.getId(), first.dismiss_.getElement().getAttribute("aria-describedby"));
      assertFalse(first.label_.hasAttribute("aria-hidden"));

      list_.dismiss("first");
      assertEquals("Warning 1", second.getElement().getAttribute("aria-label"));
      assertEquals("Dismiss warning 1", second.dismiss_.getElement().getInnerText());
      show("third", "Third warning");
      assertEquals("Warning 2", bars_.get(2).getElement().getAttribute("aria-label"));
   }

   public void testDetailsKeepLinksThatTheRowTakesOutOfTabOrder()
   {
      show("first", "See <a href=\"https://example.com/\">the guide</a>");
      WarningBar bar = bars_.get(0);
      assertEquals("See the guide", bar.label_.getTitle());
      assertEquals(-1, bar.label_.getElementsByTagName("a").getItem(0).getTabIndex());

      Element link = bar.createDetailsMessage().getElement().getElementsByTagName("a").getItem(0);
      assertEquals("https://example.com/", link.getAttribute("href"));
      assertFalse(link.hasAttribute("tabindex"));

      show("first", "Updated <b>warning</b>");
      assertEquals("Updated warning", bar.createDetailsMessage().getElement().getInnerText());
   }

   public void testAnnouncementsBatchLatestMessagesAndExcludeRemovedRows()
   {
      announcementsDisabled_ = false;
      show("first", "Old message");
      show("first", "Updated <b>message</b>");
      show("second", "Second message");
      list_.show("severe", true, "Severe message", false, null, null);
      show("dismissed", "Dismissed message");
      list_.dismiss("dismissed");
      show("resolved", "Resolved message");
      list_.hide("resolved");
      delayTestFinish(4000);
      new Timer()
      {
         @Override
         public void run()
         {
            String expected = "Updated message\nSecond message";
            assertEquals(expected, announced("status"));
            assertEquals("Severe message", announced("alert"));
            show("first", "Updated <b>message</b>");
            // An exact duplicate neither clears nor schedules a new announcement.
            assertEquals(expected, announced("status"));
            finishTest();
         }
      }.schedule(1500);
   }

   public void testStateKeyedUpdatesAreNotReannounced()
   {
      announcementsDisabled_ = false;
      list_.show("memory", "approaching", true, "Approaching: 90 MB", false, null, null);
      delayTestFinish(6000);
      new Timer()
      {
         @Override
         public void run()
         {
            assertEquals("Approaching: 90 MB", announced("alert"));
            list_.show("memory", "approaching", true, "Approaching: 91 MB", false, null, null);
            assertEquals("Approaching: 91 MB", bars_.get(0).getText());
            assertEquals("Approaching: 90 MB", announced("alert"));

            list_.show("memory", "over", true, "Over: 110 MB", false, null, null);
            assertEquals("", announced("alert"));
            new Timer()
            {
               @Override
               public void run()
               {
                  assertEquals("Over: 110 MB", announced("alert"));
                  finishTest();
               }
            }.schedule(1500);
         }
      }.schedule(1500);
   }

   public void testDisabledAnnouncementsStillExposeMessages()
   {
      show("first", "Visible message");
      assertEquals("Visible message", bars_.get(0).getText());
      delayTestFinish(4000);
      new Timer()
      {
         @Override
         public void run()
         {
            assertEquals("", list_.getAnnouncementWidget().getElement().getInnerText());
            finishTest();
         }
      }.schedule(1500);
   }

   private boolean announcementsDisabled_;
   private int focusReturns_;
   private int changes_;
   private WarningBarList list_;
   private EventBus events_;
   private List<WarningBar> bars_;
   private List<String> closed_;
   private int dismissals_;
}
