package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.events.HasEnsureVisibleHandlers;

public abstract class PreferencesPane extends VerticalPanel
   implements HasEnsureVisibleHandlers
{
   public abstract ImageResource getIcon();

   public boolean validate()
   {
      return true;
   }

   public abstract void onApply();
   public abstract String getName();

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleHandler handler)
   {
      return addHandler(handler, EnsureVisibleEvent.TYPE);
   }

   public void registerEnsureVisibleHandler(HasEnsureVisibleHandlers widget)
   {
      widget.addEnsureVisibleHandler(new EnsureVisibleHandler()
      {
         public void onEnsureVisible(EnsureVisibleEvent event)
         {
            fireEvent(new EnsureVisibleEvent());
         }
      });
   }
}
