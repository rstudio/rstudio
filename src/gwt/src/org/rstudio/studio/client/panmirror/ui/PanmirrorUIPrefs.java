/*
 * PanmirrorUIPrefs.java
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



package org.rstudio.studio.client.panmirror.ui;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;

import com.google.inject.Inject;
import com.google.inject.Provider;

import jsinterop.annotations.JsType;

@JsType
public class PanmirrorUIPrefs {
   
   public PanmirrorUIPrefs() 
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }

   @Inject
   void initialize(Provider<UserPrefs> pUIPrefs)
   {
      pUIPrefs_ = pUIPrefs;
   }
   
   public Boolean equationPreview()
   {
      return !pUIPrefs_.get().latexPreviewOnCursorIdle().getValue()
               .equals(UserPrefsAccessor.LATEX_PREVIEW_ON_CURSOR_IDLE_NEVER);
   }
   
   Provider<UserPrefs> pUIPrefs_;
}
