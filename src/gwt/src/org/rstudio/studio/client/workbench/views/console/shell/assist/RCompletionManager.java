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
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.DplyrJoinContext;
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
      public static final int TYPE_AT = 7;
      public static final int TYPE_FILE = 8;
      public static final int TYPE_CHUNK = 9;
      
      public AutoCompletionContext(
            String token,
            ArrayList<String> assocData,
            ArrayList<Integer> dataType,
            ArrayList<Integer> numCommas,
            String functionCallString)
      {
         token_ = token;
         assocData_ = assocData;
         dataType_ = dataType;
         numCommas_ = numCommas;
         functionCallString_ = functionCallString;
      }
      
      public AutoCompletionContext(
            String token,
            ArrayList<String> assocData,
            ArrayList<Integer> dataType)
      {
         token_ = token;
         assocData_ = assocData;
         dataType_ = dataType;
         numCommas_.add(0);
      }
      
      public AutoCompletionContext(
            String token,
            String assocData,
            int dataType)
      {
         token_ = token;
         assocData_.add(assocData);
         dataType_.add(dataType);
         numCommas_.add(0);
      }
      
      
      public AutoCompletionContext(
            String token,
            int dataType)
      {
         token_ = token;
         assocData_.add("");
         dataType_.add(dataType);
         numCommas_.add(0);
      }
      
      public String getToken()
      {
         return token_;
      }
      
      public ArrayList<String> getAssocData()
      {
         return assocData_;
      }
      
      public ArrayList<Integer> getDataType()
      {
         return dataType_;
      }
      
      public ArrayList<Integer> getNumCommas()
      {
         return numCommas_;
      }
      
      public String getFunctionCallString()
      {
         return functionCallString_;
      }

      private String token_ = "";
      private ArrayList<String> assocData_ = new ArrayList<String>();
      private ArrayList<Integer> dataType_ = new ArrayList<Integer>();
      private ArrayList<Integer> numCommas_ = new ArrayList<Integer>();
      private String functionCallString_ = "";
      
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
      
      context_ = new CompletionRequestContext(invalidation_.getInvalidationToken(),
                                              selection,
                                              canAutoAccept);
      
      RInfixData infixData = RInfixData.create();
      AceEditor editor = (AceEditor) docDisplay_;
      if (editor != null)
      {
         CodeModel codeModel = editor.getSession().getMode().getCodeModel();
         TokenCursor cursor = codeModel.getTokenCursor();
         
         if (cursor.moveToPosition(input_.getCursorPosition()))
         {
            String token = "";
            if (cursor.currentType() == "identifier")
               token = cursor.currentValue();
            
            String cursorPos = "left";
            if (cursor.currentValue() == "=")
               cursorPos = "right";
            
            TokenCursor clone = cursor.cloneCursor();
            if (clone.moveToPreviousToken())
               if (clone.currentValue() == "=")
                  cursorPos = "right";
            
            // Try to get a dplyr join completion
            DplyrJoinContext joinContext =
                  codeModel.getDplyrJoinContextFromInfixChain(cursor);
            
            // If that failed, try a non-infix lookup
            if (joinContext == null)
            {
               String joinString =
                     getDplyrJoinString(editor, cursor);
               
               if (!StringUtil.isNullOrEmpty(joinString))
               {
                  requester_.getDplyrJoinCompletionsString(
                        token,
                        joinString,
                        cursorPos,
                        implicit,
                        context_);

                  return true;
               }
            }
            else
            {
               requester_.getDplyrJoinCompletions(
                     joinContext,
                     implicit,
                     context_);
               return true;
               
            }
            
            // Try to see if there's an object name we should use to supplement
            // completions
            if (cursor.moveToPosition(input_.getCursorPosition()))
               infixData = codeModel.getDataFromInfixChain(cursor);
         }
      }
      
      requester_.getCompletions(
            context.getToken(),
            context.getAssocData(),
            context.getDataType(),
            context.getNumCommas(),
            context.getFunctionCallString(),
            infixData.getDataName(),
            infixData.getAdditionalArgs(),
            infixData.getExcludeArgs(),
            implicit,
            context_);

      return true ;
   }
   
   private String getDplyrJoinString(
         AceEditor editor,
         TokenCursor cursor)
   {
      while (true)
      {
         int commaCount = cursor.findOpeningBracketCountCommas("(", true);
         if (commaCount == -1)
            break;
         
         if (!cursor.moveToPreviousToken())
            return "";

         if (!cursor.currentValue().matches(".*join$"))
            continue;
         
         if (commaCount < 2)
            return "";

         Position start = cursor.currentPosition();
         if (!cursor.moveToNextToken())
            return "";

         if (!cursor.fwdToMatchingToken())
            return "";

         Position end = cursor.currentPosition();
         end.setColumn(end.getColumn() + 1);

         return editor.getTextForRange(Range.fromPoints(
               start, end));
      }
      return "";
   }
   
   
   private AutoCompletionContext getAutocompletionContextForFile(
         String token)
   {
      return new AutoCompletionContext(
            token.substring(1),
            AutoCompletionContext.TYPE_FILE);
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
               right,
               left,
               token.contains(":::") ?
                     AutoCompletionContext.TYPE_NAMESPACE_ALL :
                     AutoCompletionContext.TYPE_NAMESPACE_EXPORTED);
   }
   
   private boolean isValidAsIdentifier(TokenCursor cursor)
   {
      String type = cursor.currentType();
      return type == "identifier" ||
             type == "symbol" ||
             type == "keyword" ||
             type == "string";
   }
   
   private boolean isLookingAtInfixySymbol(TokenCursor cursor)
   {
      String value = cursor.currentValue();
      if (value == "$" ||
          value == "@" ||
          value == ":")
         return true;
      
      if (cursor.currentType().contains("infix"))
         return true;
      
      return false;
   }
   
   // Find the start of the evaluation context for a generic expression,
   // e.g.
   //
   //     x[[1]]$foo[[1]][, 2]@bar[[1]]()
   private boolean findStartOfEvaluationContext(TokenCursor cursor)
   {
      TokenCursor clone = cursor.cloneCursor();
      
//       Debug.logToConsole("Starting search at:");
//       Debug.logObject(clone.currentToken());
      
      do
      {
         if (clone.bwdToMatchingToken())
            continue;
         
         // If we land on an identifier, we keep going if the token previous is
         // 'infix-y', and bail otherwise.
         if (isValidAsIdentifier(clone))
         {
            if (!clone.moveToPreviousToken())
               break;
            
            if (isLookingAtInfixySymbol(clone))
               continue;
            
            if (!clone.moveToNextToken())
               return false;
            
            break;
               
         }
         
      } while (clone.moveToPreviousToken());
      
//      Debug.logToConsole("Stopping search at:");
//      Debug.logObject(clone.currentToken());
      
      cursor.setRow(clone.getRow());
      cursor.setOffset(clone.getOffset());
      return true;
      
   }
   
   private AutoCompletionContext getAutocompletionContextForDollar(String token)
   {
      // Failure mode context
      AutoCompletionContext defaultContext = new AutoCompletionContext(
            token,
            AutoCompletionContext.TYPE_UNKNOWN);
      
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
      
      int type = cursor.currentValue() == "$" ?
            AutoCompletionContext.TYPE_DOLLAR :
            AutoCompletionContext.TYPE_AT;
      
      // Put a cursor here
      TokenCursor contextEndCursor = cursor.cloneCursor();
      
      // We allow for arbitrary elements previous, so we want to get e.g.
      //
      //     env::foo()$bar()[1]$baz
      // Get the string forming the context
      Debug.logObject(cursor);
      if (!findStartOfEvaluationContext(cursor))
         return defaultContext;
      
      Debug.logObject(cursor);
      
      String context = editor.getTextForRange(Range.fromPoints(
            cursor.currentPosition(),
            contextEndCursor.currentPosition()));
      
      // and return!
      return new AutoCompletionContext(
            tokenToUse,
            context,
            type);
   }
   
   
   private AutoCompletionContext getAutocompletionContext()
   {
      // Objects filled by this function and later returned
      String token = "";
      ArrayList<String> assocData = new ArrayList<String>();
      ArrayList<Integer> dataType = new ArrayList<Integer>();
      ArrayList<Integer> numCommas = new ArrayList<Integer>();
      
      // Some information re: the function call, if appropriate.
      String functionCallString = "";
      
      String firstLine = input_.getText();
      int row = input_.getCursorPosition().getRow();
      
      // trim to cursor position
      firstLine = firstLine.substring(0, input_.getCursorPosition().getColumn());
      
      // If this line starts with '```{', then we're completing chunk options
      // pass the whole line as a token
      if (firstLine.startsWith("```{") || firstLine.startsWith("<<"))
         return new AutoCompletionContext(firstLine, AutoCompletionContext.TYPE_CHUNK);
      
      
      // Get the token at the cursor position
      token = firstLine.replaceAll(".*[^a-zA-Z0-9._:$@]", "");
      
      // Default case for failure modes
      AutoCompletionContext defaultContext = new AutoCompletionContext(
            token,
            AutoCompletionContext.TYPE_UNKNOWN);
      
      // escape early for roxygen
      if (firstLine.matches("\\s*#+'.*"))
         return defaultContext;
      
      // if the line is currently within a comment, bail -- this ensures
      // that we can auto-complete within a comment line (but we only
      // need context from that line)
      if (!firstLine.equals(StringUtil.stripRComment(firstLine)))
         return defaultContext;
      
      // If the token has '$' or '@', escape early as we'll be completing
      // either from names or an overloaded `$` method
      if (token.contains("$") || token.contains("@"))
         return getAutocompletionContextForDollar(token);
      
      // If the token has '::' or ':::', escape early as we'll be completing
      // something from a namespace
      if (token.contains("::"))
         return getAutocompletionContextForNamespace(token);
      
      
      // Now strip the '$' and '@' post-hoc since they're not really part
      // of the identifier
      token = token.replaceAll(".*[$@]", "");
      
      // If we're completing an object within a string, assume it's a
      // file-system completion
      if (token.indexOf('\'') != -1 || token.indexOf('"') != -1)
         return getAutocompletionContextForFile(token);
      
      // access to the R Code model
      AceEditor editor = (AceEditor) docDisplay_;
      if (editor == null)
         return defaultContext;
      
      CodeModel codeModel = editor.getSession().getMode().getCodeModel();
      
      // We might need to grab content from further up in the document than
      // the current cursor position -- so tokenize ahead.
      codeModel.tokenizeUpToRow(row + 100);
      
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
      // for completion.
      int initialNumCommas = 0;
      if (tokenCursor.currentValue() != "(" && tokenCursor.currentValue() != "[")
      {
         int commaCount = tokenCursor.findOpeningBracketCountCommas(new String[]{ "[", "(" }, true);
         if (commaCount == -1)
         {
            commaCount = tokenCursor.findOpeningBracketCountCommas("[", false);
            if (commaCount == -1)
               return defaultContext;
            else
               initialNumCommas = commaCount;
         }
         else
         {
            initialNumCommas = commaCount;
         }
      }
      numCommas.add(initialNumCommas);
      
      // Figure out whether we're looking at '(', '[', or '[[',
      // and place the token cursor on the first token preceding.
      TokenCursor endOfDecl = tokenCursor.cloneCursor();
      if (tokenCursor.currentValue() == "(")
      {
         // Don't produce function argument completions
         // if the cursor is on, or after, an '='
         if (!startedOnEquals)
            dataType.add(AutoCompletionContext.TYPE_FUNCTION);
         else
            dataType.add(AutoCompletionContext.TYPE_UNKNOWN);
         
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
            
            dataType.add(AutoCompletionContext.TYPE_DOUBLE_BRACKET);
            if (!tokenCursor.moveToPreviousToken())
               return defaultContext;
         }
         else
         {
            dataType.add(AutoCompletionContext.TYPE_SINGLE_BRACKET);
         }
      }
      
      // Get the string marking the function or data
      if (!findStartOfEvaluationContext(tokenCursor))
         return defaultContext;
      
      Position endPos = startCursor.currentPosition();
      endPos.setColumn(endPos.getColumn() + startCursor.currentValue().length());
      
      functionCallString = editor.getTextForRange(Range.fromPoints(
            tokenCursor.currentPosition(), endPos));
      
      assocData.add(
            docDisplay_.getTextForRange(Range.fromPoints(
                  tokenCursor.currentPosition(),
                  endOfDecl.currentPosition())));
      
      // Get the rest of the single-bracket contexts for completions as well
      while (true)
      {
         int commaCount = tokenCursor.findOpeningBracketCountCommas("[", false);
         if (commaCount == -1)
            break;
         
         numCommas.add(commaCount);
         
         TokenCursor declEnd = tokenCursor.cloneCursor();
         if (!tokenCursor.moveToPreviousToken())
            return defaultContext;
         
         if (tokenCursor.currentValue() == "[")
         {
            if (!declEnd.moveToPreviousToken())
               return defaultContext;
            
            dataType.add(AutoCompletionContext.TYPE_DOUBLE_BRACKET);
            if (!tokenCursor.moveToPreviousToken())
               return defaultContext;
         }
         else
         {
            dataType.add(AutoCompletionContext.TYPE_SINGLE_BRACKET);
         }
         
      assocData.add(
            docDisplay_.getTextForRange(Range.fromPoints(
                  tokenCursor.currentPosition(),
                  declEnd.currentPosition())));
      }
      
      return new AutoCompletionContext(
            token,
            assocData,
            dataType,
            numCommas,
            functionCallString);
      
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
            boolean lastInputWasTab =
                  (nativeEvent_ != null && nativeEvent_.getKeyCode() == KeyCodes.KEY_TAB);
            
            boolean lineIsWhitespace = docDisplay_.getCurrentLine().matches("^\\s*$");
            
            if (lastInputWasTab && lineIsWhitespace)
               docDisplay_.insertCode("\t");
            else
               popup_.showErrorMessage(
                     "(No matches)", 
                     new PopupPositioner(input_.getCursorBounds(), popup_));
            
            return ;
         }

         initializeHelpStrategy(completions) ;
         
         // Move range to beginning of token; we want to place the popup there.
         final String token = completions.token ;

         Rectangle rect = input_.getPositionBounds(
               selection_.getStart().movePosition(-token.length(), true));

         token_ = token ;
         suggestOnAccept_ = completions.suggestOnAccept;
         overrideInsertParens_ = completions.dontInsertParens;

         if (results.length == 1
             && canAutoAccept_
             && StringUtil.isNullOrEmpty(results[0].pkgName))
         {
            onSelection(results[0]);
         }
         else
         {
            if (results.length == 1 && canAutoAccept_)
               applyValue(results[0]);
            
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

         applyValue(qname);

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
      
      private void applyValueRmdOption(final String value)
      {
         input_.setSelection(new InputEditorSelection(
               selection_.getStart().movePosition(-token_.length(), true),
               input_.getSelection().getEnd()));

         input_.replaceSelection(value, true);
         token_ = value;
         selection_ = input_.getSelection();
      }

      private void applyValue(QualifiedName name)
      {
         final String functionName = name.name == null ? "" : name.name;
         final String pkgName = name.pkgName == null ? "" : name.pkgName;
         final boolean shouldQuote = name.shouldQuote;
         
         if (pkgName == "`chunk-option`")
         {
            applyValueRmdOption(functionName);
            return;
         }
         
         server_.isFunction(functionName, pkgName, new ServerRequestCallback<Boolean>()
         {
            
            @Override
            public void onResponseReceived(Boolean isFunction)
            {
               // Don't insert a paren if there is already a '(' following
               // the cursor
               AceEditor editor = (AceEditor) input_;
               boolean textFollowingCursorHasOpenParen = false;
               if (editor != null)
               {
                  TokenCursor cursor = 
                        editor.getSession().getMode().getCodeModel().getTokenCursor();
                  cursor.moveToPosition(editor.getCursorPosition());
                  if (cursor.moveToNextToken())
                     textFollowingCursorHasOpenParen =
                        cursor.currentValue() == "(";
               }
               
               String value = functionName;
               if (value == ":=")
                  value = quoteIfNotSyntacticNameCompletion(value);
               else if (!value.matches(".*[=:]\\s*$") && 
                   !value.matches("^\\s*([`'\"]).*\\1\\s*$") &&
                   pkgName != "<file>" &&
                   pkgName != "`chunk-option`" &&
                   !value.startsWith("@") &&
                   !shouldQuote)
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
         
               if (isFunction && !overrideInsertParens_ && !textFollowingCursorHasOpenParen)
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
                  if (shouldQuote)
                     value = "\"" + value + "\"";
                  
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
      private boolean overrideInsertParens_;
      
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
