/*
 * PanmirrorOutlineChangeEvent.java
 *
 * Copyright (C) 2009-20 by RStudio, Inc.
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
 * Represents a selection change event.
 */
public class PanmirrorOutlineChangeEvent extends
    GwtEvent<PanmirrorOutlineChangeEvent.Handler> {

  /**
   * Handler interface for {@link PanmirrorOutlineChangeEvent} events.
   */
  public static interface Handler extends EventHandler {

    /**
     * Called when a {@link PanmirrorOutlineChangeEvent} is fired.
     *
     * @param event the {@link PanmirrorOutlineChangeEvent} that was fired
     */
    void onPanmirrorOutlineChange(PanmirrorOutlineChangeEvent event);
  }

  /**
   * Interface specifying that a class can add
   * {@code PanmirrorOutlineChangeEvent.Handler}s.
   */
  public interface HasPanmirrorOutlineChangeHandlers extends HasHandlers {
    /**
     * Adds a {@link PanmirrorOutlineChangeEvent} handler.
     * 
     * @param handler the handler
     * @return {@link HandlerRegistration} used to remove this handler
     */
    HandlerRegistration addPanmirrorOutlineChangeHandler(Handler handler);
  }

  /**
   * Handler type.
   */
  private static Type<PanmirrorOutlineChangeEvent.Handler> TYPE;

  /**
   * Fires an outline change event on all registered handlers in the handler
   * manager. If no such handlers exist, this method will do nothing.
   *
   * @param source the source of the handlers
   */
  public static void fire(HasPanmirrorOutlineChangeHandlers source) {
    if (TYPE != null) {
      PanmirrorOutlineChangeEvent event = new PanmirrorOutlineChangeEvent();
      source.fireEvent(event);
    }
  }

  /**
   * Gets the type associated with this event.
   *
   * @return returns the handler type
   */
  public static Type<PanmirrorOutlineChangeEvent.Handler> getType() {
    if (TYPE == null) {
      TYPE = new Type<PanmirrorOutlineChangeEvent.Handler>();
    }
    return TYPE;
  }

  /**
   * Creates an outline change event.
   */
  PanmirrorOutlineChangeEvent() {
  }

  @Override
  public final Type<PanmirrorOutlineChangeEvent.Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(PanmirrorOutlineChangeEvent.Handler handler) {
    handler.onPanmirrorOutlineChange(this);
  }
}
