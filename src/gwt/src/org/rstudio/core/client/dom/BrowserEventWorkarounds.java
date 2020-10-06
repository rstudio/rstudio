/*
 * BrowserEventWorkarounds.java
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

package org.rstudio.core.client.dom;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BrowserEventWorkarounds
{
   @Inject
   public BrowserEventWorkarounds()
   {
      Event.addNativePreviewHandler(new NativePreviewHandler()
      {
         boolean lastEventWasMouseEvent_;
         boolean isDispatchingSyntheticMouseOverEvent_;

         private Element target_;
         private Element related_;

         int screenX_;
         int screenY_;
         int clientX_;
         int clientY_;

         @Override
         public void onPreviewNativeEvent(NativePreviewEvent preview)
         {
            int eventType = preview.getTypeInt();

            // skip synthetic mouseover events (since this handler will be live
            // even when the mouseover event is later fired)
            if (isDispatchingSyntheticMouseOverEvent_)
               return;

            // if we get a mouseover event targeting something in a GWT menu,
            // save the event target (we'll re-fire it if the user moves the
            // mouse later)
            if (eventType == Event.ONMOUSEOVER)
            {
               // if the last event was a mouse event, we can just
               // allow the mouseover event to dispatch unimpeded
               if (lastEventWasMouseEvent_)
                  return;

               NativeEvent event = preview.getNativeEvent();

               // double-check that this event is occurring within a GWT menu
               Element target = Element.as(event.getEventTarget());
               Element gwtMenuItem = DomUtils.findParentElement(target, (Element parent) -> {
                  return
                        parent.hasClassName("gwt-MenuItem") ||
                        parent.hasClassName("gwt-MenuBar");
               });

               if (gwtMenuItem == null)
                  return;

               // we have a mouseover event occurring within a GWT menu that appears
               // to not be in response to any user mouse gesture: cancel this event,
               // and save it for later dispatch
               event.stopPropagation();
               event.preventDefault();

               // save event target + its attributes
               target_ = Element.as(event.getEventTarget());
               related_ = event.getRelatedEventTarget() == null
                     ? null
                     : Element.as(event.getRelatedEventTarget());

               screenX_ = event.getScreenX();
               screenY_ = event.getScreenY();
               clientX_ = event.getClientX();
               clientY_ = event.getClientY();

               return;
            }

            // track whether the last event was a user mouse event; e.g.
            // something that would only be triggered by the user moving the
            // mouse
            if (eventType != Event.ONMOUSEOVER && eventType != Event.ONMOUSEOUT)
            {
               lastEventWasMouseEvent_ =
                     eventType == Event.ONMOUSEDOWN ||
                     eventType == Event.ONMOUSEMOVE ||
                     eventType == Event.ONMOUSEUP ||
                     eventType == Event.ONMOUSEWHEEL;
            }

            // if we just had a mouse event, check to see if we have a deferred
            // mouseover event ready to fire; if so, fire away
            if (lastEventWasMouseEvent_)
            {
               if (target_ != null)
               {
                  Scheduler.get().scheduleFinally(() -> {

                     NativeEvent overEvent = Document.get().createMouseOverEvent(
                           0,
                           screenX_, screenY_,
                           clientX_, clientY_,
                           false, false, false, false,
                           0, related_);

                     isDispatchingSyntheticMouseOverEvent_ = true;
                     target_.dispatchEvent(overEvent);
                     target_ = null;
                     isDispatchingSyntheticMouseOverEvent_ = false;

                  });
               }
            }

         }
      });
   }
}
