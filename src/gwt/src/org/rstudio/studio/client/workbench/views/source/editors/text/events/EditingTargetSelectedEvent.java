package org.rstudio.studio.client.workbench.views.source.editors.text.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditingTargetSelectedEvent.Handler;

public class EditingTargetSelectedEvent extends GwtEvent<Handler>
{
   public static final Type<Handler> TYPE = new Type<>();

   public interface Handler extends EventHandler
   {
      void onEditingTargetSelected(EditingTargetSelectedEvent event);
   }

   public EditingTargetSelectedEvent(EditingTarget target)
   {
      target_ = target;
   }

   public EditingTarget getTarget()
   {
      return target_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onEditingTargetSelected(this);
   }

   private final EditingTarget target_;
}
