/*
 * CopyPlotToClipboardWebDialog.java
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

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotDialog;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotPreviewer;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotResources;
import org.rstudio.studio.client.workbench.exportplot.model.ExportPlotOptions;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

public class CopyPlotToClipboardWebDialog extends ExportPlotDialog
{
   public CopyPlotToClipboardWebDialog(
                            final ExportPlotOptions options,
                            ExportPlotPreviewer previewer,
                            final OperationWithInput<ExportPlotOptions> onClose)
   {
      super(options, previewer);
     
      setText("Copy Plot to Clipboard");
      
      ExportPlotResources resources = ExportPlotResources.INSTANCE;
      
      ThemedButton closeButton = new ThemedButton("Close", 
            new ClickHandler() {
         public void onClick(ClickEvent event) 
         {
            // save options
            onClose.execute(getCurrentOptions(options));
            
            // close dialog
            closeDialog();
         }
      });
      addCancelButton(closeButton);
      
     
      HorizontalPanel infoPanel = new HorizontalPanel();
      
      Image rightMouseImage = new Image(new ImageResource2x(resources.rightMouse2x()));
      infoPanel.add(rightMouseImage);
      
      Label label = new Label("Right click on the plot image above to " +
                              "copy to the clipboard.");
      label.setStylePrimaryName(resources.styles().rightClickCopyLabel());
      infoPanel.add(label);
      
      addLeftWidget(infoPanel);

   }

}
