/*
 * RCompletionManager.java
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
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.NavigableSourceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.CodeModel;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.EditSession;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Mode;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.RInfixData;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenCursor;
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
                             RnwCompletionContext rnwContext,
                             DocDisplay docDisplay)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      input_ = input ;
      navigableSourceEditor_ = navigableSourceEditor;
      popup_ = popup ;
      server_ = server ;
      requester_ = new CompletionRequester(server_, rnwContext, navigableSourceEditor);
      initFilter_ = initFilter ;
      rnwContext_ = rnwContext;
      docDisplay_ = docDisplay;
      
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
   
   public void goToHelp()
   {
      InputEditorLineWithCursorPosition linePos = 
            InputEditorUtil.getLineWithCursorPosition(input_);

      server_.getHelpAtCursor(
            linePos.getLine(), linePos.getPosition(),
            new SimpleRequestCallback<Void>("Help"));
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
      
      nativeEvent_ = event;

      int modifier = KeyboardShortcut.getModifierValue(event);

      if (!popup_.isShowing())
      {
         if (CompletionUtils.isCompletionRequest(event, modifier))
         {
            if (initFilter_ == null || initFilter_.shouldComplete(event))
            {
               return beginSuggest(true, false);
            }
         }
         else if (event.getKeyCode() == 112 // F1
                  && modifier == KeyboardShortcut.NONE)
         {
            goToHelp();
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
         // chrome on ubuntu now sends this before every keydown
         // so we need to explicitly ignore it. see:
         // https://github.com/ivaynberg/select2/issues/2482
         case KeyCodes.KEY_WIN_IME: 
            return false ;
            
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
                  beginSuggest(false, false);
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
                  beginSuggest(true, true);
               }
            });
         }
         else if (CompletionUtils.handleEncloseSelection(input_, c))
         {
            return true;
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
   
   // Simple inner class that packages together a string (context) and
   // bool (did we look back to build context?)
   class AutoCompletionContext {
      
      public AutoCompletionContext(String context, boolean lookedBack)
      {
         context_ = context;
         lookedBack_ = lookedBack;
      }
      
      public String getContext()
      {
         return context_;
      }
      
      public boolean getLookedBack()
      {
         return lookedBack_;
      }
      
      private String context_;
      private boolean lookedBack_;
      
   }

   /**
    * If false, the suggest operation was aborted
    */
   private boolean beginSuggest(boolean flushCache, boolean implicit)
   {
      if (!input_.isSelectionCollapsed())
         return false ;
      
      invalidatePendingRequests(flushCache);
      
      InputEditorSelection selection = input_.getSelection() ;
      if (selection == null)
         return false;
      
      int cursorCol = selection.getStart().getPosition();
      String firstLine = input_.getText().substring(0, cursorCol);
      
      // don't auto-complete at the start of comments
      if (firstLine.matches(".*#+\\s*$"))
      {
         return false;
      }
      
      // don't auto-complete with tab on lines with only whitespace,
      // if the insertion character was a tab
      if (nativeEvent_ != null &&
            nativeEvent_.getKeyCode() == KeyCodes.KEY_TAB)
         if (firstLine.matches("^\\s*$"))
            return false;
      
      AutoCompletionContext context = getAutocompletionContext();
      String line = context.getContext();
      
      // Cheap trick -- convert all instances of '{', '}' to '(', ')' so
      // they get treated as non-named function calls. This fixes an
      // undesired behaviour whereby e.g.
      //
      // lapply(X, function(x) { |
      //
      // would produce auto-completions for lapply.
      line = line.replace('{', '(').replace('}', ')');
      
      if (!input_.hasSelection())
      {
         Debug.log("Cursor wasn't in input box or was in subelement");
         return false ;
      }

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
                                              canAutoAccept);
      
      // Try to see if there's an object name we should use to supplement
      // completions
      RInfixData infixData = RInfixData.create();
      AceEditor editor = (AceEditor) docDisplay_;
      if (editor != null)
      {
         CodeModel codeModel = editor.getSession().getMode().getCodeModel();
         TokenCursor cursor = codeModel.getTokenCursor();
         if (cursor.moveToPosition(input_.getCursorPosition()))
            infixData = codeModel.getDataFromInfixChain(cursor);
      }
      
      requester_.getCompletions(line,
                                line.length(),
                                infixData.getDataName(),
                                infixData.getAdditionalArgs(),
                                implicit,
                                context_);

      return true ;
   }
   
   private AutoCompletionContext getAutocompletionContext()
   {
      
      
      String firstLine = input_.getText();
      int row = input_.getCursorPosition().getRow();
      
      // trim to cursor position
      firstLine = firstLine.substring(0, input_.getCursorPosition().getColumn());
      
      // Default case for failure modes
      AutoCompletionContext defaultContext = new AutoCompletionContext(firstLine, false);
      
      // if we're on the first row, don't bother looking back
      if (row == 0)
         return defaultContext;
      
      // early escaping rules: if we're in Roxygen, or we have text immediately
      // preceding the cursor (as that signals we're completing a variable name)
      if (firstLine.matches("\\s*#+'.*") ||
          firstLine.matches(".*[$@]$"))
         return defaultContext;
      
      // if the line is currently within a comment, bail -- this ensures
      // that we can auto-complete within a comment line (but we only
      // need context from that line)
      if (!firstLine.equals(StringUtil.stripRComment(firstLine)))
         return defaultContext;
      
      // if we're within a string, bail -- do this by stripping
      // balanced quotes and seeing if any quote characters left over
      // TODO: use code model so this survives multiline strings
      String stripped = StringUtil.stripBalancedQuotes(firstLine);
      if (!firstLine.equals(stripped))
      {
         boolean oddSingleQuotes = StringUtil.countMatches(stripped, '\'') % 2 == 1;
         boolean oddDoubleQuotes = StringUtil.countMatches(stripped, '"') % 2 == 1;
         if (oddSingleQuotes || oddDoubleQuotes)
         {
            return defaultContext;
         }
      }
      
      // access to the R Code model
      AceEditor editor = (AceEditor) docDisplay_;
      if (editor == null)
         return defaultContext;
      
      EditSession session = editor.getSession();
      if (session == null)
         return defaultContext;
      
      Mode mode = session.getMode();
      if (mode == null)
         return defaultContext;
      
      CodeModel codeModel = mode.getCodeModel();
      if (codeModel == null)
         return defaultContext;
      
      codeModel.tokenizeUpToRow(row);
      
      // Make a token cursor and place it at the first token previous
      // to the cursor.
      TokenCursor tokenCursor = codeModel.getTokenCursor();
      if (!tokenCursor.moveToPosition(input_.getCursorPosition()))
         return defaultContext;
      
      // Walk tokens backwards until we have one more '(' than we do
      // ')' with an empty 'block' token stack -- but only if we didn't
      // already hit a '(' as the first token
      if (tokenCursor.currentValue() != "(")
      {
         boolean success = tokenCursor.findOpeningParen();
         if (!success)
            return defaultContext;
      }
      
      // Move off of the open paren to the function name
      if (!tokenCursor.moveToPreviousToken())
         return defaultContext;
      
      // Take the inferred row up to current position and return
      int startRow = tokenCursor.currentPosition().getRow();
      
      StringBuilder resultBuilder = new StringBuilder();
      for (int i = startRow; i < row; i++)
      {
         resultBuilder.append(StringUtil.stripRComment(docDisplay_.getLine(i)));
      }
      resultBuilder.append(firstLine.substring(0,
                  input_.getCursorPosition().getColumn()));
            
      String result = resultBuilder.toString();
      
      result = StringUtil.stripBalancedQuotes(result);
      
      return new AutoCompletionContext(result, startRow < row);
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
         if (helpStrategy_ != null)
            helpStrategy_.showHelp(selectedItem, popup_) ;
      }

      public void showHelpTopic()
      {
         if (helpStrategy_ != null)
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
            if (docDisplay_ == null ||
                  (nativeEvent_ != null && nativeEvent_.getKeyCode() != KeyCodes.KEY_TAB)) {
               popup_.showErrorMessage(
                     "(No matches)", 
                     new PopupPositioner(input_.getCursorBounds(), popup_));
            }
            else
            {
               docDisplay_.insertCode("\t");
            }
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
      
      // For input of the form 'something$foo' or 'something@bar', quote the
      // element following '@' if it's a non-syntactic R symbol; otherwise
      // return as is
      private String quoteIfNotSyntacticNameCompletion(String string, char chr)
      {
         if (string.matches("[a-zA-Z_.][a-zA-Z0-9_.]*\\" + chr + ".*"))
         {
            int ind = string.lastIndexOf(chr);
            String before = string.substring(0, ind + 1);
            String after = string.substring(ind + 1, string.length());
            if (after.length() > 0)
            {
               return before + StringUtil.toRSymbolName(after);
            }
         }
         return string;
      }

      private void applyValue(final String fValue)
      {
         
         String value = fValue;
         
         // If the autocompletion is inserting something following an
         // @ or a $, e.g. for completion of an entry in object 'x'
         // named 'Some Value' as x$Some Value, surround it with
         // "`" -- do this for all names with non-alphanumeric elements
         value = quoteIfNotSyntacticNameCompletion(value, '@');
         value = quoteIfNotSyntacticNameCompletion(value, '$');
         
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
   
   private final DocDisplay docDisplay_;

   private final Invalidation invalidation_ = new Invalidation();
   private CompletionRequestContext context_ ;
   private final RnwCompletionContext rnwContext_;
   
   private NativeEvent nativeEvent_;
}
