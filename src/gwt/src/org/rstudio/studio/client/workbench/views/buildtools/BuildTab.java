/*
 * BuildTab.java
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
package org.rstudio.studio.client.workbench.views.buildtools;

import com.google.inject.Inject;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;
import org.rstudio.studio.client.workbench.views.buildtools.model.BuildState;

public class BuildTab extends DelayLoadWorkbenchTab<BuildPresenter>
{
   public interface Binder extends CommandBinder<Commands, Shim> {}

   public abstract static class Shim extends DelayLoadTabShim<BuildPresenter, BuildTab> 
   {
      @Handler
      public abstract void onBuildAll();
      @Handler
      public abstract void onDevtoolsLoadAll();
      @Handler
      public abstract void onRebuildAll();
      @Handler
      public abstract void onCleanAll();
      @Handler
      public abstract void onBuildSourcePackage();
      @Handler
      public abstract void onBuildBinaryPackage();
      @Handler
      public abstract void onRoxygenizePackage();
      @Handler
      public abstract void onStopBuild();
      @Handler
      public abstract void onCheckPackage();
      @Handler
      public abstract void onTestPackage();
      @Handler
      public abstract void onTestTestthatFile();
      @Handler
      public abstract void onTestShinytestFile();

      abstract void initialize(BuildState buildState);
   }

   @Inject
   public BuildTab(final Shim shim, 
                   final Session session, 
                   Binder binder, 
                   final Commands commands,
                   EventBus eventBus,
                   UserPrefs uiPrefs)
   {
      super("Build", shim);
      session_ = session;
      binder.bind(commands, shim);

      // stop build always starts out disabled
      commands.stopBuild().setEnabled(false);

      // manage roxygen command
      commands.roxygenizePackage().setVisible(uiPrefs.useRoxygen().getValue());
      uiPrefs.useRoxygen().addValueChangeHandler(
         event -> commands.roxygenizePackage().setVisible(event.getValue()));
        
      eventBus.addHandler(SessionInitEvent.TYPE, sie ->
      {
         SessionInfo sessionInfo = session.getSessionInfo();
         BuildCommands.setBuildCommandState(commands, sessionInfo);

         // initialize from build state if necessary
         BuildState buildState = sessionInfo.getBuildState();
         if (buildState != null)
            shim.initialize(buildState);
      });
   }
   
   @Override
   public boolean isSuppressed()
   {
      return session_.getSessionInfo().getBuildToolsType() == SessionInfo.BUILD_TOOLS_NONE;
   }

   private final Session session_;
}
