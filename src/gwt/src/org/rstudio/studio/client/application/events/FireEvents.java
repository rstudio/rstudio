package org.rstudio.studio.client.application.events;

import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.core.client.dom.WindowEx;

public interface FireEvents
{
   void fireEvent(GwtEvent<?> event);

   void fireEventToAllSatellites(CrossWindowEvent<?> event);

   void fireEventToSatellite(CrossWindowEvent<?> event,
                             WindowEx satelliteWindow);

   void fireEventToMainWindow(CrossWindowEvent<?> event);
}
