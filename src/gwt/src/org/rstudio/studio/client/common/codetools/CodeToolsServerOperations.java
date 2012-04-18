/*
 * CodeToolsServerOperations.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.codetools;

import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.workbench.codesearch.model.CodeSearchServerOperations;
import org.rstudio.studio.client.workbench.views.help.model.HelpServerOperations;

public interface CodeToolsServerOperations extends HelpServerOperations,
                                                   CodeSearchServerOperations
{
   void getCompletions(String line, int cursorPos, 
         ServerRequestCallback<Completions> completions);

   void getHelpAtCursor(
         String line, int cursorPos,
         ServerRequestCallback<org.rstudio.studio.client.server.Void> callback);
}
