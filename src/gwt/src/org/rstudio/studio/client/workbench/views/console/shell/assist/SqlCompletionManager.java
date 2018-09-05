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

import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;

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
      server_.sqlGetCompletions(line, context);
   }
   
   @Override
   protected HandlerRegistration[] handlers()
   {
      return new HandlerRegistration[] {};
   }
   
   private final DocDisplay docDisplay_;
   private final CodeToolsServerOperations server_;
   private final CompletionContext context_;
}
