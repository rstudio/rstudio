/*
 * PackagesTab.java
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
package org.rstudio.studio.client.workbench.views.packages;

import com.google.inject.Inject;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;
import org.rstudio.studio.client.workbench.views.packages.events.LoadedPackageUpdatesEvent;
import org.rstudio.studio.client.workbench.views.packages.events.PackageStateChangedEvent;
import org.rstudio.studio.client.workbench.views.packages.events.RaisePackagePaneEvent;

public class PackagesTab extends DelayLoadWorkbenchTab<Packages>
{
   public interface Binder extends CommandBinder<Commands, Shim> {}

   public abstract static class Shim
         extends DelayLoadTabShim<Packages, PackagesTab>
         implements LoadedPackageUpdatesEvent.Handler,
                    RaisePackagePaneEvent.Handler,
                    PackageStateChangedEvent.Handler
   {
      @Handler
      public abstract void onInstallPackage();
      @Handler
      public abstract void onUpdatePackages();
   }

   @Inject
   public PackagesTab(Shim shim,
                      Binder binder,
                      EventBus events,
                      Commands commands,
                      UserPrefs uiPrefs,
                      Session session)
   {
      super(constants_.packagesTitle(), shim);
      binder.bind(commands, shim);
      events.addHandler(LoadedPackageUpdatesEvent.TYPE, shim);
      events.addHandler(RaisePackagePaneEvent.TYPE, shim);
      events.addHandler(PackageStateChangedEvent.TYPE, shim);
      uiPrefs_ = uiPrefs;
      session_ = session;
   }

   @Override
   public boolean isSuppressed()
   {
      return  session_.getSessionInfo().getDisablePackages() ||
              !uiPrefs_.packagesPaneEnabled().getValue();
   }

   private final UserPrefs uiPrefs_;
   private final Session session_;
   private static final PackagesConstants constants_ = com.google.gwt.core.client.GWT.create(PackagesConstants.class);
}
