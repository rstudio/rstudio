/*
 * PanmirrorCommandUI.java
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

import org.rstudio.studio.client.panmirror.command.PanmirrorCommand;

import com.google.gwt.resources.client.ImageResource;

public class PanmirrorCommandUI
{
   public PanmirrorCommandUI(PanmirrorCommand command, String menuText, ImageResource image)
   {
      this.command_ = command;
      this.menuText_ = menuText;
      this.image_ = image;
   }
   
   public String getMenuText()
   {
      return menuText_;
   }
   
   public ImageResource getImage()
   {
      return image_;
   }
   
   public boolean isVisible()
   {
      return command_ != null;
   }
   
   
   public boolean isEnabled()
   {
      if (command_ != null)
         return command_.isEnabled();
      else
         return false;
   }
   
   public boolean isActive()
   {
      if (command_ != null)
         return command_.isActive();
      else
         return false;
   }
   
   public void execute()
   {
      if (command_ != null)
         command_.execute();
   }
  
   
   private final PanmirrorCommand command_;
   private final String menuText_;
   private final ImageResource image_;
   
     
}
