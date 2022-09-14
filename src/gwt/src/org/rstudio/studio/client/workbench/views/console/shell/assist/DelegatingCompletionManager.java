/*
 * DelegatingCompletionManager.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
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
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.common.filetypes.DocumentMode;
import org.rstudio.studio.client.common.filetypes.DocumentMode.Mode;
import org.rstudio.studio.client.workbench.snippets.SnippetHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.PasteEvent;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;

public abstract class DelegatingCompletionManager
      implements CompletionManager,
                 CompletionManagerCommon
{
   public DelegatingCompletionManager(DocDisplay docDisplay, CompletionContext context)
   {
      docDisplay_ = docDisplay;
      completionManagerMap_ = new HashMap<>();
      nullManager_ = new NullCompletionManager();
      
      if (docDisplay_ instanceof AceEditor)
      {
         String path = context == null ? "" : context.getPath();
         snippets_ = new SnippetHelper((AceEditor) docDisplay_, path);
      }
      
      initialize(completionManagerMap_);
   }
   
   protected abstract void initialize(Map<Mode, CompletionManager> managers);
   
   public CompletionManager getActiveCompletionManager()
   {
      Mode mode = DocumentMode.getModeForCursorPosition(docDisplay_);
      
      // If we're in Markdown mode, but completing for inline R code,
      // then use the R completion manager instead.
      if (completionManagerMap_.containsKey(DocumentMode.Mode.R) &&
          isRequestingCompletionsForInlineRCode(mode))
      {
         return completionManagerMap_.get(DocumentMode.Mode.R);
      }
      
      // if we're in R or Python mode, but completing for embedded YAML comments
      // then use the YAML completion manager instead
      if (completionManagerMap_.containsKey(DocumentMode.Mode.YAML) &&
         isRequestingCompletionsForYamlOptions(mode))
      {
         return completionManagerMap_.get(DocumentMode.Mode.YAML);
      }
      
      if (completionManagerMap_.containsKey(mode))
         return completionManagerMap_.get(mode);
      return nullManager_;
   }
   
   @Override
   public boolean previewKeyDown(NativeEvent event)
   {
      CompletionManager manager = getActiveCompletionManager();
      if (manager.previewKeyDown(event))
         return true;
      
      if (snippets_ != null)
      {
         int keyCode = event.getKeyCode();
         int modifier = KeyboardShortcut.getModifierValue(event);
         if (modifier == KeyboardShortcut.SHIFT && keyCode == KeyCodes.KEY_TAB)
            return snippets_.attemptSnippetInsertion(true);
      }
      
      return false;
   }

   @Override
   public boolean previewKeyPress(char charCode)
   {
      CompletionManager manager = getActiveCompletionManager();
      return manager.previewKeyPress(charCode);
   }

   @Override
   public void goToHelp()
   {
      CompletionManager manager = getActiveCompletionManager();
      manager.goToHelp();
   }

   @Override
   public void goToDefinition()
   {
      CompletionManager manager = getActiveCompletionManager();
      manager.goToDefinition();
   }

   @Override
   public void codeCompletion()
   {
      CompletionManager manager = getActiveCompletionManager();
      manager.codeCompletion();
   }

   @Override
   public void close()
   {
      CompletionManager manager = getActiveCompletionManager();
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
      
      if (snippets_ != null)
         snippets_.detach();
   }
   
   private boolean isRequestingCompletionsForInlineRCode(DocumentMode.Mode mode)
   {
      // only available in Markdown right now
      if (mode != DocumentMode.Mode.MARKDOWN)
         return false;
      
      String line = docDisplay_.getCurrentLineUpToCursor();
      
      // check for an unclosed '`r' at the cursor position
      int startIndex = line.lastIndexOf("`r");
      if (startIndex == -1)
         return false;
      
      int endIndex = line.indexOf("`", startIndex + 1);
      if (endIndex != -1)
         return false;
      
      return true;
   }
   
   private boolean isRequestingCompletionsForYamlOptions(DocumentMode.Mode mode)
   {  
      // only available in R and Python right now
      if (mode != Mode.R && mode != Mode.PYTHON)
         return false;
      
      String line = docDisplay_.getCurrentLineUpToCursor(); 
      Pattern pattern = Pattern.create("^\\s*#\\s*[|]\\s*.*$", "");
      Match match = pattern.match(line, 0);
      return match != null;
   }

   private final DocDisplay docDisplay_;
   private final Map<DocumentMode.Mode, CompletionManager> completionManagerMap_;
   private final CompletionManager nullManager_;
   
   private SnippetHelper snippets_;
}
