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

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.EnabledChangedHandler;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
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
      void print();
      void popout();
      void refresh();
   }
   
   @Inject
   public ViewerPresenter(Display display, 
                          EventBus eventBus,
                          Commands commands,
                          Binder binder)
   {
      super(display);
      display_ = display;
      commands_ = commands;
      
      binder.bind(commands, this);
      
      enableCommands(false);
      
      // show a stop button when the console is busy
      commands_.viewerStop().setVisible(commands_.interruptR().isEnabled());
      commands_.interruptR().addEnabledChangedHandler(
                                                new EnabledChangedHandler() {
         @Override
         public void onEnabledChanged(AppCommand command)
         {
            commands_.viewerStop().setVisible(command.isEnabled());
         }
      });
   }
   
   public void onViewerNavigate(ViewerNavigateEvent event)
   {
      enableCommands(true);
      
      display_.bringToFront();
      
      if (event.getFullHeight())
         display_.maximize();
      
      display_.navigate(event.getURL());
   }
   
   @Handler
   public void onViewerPrint() { display_.print(); }
   @Handler
   public void onViewerPopout() { display_.popout(); }
   @Handler
   public void onViewerRefresh() { display_.refresh(); }
        
   @Handler
   public void onViewerStop()
   {
      enableCommands(false);
      
      display_.navigate("about:blank");
      
      commands_.interruptR().execute();
   }
 
   
   private void enableCommands(boolean enable)
   {
      commands_.viewerPopout().setEnabled(enable);
      commands_.viewerPrint().setEnabled(enable);
      commands_.viewerRefresh().setEnabled(enable);
   }
   
   private final Display display_ ;
   private final Commands commands_;
}