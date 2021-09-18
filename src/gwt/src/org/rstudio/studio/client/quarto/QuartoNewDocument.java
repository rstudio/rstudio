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
import org.rstudio.studio.client.quarto.ui.QuartoNewDocumentDialog;
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
   
   public void newDocument(CommandWithArg<String> onResult)
   {
      final ProgressIndicator indicator =
            globalDisplay_.getProgressIndicator("Error");
         indicator.onProgress("New Quarto Doc...");
   
         server_.quartoCapabilities(
            new SimpleRequestCallback<QuartoCapabilities>() {
   
               @Override
               public void onResponseReceived(QuartoCapabilities caps)
               {
                  indicator.onCompleted();
                  new QuartoNewDocumentDialog(caps, (result) -> {
                     onResult.execute(generateDoc(caps, result));
                  }).showModal();;
               }
   
               @Override
               public void onError(ServerError error)
               {
                  indicator.onError(error.getUserMessage());
               }
            }); 
   }
   
   private String generateDoc(QuartoCapabilities caps, 
                              QuartoNewDocumentDialog.Result result)
   {
      ArrayList<String> lines = new ArrayList<String>();
      lines.add("---");
      lines.add("title: \"" + result.getTitle() + "\"");
      if (!StringUtil.isNullOrEmpty(result.getAuthor()))
         lines.add("author: \"" + result.getAuthor() + "\"");
      boolean simpleFormat = (!result.getFormat().equals("html") || result.getTheme() == "default") && 
                             !result.getTableOfContents() && 
                             !result.getNumberSections();
      if (simpleFormat)
      {
         lines.add("format: " + result.getFormat());
      }
      else
      {
         lines.add("format:");
         lines.add("  " + result.getFormat() + ":");
         if (result.getFormat().equals("html") && result.getTheme() != "default")
            lines.add("    theme: " + result.getTheme());
         if (result.getTableOfContents())
            lines.add("    toc: true");
         if (result.getNumberSections())
            lines.add("    number-sections: true");
      }
      
      if (result.getEngine().equals(QuartoConstants.ENGINE_JUPYTER))
         lines.add("jupyter: " + result.getKernel());
      lines.add("---");
            
      if (!result.getEngine().equals(QuartoConstants.ENGINE_MARKDOWN) && 
          result.getLanguage() != null)
      {
         lines.add("");
         lines.add("```{" + result.getLanguage() + "}");
         lines.add("");
         lines.add("```");
      }
      
      lines.add("");
      lines.add("");
      return StringUtil.join(lines, "\n");
      
   }
   
   
   private QuartoServerOperations server_;
   private GlobalDisplay globalDisplay_;
}

