package org.rstudio.studio.client.workbench;

import org.rstudio.core.client.events.HighlightAppCommandEvent;
import org.rstudio.studio.client.RStudioGinjector;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CommandHighlighter implements HighlightAppCommandEvent.Handler
{
   CommandHighlighter()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      events_.addHandler(HighlightAppCommandEvent.TYPE, this);
   }
   
   @Inject
   private void initialize(EventBus events)
   {
      events_ = events;
   }
   
   private EventBus events_;

   @Override
   public void onHighlightAppCommand(HighlightAppCommandEvent event)
   {
      
   }
   
}
