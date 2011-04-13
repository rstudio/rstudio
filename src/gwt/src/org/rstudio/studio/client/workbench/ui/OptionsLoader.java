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
import org.rstudio.studio.client.workbench.prefs.views.PreferencesDialog;

public class OptionsLoader
{
   public abstract static class Shim extends AsyncShim<OptionsLoader>
   {
      public abstract void showOptions();
   }


   @Inject
   OptionsLoader(Provider<PreferencesDialog> pPrefDialog)
   {
      pPrefDialog_ = pPrefDialog;
   }

   public void showOptions()
   {
      pPrefDialog_.get().showModal();
   }

   private final Provider<PreferencesDialog> pPrefDialog_;
}
