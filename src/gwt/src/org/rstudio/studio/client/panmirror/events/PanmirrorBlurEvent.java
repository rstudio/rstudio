/*
 * PanmirrorBlurEvent.java
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

public class PanmirrorBlurEvent extends
    GwtEvent<PanmirrorBlurEvent.Handler> {

  public static interface Handler extends EventHandler {
    void onPanmirrorBlur(PanmirrorBlurEvent event);
  }

  public interface HasPanmirrorBlurHandlers extends HasHandlers {
    HandlerRegistration addPanmirrorBlurHandler(Handler handler);
  }


  private static Type<PanmirrorBlurEvent.Handler> TYPE;


  public static void fire(HasPanmirrorBlurHandlers source) {
    if (TYPE != null) {
      PanmirrorBlurEvent event = new PanmirrorBlurEvent();
      source.fireEvent(event);
    }
  }

  public static Type<PanmirrorBlurEvent.Handler> getType() {
    if (TYPE == null) {
      TYPE = new Type<>();
    }
    return TYPE;
  }

  
  public PanmirrorBlurEvent() {}
  
  @Override
  public final Type<PanmirrorBlurEvent.Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(PanmirrorBlurEvent.Handler handler) {
    handler.onPanmirrorBlur(new PanmirrorBlurEvent());
  }
  
}
