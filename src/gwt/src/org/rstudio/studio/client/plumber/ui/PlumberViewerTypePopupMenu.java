/*
 * PlumberViewerTypePopupMenu.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.plumber.ui;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.plumber.model.PlumberServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.inject.Inject;

// An extension of the toolbar popup menu that gets the current Plumber viewer
// type and checks the appropriate command before showing the menu
public class PlumberViewerTypePopupMenu extends ToolbarPopupMenu
{
   @Inject
   public PlumberViewerTypePopupMenu(Commands commands,
                                     PlumberServerOperations server)
   {
      commands_ = commands;
      server_ = server;
      addItem(commands.plumberRunInViewer().createMenuItem(false));
      addItem(commands.plumberRunInPane().createMenuItem(false));
      addSeparator();
      addItem(commands.plumberRunInBrowser().createMenuItem(false));
   }

   @Override
   public void getDynamicPopupMenu 
      (final ToolbarPopupMenu.DynamicPopupMenuCallback callback)
   {
      final ToolbarPopupMenu menu = this;
      server_.getPlumberViewerType(
            new ServerRequestCallback<String>()
            {
               @Override
               public void onResponseReceived(String viewerType)
               {
                  commands_.plumberRunInPane().setChecked(false);
                  commands_.plumberRunInViewer().setChecked(false);
                  commands_.plumberRunInBrowser().setChecked(false);
                  if (StringUtil.equals(viewerType, UserPrefs.PLUMBER_VIEWER_TYPE_PANE))
                     commands_.plumberRunInPane().setChecked(true);
                  if (StringUtil.equals(viewerType, UserPrefs.PLUMBER_VIEWER_TYPE_WINDOW))
                     commands_.plumberRunInViewer().setChecked(true);
                  if (StringUtil.equals(viewerType, UserPrefs.PLUMBER_VIEWER_TYPE_BROWSER))
                     commands_.plumberRunInBrowser().setChecked(true);
                  callback.onPopupMenu(menu);
               }
      
               @Override
               public void onError(ServerError error)
               {
                  callback.onPopupMenu(menu);
               }
            });
   }
   
   private final PlumberServerOperations server_;
   private final Commands commands_;
}
