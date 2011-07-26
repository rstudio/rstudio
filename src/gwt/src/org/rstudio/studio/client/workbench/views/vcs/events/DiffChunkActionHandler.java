package org.rstudio.studio.client.workbench.views.vcs.events;

import com.google.gwt.event.shared.EventHandler;

public interface DiffChunkActionHandler extends EventHandler
{
   void onDiffChunkAction(DiffChunkActionEvent event);
}
