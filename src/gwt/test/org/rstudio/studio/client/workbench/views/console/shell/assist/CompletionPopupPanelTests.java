/*
 * CompletionPopupPanelTests.java
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
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.Rectangle;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.QualifiedName;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;

import com.google.gwt.junit.client.GWTTestCase;

public class CompletionPopupPanelTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   @Override
   protected void gwtSetUp()
   {
      announced_ = new ArrayList<>();
      events_ = new EventBus(null, null);
      events_.addHandler(AriaLiveStatusEvent.TYPE,
                         event -> announced_.add(event.getMessage()));
   }

   @Override
   protected void gwtTearDown()
   {
      if (panel_ != null)
         panel_.hide();
   }

   private CompletionPopupPanel newPanel()
   {
      AriaLiveService ariaLive = new AriaLiveService(events_, null)
      {
         @Override
         public boolean isDisabled(String id)
         {
            return false;
         }
      };
      return new CompletionPopupPanel(null, ariaLive);
   }

   private static Rectangle viewport()
   {
      return new Rectangle(0, 0, 800, 3000);
   }

   private static QualifiedName[] completions()
   {
      return new QualifiedName[] {
            new QualifiedName("library", "base"),
            new QualifiedName("library.dynam", "base"),
            new QualifiedName("library.dynam.unload", "base"),
      };
   }

   // Shows the completions, refreshing the panel if it is already showing.
   private void show(Rectangle line)
   {
      // Prefers the top, as when an AI assistant is enabled, within an
      // editor whose text area starts at the top of the page.
      if (panel_ == null)
         panel_ = newPanel();
      panel_.showCompletionValues(
            completions(),
            new PopupPositioner(line, panel_, false, viewport()),
            false);
   }

   public void testListRunsTopDownWhenNoRoomAbove()
   {
      // #19614: on the editor's first line the popup goes below, so the best
      // match belongs at its top, next to the line, and starts selected.
      Rectangle line = new Rectangle(10, 0, 8, 13);
      show(line);

      assertTrue(panel_.getPopupTop() > line.getBottom());
      assertEquals("library", panel_.getItems()[0].name);
      assertEquals("library", panel_.getSelectedValue().name);
   }

   public void testListRunsBottomUpWhenAbove()
   {
      Rectangle line = new Rectangle(10, 2000, 8, 13);
      show(line);

      QualifiedName[] items = panel_.getItems();
      assertTrue(panel_.getPopupTop() < line.getTop());
      assertEquals("library", items[items.length - 1].name);
      assertEquals("library", panel_.getSelectedValue().name);
   }

   // Records the side the panel asks it to keep.
   private static class RecordingPositioner extends PopupPositioner
   {
      RecordingPositioner(Rectangle line, CompletionPopupDisplay popup)
      {
         super(line, popup, false, viewport());
      }

      @Override
      public void setKeepSide(Boolean below)
      {
         kept = below;
         super.setKeepSide(below);
      }

      Boolean kept;
   }

   private RecordingPositioner showRecording(Rectangle line)
   {
      RecordingPositioner positioner = new RecordingPositioner(line, panel_);
      panel_.showCompletionValues(completions(), positioner, false);
      return positioner;
   }

   public void testRefreshKeepsSide()
   {
      // Opened with no room above, the popup goes below; refreshed while
      // showing, it asks to stay there.
      Rectangle line = new Rectangle(10, 0, 8, 13);
      panel_ = newPanel();
      assertNull(showRecording(line).kept);
      assertEquals(Boolean.TRUE, showRecording(line).kept);
   }

   public void testReopenedPopupChoosesSideAfresh()
   {
      Rectangle line = new Rectangle(10, 0, 8, 13);
      panel_ = newPanel();
      showRecording(line);
      panel_.hide();
      assertNull(showRecording(line).kept);

      // An offscreen popup is still attached, but no longer has a side.
      panel_.placeOffscreen();
      assertNull(showRecording(line).kept);
   }

   public void testListAfterMessageKeepsMessageSide()
   {
      // A list shown above the line is dismissed, then "No matches" opens
      // below it; the list that replaces the message keeps the message's
      // side, not the dismissed list's.
      panel_ = newPanel();
      showRecording(new Rectangle(10, 2000, 8, 13));
      panel_.hide();
      Rectangle line = new Rectangle(10, 0, 8, 13);
      showMessage("(No matches)");
      assertEquals(Boolean.TRUE, showRecording(line).kept);
   }

   private void showMessage(String message)
   {
      Rectangle line = new Rectangle(10, 0, 8, 13);
      panel_.showErrorMessage(
            message,
            new PopupPositioner(line, panel_, false, viewport()));
   }

   private void showList(boolean truncated)
   {
      Rectangle line = new Rectangle(10, 0, 8, 13);
      panel_.showCompletionValues(
            completions(),
            new PopupPositioner(line, panel_, false, viewport()),
            truncated);
   }

   public void testMessageIsAnnounced()
   {
      panel_ = newPanel();
      showMessage("(No matches)");
      assertEquals(1, announced_.size());
      assertEquals("(No matches)", announced_.get(0));
   }

   public void testRefreshedMessageIsAnnouncedOnce()
   {
      // Refreshed as the user types, the popup shows the same message
      // again; it's announced only when it changes.
      panel_ = newPanel();
      showMessage("(No matches)");
      showMessage("(No matches)");
      assertEquals(1, announced_.size());

      showMessage("Error: timed out");
      assertEquals(2, announced_.size());
      assertEquals("Error: timed out", announced_.get(1));
   }

   public void testMessageIsAnnouncedAgainAfterHide()
   {
      panel_ = newPanel();
      showMessage("(No matches)");
      panel_.hide();
      showMessage("(No matches)");
      assertEquals(2, announced_.size());

      panel_.placeOffscreen();
      showMessage("(No matches)");
      assertEquals(3, announced_.size());
   }

   public void testEmptyMessageIsNotAnnounced()
   {
      panel_ = newPanel();
      showMessage("");
      assertTrue(announced_.isEmpty());
   }

   public void testTruncatedListIsAnnouncedOnce()
   {
      panel_ = newPanel();
      showList(false);
      assertTrue(announced_.isEmpty());

      showList(true);
      showList(true);
      assertEquals(1, announced_.size());
      assertEquals("Not all items shown", announced_.get(0));

      // Once the list is complete again, a later truncation is news.
      showList(false);
      showList(true);
      assertEquals(2, announced_.size());
   }

   public void testMessageAfterListIsAnnounced()
   {
      // A message that replaces a list is announced even if it was the
      // message the popup showed before the list.
      panel_ = newPanel();
      showMessage("(No matches)");
      showList(false);
      showMessage("(No matches)");
      assertEquals(2, announced_.size());
   }

   public void testRefreshSwitchesSideWhenItNoLongerFits()
   {
      show(new Rectangle(10, 2000, 8, 13));
      Rectangle line = new Rectangle(10, 0, 8, 13);
      show(line);

      assertTrue(panel_.getPopupTop() > line.getBottom());
      assertEquals("library", panel_.getItems()[0].name);
      assertEquals("library", panel_.getSelectedValue().name);
   }

   // Returns the list items that have a description.
   private List<Element> describedItems()
   {
      List<Element> described = new ArrayList<>();
      NodeList<Element> rows = panel_.getElement().getElementsByTagName("tr");
      for (int i = 0; i < rows.getLength(); i++)
      {
         if (rows.getItem(i).hasAttribute("aria-describedby"))
            described.add(rows.getItem(i));
      }
      return described;
   }

   public void testShownHelpDescribesSelectedItem()
   {
      show(new Rectangle(10, 0, 8, 13));
      panel_.displaySnippetHelp("library(${1:package})");

      List<Element> described = describedItems();
      assertEquals(1, described.size());
      assertEquals("true", described.get(0).getAttribute("aria-selected"));
      Element help = Document.get().getElementById(
            described.get(0).getAttribute("aria-describedby"));
      assertTrue(help.getInnerText().contains("library(${1:package})"));
   }

   public void testMovingSelectionUnlinksHelp()
   {
      // The help shown is for the item it was shown with; help for the
      // newly selected item arrives later.
      show(new Rectangle(10, 0, 8, 13));
      panel_.displaySnippetHelp("library(${1:package})");
      panel_.selectNext();
      assertTrue(describedItems().isEmpty());
   }

   public void testRefreshedListDoesNotInheritHelp()
   {
      // A list refreshed as the user types keeps showing the help it had
      // until help for its new selection arrives; that help isn't the
      // new selection's.
      Rectangle line = new Rectangle(10, 0, 8, 13);
      show(line);
      panel_.displaySnippetHelp("library(${1:package})");
      show(line);
      assertTrue(describedItems().isEmpty());
   }

   public void testHiddenHelpDoesNotDescribe()
   {
      show(new Rectangle(10, 0, 8, 13));
      panel_.displaySnippetHelp("");
      assertTrue(describedItems().isEmpty());

      panel_.displaySnippetHelp("library(${1:package})");
      panel_.clearHelp(false);
      assertTrue(describedItems().isEmpty());

      panel_.displaySnippetHelp("library(${1:package})");
      panel_.setHelpVisible(false);
      assertTrue(describedItems().isEmpty());
   }

   private CompletionPopupPanel panel_;
   private EventBus events_;
   private List<String> announced_;
}
