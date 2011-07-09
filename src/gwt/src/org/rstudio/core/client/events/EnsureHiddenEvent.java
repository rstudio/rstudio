package org.rstudio.core.client.events;

import com.google.gwt.event.shared.GwtEvent;

public class EnsureHiddenEvent extends GwtEvent<EnsureHiddenHandler>
{
   public EnsureHiddenEvent()
   {
   }

   @Override
   public Type<EnsureHiddenHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(EnsureHiddenHandler handler)
   {
      handler.onEnsureHidden(this);
   }

   public static final Type<EnsureHiddenHandler> TYPE = new Type<EnsureHiddenHandler>();
}
