package org.rstudio.studio.client.workbench.views.vcs.events;

import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.workbench.views.vcs.diff.DiffChunk;

public class DiffChunkActionEvent extends GwtEvent<DiffChunkActionHandler>
{
   public enum Action
   {
      Stage,
      Unstage,
      Discard
   }

   public DiffChunkActionEvent(Action action, DiffChunk diffChunk)
   {
      action_ = action;
      diffChunk_ = diffChunk;
   }

   public Action getAction()
   {
      return action_;
   }

   public DiffChunk getDiffChunk()
   {
      return diffChunk_;
   }

   @Override
   public Type<DiffChunkActionHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(DiffChunkActionHandler handler)
   {
      handler.onDiffChunkAction(this);
   }

   private Action action_;
   private DiffChunk diffChunk_;

   public static final Type<DiffChunkActionHandler> TYPE = new Type<DiffChunkActionHandler>();
}
