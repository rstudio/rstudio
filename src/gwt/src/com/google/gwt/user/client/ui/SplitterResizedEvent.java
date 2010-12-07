package com.google.gwt.user.client.ui;

import com.google.gwt.event.shared.GwtEvent;

public class SplitterResizedEvent extends GwtEvent<SplitterResizedHandler>
{
   public static final GwtEvent.Type<SplitterResizedHandler> TYPE =
      new GwtEvent.Type<SplitterResizedHandler>();
    
   @Override
   protected void dispatch(SplitterResizedHandler handler)
   {
      handler.onSplitterResized(this);
   }

   @Override
   public GwtEvent.Type<SplitterResizedHandler> getAssociatedType()
   {
      return TYPE;
   }
   
  
}

