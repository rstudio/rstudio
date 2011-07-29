package org.rstudio.studio.client.workbench.views.vcs.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class SwitchViewEvent extends GwtEvent<SwitchViewEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onSwitchView(SwitchViewEvent event);
   }

   public SwitchViewEvent()
   {
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onSwitchView(this);
   }

   private static final Type<Handler> TYPE = new Type<Handler>();
}
