/*
 * DelegatingCompletionManager.java
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

import java.util.HashMap;
import java.util.Map;

import org.rstudio.core.client.MapUtil;
import org.rstudio.studio.client.common.filetypes.DocumentMode;
import org.rstudio.studio.client.common.filetypes.DocumentMode.Mode;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.PasteEvent;

import com.google.gwt.dom.client.NativeEvent;

public abstract class DelegatingCompletionManager implements CompletionManager 
{
   public DelegatingCompletionManager(DocDisplay docDisplay)
   {
      docDisplay_ = docDisplay;
      completionManagerMap_ = new HashMap<Mode, CompletionManager>();
      nullManager_ = new NullCompletionManager();
      
      initialize(completionManagerMap_);
   }
   
   protected abstract void initialize(Map<Mode, CompletionManager> managers);
   
   private CompletionManager getCurrentCompletionManager()
   {
      Mode mode = DocumentMode.getModeForCursorPosition(docDisplay_);
      if (completionManagerMap_.containsKey(mode))
         return completionManagerMap_.get(mode);
      return nullManager_;
   }
   
   @Override
   public boolean previewKeyDown(NativeEvent event)
   {
      CompletionManager manager = getCurrentCompletionManager();
      return manager.previewKeyDown(event);
   }

   @Override
   public boolean previewKeyPress(char charCode)
   {
      CompletionManager manager = getCurrentCompletionManager();
      return manager.previewKeyPress(charCode);
   }

   @Override
   public void goToHelp()
   {
      CompletionManager manager = getCurrentCompletionManager();
      manager.goToHelp();
   }

   @Override
   public void goToDefinition()
   {
      CompletionManager manager = getCurrentCompletionManager();
      manager.goToDefinition();
   }

   @Override
   public void codeCompletion()
   {
      CompletionManager manager = getCurrentCompletionManager();
      manager.codeCompletion();
   }

   @Override
   public void close()
   {
      CompletionManager manager = getCurrentCompletionManager();
      manager.close();
   }

   @Override
   public void onPaste(final PasteEvent event)
   {
      MapUtil.forEach(completionManagerMap_, new MapUtil.ForEachCommand<Mode, CompletionManager>()
      {
         @Override
         public void execute(Mode mode, CompletionManager manager)
         {
            manager.onPaste(event);
         }
      });
   }

   @Override
   public void detach()
   {
      MapUtil.forEach(completionManagerMap_, new MapUtil.ForEachCommand<Mode, CompletionManager>()
      {
         @Override
         public void execute(Mode mode, CompletionManager manager)
         {
            manager.detach();
         }
      });
   }

   private final DocDisplay docDisplay_;
   private final Map<DocumentMode.Mode, CompletionManager> completionManagerMap_;
   private final CompletionManager nullManager_;
   
}
