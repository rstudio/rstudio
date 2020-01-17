/*
 * PanmirrorToolbarResources.java
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


import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;



public interface PanmirrorToolbarResources extends ClientBundle
{
   interface Styles extends CssResource
   {
      String toolbarTextMenuButton();
   }

   @Source("PanmirrorToolbar.css")
   Styles styles();
 
   @Source("bold.png")
   ImageResource bold();
   
   @Source("citation.png")
   ImageResource citation();
   
   @Source("code-block.png")
   ImageResource code_block();
   
   @Source("code.png")
   ImageResource code();
   
   @Source("italic.png")
   ImageResource italic();
   
   @Source("link.png")
   ImageResource link();
   
   @Source("media.png")
   ImageResource media();
   
   @Source("numbered-list.png")
   ImageResource numbered_list();
   
   @Source("properties.png")
   ImageResource properties();
   
   @Source("th.png")
   ImageResource th();
   
   public static PanmirrorToolbarResources INSTANCE = 
      (PanmirrorToolbarResources)GWT.create(PanmirrorToolbarResources.class) ;
}
