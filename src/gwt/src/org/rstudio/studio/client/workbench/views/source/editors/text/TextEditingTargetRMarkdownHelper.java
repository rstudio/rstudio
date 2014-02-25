/*
 * TextEditingTargetRMarkdownHelper.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.filetypes.FileTypeCommands;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.rmarkdown.events.RenderRmdEvent;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownContext;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.workbench.model.Session;

public class TextEditingTargetRMarkdownHelper
{
   public TextEditingTargetRMarkdownHelper()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   public void initialize(Session session,
                          GlobalDisplay globalDisplay,
                          EventBus eventBus,
                          FileTypeCommands fileTypeCommands,
                          RMarkdownServerOperations server)
   {
      session_ = session;
      fileTypeCommands_ = fileTypeCommands;
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
      server_ = server;
   }
   
   public String detectExtendedType(String contents,
                                    String extendedType,
                                    TextFileType fileType)
   {
      if (extendedType.length() == 0 && 
          fileType.isMarkdown() &&
          !contents.contains("<!-- rmarkdown v1 -->") && 
          session_.getSessionInfo().getRMarkdownPackageAvailable())
      {
         return "rmarkdown";
      }
      else
      {
         return extendedType;
      }
   }
   
   public void withRMarkdownPackage(String action, final Command onReady)
   {
      final ProgressIndicator progress = new GlobalProgressDelayer(
            globalDisplay_,
            200,
            "New R Markdown...").getIndicator();
      
      server_.getRMarkdownContext(new SimpleRequestCallback<RMarkdownContext>()
      {
         @Override
         public void onResponseReceived(RMarkdownContext context)
         {
            progress.onCompleted();
            
            if (onReady != null)
               onReady.execute();
         }
         
         @Override
         public void onError(ServerError error)
         {
            progress.onError(error.getUserMessage());
            
         } 
      });
   }
   
   
   public void renderRMarkdown(final String sourceFile, 
                               final int sourceLine,
                               final String encoding)
   {
      withRMarkdownPackage("Rendering R Markdown documents", new Command() {
         @Override
         public void execute()
         {
            eventBus_.fireEvent(new RenderRmdEvent(sourceFile,
                                                   sourceLine,
                                                   encoding));
         }
      });
   }
   
   
   public boolean verifyPrerequisites(WarningBarDisplay display,
                                      TextFileType fileType)
   {
      return verifyPrerequisites(null, display, fileType);
   }
   
   public boolean verifyPrerequisites(String feature,
                                      WarningBarDisplay display,
                                      TextFileType fileType)
   {
      if (feature == null)
         feature = fileType.getLabel();
      
      // if this file requires knitr then validate pre-reqs
      boolean haveRMarkdown = 
         fileTypeCommands_.getHTMLCapabiliites().isRMarkdownSupported();
      if (!haveRMarkdown)
      {
         if (fileType.isRpres())
         {
            showKnitrPreviewWarning(display, "R Presentations", "1.2");
            return false;
         }
         else if (fileType.requiresKnit())
         {
   
            showKnitrPreviewWarning(display, feature, "1.2");
            return false;
         }
      }
      
      return true;
   }
   
   private void showKnitrPreviewWarning(WarningBarDisplay display,
                                        String feature, 
                                        String requiredVersion)
   {
      display.showWarningBar(feature + " requires the " +
                             "knitr package (version " + requiredVersion + 
                             " or higher)");
   }
   
   private Session session_;
   private GlobalDisplay globalDisplay_;
   private EventBus eventBus_;
   private FileTypeCommands fileTypeCommands_;
   private RMarkdownServerOperations server_;
}
