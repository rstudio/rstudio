/*
 * RCompletionManager.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.CompletionResult;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.QualifiedName;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;

import java.util.ArrayList;


public class RCompletionManager implements CompletionManager
{
   public RCompletionManager(InputEditorDisplay input,
                             CompletionPopupDisplay popup,
                             CodeToolsServerOperations server,
                             InitCompletionFilter initFilter)
   {
      input_ = input ;
      popup_ = popup ;
      server_ = server ;
      requester_ = new CompletionRequester(server_) ;
      initFilter_ = initFilter ;
      
      input_.addBlurHandler(new BlurHandler() {
         public void onBlur(BlurEvent event)
         {
            if (!ignoreNextInputBlur_)
               invalidatePendingRequests() ;
            ignoreNextInputBlur_ = false ;
         }
      }) ;

      input_.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            invalidatePendingRequests();
         }
      });

      popup_.addSelectionCommitHandler(new SelectionCommitHandler<QualifiedName>() {
         public void onSelectionCommit(SelectionCommitEvent<QualifiedName> event)
         {
            assert context_ != null : "onSelection called but handler is null" ;
            if (context_ != null)
               context_.onSelection(event.getSelectedItem()) ;
         }
      }) ;
      
      popup_.addSelectionHandler(new SelectionHandler<QualifiedName>() {
         public void onSelection(SelectionEvent<QualifiedName> event)
         {
            popup_.clearHelp(true) ;
            context_.showHelp(event.getSelectedItem()) ;
         }
      }) ;
      
      popup_.addMouseDownHandler(new MouseDownHandler() {
         public void onMouseDown(MouseDownEvent event)
         {
            ignoreNextInputBlur_ = true ;
         }
      }) ;
   }

   public void close()
   {
      popup_.hide();
   }
   
   private boolean checkInvalidateCount(int requiredInvalidateCount)
   {
      return requiredInvalidateCount == invalidateCount_ ;
   }
   
   public boolean previewKeyDown(KeyCodeEvent<?> event)
   {
      /**
       * KEYS THAT MATTER
       *
       * When popup not showing:
       * Tab - attempt completion (handled in Console.java)
       * 
       * When popup showing:
       * Esc - dismiss popup
       * Enter/Tab/Right-arrow - accept current selection
       * Up-arrow/Down-arrow - change selected item
       * Left-arrow - dismiss popup
       * [identifier] - narrow suggestions--or if we're lame, just dismiss
       * All others - dismiss popup
       */
      
      if (!popup_.isShowing())
      {
         if (event.getNativeKeyCode() == KeyCodes.KEY_TAB
               || (event.getNativeKeyCode() == ' ' && event.isControlKeyDown()))
         {
            if (initFilter_ == null || initFilter_.shouldComplete(event))
            {
               return beginSuggest(true) ;
            }
         }
      }
      else
      {
         switch (event.getNativeKeyCode())
         {
         case KeyCodes.KEY_SHIFT:
         case KeyCodes.KEY_CTRL:
         case KeyCodes.KEY_ALT:
            return false ; // bare modifiers should do nothing
         }
         
         if (!event.isAnyModifierKeyDown())
         {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE)
            {
               invalidatePendingRequests() ;
               return true ;
            }
            else if (event.getNativeKeyCode() == KeyCodes.KEY_TAB
                  || event.getNativeKeyCode() == KeyCodes.KEY_ENTER
                  || event.isRightArrow())
            {
               QualifiedName value = popup_.getSelectedValue() ;
               if (value != null)
               {
                  context_.onSelection(value) ;
                  return true ;
               }
            }
            else if (event.isUpArrow())
               return popup_.selectPrev() ;
            else if (event.isDownArrow())
               return popup_.selectNext() ;
            else if (event.getNativeKeyCode() == KeyCodes.KEY_PAGEUP)
               return popup_.selectPrevPage() ;
            else if (event.getNativeKeyCode() == KeyCodes.KEY_PAGEDOWN)
               return popup_.selectNextPage() ;
            else if (event.getNativeKeyCode() == KeyCodes.KEY_HOME)
               return popup_.selectFirst() ;
            else if (event.getNativeKeyCode() == KeyCodes.KEY_END)
               return popup_.selectLast() ;
            else if (event.isLeftArrow())
            {
               invalidatePendingRequests() ;
               return true ;
            }
            else if (event.getNativeKeyCode() == 112) // F1
            {
               context_.showHelpTopic() ;
               return true ;
            }
         }
         
         if (isIdentifierKey(event))
            return false ;
         
         invalidatePendingRequests() ;
         return false ;
      }
      
      return false ;
   }
   
   public boolean previewKeyPress(char c)
   {
      if (popup_.isShowing())
      {
         if ((c >= 'a' && c <= 'z')
               || (c >= 'A' && c <= 'Z')
               || (c >= '0' && c <= '9')
               || c == '.' || c == '_'
               || c == ':')
         {
            DeferredCommand.addCommand(new Command() {
               public void execute()
               {
                  beginSuggest(false) ;
               }
            }) ;
         }
      }
      return false ;
   }
   
   private static boolean isIdentifierKey(KeyCodeEvent<?> event)
   {
      if (event.isAltKeyDown() 
            || event.isControlKeyDown() 
            || event.isMetaKeyDown())
      {
         return false ;
      }
      
      int keyCode = event.getNativeEvent().getKeyCode() ;
      if (keyCode >= 'a' && keyCode <= 'z')
         return true ;
      if (keyCode >= 'A' && keyCode <= 'Z')
         return true ;
      if (keyCode == 189 && event.isShiftKeyDown()) // underscore
         return true ;
      if (keyCode == 186 && event.isShiftKeyDown()) // colon
         return true ;
      
      if (event.isShiftKeyDown())
         return false ;
      
      if (keyCode >= '0' && keyCode <= '9')
         return true ;
      if (keyCode == 190) // period
         return true ;
      
      return false ;
   }

   private void invalidatePendingRequests()
   {
      invalidatePendingRequests(true) ;
   }

   private void invalidatePendingRequests(boolean flushCache)
   {
      invalidateCount_++ ;
      invalidateCount_ %= 1000000 ;
      if (popup_.isShowing())
         popup_.hide() ;
      if (flushCache)
         requester_.flushCache() ;
   }

   /**
    * If false, the suggest operation was aborted
    */
   private boolean beginSuggest(boolean flushCache)
   {
      if (!input_.isSelectionCollapsed())
         return false ;
      
      invalidatePendingRequests(flushCache) ;

      String line = input_.getText() ;
      if (!input_.hasSelection())
      {
         Debug.log("Cursor wasn't in input box or was in subelement");
         return false ;
      }
      InputEditorSelection selection = input_.getSelection() ;
      if (selection == null)
         return false;

      boolean canAutoAccept = flushCache;
      context_ = new CompletionRequestContext(invalidateCount_,
                                              selection,
                                              canAutoAccept) ;
      requester_.getCompletions(line,
                                selection.getStart().getPosition(),
                                context_);

      return true ;
   }
   
   /**
    * It's important that we create a new instance of this each time.
    * It maintains state that is associated with a completion request.
    */
   private final class CompletionRequestContext extends
         ServerRequestCallback<CompletionResult>
   {
      public CompletionRequestContext(int invalidateCount, 
                                      InputEditorSelection selection,
                                      boolean canAutoAccept)
      {
         requiredInvalidateCount_ = invalidateCount ;
         selection_ = selection ;
         canAutoAccept_ = canAutoAccept;
      }
      
      public void showHelp(QualifiedName selectedItem)
      {
         helpStrategy_.showHelp(selectedItem, popup_) ;
      }

      public void showHelpTopic()
      {
         helpStrategy_.showHelpTopic(popup_.getSelectedValue()) ;
      }

      @Override
      public void onError(ServerError error)
      {
         if (!checkInvalidateCount(requiredInvalidateCount_))
            return ;
         
         RCompletionManager.this.popup_.showErrorMessage(
                  error.getUserMessage(), 
                  new PopupPositioner(input_.getCursorBounds(), popup_)) ;
      }

      @Override
      public void onResponseReceived(CompletionResult completions)
      {
         if (!checkInvalidateCount(requiredInvalidateCount_))
            return ;
         
         final QualifiedName[] results
                     = completions.completions.toArray(new QualifiedName[0]) ;
         
         if (results.length == 0)
         {
            popup_.showErrorMessage(
                  "(No matches)", 
                  new PopupPositioner(input_.getCursorBounds(), popup_)) ;
            return ;
         }

         initializeHelpStrategy(completions) ;
         
         // Move range to beginning of token; we want to place the popup there.
         final String token = completions.token ;

         input_.beginSetSelection(new InputEditorSelection(
               selection_.getStart().movePosition(-token.length(), true),
               selection_.getEnd()), new Command()
         {
            public void execute()
            {
               Rectangle rect = input_.getCursorBounds() ;
               input_.beginSetSelection(selection_, null) ;

               token_ = token ;

               if (results.length == 1
                   && canAutoAccept_
                   && StringUtil.isNullOrEmpty(results[0].pkgName))
               {
                  onSelection(results[0]);
               }
               else
               {
                  if (results.length == 1 && canAutoAccept_)
                     applyValue(results[0].name);

                  popup_.showCompletionValues(
                        results,
                        new PopupPositioner(rect, popup_),
                        !helpStrategy_.isNull()) ;
               }
            }
         });
      }

      private void initializeHelpStrategy(CompletionResult completions)
      {
         if (completions.guessedFunctionName != null)
         {
            helpStrategy_ = HelpStrategy.createParameterStrategy(
                              server_, completions.guessedFunctionName) ;
            return;
         }

         boolean anyPackages = false;
         ArrayList<QualifiedName> qnames = completions.completions;
         for (QualifiedName qname : qnames)
         {
            if (!StringUtil.isNullOrEmpty(qname.pkgName))
               anyPackages = true;
         }

         if (anyPackages)
            helpStrategy_ = HelpStrategy.createFunctionStrategy(server_) ;
         else
            helpStrategy_ = HelpStrategy.createNullStrategy();
      }
      
      private void onSelection(QualifiedName qname)
      {
         final String value = qname.name ;
         
         if (!checkInvalidateCount(requiredInvalidateCount_))
            return ;
         
         popup_.hide() ;
         requester_.flushCache() ;
         
         if (value == null)
         {
            assert false : "Selected comp value is null" ;
            return ;
         }

         applyValue(value);
      }

      private void applyValue(final String value)
      {
         // Move range to beginning of token
         input_.setFocus(true) ;
         input_.beginSetSelection(new InputEditorSelection(
               selection_.getStart().movePosition(-token_.length(), true),
               selection_.getEnd()), new Command()
         {
            public void execute()
            {
               // Replace the token with the full completion
               input_.replaceSelection(value, false) ;
               final int delta = value.length() - token_.length();
               input_.beginSetSelection(new InputEditorSelection(
                     selection_.getStart().movePosition(delta, true)), new Command()
               {
                  public void execute()
                  {
                     /* In some cases, applyValue can be called more than once
                      * as part of the same completion instance--specifically,
                      * if there's only one completion candidate and it is in
                      * a package. To make sure that the selection movement
                      * logic works the second time, we need to reset the
                      * selection. 
                      */
                     token_ = value;
                     selection_ = input_.getSelection();
                  }
               });
            }
         });
      }

      private final int requiredInvalidateCount_ ;
      private InputEditorSelection selection_ ;
      private final boolean canAutoAccept_;
      private HelpStrategy helpStrategy_ ;
   }
   
   private final InputEditorDisplay input_ ;
   private final CompletionPopupDisplay popup_ ;
   private final CodeToolsServerOperations server_ ;
   private final CompletionRequester requester_ ;
   private final InitCompletionFilter initFilter_ ;
   // Prevents completion popup from being dismissed when you merely
   // click on it to scroll.
   private boolean ignoreNextInputBlur_ = false;
   private String token_ ;
   
   private int invalidateCount_ ;
   private CompletionRequestContext context_ ;
}
