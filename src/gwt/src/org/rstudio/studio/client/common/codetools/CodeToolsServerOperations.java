/*
 * CodeToolsServerOperations.java
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
package org.rstudio.studio.client.common.codetools;

import java.util.List;

import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.codesearch.model.CodeSearchServerOperations;
import org.rstudio.studio.client.workbench.views.help.model.HelpServerOperations;
import org.rstudio.studio.client.workbench.views.source.model.CppServerOperations;

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
         
}
