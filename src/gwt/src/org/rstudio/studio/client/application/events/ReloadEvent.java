package org.rstudio.studio.client.application.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ReloadEvent extends GwtEvent<ReloadEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onReload(ReloadEvent event);
   }

   public ReloadEvent()
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
      handler.onReload(this);
   }

   public static final Type<Handler> TYPE = new Type<Handler>();
}
