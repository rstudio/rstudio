package org.rstudio.studio.client.workbench.views.vcs.events;

import com.google.gwt.event.shared.EventHandler;

public interface DiffLineActionHandler extends EventHandler
{
   void onDiffLineAction(DiffLineActionEvent event);
}
