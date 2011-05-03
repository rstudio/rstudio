/*
 * ExportPlotDialog.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.plots.ui.export;

import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.workbench.views.plots.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ExportPlotDialog extends ModalDialogBase 
{
   public ExportPlotDialog(PlotsServerOperations server,
                           ExportPlotOptions options,
                           final OperationWithInput<ExportPlotOptions> onClose)
   { 
      server_ = server;
      options_ = options;
      
      setText("Export Plot");
     
      setButtonAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
      
      ThemedButton closeButton = new ThemedButton("Close", new ClickHandler() {
         public void onClick(ClickEvent event) {
            onClose.execute(options_);
            closeDialog();
         }
      });
      addCancelButton(closeButton); 
   }
  
   @Override
   protected Widget createMainWidget()
   {
      VerticalPanel mainPanel = new VerticalPanel();
      mainPanel.setStylePrimaryName(
                  ExportPlotDialogResources.INSTANCE.styles().mainWidget());
      
      
   
      plotSizer_ = new PlotSizer(options_.getWidth(), 
                                 options_.getHeight(),
                                 server_,
                                 new PlotSizer.Observer() {
                                    public void onPlotResized(boolean withMouse)
                                    {
                                       if (!withMouse)
                                          center();       
                                    }
                                 }); 
      mainPanel.add(plotSizer_);
       
      return mainPanel;
      
   }
   
   @Override
   protected void onDialogShown()
   {
      super.onDialogShown();
      center();
      plotSizer_.onSizerShown();
   }
   
  
   private final PlotsServerOperations server_;
   private ExportPlotOptions options_;
   
   private PlotSizer plotSizer_;

 
  
}
