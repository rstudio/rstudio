/*
 * PackageVulnerabilitiesReadyEvent.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

import org.rstudio.studio.client.workbench.views.packages.model.PackageVulnerabilityTypes.RepositoryPackageVulnerabilityListMap;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

// Fired when package vulnerability information has been fetched asynchronously
// from Posit Package Manager (see SessionPPM.cpp). The data is delivered
// separately from the package list so that a slow or unreachable PPM never
// blocks the package list -- or IDE startup.
public class PackageVulnerabilitiesReadyEvent extends
                              GwtEvent<PackageVulnerabilitiesReadyEvent.Handler>
{
   public static final Type<Handler> TYPE = new Type<>();

   public interface Handler extends EventHandler
   {
      void onPackageVulnerabilitiesReady(PackageVulnerabilitiesReadyEvent event);
   }

   public PackageVulnerabilitiesReadyEvent(RepositoryPackageVulnerabilityListMap vulns)
   {
      vulns_ = vulns;
   }

   public RepositoryPackageVulnerabilityListMap getVulnerabilities()
   {
      return vulns_;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onPackageVulnerabilitiesReady(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final RepositoryPackageVulnerabilityListMap vulns_;
}
