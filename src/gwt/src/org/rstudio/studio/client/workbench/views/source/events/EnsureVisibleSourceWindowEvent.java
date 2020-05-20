/*
 * EnsureVisibleSourceWindowEvent.java
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

package org.rstudio.studio.client.workbench.views.source.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class EnsureVisibleSourceWindowEvent extends GwtEvent<EnsureVisibleSourceWindowEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onEnsureVisibleSourceWindow(EnsureVisibleSourceWindowEvent e);
   }
   
   public static final GwtEvent.Type<EnsureVisibleSourceWindowEvent.Handler> TYPE =
      new GwtEvent.Type<EnsureVisibleSourceWindowEvent.Handler>();
   
   public EnsureVisibleSourceWindowEvent()
   {
   }
   
   @Override
   protected void dispatch(EnsureVisibleSourceWindowEvent.Handler handler)
   {
      handler.onEnsureVisibleSourceWindow(this);
   }

   @Override
   public GwtEvent.Type<EnsureVisibleSourceWindowEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
}
