/*
 * CppCompletionManager.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.source.editors.text.cpp;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.Invalidation.Token;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionListPopupPanel;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.NavigableSourceEditor;


import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;

public class CppCompletionManager implements CompletionManager
{
   public CppCompletionManager(InputEditorDisplay input,
                               NavigableSourceEditor navigableSourceEditor,
                               InitCompletionFilter initFilter,
                               CompletionManager rCompletionManager)
   {
      input_ = input;
      navigableSourceEditor_ = navigableSourceEditor;
      initFilter_ = initFilter;
      rCompletionManager_ = rCompletionManager;
   }

   // return false to indicate key not handled
   @Override
   public boolean previewKeyDown(NativeEvent event)
   {
      if (isCursorInRMode())
         return rCompletionManager_.previewKeyDown(event);
      
      /*
      if (popup_ == null)
      { 
         if (false) // check for user completion key combo 
                    // (we don't have any right now)
         {
            if (initFilter_ == null || initFilter_.shouldComplete(event))
            {
               beginSuggest();
               return true;
            }
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
            close();
            return true;
         }
         else if (event.getKeyCode() == KeyCodes.KEY_ENTER)
         {
            input_.setText(popup_.getSelectedValue());
            close();
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
            close();
            return true ;
         }

         close();
         return false;
      }
      */
      
      return false;
   }

   // return false to indicate key not handled
   @Override
   public boolean previewKeyPress(char c)
   {
      if (isCursorInRMode())
         return rCompletionManager_.previewKeyPress(c);
      
      /*
      if (popup_ != null)
      {
         // right now additional suggestions will be for attributes names
         // and parameters (identifiers) so we use these characters to 
         // indicate to do another completion query (note that _ and : are
         // valid R identifier chars but not package identifier chars)
         if ((c >= 'a' && c <= 'z') || 
             (c >= 'A' && c <= 'Z') || 
             (c >= '0' && c <= '9') || 
             c == '.' || c == '_' || c == ':')
         {
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  beginSuggest() ;
               }
            });
         }
      }
      else
      {
         if (isAttributeCompletionValidHere(c))
         {
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  beginSuggest() ;
               }
            });
         }
         else if (!input_.isSelectionCollapsed())
         {
            switch(c)
            {
            case '"':
            case '\'':
               encloseSelection(c, c);
               return true;
            case '(':
               encloseSelection('(', ')');
               return true;
            case '{':
               encloseSelection('{', '}');
               return true;
            case '[':
               encloseSelection('[', ']');
               return true;     
            }
         }
      }
      */
      
      return false ;
   }
   
   
   @SuppressWarnings("unused")
   private void encloseSelection(char beginChar, char endChar) 
   {
      StringBuilder builder = new StringBuilder();
      builder.append(beginChar);
      builder.append(input_.getSelectionValue());
      builder.append(endChar);
      input_.replaceSelection(builder.toString(), true);
   }

   @SuppressWarnings("unused")
   private boolean isAttributeCompletionValidHere(char c)
   {     
      // TODO: we can't just append the character since it could
      // be anywhere withinh the line -- need to insert it into 
      // the right spot
      String line = input_.getText() + c;
      if (line.matches("\\s*//\\s+\\[\\[.*"))
      {
         // get text up to selection
         String linePart = input_.getText().substring(
                           0, input_.getSelection().getStart().getPosition());
         return true;
      }
      return false;
   }
   
   
   // go to help at the current cursor location
   @Override
   public void goToHelp()
   {
      if (isCursorInRMode())
         rCompletionManager_.goToHelp();
   }

   // find the definition of the function at the current cursor location
   @Override
   public void goToFunctionDefinition()
   {  
      if (isCursorInRMode())
         rCompletionManager_.goToFunctionDefinition();
   }

   // perform completion at the current cursor location
   @Override
   public void codeCompletion()
   {
      if (isCursorInRMode())
      {
         rCompletionManager_.codeCompletion();
      }
      else
      {
         if (initFilter_ == null || initFilter_.shouldComplete(null))
         {
         
         }
      }
   }

   // close the completion popup (if any)
   @Override
   public void close()
   {
      if (isCursorInRMode())
      {
         rCompletionManager_.close();
      }
      else
      {
         if (popup_ != null)
         {
            popup_.hide();
            popup_ = null;
         }
      }
   }
   
   @SuppressWarnings("unused")
   private void beginSuggest()
   {
      completionRequestInvalidation_.invalidate();
      final Token token = completionRequestInvalidation_.getInvalidationToken();

      String value = input_.getText();
      Debug.logToConsole(value);
      
      getCompletions(value,
            new SimpleRequestCallback<JsArrayString>()
            {
               @Override
               public void onResponseReceived(JsArrayString resp)
               {
                  if (token.isInvalid())
                     return;

                  if (resp.length() == 0)
                  {
                     popup_ = new CompletionListPopupPanel(new String[0]);
                     popup_.setText("(No matching commands)");
                  }
                  else
                  {
                     String[] entries = JsUtil.toStringArray(resp);
                     popup_ = new CompletionListPopupPanel(entries);
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
                        close();
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
   
   
   private void getCompletions(
         String line, 
         ServerRequestCallback<JsArrayString> requestCallback) 
   {
   }

   private boolean isCursorInRMode()
   {
      String mode = input_.getLanguageMode(input_.getCursorPosition());
      if (mode == null)
         return false;
      if (mode.equals(TextFileType.R_LANG_MODE))
         return true;
      return false;
   }
  
   
   private final InputEditorDisplay input_ ;
   @SuppressWarnings("unused")
   private final NavigableSourceEditor navigableSourceEditor_;
   private CompletionListPopupPanel popup_;
   private final InitCompletionFilter initFilter_ ;
   private final CompletionManager rCompletionManager_;
   private final Invalidation completionRequestInvalidation_ = new Invalidation();
   
  

}
