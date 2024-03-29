/*
 * AceMouseMoveEvent.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class AceMouseMoveEvent extends GwtEvent<AceMouseMoveEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onMouseMove(AceMouseMoveEvent event);
   }

   public AceMouseMoveEvent(AceMouseEventNative event)
   {
      event_ = event;
   }

   public void stopPropagation()
   {
      event_.stopPropagation();
   }

   public void preventDefault()
   {
      event_.preventDefault();
   }

   public Position getDocumentPosition()
   {
      return event_.getDocumentPosition();
   }

   public NativeEvent getNativeEvent()
   {
      return event_.getNativeEvent();
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onMouseMove(this);
   }

   private final AceMouseEventNative event_;

   public static final Type<Handler> TYPE = new Type<>();
}
