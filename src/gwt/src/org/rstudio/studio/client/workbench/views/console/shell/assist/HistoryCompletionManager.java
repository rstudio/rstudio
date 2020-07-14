/*
 * HistoryCompletionManager.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;

import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.Invalidation.Token;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.console.shell.KeyDownPreviewHandler;
import org.rstudio.studio.client.workbench.views.console.shell.KeyPressPreviewHandler;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.history.model.HistoryEntry;
import org.rstudio.studio.client.workbench.views.history.model.HistoryServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;

public class HistoryCompletionManager implements KeyDownPreviewHandler,
                                                 KeyPressPreviewHandler
{
   public enum PopupMode
   {
      PopupNone,          // popup is not showing
      PopupNoResults,     // popup is showing but with no results
      PopupIncremental,   // popup is showing incremental infix search
      PopupPrefix         // popup is showing prefix search
   }

   public HistoryCompletionManager(InputEditorDisplay input,
                                   HistoryServerOperations server)
   {
      input_ = input;
      server_ = server;
      mode_ = PopupMode.PopupNone;

      // Last search executed
      lastSearch_ = "";

      // Current offset when navigating through search results
      offset_ = -1;
   }

   public boolean previewKeyDown(NativeEvent event)
   {
      if (popup_ == null)
      {
         if (event.getKeyCode() == KeyCodes.KEY_UP
             && (event.getCtrlKey() || event.getMetaKey()))
         {
            beginSuggest();
            return true;
         }
         else if (event.getKeyCode() == KeyCodes.KEY_R && event.getCtrlKey())
         {
            beginSearch();
            return true;
         }
      }
      else
      {
         switch (event.getKeyCode())
         {
         case KeyCodes.KEY_SHIFT:
         case KeyCodes.KEY_CTRL:
         case KeyCodes.KEY_ALT:
            return false; // bare modifiers should do nothing
         }

         if (event.getKeyCode() == KeyCodes.KEY_ESCAPE)
         {
            dismiss();
            return true;
         }
         else if (event.getKeyCode() == KeyCodes.KEY_ENTER)
         {
            Position pos = input_.getCursorPosition();
            input_.setText(popup_.getSelectedValue().getHistory());
            input_.setCursorPosition(pos);
            offset_ = popup_.getSelectedValue().getIndex();
            dismiss();
            return true;
         }
         else if (event.getKeyCode() == KeyCodes.KEY_UP)
         {
            popup_.selectPrev();
            return true;
         }
         else if (event.getKeyCode() == KeyCodes.KEY_DOWN)
         {
            popup_.selectNext();
            return true;
         }
         else if (event.getKeyCode() == KeyCodes.KEY_UP)
            return popup_.selectPrev();
         else if (event.getKeyCode() == KeyCodes.KEY_DOWN)
            return popup_.selectNext();
         else if (event.getKeyCode() == KeyCodes.KEY_PAGEUP)
            return popup_.selectPrevPage();
         else if (event.getKeyCode() == KeyCodes.KEY_PAGEDOWN)
            return popup_.selectNextPage();
         else if (event.getKeyCode() == KeyCodes.KEY_HOME)
            return popup_.selectFirst();
         else if (event.getKeyCode() == KeyCodes.KEY_END)
            return popup_.selectLast();
         else if (event.getKeyCode() == KeyCodes.KEY_LEFT)
         {
            dismiss();
            return true;
         }

         if (mode_ != PopupMode.PopupIncremental)
            dismiss();

         return false;
      }

      return false;
   }

   private void dismiss()
   {
      if (popup_ != null)
      {
         popup_.hide();
         popup_ = null;
         mode_ = PopupMode.PopupNone;
      }
   }

   /**
    * Navigates through a prefix search, creating a new search if necessary.
    *
    * @param offset The offset from the current position; 1 to navigate
    * forwards, -1 for backwards.
    */
   public void navigatePrefix(final int offset)
   {
      if (StringUtil.equals(getSearchText(), lastSearch_) && lastResults_ != null)
      {
         // Navigation through existing search results
         navigateSearchPrefix(offset);
      }
      else
      {
         // No last search results; start a new search
         server_.searchHistoryArchiveByPrefix(
               getSearchText(), 20, true,
               new HistoryCallback(getSearchText(), PopupMode.PopupNone)
               {
                  @Override
                  public void onResponseReceived(RpcObjectList<HistoryEntry> entries)
                  {
                     super.onResponseReceived(entries);
                     navigateSearchPrefix(offset);
                  }
               });
      }
   }

   private void navigateSearchPrefix(int offset)
   {
      // Bounds check to be sure we don't run off the end of the search results
      int target = offset_ + offset;
      if (target >= lastResults_.length() || target < 0)
         return;

      // Pull the next entry from the result set and load into the input
      Position pos = input_.getCursorPosition();
      input_.setText(lastResults_.get(target).getCommand());
      input_.setCursorPosition(pos);
      offset_ = target;
   }

   public void beginSuggest()
   {
      server_.searchHistoryArchiveByPrefix(
            getSearchText(), 20, true,
            new HistoryCallback(getSearchText(), PopupMode.PopupPrefix));
   }

   public void beginSearch()
   {
      server_.searchHistory(
            getSearchText(), 20,
            new HistoryCallback(getSearchText(), PopupMode.PopupIncremental));
   }

   private String getSearchText()
   {
      Position pos = input_.getCursorPosition();
      String text = input_.getText();
      return text.substring(0, pos.getColumn());
   }

   public boolean previewKeyPress(char charCode)
   {
      return false;
   }

   public PopupMode getMode()
   {
      return mode_;
   }

   private class HistoryCallback
           extends SimpleRequestCallback<RpcObjectList<HistoryEntry>>
   {
      public HistoryCallback(String text, PopupMode desiredMode)
      {
         historyRequestInvalidation_.invalidate();
         token_ = historyRequestInvalidation_.getInvalidationToken();
         text_ = text;
         lastSearch_ = text;
         desiredMode_ = desiredMode;

         // If we aren't re-initiating a previous search, clear the offset
         // selection
         if (lastSearch_ != text)
            offset_ = -1;
      }

      @Override
      public void onResponseReceived(RpcObjectList<HistoryEntry> resp)
      {
         if (token_.isInvalid())
            return;

         lastResults_ = resp;

         if (mode_ != PopupMode.PopupNone)
            dismiss();

         if (desiredMode_ == PopupMode.PopupNone)
            return;

         if (resp.length() == 0)
         {
            popup_ = new CompletionListPopupPanel<HistoryMatch>(
                  new HistoryMatch[0]);
            popup_.setText("(No matching commands)");
            mode_ = PopupMode.PopupNoResults;
         }
         else
         {
            HistoryMatch[] entries = new HistoryMatch[resp.length()];
            for (int i = 0; i < entries.length; i++)
               entries[i] = new HistoryMatch(resp.get(entries.length - i - 1).getCommand(), text_, i);
            popup_ = new CompletionListPopupPanel<HistoryMatch>(entries);
            mode_ = desiredMode_;
         }

         popup_.setMaxWidth(input_.getBounds().getWidth());
         popup_.setPopupPositionAndShow(new PositionCallback()
         {
            public void setPosition(int offsetWidth, int offsetHeight)
            {
               Rectangle bounds = input_.getBounds();

               int top = bounds.getTop() - offsetHeight;
               if (top < 20)
                  top = bounds.getBottom();

               if (offset_ >= 0 && offset_ < resp.length())
               {
                  // Reuse the existing search offset
                  popup_.selectIndex(offset_);
               }
               else
               {
                  // No existing search offset, so select the last entry
                  popup_.selectLast();
                  offset_ = resp.length();
               }
               popup_.setPopupPosition(bounds.getLeft() - 6, top);
            }
         });

         popup_.addSelectionCommitHandler((SelectionCommitEvent<HistoryMatch> e) ->
         {
            Position pos = input_.getCursorPosition();
            input_.setText(e.getSelectedItem().getHistory());
            input_.setCursorPosition(pos);
            dismiss();
            input_.setFocus(true);
            offset_ = e.getSelectedItem().getIndex();
         });

         popup_.addCloseHandler(new CloseHandler<PopupPanel>() {

            @Override
            public void onClose(CloseEvent<PopupPanel> event)
            {
               popup_ = null;
            }

         });
      }

      private final PopupMode desiredMode_;
      private final Token token_;
      private final String text_;
   }

   private class HistoryMatch
   {
      public HistoryMatch(String history, String match, int index)
      {
         history_ = history;
         match_ = match;
         index_ = index;
      }

      public String toString()
      {
         int idx = history_.indexOf(match_);
         if (idx >= 0)
         {
            // if we can find the match, highlight it
            return
               SafeHtmlUtils.htmlEscape(
                     history_.substring(0, idx)) +
               "<span class=\"" +
               ConsoleResources.INSTANCE.consoleStyles().searchMatch() +
               "\">" +
               SafeHtmlUtils.htmlEscape(
                     history_.substring(idx,
                                          idx + match_.length())) +
               "</span>" +
               SafeHtmlUtils.htmlEscape(
                     history_.substring(idx + match_.length(),
                                          history_.length()));
         }

         // if we can't, just escape
         return SafeHtmlUtils.htmlEscape(match_);
      }

      public String getHistory()
      {
         return history_;
      }

      public int getIndex()
      {
         return index_;
      }

      private final String history_;
      private final String match_;
      private final int index_;
   }

   private CompletionListPopupPanel<HistoryMatch> popup_;
   private PopupMode mode_;
   private RpcObjectList<HistoryEntry> lastResults_;
   private String lastSearch_;
   private int offset_;
   private final InputEditorDisplay input_;
   private final HistoryServerOperations server_;
   private final Invalidation historyRequestInvalidation_ = new Invalidation();
}
