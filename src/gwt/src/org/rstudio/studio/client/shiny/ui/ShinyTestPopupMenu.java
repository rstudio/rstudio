/*
 * ShinyTestPopupMenu.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.shiny.ui;

import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.inject.Inject;

public class ShinyTestPopupMenu extends ToolbarPopupMenu
{
   @Inject
   public ShinyTestPopupMenu(Commands commands)
   {
      addItem(commands.shinyRecordTest().createMenuItem(false));
      addItem(commands.shinyRunAllTests().createMenuItem(false));
   }
}
