/*
 * ZoomHandler.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.studio.client.application;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.ZoomLevelChangedEvent;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;

public class ZoomHandler
             implements ResizeHandler
{
   public interface Binder extends CommandBinder<Commands, ZoomHandler> {}
   
   @Inject
   public ZoomHandler(Binder binder,
                      Commands commands,
                      EventBus events)
   {
      binder.bind(commands, this);
      level_ = calcZoomLevel();
      events_ = events;
      Window.addResizeHandler(this);
   }
   
   @Handler
   public void onZoomActualSize()
   {
      // only supported in cocoa desktop
      if (BrowseCap.isCocoaDesktop())
         Desktop.getFrame().macZoomActualSize();
   }
   
   @Handler
   public void onZoomIn()
   {
      // pass on to cocoa desktop (qt desktop intercepts)
      if (BrowseCap.isCocoaDesktop())
         Desktop.getFrame().macZoomIn();
   }
   
   @Handler
   public void onZoomOut()
   {
      // pass on to cocoa desktop (qt desktop intercepts)
      if (BrowseCap.isCocoaDesktop())
         Desktop.getFrame().macZoomOut();
   }

   @Override
   public void onResize(ResizeEvent event)
   {
      // check to see if the zoom level has changed
      int newLevel = calcZoomLevel();
      if (level_ != newLevel)
      {
         events_.fireEvent(new ZoomLevelChangedEvent(
               newLevel > level_ ? 
                     ZoomLevelChangedEvent.ZOOM_IN :
                     ZoomLevelChangedEvent.ZOOM_OUT));
         level_ = newLevel;
      }
   }
   
   private int calcZoomLevel()
   {
      return Math.round(WindowEx.get().getDevicePixelRatio() * 100);
   }
   
   private int level_;
   private final EventBus events_;
}