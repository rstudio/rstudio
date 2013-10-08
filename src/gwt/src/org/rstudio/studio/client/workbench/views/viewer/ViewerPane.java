/*
 * ViewerPane.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.viewer;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.common.AutoGlassPanel;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.viewer.ui.ViewerFrame;

public class ViewerPane extends WorkbenchPane implements ViewerPresenter.Display
{
   @Inject
   public ViewerPane(Commands commands, GlobalDisplay globalDisplay)
   {
      super("Viewer");
      commands_ = commands;
      globalDisplay_ = globalDisplay;
      ensureWidget();
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
      toolbar.addLeftWidget(commands_.viewerPopout().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.viewerPrint().createToolbarButton());
      toolbar.addRightWidget(commands_.viewerStop().createToolbarButton());
      toolbar.addRightSeparator();
      toolbar.addRightWidget(commands_.viewerRefresh().createToolbarButton());
      return toolbar;
   }
   
   @Override 
   protected Widget createMainWidget()
   {
      frame_ = new ViewerFrame() ;
      frame_.setUrl("about:blank");
      frame_.setSize("100%", "100%");
      return new AutoGlassPanel(frame_);
   }
   
   @Override
   public void navigate(String url)
   {
      frame_.navigate(url);
   }
   
   @Override
   public void print()
   {
      frame_.print();
   }

   @Override
   public void popout()
   {
      String url = frame_.getUrl();
      if (url != null)
         globalDisplay_.openWindow(url);
   }

   @Override
   public void refresh()
   {
      String url = frame_.getUrl();
      if (url != null)
         navigate(url);
   }
   
   private ViewerFrame frame_;
   private final Commands commands_;
   private final GlobalDisplay globalDisplay_;
}