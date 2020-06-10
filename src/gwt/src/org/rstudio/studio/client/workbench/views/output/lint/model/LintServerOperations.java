/*
 * LintServerOperations.java
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
package org.rstudio.studio.client.workbench.views.output.lint.model;

import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.workbench.snippets.model.SnippetData;
import org.rstudio.studio.client.workbench.views.source.model.CppDiagnostic;

import com.google.gwt.core.client.JsArray;

public interface LintServerOperations
{
   void lintRSourceDocument(String documentId,
                            String documentPath,
                            boolean showMarkersPane,
                            boolean explicit,
                            ServerRequestCallback<JsArray<LintItem>> requestCallback);
   
   void getCppDiagnostics(
                String docPath,
                ServerRequestCallback<JsArray<CppDiagnostic>> requestCallback);
   
   void saveSnippets(
         JsArray<SnippetData> snippets,
         ServerRequestCallback<org.rstudio.studio.client.server.Void> callback);
   
}
