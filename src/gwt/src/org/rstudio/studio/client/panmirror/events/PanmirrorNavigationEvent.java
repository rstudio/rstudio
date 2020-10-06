/*
 * PanmirrorNavigationEvent.java
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

import org.rstudio.studio.client.panmirror.PanmirrorNavigation;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

/**
 * Represents an navigation event
 */
public class PanmirrorNavigationEvent extends
    GwtEvent<PanmirrorNavigationEvent.Handler> {

  /**
   * Handler interface for {@link PanmirrorNavigationEvent} events.
   */
  public static interface Handler extends EventHandler {

    /**
     * Called when a {@link PanmirrorNavigationEvent} is fired.
     *
     * @param event the {@link PanmirrorNavigationEvent} that was fired
     */
    void onPanmirrorNavigation(PanmirrorNavigationEvent event);
  }

  /**
   * Interface specifying that a class can add
   * {@code PanmirrorNavigationEvent.Handler}s.
   */
  public interface HasPanmirrorNavigationHandlers extends HasHandlers {
    /**
     * Adds a {@link PanmirrorNavigationEvent} handler.
     * 
     * @param handler the handler
     * @return {@link HandlerRegistration} used to remove this handler
     */
    HandlerRegistration addPanmirrorNavigationHandler(Handler handler);
  }

  /**
   * Handler type.
   */
  private static Type<PanmirrorNavigationEvent.Handler> TYPE;

  /**
   * Fires an navigation event on all registered handlers in the handler
   * manager. If no such handlers exist, this method will do nothing.
   *
   * @param source the source of the handlers
   */
  public static void fire(HasPanmirrorNavigationHandlers source, PanmirrorNavigation navigation) {
    if (TYPE != null) {
      PanmirrorNavigationEvent event = new PanmirrorNavigationEvent(navigation);
      source.fireEvent(event);
    }
  }

  /**
   * Gets the type associated with this event.
   *
   * @return returns the handler type
   */
  public static Type<PanmirrorNavigationEvent.Handler> getType() {
    if (TYPE == null) {
      TYPE = new Type<PanmirrorNavigationEvent.Handler>();
    }
    return TYPE;
  }

  /**
   * Creates an navigation event.
   */
  public PanmirrorNavigationEvent(PanmirrorNavigation navigation) {
     navigation_ = navigation;
  }
  
  public PanmirrorNavigation getNavigation()
  {
     return navigation_;
  }

  @Override
  public final Type<PanmirrorNavigationEvent.Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(PanmirrorNavigationEvent.Handler handler) {
    handler.onPanmirrorNavigation(this);
  }
  
  private final PanmirrorNavigation navigation_;
}
