package org.rstudio.studio.client.workbench.views.source.editors.text.events;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

public interface HasRenderFinishedHandlers extends HasHandlers
{
   HandlerRegistration addRenderFinishedHandler(RenderFinishedEvent.Handler handler);
}
