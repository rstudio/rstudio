/*
 * PanmirrorExecuteRmdChunkEvent.java
 *
 * Copyright (C) 2009-20 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.panmirror.events;

import org.rstudio.studio.client.panmirror.PanmirrorRmdChunk;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

/**
 * Represents an outline navigation event
 */
public class PanmirrorExecuteRmdChunkEvent extends
    GwtEvent<PanmirrorExecuteRmdChunkEvent.Handler> {

  /**
   * Handler interface for {@link PanmirrorExecuteRmdChunkEvent} events.
   */
  public static interface Handler extends EventHandler {

    /**
     * Called when a {@link PanmirrorExecuteRmdChunkEvent} is fired.
     *
     * @param event the {@link PanmirrorExecuteRmdChunkEvent} that was fired
     */
    void onPanmirrorExecuteRmdChunk(PanmirrorExecuteRmdChunkEvent event);
  }

  /**
   * Interface specifying that a class can add
   * {@code PanmirrorExecuteRmdChunkEvent.Handler}s.
   */
  public interface HasPanmirrorExecuteRmdChunkHandlers extends HasHandlers {
    /**
     * Adds a {@link PanmirrorExecuteRmdChunkEvent} handler.
     * 
     * @param handler the handler
     * @return {@link HandlerRegistration} used to remove this handler
     */
    HandlerRegistration addPanmirrorExecuteRmdChunkHandler(Handler handler);
  }

  /**
   * Handler type.
   */
  private static Type<PanmirrorExecuteRmdChunkEvent.Handler> TYPE;

  /**
   * Fires an execute chunk event on all registered handlers in the handler
   * manager. If no such handlers exist, this method will do nothing.
   *
   * @param source the source of the handlers
   */
  public static void fire(HasPanmirrorExecuteRmdChunkHandlers source, PanmirrorRmdChunk chunk) {
    if (TYPE != null) {
      PanmirrorExecuteRmdChunkEvent event = new PanmirrorExecuteRmdChunkEvent(chunk);
      source.fireEvent(event);
    }
  }

  /**
   * Gets the type associated with this event.
   *
   * @return returns the handler type
   */
  public static Type<PanmirrorExecuteRmdChunkEvent.Handler> getType() {
    if (TYPE == null) {
      TYPE = new Type<PanmirrorExecuteRmdChunkEvent.Handler>();
    }
    return TYPE;
  }

  /**
   * Creates a chunk execution event
   */
  public PanmirrorExecuteRmdChunkEvent(PanmirrorRmdChunk chunk) {
     chunk_ = chunk;
  }
  
  public PanmirrorRmdChunk getChunk()
  {
     return chunk_;
  }

  @Override
  public final Type<PanmirrorExecuteRmdChunkEvent.Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(PanmirrorExecuteRmdChunkEvent.Handler handler) {
    handler.onPanmirrorExecuteRmdChunk(this);
  }
  
  private final PanmirrorRmdChunk chunk_;
}
