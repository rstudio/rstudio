/*
 * SpellingServerOperations.java
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
package org.rstudio.studio.client.common.spelling.model;

import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.SpellingPrefsContext;

import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayString;

public interface SpellingServerOperations
{
   // check the specified array of words, returning an array of integer
   // indexes for mis-spelled words
   void checkSpelling(JsArrayString words, 
                      ServerRequestCallback<JsArrayInteger> requestCallback);
   
   void suggestionList(String word,
                       ServerRequestCallback<JsArrayString> requestCallback);
   
   void getWordChars(ServerRequestCallback<String> requestCallback);

   void addCustomDictionary(String dictPath,
                            ServerRequestCallback<JsArrayString> callback);
   
   void removeCustomDictionary(String name,
                               ServerRequestCallback<JsArrayString> callback);

   void installAllDictionaries(
           ServerRequestCallback<SpellingPrefsContext> requestCallback);
}
