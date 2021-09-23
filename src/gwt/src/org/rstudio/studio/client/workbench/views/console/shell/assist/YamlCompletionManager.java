/*
 * YamlCompletionManager.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.QualifiedName;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;

public class YamlCompletionManager extends CompletionManagerBase
                                       implements CompletionManager
{
   // Use a funstructor to create an instance in order to ensure toggleHandlers()
   // is invoked after the object is fully instantiated
   public static YamlCompletionManager create(DocDisplay docDisplay,
                                                  CompletionPopupDisplay popup,
                                                  CodeToolsServerOperations server,
                                                  CompletionContext context)
   {
      YamlCompletionManager retVal = new YamlCompletionManager(docDisplay, popup, server, context);

      retVal.toggleHandlers(true);

      return retVal;
   }

   // Use the create() funstructor above instead of invoking this constructor directly
   private YamlCompletionManager(DocDisplay docDisplay,
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
      Position pos = docDisplay_.getCursorPosition();
      String code = docDisplay_.getCode();
      
      // context.onResponseReceived([]);
            
      Debug.logToRConsole("requesting yaml completions");
      
      return false;
   }
   
   @Override
   public boolean allowInComment()
   {
      return true;
   }

   private final CompletionContext context_;
}
