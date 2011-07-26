package org.rstudio.studio.client.workbench.views.vcs.events;

import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.workbench.views.vcs.diff.Line;
import org.rstudio.studio.client.workbench.views.vcs.events.DiffChunkActionEvent.Action;

public class DiffLineActionEvent extends GwtEvent<DiffLineActionHandler>
{
   public DiffLineActionEvent(Action action, Line line)
   {
      action_ = action;
      line_ = line;
   }

   public Action getAction()
   {
      return action_;
   }

   public Line getLine()
   {
      return line_;
   }

   @Override
   public Type<DiffLineActionHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(DiffLineActionHandler handler)
   {
      handler.onDiffLineAction(this);
   }

   private final Action action_;
   private final Line line_;

   public static final Type<DiffLineActionHandler> TYPE = new Type<DiffLineActionHandler>();
}
