package org.rstudio.studio.client.workbench.views.source.events;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Fired when an explicit "Save As", or Save of a previously unsaved file,
 * occurs. Does NOT fire when a Save occurs on a previously saved file.
 */
public class SourceFileSavedEvent extends GwtEvent<SourceFileSavedHandler>
{
   public static final Type<SourceFileSavedHandler> TYPE = new Type<SourceFileSavedHandler>();

   public SourceFileSavedEvent(String path)
   {
      path_ = path;
   }

   public String getPath()
   {
      return path_;
   }

   @Override
   public Type<SourceFileSavedHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(SourceFileSavedHandler handler)
   {
      handler.onSourceFileSaved(this);
   }

   private final String path_;
}
