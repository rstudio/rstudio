/*
 * MarkdownCompletionManager.java
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

import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.QualifiedName;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;

public class MarkdownCompletionManager extends CompletionManagerBase
                                       implements CompletionManager
{

   public MarkdownCompletionManager(DocDisplay docDisplay,
                                    CompletionPopupDisplay popup,
                                    CodeToolsServerOperations server,
                                    CompletionContext context)
   {
      super(popup, docDisplay, server, context);
      
      context_ = context;
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
   public void showAdditionalHelp(QualifiedName completion)
   {
      // NYI
   }

   @Override
   public boolean getCompletions(String line, CompletionRequestContext context)
   {
      // check for completion of href
      if (getCompletionsHref(context))
         return true;
      
      return false;
   }
   
   private boolean getCompletionsHref(CompletionRequestContext context)
   {
      Token token = docDisplay_.getTokenAt(docDisplay_.getCursorPosition());
      if (token == null)
         return false;
      
      boolean isMarkupHref =
            token.hasType("markup.href") ||
            (token.hasType("text") && token.valueEquals("]("));
      
      if (!isMarkupHref)
         return false;
            
      JsObject data = JsObject.createJsObject();
      data.setString("token", token.hasType("markup.href") ? token.getValue() : "");
      data.setString("path", context_.getPath());
      data.setString("id", context_.getId());
      server_.markdownGetCompletions(COMPLETION_HREF, data, context);
      return true;
   }
   
   private static final int COMPLETION_HREF = 1;

   private final CompletionContext context_;
}
