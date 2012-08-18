/*
 * RCompletionManager.java
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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.inject.Inject;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.codesearch.model.FunctionDefinition;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.CompletionResult;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.QualifiedName;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorLineWithCursorPosition;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorUtil;
import org.rstudio.studio.client.workbench.views.source.editors.text.NavigableSourceEditor;
import org.rstudio.studio.client.workbench.views.source.events.CodeBrowserNavigationEvent;
import org.rstudio.studio.client.workbench.views.source.model.RnwCompletionContext;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

import java.util.ArrayList;


public class RCompletionManager implements CompletionManager
{  
   // globally suppress F1 and F2 so no default browser behavior takes those
   // keystrokes (e.g. Help in Chrome)
   static
   {
      Event.addNativePreviewHandler(new NativePreviewHandler() {
         @Override
         public void onPreviewNativeEvent(NativePreviewEvent event)
         {
            if (event.getTypeInt() == Event.ONKEYDOWN)
            {
               int keyCode = event.getNativeEvent().getKeyCode();
               if ((keyCode == 112 || keyCode == 113) &&
                   KeyboardShortcut.NONE ==
                      KeyboardShortcut.getModifierValue(event.getNativeEvent()))
               {
                 event.getNativeEvent().preventDefault();
               }
            }
         }
      });   
   }
   
   public RCompletionManager(InputEditorDisplay input,
                             NavigableSourceEditor navigableSourceEditor,
                             CompletionPopupDisplay popup,
                             CodeToolsServerOperations server,
                             InitCompletionFilter initFilter,
                             RnwCompletionContext rnwContext)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      input_ = input ;
      navigableSourceEditor_ = navigableSourceEditor;
      popup_ = popup ;
      server_ = server ;
      requester_ = new CompletionRequester(server_, rnwContext);
      initFilter_ = initFilter ;
      rnwContext_ = rnwContext;
      
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
   
   @Inject
   public void initialize(GlobalDisplay globalDisplay,
                          FileTypeRegistry fileTypeRegistry,
                          EventBus eventBus)
   {
      globalDisplay_ = globalDisplay;
      fileTypeRegistry_ = fileTypeRegistry;
      eventBus_ = eventBus;
   }

   public void close()
   {
      popup_.hide();
   }
   
   public void codeCompletion()
   {
      if (initFilter_ == null || initFilter_.shouldComplete(null))
         beginSuggest(true, false);
   }
   
   public void goToFunctionDefinition()
   {   
      // determine current line and cursor position
      InputEditorLineWithCursorPosition lineWithPos = 
                      InputEditorUtil.getLineWithCursorPosition(input_);
      
      // lookup function definition at this location
      
      // delayed progress indicator
      final GlobalProgressDelayer progress = new GlobalProgressDelayer(
            globalDisplay_, 1000, "Searching for function definition...");
      
      server_.getFunctionDefinition(
         lineWithPos.getLine(),
         lineWithPos.getPosition(), 
         new ServerRequestCallback<FunctionDefinition>() {
            @Override
            public void onResponseReceived(FunctionDefinition def)
            {
                // dismiss progress
                progress.dismiss();
                    
                // if we got a hit
                if (def.getFunctionName() != null)
                {   
                   // search locally if a function navigator was provided
                   if (navigableSourceEditor_ != null)
                   {
                      // try to search for the function locally
                      SourcePosition position = 
                         navigableSourceEditor_.findFunctionPositionFromCursor(
                                                         def.getFunctionName());
                      if (position != null)
                      {
                         navigableSourceEditor_.navigateToPosition(position, 
                                                                   true);
                         return; // we're done
                      }

                   }
                   
                   // if we didn't satisfy the request using a function
                   // navigator and we got a file back from the server then
                   // navigate to the file/loc
                   if (def.getFile() != null)
                   {  
                      fileTypeRegistry_.editFile(def.getFile(), 
                                                 def.getPosition());
                   }
                   
                   // if we didn't get a file back see if we got a 
                   // search path definition
                   else if (def.getSearchPathFunctionDefinition() != null)
                   {
                      eventBus_.fireEvent(new CodeBrowserNavigationEvent(
                                     def.getSearchPathFunctionDefinition()));
                      
                   }
               }
            }

            @Override
            public void onError(ServerError error)
            {
               progress.dismiss();
               
               globalDisplay_.showErrorMessage("Error Searching for Function",
                                               error.getUserMessage());
            }
         });
   }
   
   
   public boolean previewKeyDown(NativeEvent event)
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

      int modifier = KeyboardShortcut.getModifierValue(event);

      if (!popup_.isShowing())
      {
         if ((event.getKeyCode() == KeyCodes.KEY_TAB && modifier == KeyboardShortcut.NONE)
               || (event.getKeyCode() == ' ' && modifier == KeyboardShortcut.CTRL))
         {
            if (initFilter_ == null || initFilter_.shouldComplete(event))
            {
               return beginSuggest(true, false) ;
            }
         }
         else if (event.getKeyCode() == 112 // F1
                  && modifier == KeyboardShortcut.NONE)
         {
            InputEditorLineWithCursorPosition linePos = 
                        InputEditorUtil.getLineWithCursorPosition(input_);
           
            server_.getHelpAtCursor(
                  linePos.getLine(), linePos.getPosition(),
                  new SimpleRequestCallback<Void>("Help"));
         }
         else if (event.getKeyCode() == 113 // F2
                  && modifier == KeyboardShortcut.NONE)
         {
            goToFunctionDefinition();
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
         
         if (modifier == KeyboardShortcut.NONE)
         {
            if (event.getKeyCode() == KeyCodes.KEY_ESCAPE)
            {
               invalidatePendingRequests() ;
               return true ;
            }
            else if (event.getKeyCode() == KeyCodes.KEY_TAB
                  || event.getKeyCode() == KeyCodes.KEY_ENTER
                  || event.getKeyCode() == KeyCodes.KEY_RIGHT)
            {
               QualifiedName value = popup_.getSelectedValue() ;
               if (value != null)
               {
                  context_.onSelection(value) ;
                  return true ;
               }
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
               invalidatePendingRequests() ;
               return true ;
            }
            else if (event.getKeyCode() == 112) // F1
            {
               context_.showHelpTopic() ;
               return true ;
            }
            else if (event.getKeyCode() == 113) // F2
            {
               goToFunctionDefinition();
               return true;
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
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  beginSuggest(false, false) ;
               }
            });
         }
      }
      else
      {
         if ((c == '@' && isRoxygenTagValidHere()) || isSweaveCompletion(c))
         {
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  beginSuggest(true, true) ;
               }
            });
         }
      }
      return false ;
   }

   private boolean isRoxygenTagValidHere()
   {
      if (input_.getText().matches("\\s*#+'.*"))
      {
         String linePart = input_.getText().substring(0, input_.getSelection().getStart().getPosition());
         if (linePart.matches("\\s*#+'\\s*"))
            return true;
      }
      return false;
   }

   private boolean isSweaveCompletion(char c)
   {
      if (rnwContext_ == null || (c != ',' && c != ' ' && c != '='))
         return false;

      int optionsStart = rnwContext_.getRnwOptionsStart(
            input_.getText(),
            input_.getSelection().getStart().getPosition());

      if (optionsStart < 0)
      {
         return false;
      }

      String linePart = input_.getText().substring(
            optionsStart,
            input_.getSelection().getStart().getPosition());

      return c != ' ' || linePart.matches(".*,\\s*");
   }

   private static boolean isIdentifierKey(NativeEvent event)
   {
      if (event.getAltKey()
            || event.getCtrlKey()
            || event.getMetaKey())
      {
         return false ;
      }
      
      int keyCode = event.getKeyCode() ;
      if (keyCode >= 'a' && keyCode <= 'z')
         return true ;
      if (keyCode >= 'A' && keyCode <= 'Z')
         return true ;
      if (keyCode == 189 && event.getShiftKey()) // underscore
         return true ;
      if (keyCode == 186 && event.getShiftKey()) // colon
         return true ;
      
      if (event.getShiftKey())
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
      invalidation_.invalidate();
      if (popup_.isShowing())
         popup_.hide() ;
      if (flushCache)
         requester_.flushCache() ;
   }

   /**
    * If false, the suggest operation was aborted
    */
   private boolean beginSuggest(boolean flushCache, boolean implicit)
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

      String linePart = line.substring(0, selection.getStart().getPosition());

      if (line.matches("\\s*#.*") && !linePart.matches("\\s*#+'\\s*[^\\s].*"))
      {
         // No completion inside comments (except Roxygen). For the Roxygen
         // case, only do completion if we're past the first non-whitespace
         // character (to allow for easy indenting).
         return false;
      }

      boolean canAutoAccept = flushCache;
      context_ = new CompletionRequestContext(invalidation_.getInvalidationToken(),
                                              selection,
                                              canAutoAccept) ;
      requester_.getCompletions(line,
                                selection.getStart().getPosition(),
                                implicit,
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
      public CompletionRequestContext(Invalidation.Token token,
                                      InputEditorSelection selection,
                                      boolean canAutoAccept)
      {
         invalidationToken_ = token ;
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
         if (invalidationToken_.isInvalid())
            return ;
         
         RCompletionManager.this.popup_.showErrorMessage(
                  error.getUserMessage(), 
                  new PopupPositioner(input_.getCursorBounds(), popup_)) ;
      }

      @Override
      public void onResponseReceived(CompletionResult completions)
      {
         if (invalidationToken_.isInvalid())
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

         Rectangle rect = input_.getPositionBounds(
               selection_.getStart().movePosition(-token.length(), true));

         token_ = token ;
         suggestOnAccept_ = completions.suggestOnAccept;

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
         
         if (invalidationToken_.isInvalid())
            return ;
         
         popup_.hide() ;
         requester_.flushCache() ;
         
         if (value == null)
         {
            assert false : "Selected comp value is null" ;
            return ;
         }

         applyValue(value);

         if (suggestOnAccept_)
         {
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  beginSuggest(true, true);
               }
            });
         }
      }

      private void applyValue(final String value)
      {
         // Move range to beginning of token
         input_.setFocus(true) ;
         input_.setSelection(new InputEditorSelection(
               selection_.getStart().movePosition(-token_.length(), true),
               input_.getSelection().getEnd()));

         // Replace the token with the full completion
         input_.replaceSelection(value, true) ;

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

      private final Invalidation.Token invalidationToken_ ;
      private InputEditorSelection selection_ ;
      private final boolean canAutoAccept_;
      private HelpStrategy helpStrategy_ ;
      private boolean suggestOnAccept_;
   }
   
   private GlobalDisplay globalDisplay_;
   private FileTypeRegistry fileTypeRegistry_;
   private EventBus eventBus_;
      
   private final CodeToolsServerOperations server_;
   private final InputEditorDisplay input_ ;
   private final NavigableSourceEditor navigableSourceEditor_;
   private final CompletionPopupDisplay popup_ ;
   private final CompletionRequester requester_ ;
   private final InitCompletionFilter initFilter_ ;
   // Prevents completion popup from being dismissed when you merely
   // click on it to scroll.
   private boolean ignoreNextInputBlur_ = false;
   private String token_ ;

   private final Invalidation invalidation_ = new Invalidation();
   private CompletionRequestContext context_ ;
   private final RnwCompletionContext rnwContext_;
}
