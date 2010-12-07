package com.google.gwt.user.client.ui;

import com.google.gwt.event.shared.GwtEvent;

public class SplitterBeforeResizeEvent extends GwtEvent<SplitterBeforeResizeHandler>
{
   public static final GwtEvent.Type<SplitterBeforeResizeHandler> TYPE =
      new GwtEvent.Type<SplitterBeforeResizeHandler>();

   @Override
   protected void dispatch(SplitterBeforeResizeHandler handler)
   {
      handler.onSplitterBeforeResize(this);
   }

   @Override
   public GwtEvent.Type<SplitterBeforeResizeHandler> getAssociatedType()
   {
      return TYPE;
   }


}

