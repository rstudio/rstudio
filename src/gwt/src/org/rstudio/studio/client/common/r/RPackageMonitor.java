/*
 * RPackageMonitor.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.common.r;

import java.util.HashSet;
import java.util.Set;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.debugging.events.PackageLoadedEvent;
import org.rstudio.studio.client.common.debugging.events.PackageUnloadedEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class RPackageMonitor implements PackageLoadedEvent.Handler, PackageUnloadedEvent.Handler
{
   @Inject
   public RPackageMonitor(EventBus events)
   {
      events_ = events;
      loadedPackages_ = new HashSet<String>();
      
      events_.addHandler(PackageLoadedEvent.TYPE, this);
      events_.addHandler(PackageUnloadedEvent.TYPE, this);
   }
   
   public boolean isPackageLoaded(String pkg)
   {
      return loadedPackages_.contains(pkg);
   }
   
   @Override
   public void onPackageUnloaded(PackageUnloadedEvent event)
   {
      loadedPackages_.remove(event.getPackageName());
   }

   @Override
   public void onPackageLoaded(PackageLoadedEvent event)
   {
      loadedPackages_.add(event.getPackageName());
   }
   
   private final EventBus events_;
   private final Set<String> loadedPackages_;
}
