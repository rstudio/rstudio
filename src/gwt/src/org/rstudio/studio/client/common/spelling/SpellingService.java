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

import com.google.gwt.core.client.JsArrayString;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SpellingService
{
   @Inject
   public SpellingService(SpellingServerOperations server)
   {
      server_ = server;
   }

   public void checkSpelling(String word, 
                             ServerRequestCallback<Boolean> callback)
   {
      server_.checkSpelling(word, callback);
   }

   public void suggestionList(String word,
                              ServerRequestCallback<JsArrayString> callback)
   {
      server_.suggestionList(word, callback);
   }

   private final SpellingServerOperations server_;
}
