/*
 * PanmirrorStateChangedEvent.java
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

public class PanmirrorStateChangeEvent extends
    GwtEvent<PanmirrorStateChangeEvent.Handler> {

  public static interface Handler extends EventHandler {
    void onPanmirrorStateChange(PanmirrorStateChangeEvent event);
  }

  public interface HasPanmirrorStateChangeHandlers extends HasHandlers {
    HandlerRegistration addPanmirrorStateChangeHandler(Handler handler);
  }


  private static Type<PanmirrorStateChangeEvent.Handler> TYPE;


  public static void fire(HasPanmirrorStateChangeHandlers source) {
    if (TYPE != null) {
      PanmirrorStateChangeEvent event = new PanmirrorStateChangeEvent();
      source.fireEvent(event);
    }
  }

  public static Type<PanmirrorStateChangeEvent.Handler> getType() {
    if (TYPE == null) {
      TYPE = new Type<PanmirrorStateChangeEvent.Handler>();
    }
    return TYPE;
  }

  
  public PanmirrorStateChangeEvent() {}
  
  @Override
  public final Type<PanmirrorStateChangeEvent.Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(PanmirrorStateChangeEvent.Handler handler) {
    handler.onPanmirrorStateChange(new PanmirrorStateChangeEvent());
  }
  
}
