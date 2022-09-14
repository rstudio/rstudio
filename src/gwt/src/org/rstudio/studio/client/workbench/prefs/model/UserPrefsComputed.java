/* UserPrefsComputed.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.prefs.model;

import com.google.gwt.core.client.GWT;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.core.client.JsArray;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;

public class UserPrefsComputed extends UserPrefsAccessor
{
   public UserPrefsComputed(SessionInfo sessionInfo, 
                            JsArray<PrefLayer> prefLayers)
   {
      super(sessionInfo, prefLayers);
   }
   
   public PrefValue<Boolean> haveRsaKey()
   {
      return bool("have_rsa_key", constants_.haveRSAKeyTitle(), constants_.haveRSAKeyDescription(), false);
   }
   
   public PrefValue<String> rsaKeyFile()
   {
      return string("rsa_key_file", constants_.rsaKeyFileTitle(), constants_.rsaKeyFileDescription(), "");
   }

   public PrefValue<SpellingPrefsContext> spellingPrefsContext()
   {
      return object("spelling", constants_.spellingPrefsTitle(), constants_.spellingPrefsDescription(), null);
   }
   private static final PrefsConstants constants_ = GWT.create(PrefsConstants.class);
}
