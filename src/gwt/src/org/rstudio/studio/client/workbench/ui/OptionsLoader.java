/*
 * OptionsLoader.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.ui;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.AsyncShim;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.PrefValue;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.prefs.views.PreferencesDialog;

public class OptionsLoader
{
   public abstract static class Shim extends AsyncShim<OptionsLoader>
   {
      public abstract void showOptions();
      public abstract void showVersionControlOptions();
   }


   @Inject
   OptionsLoader(GlobalDisplay globalDisplay,
                 UIPrefs uiPrefs,
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
      showOptions(false);
   }
   
   public void showVersionControlOptions()
   {
      showOptions(true);
   }
   
   private void showOptions(final boolean activateSourceControl)
   {
      final ProgressIndicator indicator = globalDisplay_.getProgressIndicator(
                                                      "Error Reading Options");
      indicator.onProgress("Reading options...");

      server_.getRPrefs(
         new SimpleRequestCallback<RPrefs>() {

            @Override
            public void onResponseReceived(RPrefs rPrefs)
            {
               indicator.onCompleted();
               PreferencesDialog prefDialog = pPrefDialog_.get();
               prefDialog.initialize(rPrefs);
               if (activateSourceControl)
                  prefDialog.activateSourceControl();
               prefDialog.showModal();
               
               // if the user changes global sweave or latex options notify
               // them if this results in the current project being out
               // of sync with the global settings
               new SweaveProjectOptionsNotifier(prefDialog);
               
               // activate main window if we are in desktop mode (because on
               // the mac you can actually show prefs from a satellite window)
               if (Desktop.isDesktop())
                  Desktop.getFrame().bringMainFrameToFront();
            }

            @Override
            public void onError(ServerError error)
            {
               indicator.onError(error.getUserMessage());
            }           
         });        
   }

   
   private class SweaveProjectOptionsNotifier
   {
      public SweaveProjectOptionsNotifier(PreferencesDialog prefsDialog)
      {
         previousRnwWeaveMethod_ = 
                     uiPrefs_.defaultSweaveEngine().getGlobalValue();
         previousLatexProgram_ = 
                     uiPrefs_.defaultLatexProgram().getGlobalValue();
         
         prefsDialog.addCloseHandler(new CloseHandler<PopupPanel>() {

            @Override
            public void onClose(CloseEvent<PopupPanel> event)
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
            }
            
         });
      }
      
      private boolean notifyIfNecessary(String valueName,
                                        String previousValue, 
                                        PrefValue<String> pref)
      {
         if (!previousValue.equals(pref.getGlobalValue()) &&
             !pref.getValue().equals(pref.getGlobalValue()))
         {
            globalDisplay_.showYesNoMessage(
                  MessageDialog.WARNING, 
                  "Project Option Unchanged", 
                  "You changed the global option for " + valueName  + " to " +
                  pref.getGlobalValue() + ", however the current project is " +
                  "still configured to use " + pref.getValue() + ".\n\n" +
                  "Do you want to edit the options for the current " +
                  "project as well?", 
                  new Operation() {
                     @Override
                     public void execute()
                     {
                        commands_.projectSweaveOptions().execute();
                     }
                  }, 
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
   private final WorkbenchServerOperations server_;
   private final Commands commands_;
   private final UIPrefs uiPrefs_;
   private final Provider<PreferencesDialog> pPrefDialog_;
}
