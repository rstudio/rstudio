/*
 * PanmirrorOutlinePrefsEvent.java
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

package org.rstudio.studio.client.panmirror.outline;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

/**
 * Represents a selection change event.
 */
public class PanmirrorOutlinePrefsEvent extends
    GwtEvent<PanmirrorOutlinePrefsEvent.Handler> {

  /**
   * Handler interface for {@link PanmirrorOutlinePrefsEvent} events.
   */
  public static interface Handler extends EventHandler {

    /**
     * Called when a {@link PanmirrorOutlinePrefsEvent} is fired.
     *
     * @param event the {@link PanmirrorOutlinePrefsEvent} that was fired
     */
    void onPanmirrorOutlinePrefs(PanmirrorOutlinePrefsEvent event);
  }

  /**
   * Interface specifying that a class can add
   * {@code PanmirrorOutlinePrefsEvent.Handler}s.
   */
  public interface HasPanmirrorOutlinePrefsHandlers extends HasHandlers {
    /**
     * Adds a {@link PanmirrorOutlinePrefsEvent} handler.
     * 
     * @param handler the handler
     * @return {@link HandlerRegistration} used to remove this handler
     */
    HandlerRegistration addPanmirrorOutlinePrefsHandler(Handler handler);
  }

  /**
   * Handler type.
   */
  private static Type<PanmirrorOutlinePrefsEvent.Handler> TYPE;

  /**
   * Fires an navigation event on all registered handlers in the handler
   * manager. If no such handlers exist, this method will do nothing.
   *
   * @param source the source of the handlers
   */
  public static void fire(HasPanmirrorOutlinePrefsHandlers source, boolean visible, double width) {
    if (TYPE != null) {
      PanmirrorOutlinePrefsEvent event = new PanmirrorOutlinePrefsEvent(visible, width);
      source.fireEvent(event);
    }
  }

  /**
   * Gets the type associated with this event.
   *
   * @return returns the handler type
   */
  public static Type<PanmirrorOutlinePrefsEvent.Handler> getType() {
    if (TYPE == null) {
      TYPE = new Type<PanmirrorOutlinePrefsEvent.Handler>();
    }
    return TYPE;
  }

  /**
   * Creates an navigation event.
   */
  PanmirrorOutlinePrefsEvent(boolean visible, double width) {
     visible_ = visible;
     width_ = width;
  }
  
  public boolean getVisible()
  {
     return visible_;
  }
  
  public double geWidth()
  {
     return width_;
  }

  @Override
  public final Type<PanmirrorOutlinePrefsEvent.Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(PanmirrorOutlinePrefsEvent.Handler handler) {
    handler.onPanmirrorOutlinePrefs(this);
  }
  
  private final boolean visible_;
  private final double width_;
}
