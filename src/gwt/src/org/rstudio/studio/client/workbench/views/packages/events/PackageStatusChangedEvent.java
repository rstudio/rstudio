/*
 * PackageStatusChangedEvent.java
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
package org.rstudio.studio.client.workbench.views.packages.events;

import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.workbench.views.packages.model.PackageStatus;

public class PackageStatusChangedEvent 
                     extends GwtEvent<PackageStatusChangedHandler>
{
   public static final GwtEvent.Type<PackageStatusChangedHandler> TYPE =
      new GwtEvent.Type<PackageStatusChangedHandler>();
   
   public PackageStatusChangedEvent(PackageStatus packageStatus)
   {
      packageStatus_ = packageStatus;
   }
   
   public PackageStatus getPackageStatus()
   {
      return packageStatus_;
   }
   
   @Override
   protected void dispatch(PackageStatusChangedHandler handler)
   {
      handler.onPackageStatusChanged(this);
   }

   @Override
   public GwtEvent.Type<PackageStatusChangedHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private PackageStatus packageStatus_;
}
