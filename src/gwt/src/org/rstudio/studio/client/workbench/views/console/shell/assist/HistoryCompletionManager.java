/*
 * HistoryCompletionManager.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.Invalidation.Token;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.workbench.views.console.shell.KeyDownPreviewHandler;
import org.rstudio.studio.client.workbench.views.console.shell.KeyPressPreviewHandler;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.history.model.HistoryEntry;
import org.rstudio.studio.client.workbench.views.history.model.HistoryServerOperations;

public class HistoryCompletionManager implements KeyDownPreviewHandler,
                                                 KeyPressPreviewHandler
{
   public HistoryCompletionManager(InputEditorDisplay input,
                                   HistoryServerOperations server)
   {
      input_ = input;
      server_ = server;
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
            input_.setText(popup_.getSelectedValue());
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
      }
   }

   private void beginSuggest()
   {
      historyRequestInvalidation_.invalidate();
      final Token token = historyRequestInvalidation_.getInvalidationToken();

      String value = input_.getText();
      server_.searchHistoryArchiveByPrefix(
            value, 20, true,
            new SimpleRequestCallback<RpcObjectList<HistoryEntry>>()
            {
               @Override
               public void onResponseReceived(RpcObjectList<HistoryEntry> resp)
               {
                  if (token.isInvalid())
                     return;

                  if (resp.length() == 0)
                  {
                     popup_ = new HistoryPopupPanel(new String[0]);
                     popup_.setText("(No matching commands)");
                  }
                  else
                  {
                     String[] entries = new String[resp.length()];
                     for (int i = 0; i < entries.length; i++)
                        entries[i] = resp.get(entries.length - i - 1).getCommand();
                     popup_ = new HistoryPopupPanel(entries);
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

                  popup_.addSelectionCommitHandler(new SelectionCommitHandler<String>()
                  {
                     public void onSelectionCommit(SelectionCommitEvent<String> e)
                     {
                        input_.setText(e.getSelectedItem());
                        dismiss();
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
            });
   }

   public boolean previewKeyPress(char charCode)
   {
      return false;
   }

   private HistoryPopupPanel popup_;
   private final InputEditorDisplay input_;
   private final HistoryServerOperations server_;
   private final Invalidation historyRequestInvalidation_ = new Invalidation();
}
