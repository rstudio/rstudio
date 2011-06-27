package org.rstudio.studio.client.workbench.prefs.model;

import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

import com.google.gwt.core.client.JavaScriptObject;

public interface PrefsServerOperations
{
   void setPrefs(RPrefs rPrefs,
                 JavaScriptObject uiPrefs,
                 ServerRequestCallback<Void> requestCallback);

   void setUiPrefs(JavaScriptObject uiPrefs,
                   ServerRequestCallback<Void> requestCallback);
}
