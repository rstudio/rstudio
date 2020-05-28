/*
 * PrefsServerOperations.java
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

import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

import com.google.gwt.core.client.JavaScriptObject;

public interface PrefsServerOperations
{
   void setUserPrefs(JavaScriptObject userPrefs,
                     ServerRequestCallback<Void> requestCallback);
   
   void setUserState(JavaScriptObject userState,
                     ServerRequestCallback<Void> requestCallback);
   
   void editPreferences(ServerRequestCallback<Void> requestCallback);

   void clearPreferences(ServerRequestCallback<String> requestCallback);
   
   void viewPreferences(ServerRequestCallback<Void> requestCallback);
}
