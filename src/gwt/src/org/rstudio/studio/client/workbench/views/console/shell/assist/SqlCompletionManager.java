/*
 * SqlCompletionManager.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

import java.util.Map;

import org.rstudio.core.client.JsVectorString;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenIterator;
import org.rstudio.studio.client.workbench.views.source.editors.text.assist.RChunkHeaderParser;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.shared.HandlerRegistration;

public class SqlCompletionManager extends CompletionManagerBase
                                  implements CompletionManager
{
   public SqlCompletionManager(DocDisplay docDisplay,
                               CompletionPopupDisplay popup,
                               CodeToolsServerOperations server,
                               CompletionContext context)
   {
      super(popup, docDisplay, context);
      
      docDisplay_ = docDisplay;
      server_ = server;
      context_ = context;
   }
   
   @Override
   public void goToHelp()
   {
   }
   
   @Override
   public void goToDefinition()
   {
   }
   
   @Override
   public void getCompletions(String line,
                              CompletionRequestContext context)
   {
      String connection = discoverAssociatedConnectionString();
      server_.sqlGetCompletions(line, connection, completionContext(), context);
   }
   
   @Override
   protected HandlerRegistration[] handlers()
   {
      return new HandlerRegistration[] {};
   }
   
   private JsObject completionContext()
   {
      JsObject context = JsObject.createJsObject();
      context.setJSO("tables", discoverKnownTables());
      context.setBoolean("preferLowercaseKeywords", preferLowercaseKeywords());
      return context;
   }
   
   private JsArrayString discoverKnownTables()
   {
      JsVectorString tables = JsVectorString.createVector().cast();
      
      TokenIterator it = fromChunkStart();
      for (Token token = it.getCurrentToken();
           token != null;
           token = it.stepForward())
      {
         if (token.hasType("keyword"))
         {
            String value = token.getValue().toLowerCase();
            if (value.contentEquals("from") || value.contentEquals("join"))
            {
               // whitespace
               token = it.stepForward();
               if (token == null || !token.hasType("text"))
                  continue;
               
               // table name
               token = it.stepForward();
               if (token == null || !token.hasType("identifier"))
                  continue;
               
               tables.push(token.getValue());
            }
         }
         
         if (token.hasType("support.function.codeend"))
            break;
      }
      
      
      return tables.cast();
   }
   
   private boolean preferLowercaseKeywords()
   {
      TokenIterator it = fromChunkStart();
      for (Token token = it.getCurrentToken();
           token != null;
           token = it.stepForward())
      {
         if (token.hasType("keyword"))
         {
            String value = token.getValue();
            return value.contentEquals(value.toLowerCase());
         }
         
         if (token.hasType("support.function.codeend"))
            break;
      }
      
      return false;
   }
   
   private String discoverAssociatedConnectionString()
   {
      if (docDisplay_.getFileType().isSql())
      {
         String firstLine = docDisplay_.getLine(0);
         Match match = RE_SQL_PREVIEW.match(firstLine, 0);
         if (match == null)
            return null;
         
         return match.getGroup(1);
      }
      else
      {
         int currentRow = docDisplay_.getCursorPosition().getRow();
         
         int row = currentRow - 1;
         for (; row >= 0; row--)
         {
            JsArray<Token> tokens = docDisplay_.getTokens(row);
            if (tokens.length() == 0)
               continue;
            
            Token token = tokens.get(0);
            if (token.hasType("support.function.codebegin"))
               break;
         }
         
         String chunkHeader = docDisplay_.getLine(row);
         
         Map<String, String> chunkOptions = RChunkHeaderParser.parse(chunkHeader);
         if (!chunkOptions.containsKey("connection"))
            return null;
         
         return chunkOptions.get("connection");
      }
   }
   
   private TokenIterator fromChunkStart()
   {
      TokenIterator it = docDisplay_.createTokenIterator();
      if (docDisplay_.getFileType().isSql())
         return it;
      
      it.moveToPosition(docDisplay_.getCursorPosition());
      it.findTokenTypeBwd("support.function.codebegin", false);
      return it;
   }
   
   private static final Pattern RE_SQL_PREVIEW = Pattern.create("^-{2,}\\s*!preview\\s+conn\\s*=\\s*(.*)$", "");
   
   private final DocDisplay docDisplay_;
   private final CodeToolsServerOperations server_;
   private final CompletionContext context_;
}
