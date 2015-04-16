/*
 * PlotsToolbar.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.plots.ui;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.widget.HasCustomizableToolbar;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.model.PublishHtmlSource;
import org.rstudio.studio.client.rsconnect.model.StaticHtmlGenerator;
import org.rstudio.studio.client.rsconnect.ui.RSConnectPublishButton;
import org.rstudio.studio.client.workbench.commands.Commands;

public class PlotsToolbar extends Toolbar implements HasCustomizableToolbar
{    
   public PlotsToolbar(Commands commands, StaticHtmlGenerator plotHtmlGenerator)
   {   
      commands_ = commands ;
      plotHtmlGenerator_ = plotHtmlGenerator;
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
   
   private void installStandardUI()
   {
      // plot history navigation
      addLeftWidget(commands_.previousPlot().createToolbarButton());
      addLeftWidget(commands_.nextPlot().createToolbarButton());
      addLeftSeparator();
      
      // popout current plot
      addLeftWidget(commands_.zoomPlot().createToolbarButton());
      addLeftSeparator();
      
      // export commands
      ToolbarPopupMenu exportMenu = new ToolbarPopupMenu();
      exportMenu.addItem(commands_.savePlotAsImage().createMenuItem(false));
      exportMenu.addItem(commands_.savePlotAsPdf().createMenuItem(false));
      exportMenu.addSeparator();
      exportMenu.addItem(commands_.copyPlotToClipboard().createMenuItem(false));
      ToolbarButton exportButton = new ToolbarButton(
            "Export", StandardIcons.INSTANCE.export_menu(),
            exportMenu);
      addLeftWidget(exportButton);
      addLeftSeparator();
      
      addLeftWidget(commands_.removePlot().createToolbarButton());
      addLeftSeparator();
      
      // clear all plots
      addLeftWidget(commands_.clearPlots().createToolbarButton());
      
      // refresh
      addLeftSeparator();
      addLeftWidget(commands_.refreshPlot().createToolbarButton());
      
      // publish
      RSConnectPublishButton publishButton = new RSConnectPublishButton(
            RSConnect.CONTENT_TYPE_PLOT, true, commands_.savePlotAsImage());
      publishButton.setPublishHtmlSource(new PublishHtmlSource()
      {
         @Override
         public String getTitle()
         {
            return "Current Plot";
         }
         
         @Override
         public void generatePublishHtml(CommandWithArg<String> onComplete)
         {
            plotHtmlGenerator_.generateStaticHtml("Plot", "", onComplete);
         }
      });
      addRightWidget(publishButton);
   }
   
   private final Commands commands_;   
   private final StaticHtmlGenerator plotHtmlGenerator_;
}
