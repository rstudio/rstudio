/*
 * PackageStatusChangedEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.packages.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.workbench.views.packages.model.PackageStatus;

public class PackageStatusChangedEvent
                     extends GwtEvent<PackageStatusChangedEvent.Handler>
{
   public static final Type<Handler> TYPE = new Type<>();

   public interface Handler extends EventHandler
   {
      void onPackageStatusChanged(PackageStatusChangedEvent event);
   }

   public PackageStatusChangedEvent(PackageStatus packageStatus)
   {
      packageStatus_ = packageStatus;
   }

   public PackageStatus getPackageStatus()
   {
      return packageStatus_;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onPackageStatusChanged(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final PackageStatus packageStatus_;
}
