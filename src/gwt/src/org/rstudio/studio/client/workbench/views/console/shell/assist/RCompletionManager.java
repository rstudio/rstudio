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
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorPosition;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorUtil;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.NavigableSourceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.CodeModel;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.EditSession;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Mode;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.RInfixData;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
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
   
   // Things we need to form an appropriate autocompletion:
   //
   // 1. The token to the left of the cursor,
   // 2. The associated function call (if any -- for arguments),
   // 3. The associated data for a `[` call (if any -- completions from data object),
   // 4. The associated data for a `[[` call (if any -- completions from data object)
   class AutoCompletionContext {
      
      // Be sure to sync these with 'SessionCodeTools.R'!
      public static final int TYPE_UNKNOWN = 0;
      public static final int TYPE_FUNCTION = 1;
      public static final int TYPE_SINGLE_BRACKET = 2;
      public static final int TYPE_DOUBLE_BRACKET = 3;
      public static final int TYPE_NAMESPACE_EXPORTED = 4;
      public static final int TYPE_NAMESPACE_ALL = 5;
      public static final int TYPE_DOLLAR = 6;
      public static final int TYPE_FILE = 7;
      
      public AutoCompletionContext(
            String content,
            String token,
            String assocData,
            int dataType,
            int numCommas)
      {
         content_ = content;
         token_ = token;
         assocData_ = assocData;
         dataType_ = dataType;
         numCommas_ = numCommas;
      }
      
      public String getContent()
      {
         return content_;
      }
      
      public String getToken()
      {
         return token_;
      }
      
      public String getAssocData()
      {
         return assocData_;
      }
      
      public int getDataType()
      {
         return dataType_;
      }
      
      public int getNumCommas()
      {
         return numCommas_;
      }

      private String content_;
      private String token_;
      private String assocData_;
      private int dataType_;
      private int numCommas_;
      
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
      
      if (!input_.hasSelection())
      {
         Debug.log("Cursor wasn't in input box or was in subelement");
         return false ;
      }

      boolean canAutoAccept = flushCache;
      
      // NOTE: This logic should be synced in 'SessionCodeTools.R'.
      boolean dontInsertParens = false;
      if (context.getDataType() == AutoCompletionContext.TYPE_FUNCTION &&
          (context.getAssocData() == "debug" ||
           context.getAssocData() == "trace"))
         dontInsertParens = true;
      
      context_ = new CompletionRequestContext(invalidation_.getInvalidationToken(),
                                              selection,
                                              canAutoAccept,
                                              dontInsertParens);
      
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
      
      requester_.getCompletions(
            context.getContent(),
            context.getToken(),
            context.getAssocData(),
            context.getDataType(),
            context.getNumCommas(),
            infixData.getDataName(),
            infixData.getAdditionalArgs(),
            infixData.getExcludeArgs(),
            implicit,
            context_);

      return true ;
   }
   
   private AutoCompletionContext getAutocompletionContextForFile(
         String token)
   {
      return new AutoCompletionContext(
            "",
            token.substring(1),
            "",
            AutoCompletionContext.TYPE_FILE,
            0);
   }
   
   private AutoCompletionContext getAutocompletionContextForNamespace(
         String token)
   {
         String[] splat = token.split(":{2,3}");
         String right = "";
         String left;
         if (splat.length <= 0)
         {
            right = "";
            left = "";
         }
         if (splat.length == 1)
         {
            right = "";
            left = splat[0];
         }
         else
         {
            right = splat[1];
            left = splat[0];
         }
            
         return new AutoCompletionContext(
               "",
               right,
               left,
               token.contains(":::") ?
                     AutoCompletionContext.TYPE_NAMESPACE_ALL :
                     AutoCompletionContext.TYPE_NAMESPACE_EXPORTED,
               0);
   }
   
   private AutoCompletionContext getAutocompletionContextForDollar(String token)
   {
      // Failure mode context
      AutoCompletionContext defaultContext = new AutoCompletionContext(
            "",
            token,
            "",
            AutoCompletionContext.TYPE_UNKNOWN,
            0);
      
      int dollarIndex = Math.max(token.lastIndexOf('$'), token.lastIndexOf('@'));
      String tokenToUse = token.substring(dollarIndex + 1);
      
      // Establish an evaluation context by looking backwards
      AceEditor editor = (AceEditor) docDisplay_;
      if (editor == null)
         return defaultContext;
      
      CodeModel codeModel = editor.getSession().getMode().getCodeModel();
      codeModel.tokenizeUpToRow(input_.getCursorPosition().getRow());
      
      TokenCursor cursor = codeModel.getTokenCursor();
         
      if (!cursor.moveToPosition(input_.getCursorPosition()))
         return defaultContext;
      
      // Move back to the '$'
      while (cursor.currentValue() != "$" && cursor.currentValue() != "@")
         if (!cursor.moveToPreviousToken())
            return defaultContext;
      
      // Put a cursor here
      TokenCursor contextEndCursor = cursor.cloneCursor();
      Debug.logObject(cursor.currentToken());
      
      // We allow for arbitrary elements previous, so we want to get e.g.
      //
      //     env::foo()$bar()[1]$baz
      
      while (cursor.currentValue() == "$" || cursor.currentValue() == "@") {
         
         if (!cursor.moveToPreviousToken())
            break;

         while (cursor.bwdToMatchingToken())
            if (!cursor.moveToPreviousToken())
               break;

         String type = cursor.currentType();
         if (type == "identifier" ||
             type == "string" ||
             type == "symbol")
         {
            if (!cursor.moveToPreviousToken())
               break;
            
            if (cursor.currentValue() == ":")
            {
               while (cursor.currentValue() == ":")
                  if (!cursor.moveToPreviousToken())
                     break;
               
               if (!cursor.moveToPreviousToken())
                  break;
            }
         }
         
      }
      
      // Correct for the off-by-one above if necessary
      Position pos = cursor.currentPosition();
      if (!(pos.getRow() == 0 && pos.getColumn() == 0))
         if (!cursor.moveToNextToken())
            return defaultContext;
      
      // Get the string forming the context
      String context = editor.getTextForRange(Range.fromPoints(
            cursor.currentPosition(),
            contextEndCursor.currentPosition()));
      
      // and return!
      return new AutoCompletionContext(
            "",
            tokenToUse,
            context,
            AutoCompletionContext.TYPE_DOLLAR,
            0);
      
   }
   
   
   private AutoCompletionContext getAutocompletionContext()
   {
      // Objects filled by this function and later returned
      String content = "";
      String token = "";
      String assocData = "";
      int dataType = 0;
      int numCommas = 0;
      
      String firstLine = input_.getText();
      int row = input_.getCursorPosition().getRow();
      
      // trim to cursor position
      firstLine = firstLine.substring(0, input_.getCursorPosition().getColumn());
      
      // Get the token at the cursor position
      token = firstLine.replaceAll(".*[\\s\\(\\[\\{,]", "");
      
      // If the token has '::' or ':::', escape early as we'll be completing
      // something from a namespace
      if (token.contains("::"))
         return getAutocompletionContextForNamespace(token);
      
      // If the token has '$' or '@', escape early as we'll be completing
      // either from names or an overloaded `$` method
      if (token.contains("$") || token.contains("@"))
         return getAutocompletionContextForDollar(token);
      
      // If we're completing an object within a string, assume it's a
      // file-system completion
      if (token.indexOf('\'') != -1 || token.indexOf('"') != -1)
         return getAutocompletionContextForFile(token);
      
      // Default case for failure modes
      AutoCompletionContext defaultContext = new AutoCompletionContext(
            firstLine,
            token,
            "", 
            AutoCompletionContext.TYPE_UNKNOWN, 
            0);
      
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
      
      // access to the R Code model
      AceEditor editor = (AceEditor) docDisplay_;
      if (editor == null)
         return defaultContext;
      
      CodeModel codeModel = editor.getSession().getMode().getCodeModel();
      codeModel.tokenizeUpToRow(row);
      
      // Make a token cursor and place it at the first token previous
      // to the cursor.
      TokenCursor tokenCursor = codeModel.getTokenCursor();
      if (!tokenCursor.moveToPosition(input_.getCursorPosition()))
         return defaultContext;
      
      TokenCursor startCursor = tokenCursor.cloneCursor();
      boolean startedOnEquals = tokenCursor.currentValue() == "=";
      if (startCursor.currentType() == "identifier")
         if (startCursor.moveToPreviousToken())
            if (startCursor.currentValue() == "=")
            {
               startedOnEquals = true;
               startCursor.moveToNextToken();
            }
      
      // Find an opening '(' or '[' -- this provides the function or object
      // for completion
      if (tokenCursor.currentValue() != "(" && tokenCursor.currentValue() != "[")
      {
         boolean success = tokenCursor.findOpeningParenOrBracket();
         if (!success)
            return defaultContext;
      }
      
      // Figure out whether we're looking at '(', '[', or '[[',
      // and place the token cursor on the first token preceding.
      TokenCursor endOfDecl = tokenCursor.cloneCursor();
      
      if (tokenCursor.currentValue() == "(")
      {
         // Don't produce function argument completions
         // if the cursor is on, or after, an '='
         if (!startedOnEquals)
            dataType = AutoCompletionContext.TYPE_FUNCTION;
         else
            dataType = AutoCompletionContext.TYPE_UNKNOWN;
         
         if (!tokenCursor.moveToPreviousToken())
            return defaultContext;
      }
      else if (tokenCursor.currentValue() == "[")
      {
         if (!tokenCursor.moveToPreviousToken())
            return defaultContext;
         
         if (tokenCursor.currentValue() == "[")
         {
            if (!endOfDecl.moveToPreviousToken())
               return defaultContext;
            
            dataType = AutoCompletionContext.TYPE_DOUBLE_BRACKET;
            if (!tokenCursor.moveToPreviousToken())
               return defaultContext;
         }
         else
         {
            dataType = AutoCompletionContext.TYPE_SINGLE_BRACKET;
         }
      }
      
      // Get the string marking the function or data
      do
      {
         String value = tokenCursor.currentValue();
         String type = tokenCursor.currentType();
         
         if (type == "identifier" || value == ":" || value == "$" || value == "@")
            continue;
         
         break;
         
      } while (tokenCursor.moveToPreviousToken());
      
      // We might have to move back up one if we failed on a non-associated token
      if (tokenCursor.currentType() != "identifier")
         if (!tokenCursor.moveToNextToken())
            return defaultContext;
      
      assocData = docDisplay_.getTextForRange(Range.fromPoints(
            tokenCursor.currentPosition(),
            endOfDecl.currentPosition()));
      
      content = docDisplay_.getTextForRange(Range.fromPoints(
            Position.create(0, 0),
            startCursor.currentPosition()));
      
      return new AutoCompletionContext(
            content,
            token,
            assocData,
            dataType,
            numCommas);
      
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
                                      boolean canAutoAccept,
                                      boolean dontInsertParens)
      {
         invalidationToken_ = token ;
         selection_ = selection ;
         canAutoAccept_ = canAutoAccept;
         dontInsertParens_ = dontInsertParens;
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
               applyValue(results[0], completions.dontInsertParens);
            
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

         applyValue(qname, dontInsertParens_);

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
      private String quoteIfNotSyntacticNameCompletion(String string)
      {
         return StringUtil.toRSymbolName(string);
      }

      private void applyValue(QualifiedName name, final boolean dontInsertParens)
      {
         
         final String functionName = name.name;
         final String pkgName = name.pkgName;
         
         server_.isFunction(functionName, pkgName, new ServerRequestCallback<Boolean>()
         {
            
            @Override
            public void onResponseReceived(Boolean isFunction)
            {
               String value = functionName;
               if (!value.matches(".*[=:]\\s*$") && 
                   !value.matches("^\\s*([`'\"]).*\\1\\s*$") &&
                   pkgName != "<file>")
                  value = quoteIfNotSyntacticNameCompletion(value);
               
               /* In some cases, applyValue can be called more than once
                * as part of the same completion instance--specifically,
                * if there's only one completion candidate and it is in
                * a package. To make sure that the selection movement
                * logic works the second time, we need to reset the
                * selection.
                */
               // Move range to beginning of token
               input_.setSelection(new InputEditorSelection(
                     selection_.getStart().movePosition(-token_.length(), true),
                     input_.getSelection().getEnd()));
         
               if (isFunction.booleanValue() && !dontInsertParens)
               {
                  // Don't replace the selection if the token ends with a ')'
                  // (implies an earlier replacement handled this)
                  if (token_.endsWith("("))
                  {
                     input_.setSelection(new InputEditorSelection(
                           input_.getSelection().getEnd(),
                           input_.getSelection().getEnd()));
                  }
                  else
                  {
                     input_.replaceSelection(value + "()", true);
                     InputEditorSelection newSelection = new InputEditorSelection(
                           input_.getSelection().getEnd().movePosition(-1, true));
                     token_ = value + "(";
                     selection_ = new InputEditorSelection(
                           input_.getSelection().getStart().movePosition(-2, true),
                           newSelection.getStart());

                     input_.setSelection(newSelection);

                  }
               }
               else
               {
                  input_.replaceSelection(value, true);
                  token_ = value;
                  selection_ = input_.getSelection();
               }
            }

            @Override
            public void onError(ServerError error)
            {
            }
            
         });
         

      }

      private final Invalidation.Token invalidationToken_ ;
      private InputEditorSelection selection_ ;
      private final boolean canAutoAccept_;
      private HelpStrategy helpStrategy_ ;
      private boolean suggestOnAccept_;
      private boolean dontInsertParens_;
      
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
