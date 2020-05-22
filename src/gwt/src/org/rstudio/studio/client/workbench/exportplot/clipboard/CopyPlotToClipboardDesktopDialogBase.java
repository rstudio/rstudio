/*
 * CopyPlotToClipboardDesktopDialogBase.java
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

import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotDialog;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotPreviewer;
import org.rstudio.studio.client.workbench.exportplot.model.ExportPlotOptions;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

public abstract class CopyPlotToClipboardDesktopDialogBase extends ExportPlotDialog
{
   public CopyPlotToClipboardDesktopDialogBase(
                           final ExportPlotOptions options,
                           ExportPlotPreviewer previewer,
                           final OperationWithInput<ExportPlotOptions> onClose)
   {
      super(options, previewer);
     
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
   
   protected abstract void copyAsBitmap(final Operation onCompleted);
}
