/*
 * OptionsLoader.java
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
package org.rstudio.studio.client.workbench.ui;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.AsyncShim;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.PrefValue;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.views.PreferencesDialog;

public class OptionsLoader
{
   public abstract static class Shim extends AsyncShim<OptionsLoader>
   {
      public abstract void showOptions();
      public abstract void showOptions(Class<?> paneClass, boolean showPaneChooser);
   }

   @Inject
   OptionsLoader(GlobalDisplay globalDisplay,
                 UserPrefs uiPrefs,
                 Commands commands,
                 WorkbenchServerOperations server,
                 Provider<PreferencesDialog> pPrefDialog)
   {
      globalDisplay_ = globalDisplay;
      uiPrefs_ = uiPrefs;
      commands_ = commands;
      server_ = server;
      pPrefDialog_ = pPrefDialog;
   }

   public void showOptions()
   {
      showOptions(null, true);
   }

   public void showOptions(final Class<?> paneClass, boolean showPaneChooser)
   {
      PreferencesDialog prefDialog = pPrefDialog_.get();
      prefDialog.initialize(RStudioGinjector.INSTANCE.getUserPrefs());
      if (paneClass != null)
         prefDialog.activatePane(paneClass);
      prefDialog.setShowPaneChooser(showPaneChooser);
      prefDialog.showModal();

      // if the user changes global sweave or latex options notify
      // them if this results in the current project being out
      // of sync with the global settings
      new SweaveProjectOptionsNotifier(prefDialog);

      // activate main window if we are in desktop mode (because on
      // the mac you can actually show prefs from a satellite window)
      if (Desktop.hasDesktopFrame())
         Desktop.getFrame().bringMainFrameToFront();
   }

   private class SweaveProjectOptionsNotifier
   {
      public SweaveProjectOptionsNotifier(PreferencesDialog prefsDialog)
      {
         previousRnwWeaveMethod_ = uiPrefs_.defaultSweaveEngine().getGlobalValue();
         previousLatexProgram_ = uiPrefs_.defaultLatexProgram().getGlobalValue();

         prefsDialog.addCloseHandler(popupPanelCloseEvent ->
         {
            boolean notified = notifyIfNecessary(
                                 "weaving Rnw files",
                                 previousRnwWeaveMethod_,
                                 uiPrefs_.defaultSweaveEngine());

            if (!notified)
            {
               notifyIfNecessary("LaTeX typesetting",
                                 previousLatexProgram_,
                                 uiPrefs_.defaultLatexProgram());
            }
         });
      }

      private boolean notifyIfNecessary(String valueName,
                                        String previousValue,
                                        PrefValue<String> pref)
      {
         if (previousValue   != pref.getGlobalValue() &&
             pref.getValue() != pref.getGlobalValue())
         {
            globalDisplay_.showYesNoMessage(
                  MessageDialog.WARNING,
                  "Project Option Unchanged",
                  "You changed the global option for " + valueName  + " to " +
                  pref.getGlobalValue() + ", however the current project is " +
                  "still configured to use " + pref.getValue() + ".\n\n" +
                  "Do you want to edit the options for the current " +
                  "project as well?",
                  () -> commands_.projectSweaveOptions().execute(),
                  true);

            return true;
         }
         else
         {
            return false;
         }
      }

      private final String previousRnwWeaveMethod_;
      private final String previousLatexProgram_;
   }

   private final GlobalDisplay globalDisplay_;
   @SuppressWarnings("unused")
   private final WorkbenchServerOperations server_;
   private final Commands commands_;
   private final UserPrefs uiPrefs_;
   private final Provider<PreferencesDialog> pPrefDialog_;
}
