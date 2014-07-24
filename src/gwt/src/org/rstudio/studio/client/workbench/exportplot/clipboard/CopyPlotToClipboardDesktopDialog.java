/*
 * CopyPlotToClipboardDesktopDialog.java
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
package org.rstudio.studio.client.workbench.exportplot.clipboard;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotPreviewer;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotSizeEditor;
import org.rstudio.studio.client.workbench.exportplot.model.ExportPlotOptions;

import com.google.gwt.user.client.Command;

public class CopyPlotToClipboardDesktopDialog 
      extends CopyPlotToClipboardDesktopDialogBase
{

   public CopyPlotToClipboardDesktopDialog(
                           ExportPlotPreviewer previewer,
                           ExportPlotClipboard clipboard,
                           ExportPlotOptions options,
                           OperationWithInput<ExportPlotOptions> onClose)
   {
      super(options, previewer, onClose);
     
      clipboard_ = clipboard;
   }
   
   protected void copyAsBitmap(final Operation onCompleted)
   {  
      ExportPlotSizeEditor sizeEditor = getSizeEditor();
      
      if (BrowseCap.isCocoaDesktop()) 
      {
         clipboard_.copyPlotToCocoaPasteboard(
               sizeEditor.getImageWidth(),
               sizeEditor.getImageHeight(),
               new Command() 
               {
                  @Override
                  public void execute()
                  {
                     onCompleted.execute();
                  }
               });
      }
      else
      {
         Rectangle imgRect = sizeEditor.getImageClientRect();
         Desktop.getFrame().copyImageToClipboard(imgRect.getLeft(),
                                                 imgRect.getTop(),
                                                 imgRect.getWidth(),
                                                 imgRect.getHeight());
      }
      
      onCompleted.execute();
   }
   
   protected final ExportPlotClipboard clipboard_;
}
