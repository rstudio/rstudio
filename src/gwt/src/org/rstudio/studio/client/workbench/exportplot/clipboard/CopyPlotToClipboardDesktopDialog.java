/*
 * CopyPlotToClipboardDesktopDialog.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
import org.rstudio.core.client.dom.ElementEx;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.DesktopFrame;
import org.rstudio.studio.client.common.Timers;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotPreviewer;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotSizeEditor;
import org.rstudio.studio.client.workbench.exportplot.model.ExportPlotOptions;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
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
      final ExportPlotSizeEditor sizeEditor = getSizeEditor();
      sizeEditor.setGripperVisible(false);
      
      // NOTE: we use a timer here just to be absolutely sure the
      // browser has re-rendered and hidden the gripper before attempting
      // to get a screenshot. note that the usual tools, e.g. scheduleDeferred(),
      // don't work as expected here
      Timers.singleShot(200, () -> { doCopyAsBitmap(onCompleted); });
   }
   
   private void doCopyAsBitmap(final Operation onCompleted)
   {
      final ExportPlotSizeEditor sizeEditor = getSizeEditor();
      
      final Command completed = new Command()
      {
         @Override
         public void execute()
         {
            sizeEditor.setGripperVisible(true);
            onCompleted.execute();
         }
      };
      
      sizeEditor.prepareForExport(() -> {
         if (BrowseCap.isMacintoshDesktop())
         {
            clipboard_.copyPlotToCocoaPasteboard(
                  sizeEditor.getImageWidth(),
                  sizeEditor.getImageHeight(),
                  completed);
         }
         else
         {
            WindowEx win = sizeEditor.getPreviewIFrame().getContentWindow();
            Document doc = win.getDocument();
            NodeList<Element> images = doc.getElementsByTagName("img");
            if (images.getLength() > 0)
            {
               Element img = images.getItem(0);
               DesktopFrame frame = Desktop.getFrame();
               
               // NOTE: we use a one-pixel fudge factor here to avoid copying
               // bits of the border; see https://github.com/rstudio/rstudio/issues/4864
               frame.copyPageRegionToClipboard(
                     ElementEx.getClientLeft(img) + 1,
                     ElementEx.getClientTop(img) + 1,
                     img.getClientWidth(),
                     img.getClientHeight(),
                     completed);
            }
            else
            {
               // shouldn't happen but make sure we clean up after
               completed.execute();
            }
         }
      });
   }
   
   protected final ExportPlotClipboard clipboard_;
}
