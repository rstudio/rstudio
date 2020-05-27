/*
 * BuildToolsMakefilePanel.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;

public class BuildToolsMakefilePanel extends BuildToolsPanel
{
   public BuildToolsMakefilePanel()
   {
      pathSelector_ = new DirectorySelector("Makefile directory:");
      add(pathSelector_);

      txtMakefileArgs_ = new AdditionalArguments("Additional arguments:");
      Style style = txtMakefileArgs_.getElement().getStyle();
      style.setMarginTop(2, Unit.PX);
      add(txtMakefileArgs_);
   }

   @Override
   void load(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      pathSelector_.setText(config.getMakefilePath());

      txtMakefileArgs_.setText(options.getBuildOptions().getMakefileArgs());
   }

   @Override
   void save(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      config.setMakefilePath(pathSelector_.getText());

      options.getBuildOptions().setMakefileArgs(txtMakefileArgs_.getText().trim());
   }

   private PathSelector pathSelector_;
   private AdditionalArguments txtMakefileArgs_;
}
