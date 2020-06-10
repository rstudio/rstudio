/*
 * PanmirrorOutlineWidthEvent.java
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
 * Represents a outline width event
 */
public class PanmirrorOutlineWidthEvent extends
    GwtEvent<PanmirrorOutlineWidthEvent.Handler> {

  /**
   * Handler interface for {@link PanmirrorOutlineWidthEvent} events.
   */
  public static interface Handler extends EventHandler {

    /**
     * Called when a {@link PanmirrorOutlineWidthEvent} is fired.
     *
     * @param event the {@link PanmirrorOutlineWidthEvent} that was fired
     */
    void onPanmirrorOutlineWidth(PanmirrorOutlineWidthEvent event);
  }

  /**
   * Interface specifying that a class can add
   * {@code PanmirrorOutlineWidthEvent.Handler}s.
   */
  public interface HasPanmirrorOutlineWidthHandlers extends HasHandlers {
    /**
     * Adds a {@link PanmirrorOutlineWidthEvent} handler.
     * 
     * @param handler the handler
     * @return {@link HandlerRegistration} used to remove this handler
     */
    HandlerRegistration addPanmirrorOutlineWidthHandler(Handler handler);
  }

  /**
   * Handler type.
   */
  private static Type<PanmirrorOutlineWidthEvent.Handler> TYPE;

  /**
   * Fires an navigation event on all registered handlers in the handler
   * manager. If no such handlers exist, this method will do nothing.
   *
   * @param source the source of the handlers
   */
  public static void fire(HasPanmirrorOutlineWidthHandlers source, double width) {
    if (TYPE != null) {
      PanmirrorOutlineWidthEvent event = new PanmirrorOutlineWidthEvent(width);
      source.fireEvent(event);
    }
  }

  /**
   * Gets the type associated with this event.
   *
   * @return returns the handler type
   */
  public static Type<PanmirrorOutlineWidthEvent.Handler> getType() {
    if (TYPE == null) {
      TYPE = new Type<PanmirrorOutlineWidthEvent.Handler>();
    }
    return TYPE;
  }

  /**
   * Creates an navigation event.
   */
  PanmirrorOutlineWidthEvent(double width) {
     
     width_ = width;
  }
  
  
  
  public double getWidth()
  {
     return width_;
  }

  @Override
  public final Type<PanmirrorOutlineWidthEvent.Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(PanmirrorOutlineWidthEvent.Handler handler) {
    handler.onPanmirrorOutlineWidth(this);
  }
  
  private final double width_;
}
