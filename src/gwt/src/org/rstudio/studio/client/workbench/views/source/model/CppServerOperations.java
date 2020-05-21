/*
 * CppServerOperations.java
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
package org.rstudio.studio.client.workbench.views.source.model;

import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.buildtools.model.BuildServerOperations;

import com.google.gwt.core.client.JsArray;

public interface CppServerOperations extends BuildServerOperations
{
   void goToCppDefinition(
                String docPath, 
                int line, 
                int column,
                ServerRequestCallback<CppSourceLocation> requestCallback);
   
   void findCppUsages(
                String docPath,
                int line,
                int column,
                ServerRequestCallback<Void> requestCallback);
   
   void getCppCompletions(
                String line,
                String docPath, 
                String docId,
                int row, 
                int column,
                String userText,
                ServerRequestCallback<CppCompletionResult> requestCallback);
   
   void getCppDiagnostics(
                String docPath,
                ServerRequestCallback<JsArray<CppDiagnostic>> requestCallback);
}
