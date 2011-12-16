/*
 * VersionControlPage.java
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
package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.widget.MessageDialog;

import com.google.gwt.resources.client.ImageResource;

public abstract class VersionControlPage extends NewProjectWizardPage
{

   public VersionControlPage(String title, 
                            String subTitle, 
                            String pageCaption, 
                            ImageResource image,
                            ImageResource largeImage,
                            boolean vcsIsAvailable)
   {
      super(title, subTitle, pageCaption, image, largeImage);
      vcsIsAvailable_ = vcsIsAvailable;
      
   }
   
   @Override
   protected boolean acceptNavigation()
   {
      if (!vcsIsAvailable_)
      {
         globalDisplay_.showMessage(
               MessageDialog.INFO, 
               getTitle() + " Not Installed", 
               "An installation of " + getTitle() + " was not detected " +
               "on your system. To create projects from " + getTitle() + " " + 
               "repositories you should install it and then restart RStudio");
         
         return false;
      }
      else
      {
         return true;
      }
   }
   
   
   private final boolean vcsIsAvailable_;
}
