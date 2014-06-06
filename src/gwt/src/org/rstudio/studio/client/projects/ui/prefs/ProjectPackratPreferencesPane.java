/*
 * ProjectPackratPreferencesPane.java
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
package org.rstudio.studio.client.projects.ui.prefs;

import org.rstudio.studio.client.projects.model.RProjectOptions;

import com.google.gwt.resources.client.ImageResource;
import com.google.inject.Inject;

public class ProjectPackratPreferencesPane extends ProjectPreferencesPane
{
   @Inject
   public ProjectPackratPreferencesPane()
   {
   }

   @Override
   public ImageResource getIcon()
   {
      return ProjectPreferencesDialogResources.INSTANCE.iconPackrat();
   }

   @Override
   public String getName()
   {
      return "Packrat";
   }

   @Override
   protected void initialize(RProjectOptions options)
   {
   }

   @Override
   public boolean onApply(RProjectOptions options)
   {
      return false;
   }
}
