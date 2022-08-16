/*
 * YamlDiagnosticsManager.java
 *
 * Copyright (C) 2022 by Posit, PBC
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

package org.rstudio.studio.client.workbench.views.source.editors.text.yaml;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintItem;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor.EditorBehavior;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;

import com.google.gwt.core.client.JsArray;

import jsinterop.base.Js;

public class YamlDocumentLinter
{
   public YamlDocumentLinter(CompletionContext context, DocDisplay docDisplay)
   {
      context_ = context;
      docDisplay_ = docDisplay;
   }
   
   public void getLint(boolean explicit, CommandWithArg<JsArray<LintItem>> ready)
   {
      // do we have a provider that can handle this context
      YamlEditorToolsProvider provider = providers_.getActiveProvider(
         context_.getPath(), context_.getExtendedFileType()
      );
      if (provider == null)
      {
         ready.execute(JsArray.createArray().cast());
         return;
      }
      
      // request lint
      YamlEditorContext editorContext = YamlEditorContext.create(explicit, context_, docDisplay_);
      provider.getLint(editorContext, (res) -> {
         if (res != null)
         {
            JsArray<LintItem> items = Js.uncheckedCast(res);
            // if this was for an embedded editor then offset results by -1 to account
            // for client assumptions about not sending the preamble (```)
            if (docDisplay_.getEditorBehavior().equals(EditorBehavior.AceBehaviorEmbedded))
            {
               for (int i=0; i<items.length(); i++)
               {
                  items.get(i).setStartRow(items.get(i).getStartRow() - 1);
                  items.get(i).setEndRow(items.get(i).getEndRow() - 1);
               }
            }
            
            ready.execute(items);
         }
         else
         {
            ready.execute(JsArray.createArray().cast());
         }
      });
         
   }
   
   private final YamlEditorToolsProviders providers_ = new YamlEditorToolsProviders();
   
   private final CompletionContext context_;
   private final DocDisplay docDisplay_;
}
