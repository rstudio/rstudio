/*
 * HistoryCompletionManager.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.console.shell.KeyDownPreviewHandler;
import org.rstudio.studio.client.workbench.views.console.shell.KeyPressPreviewHandler;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.history.model.HistoryEntry;
import org.rstudio.studio.client.workbench.views.history.model.HistoryServerOperations;

public class HistoryCompletionManager implements KeyDownPreviewHandler,
                                                 KeyPressPreviewHandler
{
   public enum PopupMode
   {
      PopupNone,          // popup is not showing
      PopupNoResults,     // popup is showing but with no results
      PopupIncremental,   // popup is showing incremental infix search
      PopupPrefix         // popup is showing prefix search
   };

   public HistoryCompletionManager(InputEditorDisplay input,
                                   HistoryServerOperations server)
   {
      input_ = input;
      server_ = server;
      mode_ = PopupMode.PopupNone;
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
            return false ; // bare modifiers should do nothing
         }
         
         if (event.getKeyCode() == KeyCodes.KEY_ESCAPE)
         {
            dismiss();
            return true;
         }
         else if (event.getKeyCode() == KeyCodes.KEY_ENTER)
         {
            input_.setText(popup_.getSelectedValue().getHistory());
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
            return popup_.selectPrev() ;
         else if (event.getKeyCode() == KeyCodes.KEY_DOWN)
            return popup_.selectNext() ;
         else if (event.getKeyCode() == KeyCodes.KEY_PAGEUP)
            return popup_.selectPrevPage() ;
         else if (event.getKeyCode() == KeyCodes.KEY_PAGEDOWN)
            return popup_.selectNextPage() ;
         else if (event.getKeyCode() == KeyCodes.KEY_HOME)
            return popup_.selectFirst() ;
         else if (event.getKeyCode() == KeyCodes.KEY_END)
            return popup_.selectLast() ;
         else if (event.getKeyCode() == KeyCodes.KEY_LEFT)
         {
            dismiss();
            return true ;
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

   public void beginSuggest()
   {
      server_.searchHistoryArchiveByPrefix(
            input_.getText(), 20, true, 
            new HistoryCallback(input_.getText(), PopupMode.PopupPrefix));
   }
   
   public void beginSearch()
   {
      server_.searchHistoryArchive(
            input_.getText(), 20, 
            new HistoryCallback(input_.getText(), PopupMode.PopupIncremental));
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
         desiredMode_ = desiredMode;
      }

      @Override
      public void onResponseReceived(RpcObjectList<HistoryEntry> resp)
      {
         if (token_.isInvalid())
            return;
         
         if (mode_ != PopupMode.PopupNone)
            dismiss();

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
               entries[i] = new HistoryMatch(resp.get(i).getCommand(), text_);
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

               popup_.selectLast();
               popup_.setPopupPosition(bounds.getLeft() - 6, top);
            }
         });

         popup_.addSelectionCommitHandler(
               new SelectionCommitHandler<HistoryMatch>()
         {
            public void onSelectionCommit(SelectionCommitEvent<HistoryMatch> e)
            {
               input_.setText(e.getSelectedItem().getHistory());
               dismiss();
               input_.setFocus(true);
            }
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
      public HistoryMatch(String history, String match)
      {
         history_ = history;
         match_ = match;
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
      
      private final String history_;
      private final String match_;
   }
   
   private CompletionListPopupPanel<HistoryMatch> popup_;
   private PopupMode mode_;
   private final InputEditorDisplay input_;
   private final HistoryServerOperations server_;
   private final Invalidation historyRequestInvalidation_ = new Invalidation();
}
