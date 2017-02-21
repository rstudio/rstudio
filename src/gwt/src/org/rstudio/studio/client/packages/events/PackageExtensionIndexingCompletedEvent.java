/*
 * PackageExtensionIndexingCompletedEvent.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.studio.client.packages.events;

import org.rstudio.studio.client.workbench.views.packages.model.PackageProvidedExtensions;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class PackageExtensionIndexingCompletedEvent
      extends GwtEvent<PackageExtensionIndexingCompletedEvent.Handler>
{
   public PackageExtensionIndexingCompletedEvent(PackageProvidedExtensions.Data data)
   {
      data_ = data;
   }
   
   public PackageProvidedExtensions.Data getData()
   {
      return data_;
   }
   
   private final PackageProvidedExtensions.Data data_;
   
   // Boilerplate ----
   
   public interface Handler extends EventHandler
   {
      void onPackageExtensionIndexingCompleted(PackageExtensionIndexingCompletedEvent event);
   }
   
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onPackageExtensionIndexingCompleted(this);
   }

   public static final Type<Handler> TYPE = new Type<Handler>();
}
