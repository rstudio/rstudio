/*
 * ShinyViewerTypePopupMenu.java
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
package org.rstudio.studio.client.shiny.ui;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.core.client.widget.UserPrefMenuItem;
import org.rstudio.studio.client.common.shiny.model.ShinyServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.inject.Inject;

// An extension of the toolbar popup menu that gets the current Shiny viewer
// type and checks the appropriate command before showing the menu
public class ShinyViewerTypePopupMenu extends ToolbarPopupMenu
{
   @Inject
   public ShinyViewerTypePopupMenu(Commands commands,
                                   UserPrefs prefs,
                                   ShinyServerOperations server)
   {
      commands_ = commands;
      server_ = server;
      addItem(commands.shinyRunInViewer().createMenuItem(false));
      addItem(commands.shinyRunInPane().createMenuItem(false));
      addSeparator();
      addItem(commands.shinyRunInBrowser().createMenuItem(false));
      addSeparator();
      addItem(new UserPrefMenuItem<Boolean>(prefs.shinyBackgroundJobs(), 
            false, "In R Console", prefs));
      addItem(new UserPrefMenuItem<Boolean>(prefs.shinyBackgroundJobs(), 
            true, "In Background Job", prefs));
      addSeparator();
      addItem(commands.shinyRecordTest().createMenuItem(false));
      addItem(commands.shinyRunAllTests().createMenuItem(false));
   }

   @Override
   public void getDynamicPopupMenu 
      (final ToolbarPopupMenu.DynamicPopupMenuCallback callback)
   {
      final ToolbarPopupMenu menu = this;
      server_.getShinyViewerType(
            new ServerRequestCallback<String>()
            {
               @Override
               public void onResponseReceived(String viewerType)
               {
                  commands_.shinyRunInPane().setChecked(false);
                  commands_.shinyRunInViewer().setChecked(false);
                  commands_.shinyRunInBrowser().setChecked(false);
                  if (StringUtil.equals(viewerType, UserPrefs.SHINY_VIEWER_TYPE_PANE))
                     commands_.shinyRunInPane().setChecked(true);
                  if (StringUtil.equals(viewerType, UserPrefs.SHINY_VIEWER_TYPE_WINDOW))
                     commands_.shinyRunInViewer().setChecked(true);
                  if (StringUtil.equals(viewerType, UserPrefs.SHINY_VIEWER_TYPE_BROWSER))
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
