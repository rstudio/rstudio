/*
 * StanCompletionManager.java
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

import org.rstudio.core.client.Invalidation;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.filetypes.DocumentMode;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;

import com.google.gwt.dom.client.NativeEvent;

public class StanCompletionManager extends CompletionManagerBase
                                   implements CompletionManager
{
   public StanCompletionManager(DocDisplay docDisplay,
                                CompletionPopupDisplay popup,
                                CodeToolsServerOperations server,
                                CompletionContext context)
   {
      super(popup, docDisplay, context);
      
      docDisplay_ = docDisplay;
      popup_ = popup;
      server_ = server;
      context_ = context;
      
      invalidation_ = new Invalidation();
      completionCache_ = new CompletionCache();
   }
   
   @Override
   public void getCompletions(String line, CompletionRequestContext context)
   {
      server_.stanGetCompletions(line, context);
   }
   
   @Override
   public boolean previewKeyDown(NativeEvent event)
   {
      if (!DocumentMode.isCursorInStanMode(docDisplay_))
         return false;
      
      Boolean status = onKeyDown(event);
      if (status != null)
         return status;
      
      return false;
   }

   @Override
   public boolean previewKeyPress(char ch)
   {
      if (!DocumentMode.isCursorInStanMode(docDisplay_))
         return false;
      
      Boolean status = onKeyPress(ch);
      if (status != null)
         return status;
      
      return false;
   }

   @Override
   public void goToHelp()
   {
      // TODO Auto-generated method stub
   }

   @Override
   public void goToDefinition()
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void codeCompletion()
   {
      // TODO Auto-generated method stub
      
   }

   private final DocDisplay docDisplay_;
   private final CompletionPopupDisplay popup_;
   private final CodeToolsServerOperations server_;
   private final CompletionContext context_;
   
   private final Invalidation invalidation_;
   private final CompletionCache completionCache_;
}
