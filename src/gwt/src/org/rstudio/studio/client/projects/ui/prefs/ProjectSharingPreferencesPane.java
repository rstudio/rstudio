/*
 * ProjectSharingPreferencesPane.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.studio.client.projects.ui.prefs;

import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.projects.StudioClientProjectConstants;
import org.rstudio.studio.client.projects.model.RProjectOptions;

import com.google.gwt.resources.client.ImageResource;

public class ProjectSharingPreferencesPane extends ProjectPreferencesPane
{
   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(ProjectPreferencesDialogResources.INSTANCE.iconShare2x());
   }

   @Override
   public String getName()
   {
      return constants_.sharingText();
   }

   @Override
   protected void initialize(RProjectOptions prefs)
   {
      
   }

   @Override
   public RestartRequirement onApply(RProjectOptions prefs)
   {
      return new RestartRequirement();
   }
   private static final StudioClientProjectConstants constants_ = com.google.gwt.core.client.GWT.create(StudioClientProjectConstants.class);
}
