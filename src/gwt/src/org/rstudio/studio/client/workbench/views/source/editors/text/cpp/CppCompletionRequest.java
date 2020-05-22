/*
 * CppCompletionRequest.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text.cpp;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.snippets.SnippetHelper;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintItem;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.model.CppCompletion;
import org.rstudio.studio.client.workbench.views.source.model.CppCompletionResult;
import org.rstudio.studio.client.workbench.views.source.model.CppDiagnostic;
import org.rstudio.studio.client.workbench.views.source.model.CppServerOperations;

import java.util.ArrayList;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.inject.Inject;

public class CppCompletionRequest 
                           extends ServerRequestCallback<CppCompletionResult>
{
   public CppCompletionRequest(String docPath,
                               String docId,
                               CompletionPosition completionPosition,
                               DocDisplay docDisplay, 
                               Invalidation.Token token,
                               boolean explicit,
                               CppCompletionManager manager,
                               Command onTerminated)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      docDisplay_ = docDisplay;
      completionPosition_ = completionPosition;
      invalidationToken_ = token;
      explicit_ = explicit;
      manager_ = manager;
      onTerminated_ = onTerminated;
      snippets_ = new SnippetHelper((AceEditor) docDisplay, docPath);
      
      // Get the current line (up to the cursor position)
      Position cursorPos = docDisplay_.getCursorPosition();
      String line = docDisplay_.getLine(cursorPos.getRow()).substring(0, cursorPos.getColumn());
      
      Position completionPos = completionPosition_.getPosition();
      server_.getCppCompletions(line,
                                docPath,
                                docId,
                                completionPos.getRow() + 1, 
                                completionPos.getColumn() + 1, 
                                completionPosition_.getUserText(),
                                this);
   }

   @Inject
   void initialize(CppServerOperations server, UserPrefs uiPrefs)
   {
      server_ = server;
      userPrefs_ = uiPrefs;
   }
   
   public boolean isExplicit()
   {
      return explicit_;
   }
   
   public CompletionPosition getCompletionPosition()
   {
      return completionPosition_;
   }
   
   public CppCompletionPopupMenu getCompletionPopup()
   {
      return popup_;
   }
     
   public void updateUI(boolean autoAccept)
   {
      if (invalidationToken_.isInvalid())
         return;

      // if we don't have the completion list back from the server yet
      // then just ignore this (this function will get called again when
      // the request completes)
      if (completions_ == null)
         return;

      // discover text already entered
      String userTypedText = getUserTypedText();

      // build list of entries (filter on text already entered)
      JsArray<CppCompletion> filtered = JsArray.createArray().cast();
      for (int i = 0; i < completions_.length(); i++)
      {
         CppCompletion completion = completions_.get(i);
         String typedText = completion.getTypedText();
         if ((userTypedText.length() == 0) || 
               typedText.startsWith(userTypedText))
         {
            // be more picky for member scope completions because clang
            // returns a bunch of noise like constructors, destructors, 
            // compiler generated assignments, etc.
            if (completionPosition_.getScope() == 
                  CompletionPosition.Scope.Member)
            {
               if (completion.getType() == CppCompletion.VARIABLE ||
                     (completion.getType() == CppCompletion.FUNCTION &&
                     !typedText.startsWith("operator=")))
               {
                  filtered.push(completion);
               }

            }
            else
            {
               filtered.push(completion);
            }
         }
      }
      
      // add in snippets if they are enabled and this is a global scope
      if (userPrefs_.enableSnippets().getValue() &&
          (completionPosition_.getScope() == CompletionPosition.Scope.Global))
      {
         ArrayList<String> snippets = snippets_.getAvailableSnippets();
         for (String snippet : snippets)
            if (snippet.startsWith(userTypedText))
            {
               String content = snippets_.getSnippet(snippet).getContent();
               content = content.replace("\t", "  ");
               filtered.unshift(CppCompletion.createSnippetCompletion(snippet,
                                                                      content));
            }
      }

      // check for no hits on explicit request
      if ((filtered.length() == 0) && explicit_)
      {
         showCompletionPopup("(No matches)");
      }
      // check for auto-accept
      else if ((filtered.length() == 1) && autoAccept && explicit_)
      {
         applyValue(filtered.get(0));
      }
      // check for one completion that's already present
      else if (filtered.length() == 1 && 
            filtered.get(0).getTypedText() == getUserTypedText() &&
            filtered.get(0).getType() != CppCompletion.SNIPPET)
      {
         terminate();
      }
      else
      {
         showCompletionPopup(filtered);
      }
   }
   
   public void terminate()
   {
      closeCompletionPopup();
      terminated_ = true;
      if (onTerminated_ != null)
         onTerminated_.execute();
   }
   
   public boolean isTerminated()
   {
      return terminated_;
   }
   
   @Override
   public void onResponseReceived(CppCompletionResult result)
   {
      if (invalidationToken_.isInvalid())
         return;
      
      // null result means that completion is not supported for this file
      if (result == null)
         return;    
       
      // get the completions
      completions_ = result.getCompletions();
      
      // update the UI
      updateUI(true);
   }
   
   static Pattern RE_NO_MEMBER_NAMED =
         Pattern.create("^no member named '(.*)' in '(.*)'$");
   
   static Pattern RE_USE_UNDECLARED_IDENTIFIER =
         Pattern.create("^use of undeclared identifier '(.*)'");
   
   private static Range createRangeFromMatch(CppDiagnostic diagnostic,
                                      Match match)
   {
      return Range.create(
            diagnostic.getPosition().getLine() - 1,
            diagnostic.getPosition().getColumn() - 1,
            diagnostic.getPosition().getLine() - 1,
            diagnostic.getPosition().getColumn() - 1 +
               match.getGroup(1).length());
   }
   
   private static Range getRangeForSpecializedDiagnostic(CppDiagnostic diagnostic)
   {
      String message = diagnostic.getMessage();
      Match match;
      
      match = RE_NO_MEMBER_NAMED.match(message, 0);
      if (match != null)
         return createRangeFromMatch(diagnostic, match);
      
      match = RE_USE_UNDECLARED_IDENTIFIER.match(message, 0);
      if (match != null)
         return createRangeFromMatch(diagnostic, match);
      
      return null;
   }
   
   private static Range getRangeForDiagnostic(CppDiagnostic diagnostic)
   {
      // Try to get a range for specialized diagnostics
      Range range;
      
      range = getRangeForSpecializedDiagnostic(diagnostic);
      if (range != null)
         return range;
      
      // Default range -- override if we infer a better range
      return Range.create(
            diagnostic.getPosition().getLine() - 1,
            diagnostic.getPosition().getColumn() - 1,
            diagnostic.getPosition().getLine() - 1,
            diagnostic.getPosition().getColumn());
   }
   
   public static JsArray<LintItem> asLintArray(
         JsArray<CppDiagnostic> diagnostics)
   {
      JsArray<LintItem> lint = JsArray.createArray(diagnostics.length()).cast();
      for (int i = 0; i < diagnostics.length(); i++)
      {
         CppDiagnostic d = diagnostics.get(i);
         if (d.getPosition() != null)
         {
            Range range = getRangeForDiagnostic(d);
               lint.set(i, LintItem.create(
                     range.getStart().getRow(),
                     range.getStart().getColumn(),
                     range.getEnd().getRow(),
                     range.getEnd().getColumn(),
                     d.getMessage(),
                     cppDiagnosticSeverityToLintType(d.getSeverity())));
         }
      }
      
      return lint;
   }
   
   private static String cppDiagnosticSeverityToLintType(int type)
   {
      switch (type)
      {
         case CppDiagnostic.IGNORED: return "ignored";
         case CppDiagnostic.NOTE:    return "note";
         case CppDiagnostic.WARNING: return "warning";
         case CppDiagnostic.ERROR:   return "error";
         case CppDiagnostic.FATAL:   return "fatal";
         default: return "";
      }
   }
   
   private void showCompletionPopup(String message)
   {
      if (popup_ == null)
         popup_ = createCompletionPopup();
      popup_.setText(message);
      
      docDisplay_.setPopupVisible(true);
     
   }
   
   private void showCompletionPopup(JsArray<CppCompletion> completions)
   {
      // clear any existing signature tips
      if (completions.length() > 0)
         CppCompletionSignatureTip.hideAll();
        
      if (popup_ == null)
         popup_ = createCompletionPopup();
      popup_.setCompletions(completions, new CommandWithArg<CppCompletion>() {
         @Override
         public void execute(CppCompletion completion)
         {
            applyValue(completion);
         } 
      });
      
      // notify document of popup status
      docDisplay_.setPopupVisible(completions.length() > 0);
   }
      
   
   private CppCompletionPopupMenu createCompletionPopup()
   {
      CppCompletionPopupMenu popup = new CppCompletionPopupMenu(
          docDisplay_, completionPosition_);
      
      popup.addCloseHandler(new CloseHandler<PopupPanel>() {

         @Override
         public void onClose(CloseEvent<PopupPanel> event)
         {
            closeCompletionPopup();
            terminated_ = true;
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  docDisplay_.setPopupVisible(false);
               }
            });
         } 
      });
      
      return popup;
   }
   
   private void closeCompletionPopup()
   {
      if (popup_ != null)
      {
         popup_.hide();
         popup_ = null;
      }
   }
   
   @Override
   public void onError(ServerError error)
   {
      if (invalidationToken_.isInvalid())
         return;
      
      if (explicit_)
         showCompletionPopup(error.getUserMessage());
   }
   
   private void applyValue(CppCompletion completion)
   {
      if (invalidationToken_.isInvalid())
         return;
      
      terminate();
      
      if (completion.getType() == CppCompletion.SNIPPET)
      {
         snippets_.applySnippet(getUserTypedText(), completion.getTypedText());
         return;
      }
     
      String insertText = completion.getTypedText();
      if (completion.getType() == CppCompletion.FUNCTION &&
            userPrefs_.insertParensAfterFunctionCompletion().getValue())
      {
         if (userPrefs_.insertMatching().getValue())
            insertText = insertText + "()";
         else
            insertText = insertText + "(";
      }
      else if (completion.getType() == CppCompletion.DIRECTORY)
      {
         insertText += "/";
         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {
            @Override
            public void execute()
            {
               manager_.codeCompletion();
            }
         });
      }
      
      docDisplay_.setFocus(true); 
      docDisplay_.setSelection(getReplacementSelection());
      docDisplay_.replaceSelection(insertText, true);
      
      if (completion.hasParameters() &&
            userPrefs_.insertParensAfterFunctionCompletion().getValue() &&
            userPrefs_.insertMatching().getValue())
      {
         Position pos = docDisplay_.getCursorPosition();
         pos = Position.create(pos.getRow(), pos.getColumn() - 1);
         docDisplay_.setSelectionRange(Range.fromPoints(pos, pos));
      }
      else if (completion.getType() == CppCompletion.FILE)
      {
         char ch = docDisplay_.getCharacterAtCursor();
         
         // if there is a '>' or '"' following the cursor, move over it
         if (ch == '>' || ch == '"')
         {
            docDisplay_.moveCursorForward();
         }
         
         // otherwise, insert the corresponding closing character
         else
         {
            String line = docDisplay_.getCurrentLine();
            if (line.contains("<"))
               docDisplay_.insertCode(">");
            else if (line.contains("\""))
               docDisplay_.insertCode("\"");
         }
      }
      
      if (completion.hasParameters() &&
          userPrefs_.showFunctionSignatureTooltips().getValue())
      {
         new CppCompletionSignatureTip(completion, docDisplay_);
      }
   }
   
   private InputEditorSelection getReplacementSelection()
   {
      return docDisplay_.createSelection(completionPosition_.getPosition(), 
                                         docDisplay_.getCursorPosition());
   }
   
   private String getUserTypedText()
   {
      return docDisplay_.getCode(
        completionPosition_.getPosition(), docDisplay_.getCursorPosition());
   }
   
   private CppServerOperations server_;
   private UserPrefs userPrefs_;
  
   private final DocDisplay docDisplay_; 
   private final boolean explicit_;
   private final CppCompletionManager manager_;
   private final Invalidation.Token invalidationToken_;
   
   private final SnippetHelper snippets_;
   
   private final CompletionPosition completionPosition_;
   
   private CppCompletionPopupMenu popup_;
   private JsArray<CppCompletion> completions_;
   
   private boolean terminated_ = false;
   private Command onTerminated_ = null;
}
