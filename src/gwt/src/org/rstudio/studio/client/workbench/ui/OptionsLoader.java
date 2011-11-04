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

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.AsyncShim;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
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
                 WorkbenchServerOperations server,
                 Provider<PreferencesDialog> pPrefDialog)
   {
      globalDisplay_ = globalDisplay;
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
               prefDialog.initializeRPrefs(rPrefs);
               if (activateSourceControl)
                  prefDialog.activateSourceControl();
               prefDialog.showModal();
            }

            @Override
            public void onError(ServerError error)
            {
               indicator.onError(error.getUserMessage());
            }           
         });        
   }

   private final GlobalDisplay globalDisplay_;
   private final WorkbenchServerOperations server_;
   private final Provider<PreferencesDialog> pPrefDialog_;
}
