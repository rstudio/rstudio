/*
 * ExportPlotDialog.java
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
package org.rstudio.studio.client.workbench.exportplot;

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.studio.client.workbench.exportplot.model.ExportPlotOptions;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ExportPlotDialog extends ModalDialogBase 
{
   public ExportPlotDialog(ExportPlotOptions options,
                           ExportPlotPreviewer previewer)
   {
      super(Roles.getDialogRole());
      options_ = options;
      previewer_ = previewer;
   }
  
   @Override
   protected Widget createMainWidget()
   {
      VerticalPanel mainPanel = new VerticalPanel();
   
      // enforce maximum initial dimensions based on screen size
      Size maxSize = new Size(Window.getClientWidth() - 100,
                              Window.getClientHeight() - 200);
      
      int width = Math.min(options_.getWidth(), maxSize.width);
      int height = Math.min(options_.getHeight(), maxSize.height);
       
      sizeEditor_ = new ExportPlotSizeEditor(
                                 width, 
                                 height,
                                 options_.getKeepRatio(),
                                 createTopLeftWidget(),
                                 previewer_,
                                 new ExportPlotSizeEditor.Observer()
                                 {     
                                    @Override
                                    public void onResized(boolean withMouse)
                                    {
                                       if (!withMouse)
                                          center();   
                                    }
                                 }); 
      mainPanel.add(sizeEditor_);
      
      Widget bottomWidget = createBottomWidget();
      if (bottomWidget != null)
         mainPanel.add(bottomWidget);
       
      return mainPanel;
      
   }
   
   protected Widget createTopLeftWidget()
   {
      return null;
   }
   
   protected Widget createBottomWidget()
   {
      return null;
   }
   
   protected ExportPlotSizeEditor getSizeEditor()
   {
      return sizeEditor_;
   }
   
   protected ExportPlotOptions getCurrentOptions(ExportPlotOptions previous)
   {
      ExportPlotSizeEditor sizeEditor = getSizeEditor();
      return ExportPlotOptions.create(sizeEditor.getImageWidth(), 
                                      sizeEditor.getImageHeight(), 
                                      sizeEditor.getKeepRatio(),
                                      previous.getFormat(),
                                      previous.getViewAfterSave(),
                                      previous.getCopyAsMetafile());    
   }
    
  
   @Override
   protected void focusInitialControl()
   {
      sizeEditor_.onSizerShown();
   }
      
   private final ExportPlotOptions options_;
   private final ExportPlotPreviewer previewer_;
   private ExportPlotSizeEditor sizeEditor_;
}
