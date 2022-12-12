package org.rstudio.studio.client.application.events;

import org.rstudio.core.client.dom.WindowEx;

import com.google.gwt.event.shared.GwtEvent;

public class DummyFireEvents implements FireEvents
{

    @Override
    public void fireEvent(GwtEvent<?> event) {}

    @Override
    public void fireEventToAllSatellites(CrossWindowEvent<?> event) {}

    @Override
    public void fireEventToSatellite(CrossWindowEvent<?> event, WindowEx satelliteWindow) {}

    @Override
    public void fireEventToMainWindow(CrossWindowEvent<?> event) {}

}
