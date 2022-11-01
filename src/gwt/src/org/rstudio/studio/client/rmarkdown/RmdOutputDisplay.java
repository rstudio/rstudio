/*
 * RmdDisplayBinaryOutput.java
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

package org.rstudio.studio.client.rmarkdown;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperation;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;

public class RmdOutputDisplay
{ 
   public static void displayWordDoc(String targetFile, final String outputFile)
   {
      GlobalDisplay globalDisplay = RStudioGinjector.INSTANCE.getGlobalDisplay();
      displayOfficeDoc(targetFile, outputFile,  (r) -> globalDisplay.showWordDoc(r));
   }
   
   public static void displayPptDoc(String targetFile, final String outputFile)
   {
      GlobalDisplay globalDisplay = RStudioGinjector.INSTANCE.getGlobalDisplay();
      displayOfficeDoc(targetFile, outputFile,  (r) -> globalDisplay.showPptPresentation(r));
   }
   
   public static void displayBinaryDoc(String targetFile, String outputFile)
   {
      GlobalDisplay globalDisplay = RStudioGinjector.INSTANCE.getGlobalDisplay();
      RMarkdownServerOperations server = RStudioGinjector.INSTANCE.getServer();
      if (Desktop.hasDesktopFrame())
      {
         Desktop.getFrame().showFile(StringUtil.notNull(outputFile));
      }
      else
      {
         displayDownloadableDoc(targetFile, outputFile, new Command() {
            @Override
            public void execute()
            {
               String url = server.getFileUrl(
                     FileSystemItem.createFile(outputFile));
               globalDisplay.openWindow(url);
            }
         });
      }
   }
   
   private static void displayDownloadableDoc(
      final String targetFile, final String outputFile, final Command onDownload)
   {
      GlobalDisplay globalDisplay = RStudioGinjector.INSTANCE.getGlobalDisplay();
      globalDisplay.showYesNoMessage(GlobalDisplay.MSG_INFO,
            constants_.renderCompletedCaption(),
            constants_.renderCompletedMsg(targetFile, outputFile),
            false,
            new ProgressOperation()
      {
         @Override
         public void execute(ProgressIndicator indicator)
         {
            onDownload.execute();
            indicator.onCompleted();
         }
      },
      null,
      constants_.yesLabel(),
      constants_.noLabel(),
      false);
   }


   
   private static void displayOfficeDoc(
      final String targetFile, final String outputFile,
      final CommandWithArg<String> displayResult)
   {
      // in desktop mode, the document can be displayed directly
      if (Desktop.isDesktop())
         displayResult.execute(outputFile);

      // it's not possible to show Office docs inline in a useful way from
      // within the browser, so just offer to download the file.
      else
      {
         displayDownloadableDoc(targetFile, outputFile, new Command() {
            @Override
            public void execute()
            {
               displayResult.execute(outputFile);
            }
         });
      }
   }
   
   private static final RMarkdownConstants constants_ = GWT.create(RMarkdownConstants.class);
}
