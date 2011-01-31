package org.rstudio.studio.client.application.events;

import com.google.gwt.event.shared.EventHandler;

public interface ChangeFontSizeHandler extends EventHandler
{
   void onChangeFontSize(ChangeFontSizeEvent event);
}
