package org.rstudio.studio.client.workbench.prefs.events;

import com.google.gwt.event.shared.EventHandler;

public interface PreferenceChangedHandler extends EventHandler
{
   void onPreferenceChanged(PreferenceChangedEvent e);
}
