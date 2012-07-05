/*
 * BuildToolsMakefilePanel.java
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


package org.rstudio.studio.client.projects.ui.prefs.buildtools;

import org.rstudio.studio.client.projects.model.RProjectConfig;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;


public class BuildToolsMakefilePanel extends BuildToolsPanel
{
   public BuildToolsMakefilePanel()
   {
      pathSelector_ = new DirectorySelector("Makefile directory:");
      add(pathSelector_);
      
      add(new Label("Additional arguments:"));
      txtMakefileArgs_ = new TextBox();
      add(txtMakefileArgs_);
      
   }

   @Override
   void load(RProjectConfig config)
   {
      pathSelector_.setText(config.getMakefilePath());
      txtMakefileArgs_.setText(config.getMakefileArgs());
      
   }

   @Override
   void save(RProjectConfig config)
   {
      config.setMakefilePath(pathSelector_.getText());
      config.setMakefileArgs(txtMakefileArgs_.getText().trim());
   }

   private PathSelector pathSelector_;
   private TextBox txtMakefileArgs_;
}
