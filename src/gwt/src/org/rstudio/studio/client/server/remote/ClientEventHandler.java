package org.rstudio.studio.client.server.remote;

import com.google.gwt.core.client.JavaScriptObject;

public interface ClientEventHandler
{
   void onClientEvent(JavaScriptObject clientEvent);
}
