/*
 * QuartoNewDocument.java
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

package org.rstudio.studio.client.quarto;

import java.util.ArrayList;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.quarto.model.QuartoCapabilities;
import org.rstudio.studio.client.quarto.model.QuartoConstants;
import org.rstudio.studio.client.quarto.model.QuartoServerOperations;
import org.rstudio.studio.client.quarto.ui.NewQuartoDocumentDialog;
import org.rstudio.studio.client.server.ServerError;

import com.google.inject.Inject;

public class QuartoNewDocument
{
   public QuartoNewDocument()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   private void initialize(GlobalDisplay globalDisplay,
                           QuartoServerOperations server)
   {
      globalDisplay_ = globalDisplay;
      server_ = server;
   }
   
   public void newDocument(boolean presentation, CommandWithArg<String> onResult)
   {
      final ProgressIndicator indicator =
            globalDisplay_.getProgressIndicator("Error");
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
                     onResult.execute(generateDoc(caps, result));
                  }).showModal();
                  
               }
   
               @Override
               public void onError(ServerError error)
               {
                  indicator.onError(error.getUserMessage());
               }
            }); 
   }
   
   private String generateDoc(QuartoCapabilities caps, 
                              NewQuartoDocumentDialog.Result result)
   {
      ArrayList<String> lines = new ArrayList<String>();
      lines.add("---");
      lines.add("title: \"" + result.getTitle() + "\"");
      if (!StringUtil.isNullOrEmpty(result.getAuthor()))
         lines.add("author: \"" + result.getAuthor() + "\"");
   
      lines.add("format: " + result.getFormat());
      
      if (result.getEditor().equals(QuartoConstants.EDITOR_VISUAL));
         lines.add("editor: " + QuartoConstants.EDITOR_VISUAL);
      
      if (result.getEngine().equals(QuartoConstants.ENGINE_JUPYTER))
         lines.add("jupyter: " + result.getKernel());
      lines.add("---");
      lines.add("");
      
      if (result.getEditor().equals(QuartoConstants.EDITOR_VISUAL))
      {
         lines.add("This document uses the Quarto [visual markdown editor]" +
                   "(https://quarto.org/docs/visual-editor/). Use the button " + 
                   "at the far right of the editor toolbar to switch between " +
                   "visual and source code mode.");
         lines.add("");
      }
            
      if (!result.getEngine().equals(QuartoConstants.ENGINE_MARKDOWN) && 
          result.getLanguage() != null)
      {
         lines.add("This is an executable code chunk (click the run button on " + 
                   "the right to execute it):");   
         lines.add("");
         lines.add("```{" + result.getLanguage() + "}");
         lines.add("1 + 1");
         lines.add("```");
         lines.add("");
         lines.add("Insert additional code chunks using the insert chunk " +
                   "toolbar button (or **Insert** menu).");
         lines.add("");
      }
      
      lines.add("Use the toolbar menus (**Format**, **Insert**, **Reference**, " +
                "and **Table**) to apply formatting and insert various content " + 
                "types (e.g. images, links, references, math, tables, etc.).");
      lines.add("");
      lines.add("Click the **Render** button on the editor toolbar to create an " + 
                "output document from this file.");
      lines.add("");
      lines.add("Learn more about Quarto at <https://quarto.org>.");
      
      lines.add("");
      lines.add("");
      return StringUtil.join(lines, "\n");
      
   }
   
   
   private QuartoServerOperations server_;
   private GlobalDisplay globalDisplay_;
}

