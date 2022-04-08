/*
 * LocalStoragePrefs.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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

import com.google.gwt.storage.client.Storage;
import com.google.gwt.storage.client.StorageMap;
import org.rstudio.core.client.Debug;

/**
 * Manage preferences stored in HTML5 local storage.
 */
public class LocalStoragePrefs
{
   public LocalStoragePrefs()
   {
      if (haveLocalStorage())
         storageMap_ = new StorageMap(localStorage_);
      else
         Debug.logWarning("Unable to use local storage");
   }

   /**
    * @return Whether browser supports local storage
    */
   public boolean haveLocalStorage()
   {
      return localStorage_ != null;
   }

   /**
    * @return Current UI language setting, or empty string if none set
    */
   public String getUiLanguage()
   {
      if (storageMap_ != null && storageMap_.containsKey(UI_LANG_PREF))
      {
         return localStorage_.getItem(UI_LANG_PREF);
      }
      return "";
   }

   public void setUiLanguage(String uiLanguage)
   {
      if (haveLocalStorage())
      {
         localStorage_.setItem(UI_LANG_PREF, uiLanguage);
      }
   }

   private final Storage localStorage_ = Storage.getLocalStorageIfSupported();
   private StorageMap storageMap_ = null;
   public static final String UI_LANG_PREF = "uilang";
}
