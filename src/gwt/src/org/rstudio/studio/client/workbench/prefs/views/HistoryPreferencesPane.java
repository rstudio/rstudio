/*
 * HistoryPreferencesPane.java
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
package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.inject.Inject;

import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.prefs.model.HistoryPrefs;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;

public class HistoryPreferencesPane extends PreferencesPane
{
   @Inject
   public HistoryPreferencesPane(WorkbenchServerOperations server,
                                 PreferencesDialogResources res)
   {
      server_ = server;
      res_ = res;

      add(alwaysSaveHistory_ = new CheckBox(
            "Always save .Rhistory (even when not saving .RData)"));
      alwaysSaveHistory_.setEnabled(false);
      
      add(useGlobalHistory_ = new CheckBox(
            "Use global .Rhistory (rather than per-working directory)"));
      useGlobalHistory_.setEnabled(false);
   }

   @Override
   protected void initializeRPrefs(RPrefs rPrefs)
   {
      HistoryPrefs prefs = rPrefs.getHistoryPrefs();
      
      alwaysSaveHistory_.setEnabled(true);
      alwaysSaveHistory_.setValue(prefs.getAlwaysSave());
      
      useGlobalHistory_.setEnabled(true);
      useGlobalHistory_.setValue(prefs.getUseGlobal()); 
   }

   @Override
   public ImageResource getIcon()
   {
      return res_.iconHistory();
   }

   @Override
   public boolean validate()
   {
      return true;
   }

   @Override
   public String getName()
   {
      return "History";
   }

   @Override
   public void onApply()
   {
      super.onApply();
     
      server_.setHistoryPrefs(alwaysSaveHistory_.getValue(),
                              useGlobalHistory_.getValue(),
                              new SimpleRequestCallback<Void>());
   }

   private final WorkbenchServerOperations server_;
   private final PreferencesDialogResources res_;
   private final CheckBox alwaysSaveHistory_;
   private final CheckBox useGlobalHistory_;
}
