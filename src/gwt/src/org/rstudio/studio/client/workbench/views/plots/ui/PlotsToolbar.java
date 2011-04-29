/*
 * PlotsToolbar.java
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
package org.rstudio.studio.client.workbench.views.plots.ui;

import org.rstudio.core.client.widget.HasCustomizableToolbar;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.workbench.commands.Commands;

public class PlotsToolbar extends Toolbar implements HasCustomizableToolbar
{    
   public PlotsToolbar(Commands commands)
   {   
      commands_ = commands ;
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
      
      // export as image
      addLeftWidget(commands_.exportPlotAsImage().createToolbarButton());
      addLeftSeparator();
      
      // print
      addLeftWidget(commands_.printPlot().createToolbarButton());
      addLeftSeparator();
      
      addLeftWidget(commands_.removePlot().createToolbarButton());
      addLeftSeparator();
      
      // clear all plots
      addLeftWidget(commands_.clearPlots().createToolbarButton());
      
      // refresh
      addRightWidget(commands_.refreshPlot().createToolbarButton());
   }
   
   private Commands commands_;   
}
