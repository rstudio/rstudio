/*
 * PanmirrorUpdatedEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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

public class PanmirrorUpdatedEvent extends
    GwtEvent<PanmirrorUpdatedEvent.Handler> {

  public static interface Handler extends EventHandler {
    void onPanmirrorUpdated(PanmirrorUpdatedEvent event);
  }

  public interface HasPanmirrorUpdatedHandlers extends HasHandlers {
    HandlerRegistration addPanmirrorUpdatedHandler(Handler handler);
  }


  private static Type<PanmirrorUpdatedEvent.Handler> TYPE;


  public static void fire(HasPanmirrorUpdatedHandlers source) {
    if (TYPE != null) {
      PanmirrorUpdatedEvent event = new PanmirrorUpdatedEvent();
      source.fireEvent(event);
    }
  }

  public static Type<PanmirrorUpdatedEvent.Handler> getType() {
    if (TYPE == null) {
      TYPE = new Type<>();
    }
    return TYPE;
  }

  
  public PanmirrorUpdatedEvent() {}
  
  @Override
  public final Type<PanmirrorUpdatedEvent.Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(PanmirrorUpdatedEvent.Handler handler) {
    handler.onPanmirrorUpdated(new PanmirrorUpdatedEvent());
  }
  
}
