/* UserPrefsComputed.java
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
package org.rstudio.studio.client.workbench.prefs.model;

import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.core.client.JsArray;

public class UserPrefsComputed extends UserPrefsAccessor
{
   public UserPrefsComputed(SessionInfo sessionInfo, 
                            JsArray<PrefLayer> prefLayers)
   {
      super(sessionInfo, prefLayers);
   }
   
   public PrefValue<Boolean> haveRsaKey()
   {
      return bool("have_rsa_key", "Has RSA Key", "Whether the user has an RSA key", false);
   }
   
   public PrefValue<SpellingPrefsContext> spellingPrefsContext()
   {
      return object("spelling", "Spelling Prefs", "The context for the user's spelling preferences", null);
   }
}
