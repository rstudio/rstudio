/*
 * SpellingService.java
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

package org.rstudio.studio.client.common.spelling;

import org.rstudio.studio.client.common.spelling.model.SpellingServerOperations;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import com.google.gwt.core.client.JsArrayString;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SpellingService
{
   @Inject
   public SpellingService(SpellingServerOperations server,
                          UIPrefs uiPrefs)
   {
      server_ = server;
      uiPrefs_ = uiPrefs;
   }

   public void checkSpelling(String docId,
                             String word, 
                             ServerRequestCallback<Boolean> callback)
   {
      server_.checkSpelling(getLangId(), word, callback);
   }

   public void suggestionList(String docId,
                              String word,
                              ServerRequestCallback<JsArrayString> callback)
   {
      server_.suggestionList(getLangId(), word, callback);
   }

   public void addToDictionary(String docId,
                               String word,
                               ServerRequestCallback<Boolean> callback)
   {
      server_.addToDictionary(getLangId(), word, callback);
   }
   
   private String getLangId()
   {
      return uiPrefs_.spellingDictionaryLanguage().getValue();
   }
   
   private final UIPrefs uiPrefs_;
   private final SpellingServerOperations server_;
}
