/*
 * MaximizeSourceWindowEvent.java
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

package org.rstudio.studio.client.workbench.views.source.events;


import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class MaximizeSourceWindowEvent extends GwtEvent<MaximizeSourceWindowEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onMaximizeSourceWindow(MaximizeSourceWindowEvent e);
   }
   
   
   public static final GwtEvent.Type<MaximizeSourceWindowEvent.Handler> TYPE = new GwtEvent.Type<>();
   
   public MaximizeSourceWindowEvent()
   {
   }
   
   @Override
   protected void dispatch(MaximizeSourceWindowEvent.Handler handler)
   {
      handler.onMaximizeSourceWindow(this);
   }

   @Override
   public GwtEvent.Type<MaximizeSourceWindowEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
}
