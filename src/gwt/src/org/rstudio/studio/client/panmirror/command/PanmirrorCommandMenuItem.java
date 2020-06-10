
/*
 * PanmirrorCommandMenuItem.java
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



package org.rstudio.studio.client.panmirror.command;

import org.rstudio.core.client.AriaUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.theme.res.ThemeStyles;

import com.google.gwt.aria.client.MenuitemcheckboxRole;
import com.google.gwt.aria.client.MenuitemradioRole;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.MenuItem;

public class PanmirrorCommandMenuItem extends MenuItem implements PanmirrorCommandUIObject
{
   public PanmirrorCommandMenuItem(PanmirrorCommandUI commandUI)
   {
      this(commandUI, null);
   }
   
   public PanmirrorCommandMenuItem(PanmirrorCommandUI commandUI, String menuText)
   {
      super(
        AppCommand.formatMenuLabel(
         null, 
         menuText != null ? menuText : commandUI.getMenuText(), 
         null
        ), 
        true, 
        commandUI.getMenuRole(),
        commandUI.isActive(),
        commandUI
      );
      commandUI_ = commandUI;
      menuText_ = menuText;
      sync(true);
   }
   
  
   @Override 
   public void sync(boolean images)
   {  
      setVisible(commandUI_.isVisible());
      
      if (isVisible())
      {
         setEnabled(commandUI_.isEnabled());
         if (isEnabled())
            getElement().removeClassName("disabled");
         else
            getElement().addClassName("disabled");
      }
      
      if (isCheckable())
         setChecked(commandUI_.isActive());
      
      setTitle(commandUI_.getDesc());
      
      setHTML(menuHTML());
   }
   
   
   private String menuHTML()
   {
      return AppCommand.formatMenuLabelWithStyle(
         menuImageResource(), 
         menuText_ != null ? menuText_ : commandUI_.getMenuText(), 
         commandUI_.getShortcut(), 
         isCheckable() ? ThemeStyles.INSTANCE.menuCheckable() : null
      );
   }

   private ImageResource menuImageResource()
   {
      return AppCommand.menuImageResource(isCheckable(), isChecked(), commandUI_.getImage());
   }
   
   private boolean isChecked()
   {
      return AriaUtil.isMenuChecked(commandUI_.getMenuRole(), getElement());
   }
   
   private boolean isCheckable()
   {
      return commandUI_.getMenuRole() instanceof MenuitemradioRole || 
             commandUI_.getMenuRole() instanceof MenuitemcheckboxRole;
   }
   
  
   private final PanmirrorCommandUI commandUI_;
   private final String menuText_;
   

}
