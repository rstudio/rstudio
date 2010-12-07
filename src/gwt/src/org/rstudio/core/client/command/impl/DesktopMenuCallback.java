/*
 * DesktopMenuCallback.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.command.impl;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.MenuCallback;

public class DesktopMenuCallback implements MenuCallback
{
   public native final void beginMainMenu() /*-{
      $wnd.desktopMenuCallback.beginMainMenu();
   }-*/;

   public native final void beginMenu(String label) /*-{
      $wnd.desktopMenuCallback.beginMenu(label);
   }-*/;

   public void addCommand(String commandId, AppCommand command)
   {
      addCommand(commandId,
                 command.getMenuLabel(),
                 command.getTooltip(),
                 command.getShortcutRaw());
   }

   private native final void addCommand(String cmdId,
                                        String label,
                                        String tooltip,
                                        String shortcut) /*-{
      $wnd.desktopMenuCallback.addCommand(cmdId, label, tooltip, shortcut);
   }-*/;

   public native final void addSeparator() /*-{
      $wnd.desktopMenuCallback.addSeparator();
   }-*/;

   public native final void endMenu() /*-{
      $wnd.desktopMenuCallback.endMenu();
   }-*/;

   public native final void endMainMenu() /*-{
      $wnd.desktopMenuCallback.endMainMenu();
   }-*/;
}
