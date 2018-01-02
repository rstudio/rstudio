/*
 * DesktopMenuCallback.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.core.client.command.impl;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.MenuCallback;

public class DesktopMenuCallback implements MenuCallback
{
   public native final void beginMainMenu() /*-{
      $wnd.desktopMenuCallback.beginMainMenu();
   }-*/;

   public native final void beginMenu(String label) /*-{
      label = @org.rstudio.core.client.command.AppMenuItem::replaceMnemonics(Ljava/lang/String;Ljava/lang/String;)(label, "&");
      $wnd.desktopMenuCallback.beginMenu(label);
   }-*/;

   // TODO: Rather than adding commands one-at-a-time, we should
   // see if we could instead add all of the commands in one message.
   public void addCommand(String commandId, AppCommand command)
   {
      addCommand(commandId,
                 StringUtil.notNull(command.getMenuLabel(true)),
                 StringUtil.notNull(command.getTooltip()),
                 StringUtil.notNull(command.getShortcutRaw()),
                 command.isCheckable());
   }

   private native void addCommand(String cmdId,
                                  String label,
                                  String tooltip,
                                  String shortcut,
                                  boolean isCheckable) /*-{
      $wnd.desktopMenuCallback.addCommand(cmdId, label, tooltip, shortcut, isCheckable);
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
   
   public native static final void setCommandVisible(String commandId, boolean visible) /*-{
      $wnd.desktopMenuCallback.setCommandVisible(commandId, visible);
   }-*/;

   public native static final void setCommandEnabled(String commandId, boolean enabled) /*-{
      $wnd.desktopMenuCallback.setCommandEnabled(commandId, enabled);
   }-*/;

   public native static final void setCommandChecked(String commandId, boolean checked) /*-{
      $wnd.desktopMenuCallback.setCommandChecked(commandId, checked);
   }-*/;

   public native static final void setCommandLabel(String commandId, String label) /*-{
      label = @org.rstudio.core.client.command.AppMenuItem::replaceMnemonics(Ljava/lang/String;Ljava/lang/String;)(label, "&");
      $wnd.desktopMenuCallback.setCommandLabel(commandId, label);
   }-*/;
}
