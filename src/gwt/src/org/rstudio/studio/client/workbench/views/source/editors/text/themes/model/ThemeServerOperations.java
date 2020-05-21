/*
 * ThemeServerOperations.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text.themes.model;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceTheme;

public interface ThemeServerOperations
{
   void getThemes(ServerRequestCallback<JsArray<AceTheme>> requestCallback);
   
   void addTheme(ServerRequestCallback<String> request, String themeLocation);
   
   void removeTheme(ServerRequestCallback<Void> request, String themeName);
   
   void getThemeName(ServerRequestCallback<String> request, String themeLocation);
   
   void setComputedThemeColors(String foreground, String background, VoidServerRequestCallback callback);
   
   void getInstalledFonts(ServerRequestCallback<JsArrayString> callback);
}
