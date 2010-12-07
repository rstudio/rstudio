package org.rstudio.studio.client.common;

import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.widget.GlassPanel;
import org.rstudio.core.client.widget.events.GlassVisibilityEvent;
import org.rstudio.core.client.widget.events.GlassVisibilityHandler;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;

/**
 * Automatically glasses over the child element in response to the global
 * GlassVisibilityEvent event. Use this to prevent splitters from losing their
 * draggability over iframes. 
 */
public class AutoGlassPanel extends GlassPanel
{
   public AutoGlassPanel(Widget child)
   {
      super(child);

      EventBus eventBus = RStudioGinjector.INSTANCE.getEventBus();
      eventBus.addHandler(GlassVisibilityEvent.TYPE, new GlassVisibilityHandler()
      {
         public void onGlass(GlassVisibilityEvent event)
         {
            setGlass(event.isShow());
         }
      });
      setGlass(false);
   }
}
