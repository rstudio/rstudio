/*
 * ProjectPreferencesDialogResources.java
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
package org.rstudio.studio.client.projects.ui.prefs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

public interface ProjectPreferencesDialogResources extends ClientBundle
{
   static interface Styles extends CssResource
   {
      String panelContainer();
      String buildToolsPanel();
      String workspaceGrid();
      String enableCodeIndexing();
      String useSpacesForTab();
      String numberOfTabs();
      String encodingChooser();
      String vcsSelectExtraSpaced();
      String vcsOriginLabel();
      String vcsOriginUrl();
      String vcsNoOriginUrl();
      String buildToolsAdditionalArguments();
      String buildToolsRoxygenize();
   }
  
   @Source("ProjectPreferencesDialog.css")
   Styles styles();
  
   ImageResource iconBuild();
   
   static ProjectPreferencesDialogResources INSTANCE = (ProjectPreferencesDialogResources)GWT.create(ProjectPreferencesDialogResources.class);
}
