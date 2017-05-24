/*
 * ProjectGeneralPreferencesPane.java
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

import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.model.RProjectRVersion;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.inject.Inject;

public class ProjectGeneralPreferencesPane extends ProjectPreferencesPane
{
   @Inject
   public ProjectGeneralPreferencesPane(Session session)
   {        
      sessionInfo_ = session.getSessionInfo();

      Grid grid = new Grid(5, 2);
      grid.addStyleName(RESOURCES.styles().workspaceGrid());
      grid.setCellSpacing(8);

      Label infoLabel = new Label("Use (Default) to inherit the global default setting");
      infoLabel.addStyleName(PreferencesDialogBaseResources.INSTANCE.styles().infoLabel());
      grid.setWidget(0, 0, infoLabel);

      // restore workspace
      grid.setWidget(1, 0, new Label("Restore .RData into workspace at startup"));
      grid.setWidget(1, 1, restoreWorkspace_ = new YesNoAskDefault(false));

      // save workspace      
      grid.setWidget(2, 0, new Label("Save workspace to .RData on exit"));
      grid.setWidget(2, 1, saveWorkspace_ = new YesNoAskDefault(true));

      // always save history
      grid.setWidget(3, 0, new Label("Always save history (even if not saving .RData)"));
      grid.setWidget(3, 1, alwaysSaveHistory_ = new YesNoAskDefault(false));

      // quit child processes
      grid.setWidget(4, 0, quitChildProcessesOnExit_ = new CheckBox("Quit child processes on exit"));

      add(grid);
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(PreferencesDialogBaseResources.INSTANCE.iconR2x());
   }

   @Override
   public String getName()
   {
      return "General";
   }

   @Override
   protected void initialize(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      restoreWorkspace_.setSelectedIndex(config.getRestoreWorkspace());
      saveWorkspace_.setSelectedIndex(config.getSaveWorkspace());
      alwaysSaveHistory_.setSelectedIndex(config.getAlwaysSaveHistory());
      tutorialPath_ = config.getTutorialPath();
      rVersion_ = config.getRVersion();
      
      // check or uncheck the checkbox for child processes based on the configuration value
      // if default is specified, we need to use the current session setting
      // otherwise, yes or no indicate the check state exactly
      int quitChildProcessesOnExit = config.getQuitChildProcessesOnExit();
      boolean quitChildProcessesChecked = sessionInfo_.quitChildProcessesOnExit();
      
      switch (quitChildProcessesOnExit)
      {
      case YES_VALUE:
         quitChildProcessesChecked = true;
         break;
      case NO_VALUE:
         quitChildProcessesChecked = false;
         break;
      }
      
      quitChildProcessesOnExit_.setValue(quitChildProcessesChecked);
      
   }

   @Override
   public boolean onApply(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      config.setRestoreWorkspace(restoreWorkspace_.getSelectedIndex());
      config.setSaveWorkspace(saveWorkspace_.getSelectedIndex());
      config.setAlwaysSaveHistory(alwaysSaveHistory_.getSelectedIndex());
      config.setTutorialPath(tutorialPath_);
      config.setRVersion(rVersion_);
      
      // turn the quit child processes checkbox from a boolean into the 
      // YesNoAsk value that it should be in the configuration
      boolean quitChildProcessesChecked = quitChildProcessesOnExit_.getValue();
      int quitChildProcessesOnExit = 0;
      if (quitChildProcessesChecked != sessionInfo_.quitChildProcessesOnExit())
      {
         quitChildProcessesOnExit = (quitChildProcessesChecked ? YES_VALUE : NO_VALUE);
      }
      
      config.setQuitChildProcessesOnExit(quitChildProcessesOnExit);
      return false;
   }
   
   private class YesNoAskDefault extends ListBox
   {
      public YesNoAskDefault(boolean includeAsk)
      {
         super();
         setMultipleSelect(false);
         
         String[] items = includeAsk ? new String[] {USE_DEFAULT, YES, NO, ASK}:
                                       new String[] {USE_DEFAULT, YES, NO};
         
         for (int i=0; i<items.length; i++)
            addItem(items[i]);
      }
      
      @Override
      public void setSelectedIndex(int value)
      {
         if (value < getItemCount())
            super.setSelectedIndex(value);
         else
            super.setSelectedIndex(0);
      }
   }
   
   private static final String USE_DEFAULT = "(Default)";
   private static final String YES = "Yes";
   private static final int YES_VALUE = 1;
   private static final String NO = "No";
   private static final int NO_VALUE = 2;
   private static final String ASK ="Ask";
   
   private YesNoAskDefault restoreWorkspace_;
   private YesNoAskDefault saveWorkspace_;
   private YesNoAskDefault alwaysSaveHistory_;
   private CheckBox quitChildProcessesOnExit_;
   private SessionInfo sessionInfo_;
   
   private String tutorialPath_;
   
   private RProjectRVersion rVersion_;
}
