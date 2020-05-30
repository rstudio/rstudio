/*
 * WebApplicationHeaderOverlay.java
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

package org.rstudio.studio.client.application.ui.impl;

import org.rstudio.core.client.command.AppMenuBar;
import org.rstudio.studio.client.RStudioGinjector;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class WebApplicationHeaderOverlay
{
   public interface Context
   {
      void addCommand(Widget widget);
      Widget addCommandSeparator();
      void addLeftCommand(Widget widget);
      void addLeftCommand(Widget widget, String width);
      void addRightCommand(Widget widget);
      Widget addRightCommandSeparator();
      void addProjectCommand(Widget widget);
      Widget addProjectCommandSeparator();
      void addProjectRightCommand(Widget widget);
      Widget addProjectRightCommandSeparator();
      void addUserCommand(Widget widget);
      AppMenuBar getMainMenu();
   }
   
   public WebApplicationHeaderOverlay()
   {  
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   public void initialize()
   {
   }
   
   public void addRVersionsToolbar(Context context)
   {
   }

   public void addConnectionStatusToolbar(Context context)
   {
   }

   public void addSessionsToolbar(Context context)
   {
   }

   public void addCommands(Context context)
   {
      context.addLeftCommand(new HTML(), "100%");
   }
   
   public void setGlobalToolbarVisible(Context context, boolean visible)
   {
   }
}
