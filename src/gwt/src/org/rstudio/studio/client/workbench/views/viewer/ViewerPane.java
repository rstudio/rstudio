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
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.viewer.ui.ViewerFrame;

public class ViewerPane extends WorkbenchPane implements ViewerPresenter.Display
{
   @Inject
   public ViewerPane(Commands commands)
   {
      super("Viewer");
      commands_ = commands;
      ensureWidget();
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
      toolbar.addLeftWidget(commands_.viewerBack().createToolbarButton());
      toolbar.addLeftWidget(commands_.viewerForward().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.viewerPrint().createToolbarButton());
      toolbar.addLeftWidget(commands_.viewerPopout().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.viewerClearHistory().createToolbarButton());
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
   public void back()
   {
      
      
   }

   @Override
   public void forward()
   {
      
      
   }

   @Override
   public void print()
   {
      
      
   }

   @Override
   public void popout()
   {
      
      
   }

   @Override
   public void refresh()
   {
      
      
   }
   
   private ViewerFrame frame_;
   private Commands commands_;
}