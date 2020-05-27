/*
 * CodeToolsServerOperations.java
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
package org.rstudio.studio.client.common.codetools;

import java.util.List;

import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.codesearch.model.CodeSearchServerOperations;
import org.rstudio.studio.client.workbench.views.console.shell.assist.PythonCompletionContext;
import org.rstudio.studio.client.workbench.views.console.shell.assist.SqlCompletionParseContext;
import org.rstudio.studio.client.workbench.views.help.model.HelpServerOperations;
import org.rstudio.studio.client.workbench.views.output.lint.model.AceAnnotation;
import org.rstudio.studio.client.workbench.views.source.model.CppServerOperations;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

public interface CodeToolsServerOperations extends HelpServerOperations,
                                                   CodeSearchServerOperations,
                                                   CppServerOperations
{
   void isFunction(
         String functionName,
         String envName,
         ServerRequestCallback<Boolean> result);
   
   void getCompletions(
         String token,
         List<String> assocData,
         List<Integer> dataType,
         List<Integer> numCommas,
         String chainObjectName,
         String functionCallString,
         JsArrayString chainAdditionalArgs,
         JsArrayString chainExcludeArgs,
         boolean chainExcludeArgsFromObject,
         String filePath,
         String documentId,
         String line,
         boolean isConsole,
         ServerRequestCallback<Completions> completions);
   
   void getDplyrJoinCompletions(
         String token,
         String leftDataName,
         String rightDataName,
         String joinVerb,
         String cursorPos,
         ServerRequestCallback<Completions> completions);
   
   void getDplyrJoinCompletionsString(
         String token,
         String string,
         String cursorPos,
         ServerRequestCallback<Completions> completions);

   void getHelpAtCursor(
         String line, int cursorPos,
         ServerRequestCallback<org.rstudio.studio.client.server.Void> callback);
   
   void getArgs(String name,
                String source,
                String helpHandler,
                ServerRequestCallback<String> callback);
   
   void extractChunkOptions(
         String chunkText,
         ServerRequestCallback<JsObject> callback);
   
   void executeUserCommand(String name, ServerRequestCallback<Void> callback);
   
   void markdownGetCompletions(
         int completionType,
         JavaScriptObject completionData,
         ServerRequestCallback<Completions> requestCallback);
   
   void pythonGetCompletions(
         String line,
         PythonCompletionContext context,
         ServerRequestCallback<Completions> requestCallback);
   
   void pythonGoToDefinition(
         String line,
         int column,
         ServerRequestCallback<Boolean> requestCallback);
   
   void pythonGoToHelp(
         String line,
         int column,
         ServerRequestCallback<Boolean> requestCallback);
   
   void stanGetCompletions(
         String line,
         ServerRequestCallback<Completions> requestCallback);
   
   void stanGetArguments(
         String function,
         ServerRequestCallback<String> requestCallback);
   
   void stanRunDiagnostics(
         String filename,
         boolean useSourceDatabase,
         ServerRequestCallback<JsArray<AceAnnotation>> requestCallback);
   
   void sqlGetCompletions(
         String line,
         String connection,
         SqlCompletionParseContext context,
         ServerRequestCallback<Completions> requestCallback);
   
}
