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

import org.rstudio.studio.client.workbench.prefs.model.HistoryPrefs;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;

public class HistoryPreferencesPane extends PreferencesPane
{
   @Inject
   public HistoryPreferencesPane(PreferencesDialogResources res)
   {
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
   public void onApply(RPrefs rPrefs)
   {
      super.onApply(rPrefs);
     
      // set history prefs
      HistoryPrefs historyPrefs = HistoryPrefs.create(
                                       alwaysSaveHistory_.getValue(),
                                       useGlobalHistory_.getValue());
      rPrefs.setHistoryPrefs(historyPrefs);
   }

   private final PreferencesDialogResources res_;
   private final CheckBox alwaysSaveHistory_;
   private final CheckBox useGlobalHistory_;
}
