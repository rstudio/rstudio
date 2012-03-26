/*
 * SpellingPrefsContext.java
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
package org.rstudio.studio.client.workbench.prefs.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class SpellingPrefsContext extends JavaScriptObject
{
   public static class SpellingLanguage extends JavaScriptObject
   {
      protected SpellingLanguage()
      {
      }
      
      public native final String getId() /*-{
         return this.id;
      }-*/;
   
      public native final String getName() /*-{
         return this.name;
      }-*/;
   }
   
   protected SpellingPrefsContext() {}

   public native final JsArray<SpellingLanguage> getAvailableLanguages() /*-{
      return this.available_languages;
   }-*/; 
}
