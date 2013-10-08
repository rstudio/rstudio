/*
 * ViewerPresenter.java
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

import com.google.inject.Inject;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.viewer.events.ViewerNavigateEvent;

public class ViewerPresenter extends BasePresenter 
{
   public interface Binder extends CommandBinder<Commands, ViewerPresenter> {}
   
   public interface Display extends WorkbenchView
   {
      void navigate(String url);
      void back();
      void forward();
      void print();
      void popout();
      void refresh();
   }
   
   @Inject
   public ViewerPresenter(Display display, 
                          Commands commands,
                          Binder binder)
   {
      super(display);
      display_ = display;
      
      binder.bind(commands, this);
   }
   
   @Handler
   public void onViewerBack() { display_.back(); }
   @Handler
   public void onViewerForward() { display_.forward(); }
   @Handler
   public void onViewerPrint() { display_.print(); }
   @Handler
   public void onViewerPopout() { display_.popout(); }
   @Handler
   public void onViewerRefresh() { display_.refresh(); }
        
   @Handler
   public void onViewerClearHistory()
   {
      
   }
   
   public void onViewerNavigate(ViewerNavigateEvent event)
   {
      display_.bringToFront();
      
      if (event.getFullHeight())
         display_.maximize();
      
      display_.navigate(event.getURL());
   }
   
   private final Display display_ ;

   
}