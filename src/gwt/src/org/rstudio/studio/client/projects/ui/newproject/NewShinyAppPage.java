/*
 * NewShinyAppPage.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.projects.ui.newproject;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.projects.StudioClientProjectConstants;
import org.rstudio.studio.client.projects.model.NewProjectInput;
import org.rstudio.studio.client.projects.model.NewShinyAppOptions;

import com.google.gwt.user.client.ui.HorizontalPanel;


public class NewShinyAppPage extends NewDirectoryPage
{
   public NewShinyAppPage()
   {
      super(constants_.shinyApplicationTitle(),
            constants_.shinyApplicationSubTitle(),
            constants_.shinyApplicationPageCaption(),
            new ImageResource2x(NewProjectResources.INSTANCE.shinyAppIcon2x()),
            new ImageResource2x(NewProjectResources.INSTANCE.shinyAppIconLarge2x()));
   }
    
   @Override 
   protected void onAddTopPanelWidgets(HorizontalPanel panel)
   {
      
   }
   
   @Override
   protected void onAddTopWidgets()
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
   private static final StudioClientProjectConstants constants_ = GWT.create(StudioClientProjectConstants.class);
}
