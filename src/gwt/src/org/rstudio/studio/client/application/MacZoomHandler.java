/*
 * MacZoomHandler.java
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
package org.rstudio.studio.client.application;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.inject.Inject;

public class MacZoomHandler
{
   public interface Binder extends CommandBinder<Commands, MacZoomHandler> {}
   
   @Inject
   public MacZoomHandler(Binder binder,
                         Commands commands)
   {
      binder.bind(commands, this);
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
}
