/*
 * PythonCompletionManager.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.console.ConsoleConstants;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenIterator;

import com.google.gwt.core.client.JsArray;

import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.QualifiedName;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;

public class PythonCompletionManager extends CompletionManagerBase
                                     implements CompletionManager
{
   // Use a funstructor to create an instance in order to ensure toggleHandlers()
   // is invoked after the object is fully instantiated
   public static PythonCompletionManager create(DocDisplay docDisplay,
                                                CompletionPopupDisplay popup,
                                                CodeToolsServerOperations server,
                                                CompletionContext context)
   {
      PythonCompletionManager retVal = new PythonCompletionManager(docDisplay, popup, server, context);

      retVal.toggleHandlers(true);

      return retVal;
   }

   // Use the create() funstructor above instead of invoking this constructor directly
   private PythonCompletionManager(DocDisplay docDisplay,
                                  CompletionPopupDisplay popup,
                                  CodeToolsServerOperations server,
                                  CompletionContext context)
   {
      super(popup, docDisplay, server, context);
   }
   
   // Helper class for determining the appropriate line + cursor
   // position to send down to the session when requesting completions
   private static class PythonEditorContext
   {
      public PythonEditorContext(DocDisplay docDisplay)
      {
         Position cursorPos = docDisplay.getCursorPosition();
         
         String line = docDisplay.getLine(cursorPos.getRow());
         
         int position = cursorPos.getColumn();
         
         // record position as offset from end of line
         int endOffset = line.length() - position;
         
         for (int row = cursorPos.getRow() - 1;
              row >= 0;
              row--)
         {
            JsArray<Token> tokens = docDisplay.getTokens(row);
            if (tokens.length() == 0)
               continue;
            
            Token token = tokens.get(tokens.length() - 1);
            
            boolean isContinuation =
                  token.hasType("text") &&
                  token.getValue().endsWith("\\");
            
            if (!isContinuation)
               break;
            
            String prevLine = docDisplay.getLine(row);
            
            // we need to add the previous line sans the ending '\',
            // and also trim off any leading whitespace on the current line
            line =
                  prevLine.replaceAll("\\s*\\\\$", "") +
                  line.replaceAll("^\\s*", "");
         }
         
         // update line
         line_ = line;
         
         // update cursor position, using end offset to recompute
         position_ = line.length() - endOffset;
      }
      
      public String getLine()
      {
         return line_;
      }
      
      public int getPosition()
      {
         return position_;
      }
      
      private String line_;
      private int position_;
   }
   
   @Override
   public void goToHelp()
   {
      final GlobalProgressDelayer progress = new GlobalProgressDelayer(
            RStudioGinjector.INSTANCE.getGlobalDisplay(),
            1000,
            constants_.openingHelpProgressMessage());
      
      PythonEditorContext context = new PythonEditorContext(docDisplay_);
      
      server_.pythonGoToHelp(
            context.getLine(),
            context.getPosition(),
            new ServerRequestCallback<Boolean>()
            {
               @Override
               public void onResponseReceived(Boolean response)
               {
                  progress.dismiss();
               }

               @Override
               public void onError(ServerError error)
               {
                  progress.dismiss();
                  Debug.logError(error);
               }
            });
   }

   @Override
   public void goToDefinition()
   {
      final GlobalProgressDelayer progress = new GlobalProgressDelayer(
            RStudioGinjector.INSTANCE.getGlobalDisplay(),
            1000,
            constants_.findingDefinitionProgressMessage());
      
      PythonEditorContext context = new PythonEditorContext(docDisplay_);
      
      server_.pythonGoToDefinition(
            context.getLine(),
            context.getPosition(),
            new ServerRequestCallback<Boolean>()
            {
               @Override
               public void onResponseReceived(Boolean response)
               {
                  progress.dismiss();
               }

               @Override
               public void onError(ServerError error)
               {
                  progress.dismiss();
                  Debug.logError(error);
               }
            });
   }
   
   @Override
   public void showAdditionalHelp(QualifiedName completion)
   {
      final GlobalProgressDelayer progress = new GlobalProgressDelayer(
            RStudioGinjector.INSTANCE.getGlobalDisplay(),
            1000,
            constants_.openingHelpProgressMessage());
      
      String line;
      int position;
      
      if (StringUtil.isNullOrEmpty(completion.source))
      {
         line = completion.name;
         position = 0;
      }
      else
      {
         line = completion.source + "." + completion.name;
         position = completion.source.length() + 2;
      }
      
      server_.pythonGoToHelp(line, position, new ServerRequestCallback<Boolean>()
      {
         @Override
         public void onResponseReceived(Boolean response)
         {
            progress.dismiss();
         }

         @Override
         public void onError(ServerError error)
         {
            progress.dismiss();
            Debug.logError(error);
         }
      });
   }

   @Override
   public boolean getCompletions(String line, CompletionRequestContext context)
   {
      server_.pythonGetCompletions(buildCompletionLine(), completionContext(), context);
      return true;
   }
   
   @Override
   public boolean isTriggerCharacter(char ch)
   {
      return ch == '.';
   }
   
   private PythonCompletionContext completionContext()
   {
      PythonCompletionContext context = new PythonCompletionContext();
      discoverImports(context);
      return context;
   }
   
   private void discoverImports(PythonCompletionContext context)
   {
      Match match;
      
      int row = docDisplay_.getCursorPosition().getRow();
      String cursorLine = docDisplay_.getLine(row);
      String currentIndent = StringUtil.getIndent(cursorLine);
      
      for (; row >= 0; row--)
      {
         String line = docDisplay_.getLine(row);
         String indent = StringUtil.getIndent(line);
         
         // if the indent of this line is greater than that
         // of the cursor, assume it's part of a different
         // scope and don't use it
         if (indent.length() > currentIndent.length())
            continue;
         
         // shrink current indent if it's smaller now
         if (indent.length() < currentIndent.length())
            currentIndent = indent;
         
         // try to match imports
         match = RE_IMPORT.match(line, 0);
         if (match != null)
         {
            context.aliases.set(match.getGroup(1), match.getGroup(1));
            continue;
         }
         
         match = RE_IMPORT_AS.match(line, 0);
         if (match != null)
         {
            context.aliases.set(match.getGroup(2), match.getGroup(1));
            continue;
         }
      }
   }
   
   private String singleLineCompletion()
   {
      PythonEditorContext context = new PythonEditorContext(docDisplay_);
      
      String line = context.getLine();
      int position = context.getPosition();
      return line.substring(0, position);
   }
   
   // this routine is primarily used to provide some extra context
   // for argument completions and import completions; e.g. when
   // the document contains:
   //
   //    from numpy import (
   //       co|
   //
   // or, for function calls:
   //
   //    x = pandas.DataFrame(
   //       da|
   //
   // to handle this, we attempt to walk back tokens until we
   // see an opening bracket; if we do, we use that as the
   // 'anchor' for completion. 
   private String buildCompletionLine()
   {
      // move to current token
      TokenIterator it = docDisplay_.createTokenIterator();
      Token token = it.moveToPosition(docDisplay_.getCursorPosition());
      if (token == null)
         return singleLineCompletion();
      
      // move off of comments, text
      while (token.hasType("text", "comment"))
      {
         token = it.stepBackward();
         if (token == null)
            return singleLineCompletion();
      }
      
      // if we're on a ',' or a '(', assume that we may be
      // within a function call, and attempt to provide the
      // context for argument name completions
      boolean maybeArgument =
            token.valueEquals("(") ||
            token.valueEquals(",");
      
      if (!maybeArgument)
      {
         TokenIterator clone = it.clone();
         
         Token peek = clone.stepBackward();
         if (peek == null)
            return singleLineCompletion();
         
         while (peek.hasType("text", "comment"))
         {
            peek = clone.stepBackward();
            if (peek == null)
               return singleLineCompletion();
         }
         
         maybeArgument =
               peek.valueEquals("(") ||
               peek.valueEquals(",");
         
         if (!maybeArgument)
            return singleLineCompletion();
      }
      
      // start walking backwards until we see an 'anchor'
      int cursorRow = docDisplay_.getCursorPosition().getRow();
      int anchorRow = cursorRow;
      for (int i = 0; i < 200; i++)
      {
         // skip matching brackets
         if (it.bwdToMatchingToken())
         {
            if (it.stepBackward() == null)
               break;
            
            continue;
         }
         
         // if we hit a ':', check to see if it's at the end
         // of the line. if so, we can exit
         if (token.valueEquals(":"))
         {
            String line = docDisplay_.getLine(it.getCurrentTokenRow());
            Pattern pattern = Pattern.create("[:]\\s*(?:#|$)", "");
            if (pattern.test(line))
               break;
         }
         
         // if we hit an 'import' or a 'from', we can bail
         if (token.valueEquals("from") || token.valueEquals("import"))
            break;
         
         // if we hit an open bracket, use this line as the anchor
         if (token.valueEquals("("))
         {
            // double-check that this is a function invocation, not a definition
            int row = it.getCurrentTokenRow();
            String line = docDisplay_.getLine(row);
            Pattern pattern = Pattern.create("^\\s*def\\s+", "");
            if (pattern.test(line))
               break;
            
            // this appears to be a function invocation; use the anchor
            anchorRow = row;
            break;
         }
         
         // attempt to step backward
         token = it.stepBackward();
         if (token == null)
            break;
      }
      
      // build the line to be used for completion
      StringBuilder builder = new StringBuilder();
      for (int i = anchorRow; i < cursorRow; i++)
      {
         builder.append(docDisplay_.getLine(i));
      }
      builder.append(docDisplay_.getCurrentLineUpToCursor());
      
      return builder.toString();
   }
   
   private static final Pattern RE_IMPORT =
         Pattern.create("^\\s*import\\s+([\\w._]+)\\s*(?:#.*|$)", "");
   
   private static final Pattern RE_IMPORT_AS =
         Pattern.create("^\\s*import\\s+([\\w._]+)\\s+as\\s+([\\w._]+)\\s*(?:#.*|$)", "");
   private static final ConsoleConstants constants_ = GWT.create(ConsoleConstants.class);
}
