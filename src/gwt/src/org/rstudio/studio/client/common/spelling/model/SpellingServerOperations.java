/*
 * SpellingServerOperations.java
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
package org.rstudio.studio.client.common.spelling.model;

import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.SpellingPrefsContext;

import com.google.gwt.core.client.JsArrayString;

public interface SpellingServerOperations
{
   void checkSpelling(String langId,
                      String word, 
                      ServerRequestCallback<Boolean> requestCallback);
   
   void suggestionList(String langId,
                       String word,
                       ServerRequestCallback<JsArrayString> requestCallback);
   
   void addToDictionary(String langId,
                        String word,
                        ServerRequestCallback<Boolean> requestCallback);
   
   void installAllDictionaries(
           ServerRequestCallback<SpellingPrefsContext> requestCallback);
}
