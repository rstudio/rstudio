/*
 * PackageStateChangedEvent.java
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

import org.rstudio.studio.client.workbench.views.packages.model.PackageState;

import com.google.gwt.event.shared.GwtEvent;

public class PackageStateChangedEvent extends
                              GwtEvent<PackageStateChangedHandler>
{
   public static final GwtEvent.Type<PackageStateChangedHandler> TYPE =
      new GwtEvent.Type<PackageStateChangedHandler>();
   
   public PackageStateChangedEvent(PackageState newState)
   {
      packageState_ = newState;
   }
   
   public PackageState getPackageState()
   {
      return packageState_;
   }
   
   @Override
   protected void dispatch(PackageStateChangedHandler handler)
   {
      handler.onPackageStateChanged(this);
   }

   @Override
   public GwtEvent.Type<PackageStateChangedHandler> getAssociatedType()
   {
      return TYPE;
   }
  
   private PackageState packageState_;
}
