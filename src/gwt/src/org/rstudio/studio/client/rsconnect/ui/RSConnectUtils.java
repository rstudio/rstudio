/*
 * RSConnectUtils.java
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

package org.rstudio.studio.client.rsconnect.ui;

import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.commands.Commands;

public class RSConnectUtils
{
   public static ToolbarPopupMenu createToolbarPopupMenu()
   {
      Commands commands = RStudioGinjector.INSTANCE.getCommands();   
      ToolbarPopupMenu menu = new ToolbarPopupMenu();
      menu.addItem(commands.rsconnectDeploy().createMenuItem(false));
      menu.addItem(commands.rsconnectManage().createMenuItem(false));
      menu.addSeparator();
      menu.addItem(commands.rsconnectManageAccounts().createMenuItem(false));
      return menu;
   }
}
