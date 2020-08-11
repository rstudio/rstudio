/*
 * FocusVisiblePolyfill.java
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
package org.rstudio.studio.client.workbench.ui.polyfill;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.ExternalStyleSheetLoader;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

@Singleton
public class FocusVisiblePolyfill
{
   @Inject
   public FocusVisiblePolyfill(Provider<UserPrefs> pPrefs)
   {
      pPrefs_ = pPrefs;
   }

   /**
    * Load the polyfill
    *
    * @param command An optional callback to invoke when loading is complete
    */
   public void load(final Command command)
   {
      if (pPrefs_.get().showFocusRectangles().getValue())
      {
         focusVisibleCssLoader_.addCallback(() ->
         {
            focusVisibleLoader_.addCallback(() ->
            {
               if (command != null)
                  command.execute();
            });
         });
      }
      else if (command != null)
      {
         command.execute();
      }
   }

   private static final ExternalStyleSheetLoader focusVisibleCssLoader_ =
      new ExternalStyleSheetLoader(FocusVisibleResources.INSTANCE.focusVisibleCss().getSafeUri().asString());

   private static final ExternalJavaScriptLoader focusVisibleLoader_ =
      new ExternalJavaScriptLoader(FocusVisibleResources.INSTANCE.focusVisibleJs().getSafeUri().asString());

   private final Provider<UserPrefs> pPrefs_;
}
