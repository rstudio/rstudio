package org.rstudio.studio.client.workbench.views.source.editors.text.themes.model;

import com.google.gwt.core.client.JsArray;

import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceTheme;

public interface ThemeServerOperations
{
   void getThemes(ServerRequestCallback<JsArray<AceTheme>> requestCallback);
   
   void addTheme(ServerRequestCallback<String> request, String themeLocation);
   
   void removeTheme(ServerRequestCallback<Void> request, String themeName);
}
