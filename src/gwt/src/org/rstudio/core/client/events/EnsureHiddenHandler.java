package org.rstudio.core.client.events;

import com.google.gwt.event.shared.EventHandler;

public interface EnsureHiddenHandler extends EventHandler
{
   void onEnsureHidden(EnsureHiddenEvent event);
}
