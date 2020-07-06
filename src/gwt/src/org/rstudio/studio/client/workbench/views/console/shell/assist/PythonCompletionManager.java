/*
 * PythonCompletionManager.java
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
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenIterator;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.QualifiedName;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;

public class PythonCompletionManager extends CompletionManagerBase
                                     implements CompletionManager
{  
   public PythonCompletionManager(DocDisplay docDisplay,
                                  CompletionPopupDisplay popup,
                                  CodeToolsServerOperations server,
                                  CompletionContext context)
   {
      super(popup, docDisplay, server, context);
   }
   
   @Override
   public void goToHelp()
   {
      final GlobalProgressDelayer progress = new GlobalProgressDelayer(
            RStudioGinjector.INSTANCE.getGlobalDisplay(),
            1000,
            "Opening help...");
      
      server_.pythonGoToHelp(
            docDisplay_.getCurrentLine(),
            docDisplay_.getCursorPosition().getColumn(),
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
            "Finding definition...");
      
      server_.pythonGoToDefinition(
            docDisplay_.getCurrentLine(),
            docDisplay_.getCursorPosition().getColumn(),
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
            "Opening help...");
      
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
         return docDisplay_.getCurrentLineUpToCursor();
      
      // move off of comments, text
      while (token.hasType("text", "comment"))
      {
         token = it.stepBackward();
         if (token == null)
            return docDisplay_.getCurrentLineUpToCursor();
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
            return docDisplay_.getCurrentLineUpToCursor();
         
         while (peek.hasType("text", "comment"))
         {
            peek = clone.stepBackward();
            if (peek == null)
               return docDisplay_.getCurrentLineUpToCursor();
         }
         
         maybeArgument =
               peek.valueEquals("(") ||
               peek.valueEquals(",");
         
         if (!maybeArgument)
            return docDisplay_.getCurrentLineUpToCursor();
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
}
