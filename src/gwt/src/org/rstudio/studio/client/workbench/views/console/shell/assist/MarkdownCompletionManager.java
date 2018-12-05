/*
 * MarkdownCompletionManager.java
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

import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;

public class MarkdownCompletionManager extends CompletionManagerBase
                                       implements CompletionManager
{

   public MarkdownCompletionManager(CompletionPopupDisplay popup,
                                    DocDisplay docDisplay,
                                    CodeToolsServerOperations server,
                                    CompletionContext context)
   {
      super(popup, docDisplay, server, context);
   }

   @Override
   public void goToHelp()
   {
      // NYI
   }

   @Override
   public void goToDefinition()
   {
      // NYI
   }

   @Override
   public void getCompletions(String line, CompletionRequestContext context)
   {
   }

}
