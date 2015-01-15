/*
 * PublishingPreferencesPane.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

import com.google.gwt.resources.client.ImageResource;
import com.google.inject.Inject;

import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

public class PublishingPreferencesPane extends PreferencesPane
{
   @Inject
   public PublishingPreferencesPane(GlobalDisplay globalDisplay,
                                  UIPrefs prefs)
   {
      globalDisplay_ = globalDisplay;
      uiPrefs_ = prefs;

      add(checkboxPref("Enable publishing", prefs.autoAppendNewline()));
   }

   @Override
   protected void initialize(RPrefs rPrefs)
   {
   }

   @Override
   public boolean onApply(RPrefs rPrefs)
   {
      return super.onApply(rPrefs);
   }

   
   @Override
   public ImageResource getIcon()
   {
      return PreferencesDialogBaseResources.INSTANCE.iconPublishing();
   }

   @Override
   public boolean validate()
   {
      return true;
   }

   @Override
   public String getName()
   {
      return "Publishing";
   }

   private final GlobalDisplay globalDisplay_;
   private final UIPrefs uiPrefs_;
}