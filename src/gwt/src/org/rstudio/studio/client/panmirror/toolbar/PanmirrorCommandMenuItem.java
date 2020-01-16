
/*
 * PanmirrorCommandMenuItem.java
 *
 * Copyright (C) 2009-20 by RStudio, Inc.
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



package org.rstudio.studio.client.panmirror.toolbar;

import org.rstudio.core.client.command.AppCommand;

import com.google.gwt.user.client.ui.MenuItem;

public class PanmirrorCommandMenuItem extends MenuItem implements PanmirrorCommandUIObject
{
   public PanmirrorCommandMenuItem(PanmirrorCommandUI commandUI)
   {
      super(
        AppCommand.formatMenuLabel(null, commandUI.getMenuText(), null), 
        true, 
        commandUI.getMenuRole(),
        commandUI.isActive(),
        commandUI
      );
      commandUI_ = commandUI;
      sync();
   }
   
  
   @Override
   public void sync()
   {
      setEnabled(commandUI_.isEnabled());
      if (isEnabled())
         getElement().removeClassName("disabled");
      else
         getElement().addClassName("disabled");

      
      setChecked(commandUI_.isActive());
      setVisible(commandUI_.isVisible());
   }
   
   private final PanmirrorCommandUI commandUI_;

}
