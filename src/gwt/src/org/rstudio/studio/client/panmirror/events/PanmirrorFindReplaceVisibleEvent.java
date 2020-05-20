/*
 * PanmirroFindReplaceVisibleEvent.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

/**
 * Represents an find replace visible event
 */
public class PanmirrorFindReplaceVisibleEvent extends
    GwtEvent<PanmirrorFindReplaceVisibleEvent.Handler> {

  /**
   * Handler interface for {@link PanmirrorFindReplaceVisibleEvent} events.
   */
  public static interface Handler extends EventHandler {

    /**
     * Called when a {@link PanmirrorFindReplaceVisibleEvent} is fired.
     *
     * @param event the {@link PanmirrorFindReplaceVisibleEvent} that was fired
     */
    void onPanmirrorFindReplaceVisible(PanmirrorFindReplaceVisibleEvent event);
  }

  /**
   * Interface specifying that a class can add
   * {@code PanmirrorOutlineVisibleEvent.Handler}s.
   */
  public interface HasPanmirrorFindReplaceVisibleHandlers extends HasHandlers {
    /**
     * Adds a {@link PanmirrorFindReplaceVisibleEvent} handler.
     * 
     * @param handler the handler
     * @return {@link HandlerRegistration} used to remove this handler
     */
    HandlerRegistration addPanmirrorFindReplaceVisibleHandler(Handler handler);
  }

  /**
   * Handler type.
   */
  private static Type<PanmirrorFindReplaceVisibleEvent.Handler> TYPE;

  /**
   * Fires an navigation event on all registered handlers in the handler
   * manager. If no such handlers exist, this method will do nothing.
   *
   * @param source the source of the handlers
   */
  public static void fire(HasPanmirrorFindReplaceVisibleHandlers source, boolean visible) {
    if (TYPE != null) {
      PanmirrorFindReplaceVisibleEvent event = new PanmirrorFindReplaceVisibleEvent(visible);
      source.fireEvent(event);
    }
  }

  /**
   * Gets the type associated with this event.
   *
   * @return returns the handler type
   */
  public static Type<PanmirrorFindReplaceVisibleEvent.Handler> getType() {
    if (TYPE == null) {
      TYPE = new Type<PanmirrorFindReplaceVisibleEvent.Handler>();
    }
    return TYPE;
  }

  /**
   * Creates an navigation event.
   */
  PanmirrorFindReplaceVisibleEvent(boolean visible) {
     visible_ = visible;
  }
  
  public Boolean getVisible()
  {
     return visible_;
  }
 
  @Override
  public final Type<PanmirrorFindReplaceVisibleEvent.Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(PanmirrorFindReplaceVisibleEvent.Handler handler) {
    handler.onPanmirrorFindReplaceVisible(this);
  }
  
  private final boolean visible_;
}
