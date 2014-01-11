/*
 * ShinyViewerTypePopupMenu.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.shiny.ui;

import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.shiny.model.ShinyServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.shiny.model.ShinyViewerType;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.inject.Inject;

// An extension of the toolbar popup menu that gets the 
public class ShinyViewerTypePopupMenu extends ToolbarPopupMenu
{
   @Inject
   public ShinyViewerTypePopupMenu(Commands commands,
                                   ShinyServerOperations server)
   {
      commands_ = commands;
      server_ = server;
      addItem(commands.shinyRunInPane().createMenuItem(false));
      addItem(commands.shinyRunInViewer().createMenuItem(false));
      addItem(commands.shinyRunInBrowser().createMenuItem(false));
   }

   @Override
   public void getDynamicPopupMenu 
      (final ToolbarPopupMenu.DynamicPopupMenuCallback callback)
   {
      final ToolbarPopupMenu menu = this;
      server_.getShinyViewerType(
            new ServerRequestCallback<ShinyViewerType>()
            {
               @Override
               public void onResponseReceived(ShinyViewerType response)
               {
                  int viewerType = response.getViewerType();
                  commands_.shinyRunInPane().setChecked(false);
                  commands_.shinyRunInViewer().setChecked(false);
                  commands_.shinyRunInBrowser().setChecked(false);
                  if (ShinyViewerType.SHINY_VIEWER_PANE == viewerType)
                     commands_.shinyRunInPane().setChecked(true);
                  if (ShinyViewerType.SHINY_VIEWER_WINDOW == viewerType)
                     commands_.shinyRunInViewer().setChecked(true);
                  if (ShinyViewerType.SHINY_VIEWER_BROWSER == viewerType)
                     commands_.shinyRunInBrowser().setChecked(true);
                  callback.onPopupMenu(menu);
               }
      
               @Override
               public void onError(ServerError error)
               {
                  callback.onPopupMenu(menu);
               }
            });
   }
   
   private final ShinyServerOperations server_;
   private final Commands commands_;
}
