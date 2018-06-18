package org.rstudio.studio.client.workbench.views.source.editors.text.themes.model;

import com.google.gwt.core.client.JsArray;

import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceTheme;

public interface ThemeServerOperations
{
   void getThemes(ServerRequestCallback<JsArray<AceTheme>> requestCallback);
}
