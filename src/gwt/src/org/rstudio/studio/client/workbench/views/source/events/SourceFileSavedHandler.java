package org.rstudio.studio.client.workbench.views.source.events;

import com.google.gwt.event.shared.EventHandler;

public interface SourceFileSavedHandler extends EventHandler
{
   public void onSourceFileSaved(SourceFileSavedEvent event);
}
