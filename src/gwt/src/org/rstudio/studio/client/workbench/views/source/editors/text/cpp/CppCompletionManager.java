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


import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionUtils;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.model.CppServerOperations;
import org.rstudio.studio.client.workbench.views.source.model.CppSourceLocation;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.inject.Inject;

public class CppCompletionManager implements CompletionManager
{
   public CppCompletionManager(DocDisplay docDisplay,
                               InitCompletionFilter initFilter,
                               CppCompletionContext completionContext,
                               CompletionManager rCompletionManager)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      docDisplay_ = docDisplay;
      initFilter_ = initFilter;
      completionContext_ = completionContext;
      rCompletionManager_ = rCompletionManager; 
      docDisplay_.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            terminateCompletionRequest();
         }
      });
   }
 
   @Inject
   void initialize(CppServerOperations server, 
                   FileTypeRegistry fileTypeRegistry)
   {
      server_ = server;
      fileTypeRegistry_ = fileTypeRegistry;
   }
   
   // close the completion popup (if any)
   @Override
   public void close()
   {
      // delegate to R mode if necessary
      if (isCursorInRMode())
      {
         rCompletionManager_.close();
      }
      else
      {
         terminateCompletionRequest();
      }
   }
   
   
   // perform completion at the current cursor location
   @Override
   public void codeCompletion()
   {
      // delegate to R mode if necessary
      if (isCursorInRMode())
      {
         rCompletionManager_.codeCompletion();
      }
      // check whether it's okay to do a completion
      else if (shouldComplete(null))
      {
         suggestCompletions(true); 
      }
   }

   // go to help at the current cursor location
   @Override
   public void goToHelp()
   {
      // delegate to R mode if necessary
      if (isCursorInRMode())
      {
         rCompletionManager_.goToHelp();
      }
      else
      {
         // no implementation here yet since we don't have access
         // to C/C++ help (we could implement this via using libclang
         // to parse doxygen though)   
      }
   }

   // find the definition of the function at the current cursor location
   @Override
   public void goToFunctionDefinition()
   {  
      // delegate to R mode if necessary
      if (isCursorInRMode())
      {
         rCompletionManager_.goToFunctionDefinition();
      }
      else
      {
         if (completionContext_.isCompletionEnabled())
         {
            completionContext_.withUpdatedDoc(new CommandWithArg<String>() {
               @Override
               public void execute(final String docPath)
               {
                  Position pos = docDisplay_.getCursorPosition();
                  
                  server_.goToCppDefinition(
                      docPath, 
                      pos.getRow() + 1, 
                      pos.getColumn() + 1, 
                      new SimpleRequestCallback<CppSourceLocation>() {
                         @Override
                         public void onResponseReceived(CppSourceLocation loc)
                         {
                            if (loc != null)
                            {
                               fileTypeRegistry_.editFile(loc.getFile(), 
                                                          loc.getPosition());  
                            }
                         }
                      });
                  
               }
            });
         }
      }
   }
   
   // return false to indicate key not handled
   @Override
   public boolean previewKeyDown(NativeEvent event)
   {
      // delegate to R mode if appropriate
      if (isCursorInRMode())
         return rCompletionManager_.previewKeyDown(event);
      
      // if there is no completion request active then 
      // check for a key-combo that triggers completion or 
      // navigation / help
      int modifier = KeyboardShortcut.getModifierValue(event);
      if ((request_ == null) || request_.isTerminated())
      {  
         // check for user completion key combo 
         if (CompletionUtils.isCompletionRequest(event, modifier) &&
             shouldComplete(event)) 
         {
            return suggestCompletions(true);
         }
         else if (event.getKeyCode() == 112 // F1
                  && modifier == KeyboardShortcut.NONE)
         {
            goToHelp();
            return true;
         }
         else if (event.getKeyCode() == 113 // F2
                  && modifier == KeyboardShortcut.NONE)
         {
            goToFunctionDefinition();
            return true;
         }
         else
         {
            return false;
         }
      }
      // otherwise handle keys within the completion popup
      else
      {   
         // get the key code
         int keyCode = event.getKeyCode();
         
         // chrome on ubuntu now sends this before every keydown
         // so we need to explicitly ignore it. see:
         // https://github.com/ivaynberg/select2/issues/2482
         if (keyCode == KeyCodes.KEY_WIN_IME)
         {
            return false ;
         }
         
         // modifier keys always no-op
         if (keyCode == KeyCodes.KEY_SHIFT ||
             keyCode == KeyCodes.KEY_CTRL ||
             keyCode == KeyCodes.KEY_ALT)
         {          
            return false ; 
         }
         
         // if there is no popup then bail
         CppCompletionPopupMenu popup = getCompletionPopup();
         if (popup == null)
            return false;
         
         // backspace triggers completion if the popup is visible
         if (keyCode == KeyCodes.KEY_BACKSPACE)
         {
            delayedSuggestCompletions(false);
            return false;
         }
         
         // left key terminates the request (popup handles Esc)
         if (event.getKeyCode() == KeyCodes.KEY_LEFT)
         {
            terminateCompletionRequest();
            return true;
         }
          
         // tab/right accept the current selection (popup handles Enter)
         else if (event.getKeyCode() == KeyCodes.KEY_TAB ||
                  event.getKeyCode() == KeyCodes.KEY_RIGHT)
         {
            popup.acceptSelected();
            return true;
         }
         
         // non c++ identifier keys (that aren't navigational) close the popup
         else if (!CppCompletionUtils.isCppIdentifierKey(event))
         {
            terminateCompletionRequest();
            return false;
         }
         
         // otherwise leave it alone
         else
         {   
            return false;
         }
      }
   }

   // return false to indicate key not handled
   @Override
   public boolean previewKeyPress(char c)
   {
      // delegate to R mode if necessary
      if (isCursorInRMode())
      {
         return rCompletionManager_.previewKeyPress(c);
      }
      else if (CompletionUtils.handleEncloseSelection(docDisplay_, c))
      {
         return true;
      }
      else
      {
         delayedSuggestCompletions(false);
         return false;
      }
   }
   
   private void delayedSuggestCompletions(final boolean explicit)
   {
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
         @Override
         public void execute()
         {
            suggestCompletions(explicit);  
         }
      });
   }
   
   private boolean suggestCompletions(final boolean explicit)
   {
      // check for completions disabled
      if (!completionContext_.isCompletionEnabled())
         return false;
      
      // check for no selection
      InputEditorSelection selection = docDisplay_.getSelection() ;
      if (selection == null)
         return false;
      
      // check for contiguous selection
      if (!docDisplay_.isSelectionCollapsed())
         return false;    
  
      // calculate explicit value for getting completion position (if a 
      // previous request was explicit then count this as explicit)
      boolean positionExplicit = explicit || 
                                 ((request_ != null) && request_.isExplicit());
      
      // see if we even have a completion position
      final CompletionPosition completionPosition = 
            CppCompletionUtils.getCompletionPosition(docDisplay_,
                                                     positionExplicit);
      if (completionPosition == null)
      {
         terminateCompletionRequest();
         return false;
      }
      
      if ((request_ != null) &&
          !request_.isTerminated() &&
          request_.getCompletionPosition().isSupersetOf(completionPosition))
      {
         request_.updateUI(false);
      }
      else
      {
         terminateCompletionRequest();
         
         final Invalidation.Token invalidationToken = 
               completionRequestInvalidation_.getInvalidationToken();
         
         completionContext_.withUpdatedDoc(new CommandWithArg<String>() {

            @Override
            public void execute(String docPath)
            {
               if (invalidationToken.isInvalid())
                  return;
               
               request_ = new CppCompletionRequest(
                  docPath,
                  completionPosition,
                  docDisplay_,
                  invalidationToken,
                  explicit);
            }
         });
         
      }
      
      return true;
   }
     
   private CppCompletionPopupMenu getCompletionPopup()
   {
      CppCompletionPopupMenu popup = request_ != null ?
            request_.getCompletionPopup() : null;
      return popup;
   }
   
   private void terminateCompletionRequest()
   {
      completionRequestInvalidation_.invalidate();
      if (request_ != null)
      {
         request_.terminate();
         request_ = null;
      }
   }

   private boolean shouldComplete(NativeEvent event)
   {
      return initFilter_ == null || initFilter_.shouldComplete(event);
   }

   private boolean isCursorInRMode()
   {
      String m = docDisplay_.getLanguageMode(docDisplay_.getCursorPosition());
      if (m == null)
         return false;
      if (m.equals(TextFileType.R_LANG_MODE))
         return true;
      return false;
   }
   
   private CppServerOperations server_;
   private FileTypeRegistry fileTypeRegistry_;
   private final DocDisplay docDisplay_;
   private final CppCompletionContext completionContext_;
   private CppCompletionRequest request_;
   private final InitCompletionFilter initFilter_ ;
   private final CompletionManager rCompletionManager_;
   private final Invalidation completionRequestInvalidation_ = new Invalidation();
   
  

}
