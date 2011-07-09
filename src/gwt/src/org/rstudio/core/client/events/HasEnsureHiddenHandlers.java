package org.rstudio.core.client.events;

import com.google.gwt.event.shared.HandlerRegistration;

public interface HasEnsureHiddenHandlers
{
   HandlerRegistration addEnsureHiddenHandler(EnsureHiddenHandler handler);
}
