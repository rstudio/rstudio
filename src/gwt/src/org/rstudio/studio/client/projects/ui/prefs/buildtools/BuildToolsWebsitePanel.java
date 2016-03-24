/*
 * BuildToolsWebsitePanel.java
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


package org.rstudio.studio.client.projects.ui.prefs.buildtools;

import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;

public class BuildToolsWebsitePanel extends BuildToolsPanel
{
   public BuildToolsWebsitePanel()
   {
      pathSelector_ = new DirectorySelector("Website directory:");
      add(pathSelector_);      
   }

   @Override
   void load(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      pathSelector_.setText(config.getWebsitePath());  
   }

   @Override
   void save(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      config.setWebsitePath(pathSelector_.getText());
   }

   private PathSelector pathSelector_;
}
