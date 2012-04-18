/*
 * NewProjectResources.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.projects.ui.newproject;



import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;


public interface NewProjectResources extends ClientBundle
{
   static NewProjectResources INSTANCE = 
                  (NewProjectResources)GWT.create(NewProjectResources.class);
   
   
   ImageResource newProjectDirectoryIcon();
   ImageResource newProjectDirectoryIconLarge();
   ImageResource existingDirectoryIcon();
   ImageResource existingDirectoryIconLarge();
   ImageResource projectFromRepositoryIcon();
   ImageResource projectFromRepositoryIconLarge();
   
   ImageResource gitIcon();
   ImageResource gitIconLarge();
   ImageResource svnIcon();
   ImageResource svnIconLarge();
   
   static interface Styles extends CssResource
   {
      String wizardWidget();
      String wizardTextEntry();
      String wizardTextEntryLabel();
      String wizardSpacer();
      String vcsSelectorDesktop();
      String wizardCheckbox();
      String vcsNotInstalledWidget();
      String vcsHelpLink();
   }
   
   @Source("NewProjectWizard.css")
   Styles styles();
}
