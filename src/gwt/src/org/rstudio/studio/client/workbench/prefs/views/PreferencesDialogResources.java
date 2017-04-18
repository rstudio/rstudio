/*
 * PreferencesDialogResources.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

public interface PreferencesDialogResources extends ClientBundle
{
   public interface Styles extends CssResource
   {
      String panelContainer();
      String paneLayoutTable();
      String themeChooser();
      String sshKeyWidget();
      String usingVcsHelp();
      String newSection();
      String alwaysCompletePanel();
   }

   @Source("PreferencesDialog.css")
   Styles styles();

   @Source("iconAppearance_2x.png")
   ImageResource iconAppearance2x();

   @Source("iconPanes_2x.png")
   ImageResource iconPanes2x();

   @Source("iconPackages_2x.png")
   ImageResource iconPackages2x();

   @Source("iconRMarkdown_2x.png")
   ImageResource iconRMarkdown2x();

   @Source("iconTerminal_2x.png")
   ImageResource iconTerminal2x();
}
