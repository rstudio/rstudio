/*
 * AceClickEvent.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class AceClickEvent extends GwtEvent<AceClickEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onAceClick(AceClickEvent event);
   }

   public AceClickEvent(AceMouseEventNative event)
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
      handler.onAceClick(this);
   }

   private final AceMouseEventNative event_;

   public static final Type<Handler> TYPE = new Type<>();
}
