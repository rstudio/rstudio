/*
 * FocusVisiblePolyfill.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.ui.polyfill;

import com.google.gwt.user.client.Command;
import com.google.inject.Singleton;
import org.rstudio.core.client.ExternalStyleSheetLoader;

@Singleton
public class FocusVisibleStyles
{
   /**
    * Load the focus-visible css
    *
    * @param command An optional callback to invoke when loading is complete
    */
   public void load(final Command command)
   {
      focusVisibleCssLoader_.addCallback(() ->
      {
         if (command != null)
            command.execute();
      });
   }

   private static final ExternalStyleSheetLoader focusVisibleCssLoader_ =
      new ExternalStyleSheetLoader(FocusVisibleResources.INSTANCE.focusVisibleCss().getSafeUri().asString());
}
