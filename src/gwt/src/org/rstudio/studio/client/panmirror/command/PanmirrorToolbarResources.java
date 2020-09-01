/*
 * PanmirrorToolbarResources.java
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


import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;



public interface PanmirrorToolbarResources extends ClientBundle
{
   interface Styles extends CssResource
   {
      String toolbarTextMenuButton();
      String toolbarPopupMenu();
      String toolbarPopupSubmenu();
      String toolbarSeparator();
      String toolbarButton();
      String toolbarButtonLatched();
   }

   @Source("PanmirrorToolbar.css")
   Styles styles();
 
   @Source("bold_2x.png")
   ImageResource bold();
   
   @Source("bold_dm_2x.png")
   ImageResource bold_dm();
   
   @Source("blockquote_2x.png")
   ImageResource blockquote();
   
   @Source("citation_2x.png")
   ImageResource citation();
   
   @Source("citation_dm_2x.png")
   ImageResource citation_dm();
   
   @Source("code_2x.png")
   ImageResource code();

   @Source("code_dm_2x.png")
   ImageResource code_dm();

   @Source("italic_2x.png")
   ImageResource italic();
   
   @Source("italic_dm_2x.png")
   ImageResource italic_dm();
   
   @Source("omni_2x.png")
   ImageResource omni();
   
   @Source("link_2x.png")
   ImageResource link();
   
   @Source("image_2x.png")
   ImageResource image();
   
   @Source("numbered_list_2x.png")
   ImageResource numbered_list();
   
   @Source("numbered_list_dm_2x.png")
   ImageResource numbered_list_dm();
   
   @Source("bullet_list_2x.png")
   ImageResource bullet_list();
   
   @Source("bullet_list_dm_2x.png")
   ImageResource bullet_list_dm();
   
   @Source("table_2x.png")
   ImageResource table();
   
   @Source("clear_formatting_2x.png")
   ImageResource clear_formatting();
   
   @Source("clear_formatting_dm_2x.png")
   ImageResource clear_formatting_dm();
   
   @Source("comment_2x.png")
   ImageResource comment();
   
   public static PanmirrorToolbarResources INSTANCE = 
      (PanmirrorToolbarResources)GWT.create(PanmirrorToolbarResources.class);
}
