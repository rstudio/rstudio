/*
 * PanmirrorOutlineNavigationEvent.java
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
 * Represents an outline navigation event
 */
public class PanmirrorOutlineNavigationEvent extends
    GwtEvent<PanmirrorOutlineNavigationEvent.Handler> {

  /**
   * Handler interface for {@link PanmirrorOutlineNavigationEvent} events.
   */
  public static interface Handler extends EventHandler {

    /**
     * Called when a {@link PanmirrorOutlineNavigationEvent} is fired.
     *
     * @param event the {@link PanmirrorOutlineNavigationEvent} that was fired
     */
    void onPanmirrorOutlineNavigation(PanmirrorOutlineNavigationEvent event);
  }

  /**
   * Interface specifying that a class can add
   * {@code PanmirrorOutlineNavigationEvent.Handler}s.
   */
  public interface HasPanmirrorOutlineNavigationHandlers extends HasHandlers {
    /**
     * Adds a {@link PanmirrorOutlineNavigationEvent} handler.
     * 
     * @param handler the handler
     * @return {@link HandlerRegistration} used to remove this handler
     */
    HandlerRegistration addPanmirrorOutlineNavigationHandler(Handler handler);
  }

  /**
   * Handler type.
   */
  private static Type<PanmirrorOutlineNavigationEvent.Handler> TYPE;

  /**
   * Fires an navigation event on all registered handlers in the handler
   * manager. If no such handlers exist, this method will do nothing.
   *
   * @param source the source of the handlers
   */
  public static void fire(HasPanmirrorOutlineNavigationHandlers source, String id) {
    if (TYPE != null) {
      PanmirrorOutlineNavigationEvent event = new PanmirrorOutlineNavigationEvent(id);
      source.fireEvent(event);
    }
  }

  /**
   * Gets the type associated with this event.
   *
   * @return returns the handler type
   */
  public static Type<PanmirrorOutlineNavigationEvent.Handler> getType() {
    if (TYPE == null) {
      TYPE = new Type<PanmirrorOutlineNavigationEvent.Handler>();
    }
    return TYPE;
  }

  /**
   * Creates an navigation event.
   */
  PanmirrorOutlineNavigationEvent(String id) {
     id_ = id;
  }
  
  public String getId()
  {
     return id_;
  }

  @Override
  public final Type<PanmirrorOutlineNavigationEvent.Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(PanmirrorOutlineNavigationEvent.Handler handler) {
    handler.onPanmirrorOutlineNavigation(this);
  }
  
  private final String id_;
}
