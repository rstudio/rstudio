/*
 * PreferencesDialog.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.prefs.PreferencesDialogBase;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.prefs.events.UiPrefsChangedEvent;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

public class PreferencesDialog extends PreferencesDialogBase<RPrefs>
{
   @Inject
   public PreferencesDialog(WorkbenchServerOperations server,
                            Session session,
                            PreferencesDialogResources res,
                            Provider<GeneralPreferencesPane> pR,
                            EditingPreferencesPane source,
                            RMarkdownPreferencesPane rmarkdown,
                            CompilePdfPreferencesPane compilePdf,
                            AppearancePreferencesPane appearance,
                            PaneLayoutPreferencesPane paneLayout,
                            PackagesPreferencesPane packages,
                            SourceControlPreferencesPane sourceControl,
                            SpellingPreferencesPane spelling, 
                            PublishingPreferencesPane publishing,
                            TerminalPreferencesPane terminal,
                            UIPrefs uiPrefs)
   {
      super("Options", 
            res.styles().panelContainer(),
            true,
            new PreferencesPane[] {pR.get(),
                                   source, 
                                   appearance, 
                                   paneLayout,
                                   packages,
                                   rmarkdown,
                                   compilePdf,
                                   spelling,
                                   sourceControl, 
                                   publishing,
                                   terminal}); 
      session_ = session;
      server_ = server;
      
      if (!session.getSessionInfo().getAllowVcs())
         hidePane(SourceControlPreferencesPane.class);
      
      if (!session.getSessionInfo().getAllowPublish())
         hidePane(PublishingPreferencesPane.class);
      
      else if (!session.getSessionInfo().getAllowExternalPublish() &&
               !uiPrefs.enableRStudioConnect().getValue())
      {
         hidePane(PublishingPreferencesPane.class);
      }
      
      if (!session.getSessionInfo().getAllowShell())
      {
         hidePane(TerminalPreferencesPane.class);
      }
   }
   
   @Override
   protected RPrefs createEmptyPrefs()
   {
      return RPrefs.createEmpty();
   }

  
   @Override
   protected void doSaveChanges(final RPrefs rPrefs,
                                final Operation onCompleted,
                                final ProgressIndicator progressIndicator,
                                final boolean reload)
   {
      // save changes
      server_.setPrefs(
         rPrefs, 
         session_.getSessionInfo().getUiPrefs(),
         new SimpleRequestCallback<Void>() {

            @Override
            public void onResponseReceived(Void response)
            {
               progressIndicator.onCompleted();
               if (onCompleted != null)
                  onCompleted.execute();
               if (reload)
                  reload();
            }

            @Override
            public void onError(ServerError error)
            {
               progressIndicator.onError(error.getUserMessage());
            }           
         });  
      
      // broadcast UI pref changes to satellites
      RStudioGinjector.INSTANCE.getSatelliteManager().dispatchCrossWindowEvent(
                     new UiPrefsChangedEvent(UiPrefsChangedEvent.Data.create(
                           UiPrefsChangedEvent.GLOBAL_TYPE,
                           session_.getSessionInfo().getUiPrefs())));
   }
  
   public static void ensureStylesInjected()
   {
      GWT.<PreferencesDialogResources>create(PreferencesDialogResources.class).styles().ensureInjected();
   }


  
   private final WorkbenchServerOperations server_;
   private final Session session_;
  
  
}
