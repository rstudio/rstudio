/*
 * PlotsToolbar.java
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
package org.rstudio.studio.client.workbench.views.plots.ui;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.HasCustomizableToolbar;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.rsconnect.ui.RSConnectPublishButton;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.plots.PlotsConstants;

public class PlotsToolbar extends Toolbar implements HasCustomizableToolbar
{    
   public PlotsToolbar(Commands commands, RSConnectPublishButton publishButton)
   {
      super(constants_.plotsPaneLabel());
      commands_ = commands;
      publishButton_ = publishButton;
      installStandardUI();
   }
   
   public void installCustomToolbar(Customizer customizer)
   {
      removeAllWidgets();
      customizer.setToolbarContents(this);
   }
   
   public void removeCustomToolbar()
   {
      removeAllWidgets();
      installStandardUI();
   }

   public void setSizeLabel(String label)
   {
      sizeLabel_ = label;
      if (sizeButton_ != null)
         sizeButton_.setText(label);
   }
   
   private void installStandardUI()
   {
      // plot history navigation
      addLeftWidget(commands_.previousPlot().createToolbarButton());
      addLeftWidget(commands_.nextPlot().createToolbarButton());
      addLeftSeparator();
      
      // popout current plot
      addLeftWidget(commands_.zoomPlot().createToolbarButton());
      addLeftSeparator();

      // plot size
      ToolbarPopupMenu sizeMenu = new ToolbarPopupMenu();
      sizeMenu.addItem(commands_.fitPlotToPane().createMenuItem(false));
      sizeMenu.addItem(commands_.useFixedPlotSize().createMenuItem(false));

      // the empty icon lines the label up with the labels of buttons with icons
      sizeButton_ = new ToolbarMenuButton(
            sizeLabel_, constants_.plotSizeTitle(), StandardIcons.INSTANCE.empty_command(), sizeMenu);
      ElementIds.assignElementId(sizeButton_, ElementIds.MB_PLOTS_SIZE);

      addLeftWidget(sizeButton_);
      addLeftSeparator();
      
      // export commands
      ToolbarPopupMenu exportMenu = new ToolbarPopupMenu();
      exportMenu.addItem(commands_.savePlotAsImage().createMenuItem(false));
      exportMenu.addItem(commands_.savePlotAsPdf().createMenuItem(false));
      exportMenu.addSeparator();
      exportMenu.addItem(commands_.copyPlotToClipboard().createMenuItem(false));
      
      ToolbarMenuButton exportButton = new ToolbarMenuButton(
            constants_.exportText(), ToolbarButton.NoTitle,
            new ImageResource2x(StandardIcons.INSTANCE.export_menu2x()),
            exportMenu);
      ElementIds.assignElementId(exportButton, ElementIds.MB_PLOTS_EXPORT);
      
      addLeftWidget(exportButton);
      addLeftSeparator();
      
      addLeftWidget(commands_.removePlot().createToolbarButton());
      addLeftSeparator();
      
      // clear all plots
      addLeftWidget(commands_.clearPlots().createToolbarButton());  
      
      // publish
      addRightWidget(publishButton_);
    
      // refresh
      addRightSeparator();

      ToolbarButton refreshButton = commands_.refreshPlot().createToolbarButton();
      refreshButton.addStyleName(ThemeStyles.INSTANCE.refreshToolbarButton());
      addRightWidget(refreshButton);
   }
   
   private final Commands commands_;   
   private final RSConnectPublishButton publishButton_;
   private ToolbarMenuButton sizeButton_;
   private String sizeLabel_ = constants_.fitToPaneLabel();
   private static final PlotsConstants constants_ = com.google.gwt.core.client.GWT.create(PlotsConstants.class);
}
