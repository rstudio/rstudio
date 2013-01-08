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
package org.rstudio.studio.client.workbench.views.plots.ui.export.impl;

import org.rstudio.core.client.dom.ElementEx;
import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.ImageFrame;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.workbench.views.plots.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;
import org.rstudio.studio.client.workbench.views.plots.ui.export.ExportPlotDialog;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

public class CopyPlotToClipboardDesktopDialog extends ExportPlotDialog
{

   public CopyPlotToClipboardDesktopDialog(
                           PlotsServerOperations server,
                           final ExportPlotOptions options,
                           final OperationWithInput<ExportPlotOptions> onClose)
   {
      super(server, options);
     
      setText("Copy Plot to Clipboard");
      
      ThemedButton copyButton = new ThemedButton("Copy Plot", 
            new ClickHandler() {
         public void onClick(ClickEvent event) 
         {
            // do the copy
            performCopy(new Operation() {

               @Override
               public void execute()
               {
                  // save options
                  onClose.execute(getCurrentOptions(options));
                  
                  // close dialog
                  closeDialog();  
               }        
            });
         }
      });
      
      addOkButton(copyButton);
      addCancelButton();
   }
   
   
   protected void performCopy(Operation onCompleted)
   {
      copyAsBitmap(onCompleted);
   }
   
   protected void copyAsBitmap(Operation onCompleted)
   {
      ImageFrame imageFrame = getSizeEditor().getImageFrame();
      final WindowEx win = imageFrame.getElement().<IFrameElementEx>cast()
            .getContentWindow();

      Document doc = win.getDocument();
      NodeList<Element> images = doc.getElementsByTagName("img");
      if (images.getLength() > 0)
      {
         ElementEx img = images.getItem(0).cast();

         Desktop.getFrame().copyImageToClipboard(img.getClientLeft(),
                                                 img.getClientTop(),
                                                 img.getClientWidth(),
                                                 img.getClientHeight());
      }
      
      onCompleted.execute();
   }
}
