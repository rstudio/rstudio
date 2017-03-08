/*
 * BuildPaneResources.java
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
package org.rstudio.studio.client.workbench.views.buildtools.ui;


import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

public interface BuildPaneResources extends ClientBundle
{  
   public static interface Styles extends CssResource
   {
      
   }

  
   @Source("BuildPane.css")
   Styles styles();
  
   @Source("iconBuild_2x.png")
   ImageResource iconBuild2x();
   
   public static BuildPaneResources INSTANCE = 
      (BuildPaneResources)GWT.create(BuildPaneResources.class) ;
  
}