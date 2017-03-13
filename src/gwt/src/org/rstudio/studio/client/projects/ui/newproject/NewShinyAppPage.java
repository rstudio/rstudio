/*
 * NewShinyAppPage.java
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
package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.projects.model.NewProjectInput;
import org.rstudio.studio.client.projects.model.NewShinyAppOptions;

import com.google.gwt.user.client.ui.HorizontalPanel;


public class NewShinyAppPage extends NewDirectoryPage
{
   public NewShinyAppPage()
   {
      super("Shiny Web Application", 
            "Create a new Shiny web application",
            "Create Shiny Web Application",
            new ImageResource2x(NewProjectResources.INSTANCE.shinyAppIcon2x()),
            new ImageResource2x(NewProjectResources.INSTANCE.shinyAppIconLarge2x()));
   }
    
   @Override 
   protected void onAddTopPanelWidgets(HorizontalPanel panel)
   {
      
   }
   
   @Override
   protected void onAddBodyWidgets()
   {
     
   }
   
   @Override 
   protected void initialize(NewProjectInput input)
   {
      super.initialize(input);
      
   }
   
   @Override
   protected NewShinyAppOptions getNewShinyAppOptions()
   {
      return NewShinyAppOptions.create();
   }
}
