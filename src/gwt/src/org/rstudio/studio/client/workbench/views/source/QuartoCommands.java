/*
 * QuartoCommands.java
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

package org.rstudio.studio.client.workbench.views.source;

import java.util.ArrayList;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.TransformerCommand;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.quarto.model.QuartoCapabilities;
import org.rstudio.studio.client.quarto.model.QuartoConfig;
import org.rstudio.studio.client.quarto.model.QuartoConstants;
import org.rstudio.studio.client.quarto.model.QuartoServerOperations;
import org.rstudio.studio.client.quarto.ui.NewQuartoDocumentDialog;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;

// Substitute Python or Julia for R when approriate
// Optional for author
// Shiny template
// OJS template
// Custom help links for various types

public class QuartoCommands
{
   public QuartoCommands(SourceColumnManager columnManager, QuartoServerOperations server)
   {
      columnManager_ = columnManager;
      server_ = server;
   }
   
   public void onSessionInit(SessionInfo sessionInfo, Commands commands)
   {
      QuartoConfig quartoConfig = sessionInfo.getQuartoConfig();
      commands.newQuartoDoc().setVisible(quartoConfig.installed);
      commands.newQuartoPres().setVisible(quartoConfig.installed);
   }
   
   public void newQuarto(boolean presentation)
   {
      final ProgressIndicator indicator = 
         RStudioGinjector.INSTANCE.getGlobalDisplay().getProgressIndicator("Error");
      indicator.onProgress(
         "New Quarto " + 
         (presentation ? "Presentation" : "Document") + 
         "...");

      server_.quartoCapabilities(
         new SimpleRequestCallback<QuartoCapabilities>() {
            @Override
            public void onResponseReceived(QuartoCapabilities caps)
            {
               indicator.onCompleted();
               new NewQuartoDocumentDialog(caps, presentation, result -> {
          
                  ArrayList<String> lines = new ArrayList<String>();
                  String template = "default.qmd";
                  if (result != null)
                  {
                     // select appropriate template
                     String format = result.getFormat();
                     if (format.equals(QuartoConstants.FORMAT_HTML) ||
                         format.equals(QuartoConstants.FORMAT_PDF) ||
                         format.equals(QuartoConstants.FORMAT_DOCX))
                     {
                        template = "document.qmd";
                     }
                     else if (format.equals(QuartoConstants.FORMAT_REVEALJS) ||
                              format.equals(QuartoConstants.FORMAT_BEAMER) ||
                              format.equals(QuartoConstants.FORMAT_PPTX))
                     {
                        template = "presentation.qmd";
                     }
                     else if (format.equals(QuartoConstants.INTERACTIVE_SHINY))
                     {
                        template = "shiny.qmd";
                     }
                     else if (format.equals(QuartoConstants.INTERACTIVE_OJS))
                     {
                        template = "ojs.qmd";
                     }
                     else
                     {
                        template = "default.qmd";
                     }
                  }
                 
                  // generate preamble
                  lines.add("---");
                  lines.add("title: \"" + result.getTitle() + "\"");
                  if (!StringUtil.isNullOrEmpty(result.getAuthor()))
                     lines.add("author: \"" + result.getAuthor() + "\"");
               
                  lines.add("format: " + result.getFormat());
                  
                  
                  final boolean visualEditor = result.getEditor().equals(QuartoConstants.EDITOR_VISUAL);
                  if (visualEditor)
                     lines.add("editor: " + QuartoConstants.EDITOR_VISUAL);
                  
                  if (result.getEngine().equals(QuartoConstants.ENGINE_JUPYTER))
                     lines.add("jupyter: " + result.getKernel());
                  lines.add("---");
                  lines.add("");
                  final String preamble = StringUtil.join(lines, "\n");
                  
   
                  columnManager_.newSourceDocWithTemplate(FileTypeRegistry.QUARTO,
                     "",
                     template,
                     Position.create(1, 0),
                     null,
                     new TransformerCommand<String>()
                     {
                        @Override
                        public String transform(String input)
                        {       
                           if (!visualEditor)
                              input = removeVisualEditorLine(input); 
                           
                           return preamble + "\n" + input;
                        }
                     });
                  
               }).showModal();
            }
            @Override
            public void onError(ServerError error)
            {
               indicator.onError(error.getUserMessage());
            }
         }); 
   }
   
   private String removeVisualEditorLine(String input)
   {
      return input.replaceFirst("\\n.*?\\[visual markdown editor\\].*?\\n", "");
   }
   

   private final SourceColumnManager columnManager_;
   private final QuartoServerOperations server_;
}
