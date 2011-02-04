package org.rstudio.studio.client.workbench.views.source.editors.text.events;

import com.google.gwt.event.shared.GwtEvent;

public class EditorLoadedEvent extends GwtEvent<EditorLoadedHandler>
{
   public static final Type<EditorLoadedHandler> TYPE = new Type<EditorLoadedHandler>();

   @Override
   public Type<EditorLoadedHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(EditorLoadedHandler handler)
   {
      handler.onEditorLoaded(this);
   }
}
