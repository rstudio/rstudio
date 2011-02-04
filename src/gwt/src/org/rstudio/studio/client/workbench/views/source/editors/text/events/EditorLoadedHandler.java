package org.rstudio.studio.client.workbench.views.source.editors.text.events;

import com.google.gwt.event.shared.EventHandler;

public interface EditorLoadedHandler extends EventHandler
{
   void onEditorLoaded(EditorLoadedEvent event);
}
