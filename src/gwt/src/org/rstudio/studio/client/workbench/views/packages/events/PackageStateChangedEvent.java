/*
 * PackageStateChangedEvent.java
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
package org.rstudio.studio.client.workbench.views.packages.events;

import com.google.gwt.event.shared.EventHandler;
import org.rstudio.studio.client.workbench.views.packages.model.PackageState;

import com.google.gwt.event.shared.GwtEvent;

public class PackageStateChangedEvent extends
                              GwtEvent<PackageStateChangedEvent.Handler>
{
   public static final Type<Handler> TYPE = new Type<>();

   public interface Handler extends EventHandler
   {
      void onPackageStateChanged(PackageStateChangedEvent event);
   }

   public PackageStateChangedEvent(PackageState newState)
   {
      packageState_ = newState;
   }

   public PackageState getPackageState()
   {
      return packageState_;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onPackageStateChanged(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final PackageState packageState_;
}
