/*
 * BuildToolsCustomPanel.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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
package org.rstudio.studio.client.projects.ui.prefs.buildtools;

import org.rstudio.core.client.ElementIds;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.projects.StudioClientProjectConstants;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;


public class BuildToolsCustomPanel extends BuildToolsPanel
{
   public BuildToolsCustomPanel()
   {
      pathSelector_ = new FileSelector(constants_.pathSelectorLabel(), ElementIds.TextBoxButtonId.BUILD_SCRIPT);
      pathSelector_.setTextWidth("250px");
      add(pathSelector_);
   }

   @Override
   void load(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      pathSelector_.setText(config.getCustomScriptPath());
   }

   @Override
   void save(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      config.setCustomScriptPath(pathSelector_.getText());
   }
   
   @Override
   boolean validate()
   {
      boolean valid = pathSelector_.getText().length() != 0;
      if (!valid)
      {
         RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
                  constants_.validateScriptCaption(),
                  constants_.validateScriptMessage());
      }
      
      return valid;
   }

   private PathSelector pathSelector_;
   private static final StudioClientProjectConstants constants_ = com.google.gwt.core.client.GWT.create(StudioClientProjectConstants.class);
}
