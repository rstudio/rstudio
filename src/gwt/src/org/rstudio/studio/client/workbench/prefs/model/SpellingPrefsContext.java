/*
 * SpellingPrefsContext.java
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

import org.rstudio.studio.client.common.spelling.model.SpellingLanguage;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

public class SpellingPrefsContext extends JavaScriptObject
{ 
   protected SpellingPrefsContext() {}

   public native final boolean getAllLanguagesInstalled() /*-{
      return this.all_languages_installed;
   }-*/;
   
   public native final JsArray<SpellingLanguage> getAvailableLanguages() /*-{
      return this.available_languages;
   }-*/; 
   
   public native final JsArrayString getCustomDictionaries() /*-{
      return this.custom_dictionaries;
   }-*/; 
   
}
