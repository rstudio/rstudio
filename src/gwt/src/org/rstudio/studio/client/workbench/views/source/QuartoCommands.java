/*
 * QuartoCommands.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.workbench.views.source;

import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.TransformerCommand;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.quarto.QuartoHelper;
import org.rstudio.studio.client.quarto.model.QuartoCapabilities;
import org.rstudio.studio.client.quarto.model.QuartoConfig;
import org.rstudio.studio.client.quarto.model.QuartoCommandConstants;
import org.rstudio.studio.client.quarto.model.QuartoServerOperations;
import org.rstudio.studio.client.quarto.ui.NewQuartoDocumentDialog;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;

// OJS template

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
      commands.newQuartoDoc().setVisible(quartoConfig.enabled);
      commands.newQuartoPres().setVisible(quartoConfig.enabled);
      sessionInfo_ = sessionInfo;
   }
   
   public void newQuarto(boolean presentation)
   {
      final ProgressIndicator indicator = 
         RStudioGinjector.INSTANCE.getGlobalDisplay().getProgressIndicator(constants_.errorCapitalized());
      indicator.onProgress(constants_.newQuatroProgressIndicator(
         presentation ? constants_.presentationCapitalized() : constants_.documentCapitalized()));

      server_.quartoCapabilities(
         new SimpleRequestCallback<QuartoCapabilities>() {
            @Override
            public void onResponseReceived(QuartoCapabilities caps)
            {
               indicator.onCompleted();
               new NewQuartoDocumentDialog(caps, presentation, result -> {
          
                  ArrayList<String> lines = new ArrayList<String>();
                  String template = "default.qmd";
                  String format = "html";
                  final boolean interactive = 
                     (result.getFormat().equals(QuartoCommandConstants.INTERACTIVE_SHINY) ||
                      result.getFormat().equals(QuartoCommandConstants.INTERACTIVE_OJS));
                  
                  final boolean visualEditor = result.getEditor().equals(QuartoCommandConstants.EDITOR_VISUAL) &&
                                               !interactive;
                 
                  // select appropriate template
                  format = result.getFormat();
                  if (format.equals(QuartoCommandConstants.FORMAT_HTML) ||
                      format.equals(QuartoCommandConstants.FORMAT_PDF) ||
                      format.equals(QuartoCommandConstants.FORMAT_DOCX))
                  {
                     template = "document.qmd";
                  }
                  else if (format.equals(QuartoCommandConstants.FORMAT_REVEALJS) ||
                           format.equals(QuartoCommandConstants.FORMAT_BEAMER) ||
                           format.equals(QuartoCommandConstants.FORMAT_PPTX))
                  {
                     template = "presentation.qmd";
                  }
                  else if (format.equals(QuartoCommandConstants.INTERACTIVE_SHINY))
                  {
                     format = "html";
                     template = "shiny.qmd";
                  }
                  else if (format.equals(QuartoCommandConstants.INTERACTIVE_OJS))
                  {
                     format = "html";
                     template = "ojs.qmd";
                  }
                  else
                  {
                     format = "html";
                     template = "default.qmd";
                  }
                  
                  // check project level config and use that to lean down the yaml we generate
                  QuartoConfig config = sessionInfo_.getQuartoConfig();
                  boolean isWebsite = QuartoHelper.isQuartoWebsiteConfig(config);
                  boolean isBook = QuartoHelper.isQuartoBookConfig(config);

                  // generate preamble
                  lines.add("---");
                  lines.add("title: \"" + result.getTitle() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
                  if (!StringUtil.isNullOrEmpty(result.getAuthor()))
                     lines.add("author: \"" + result.getAuthor() + "\""); //$NON-NLS-1$ //$NON-NLS-2$

                  if (!isBook &&
                      (config.project_formats.length == 0 ||
                      !config.project_formats[0].equals(format)))
                  {
                     lines.add("format: " + format); //$NON-NLS-1$
                  }
                  
                  if (visualEditor && !QuartoCommandConstants.EDITOR_VISUAL.equals(config.project_editor.mode))
                     lines.add("editor: " + QuartoCommandConstants.EDITOR_VISUAL); //$NON-NLS-1$
                  
                  if (result.getFormat().equals(QuartoCommandConstants.INTERACTIVE_SHINY))
                     lines.add("server: shiny"); //$NON-NLS-1$
                  else if (!interactive && result.getEngine().equals(QuartoCommandConstants.ENGINE_JUPYTER))
                     lines.add("jupyter: " + result.getKernel()); //$NON-NLS-1$
                  
                  lines.add("---");
                  lines.add("");
                  
                 
                  final String preamble = StringUtil.join(lines, "\n");
                  
   
                  columnManager_.newSourceDocWithTemplate(FileTypeRegistry.QUARTO,
                     "",
                     result.getEmpty() || result.getEngine().equals(QuartoCommandConstants.ENGINE_MARKDOWN)
                        ? "default.qmd" 
                        : template,
                     Position.create(1, 0),
                     null,
                     new TransformerCommand<String>()
                     {
                        @Override
                        public String transform(String input)
                        {         
                           // if this is a website then we don't even use the template
                           if (isWebsite)
                           {
                              return preamble + "\n";
                           }
                           else
                           {

                              // remove bit about visual editor if we aren't using
                              // the visual editor
                              if (!visualEditor)
                                 input = removeVisualEditorLine(input);

                              // was this a built-in template?
                              boolean builtIn = input.startsWith("## Quarto");

                              // if it was built in and not interactive then do some
                              // language fixups
                              if (builtIn && !interactive)
                              {
                                 input = updateLanguage(input, result.getLanguage());
                              }

                              return preamble + "\n" + input;
                           }
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
      input = input.replaceFirst("\\n.*?\\[visual markdown editor\\].*?\\n", "");
      input = input.replaceFirst("\\nUse the \\*\\*Insert\\*\\* menu.*?\\n", "");
      return input;
   }
   
   
   private String updateLanguage(String input, String language)
   {
      // if it's R we are good to go
      if (language.equals("r")) 
      {
         return input;
      }
      // otherwise change out the chunk language
      else
      {
         // change out the chunk language
         input = input.replaceAll("```{r}", "```{" + language + "}");
         
         // if it's python then substitute plotting code, otherwise
         // just eliminate the Plot section entirely
         if (language.equals("python"))
         {
            input = input.replaceFirst(
              "\nplot\\(pressure\\)\n", 
              "\nimport matplotlib.pyplot as plt\n" + 
              "plt.show(plt.plot([1,2,3,4]))\n"
            );
         }
         else
         {
            int plotCodePos = input.indexOf("## Plot Output");
            if (plotCodePos == -1 )
               plotCodePos = input.indexOf("## Slide with Plot");
            if (plotCodePos != -1)
               input = input.substring(0, plotCodePos - 1);
         }
      }
      
      
      return input;
   }

   private final SourceColumnManager columnManager_;
   private final QuartoServerOperations server_;
   private SessionInfo sessionInfo_;
   private static final ViewsSourceConstants constants_ = GWT.create(ViewsSourceConstants.class);
}
