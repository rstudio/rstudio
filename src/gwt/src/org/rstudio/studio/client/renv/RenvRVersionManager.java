/*
 * RenvRVersionManager.java
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
package org.rstudio.studio.client.renv;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.ApplicationQuit;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.model.RVersionSpec;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.renv.events.ProjectRVersionMismatchEvent;
import org.rstudio.studio.client.renv.events.RInstallCompletedEvent;
import org.rstudio.studio.client.renv.events.RenvConsoleNoticeEvent;
import org.rstudio.studio.client.renv.model.RVersionInstall;
import org.rstudio.studio.client.renv.model.RenvServerOperations;
import org.rstudio.studio.client.renv.ui.InstallRDialog;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Warns when a project asks for a version of R other than the running one,
 * and offers to install it (via rig) and restart the session with it.
 */
@Singleton
public class RenvRVersionManager
      implements ProjectRVersionMismatchEvent.Handler,
                 RInstallCompletedEvent.Handler
{
   @Inject
   public RenvRVersionManager(EventBus events,
                              GlobalDisplay globalDisplay,
                              Session session,
                              ApplicationQuit quit,
                              RenvServerOperations server)
   {
      events_ = events;
      globalDisplay_ = globalDisplay;
      session_ = session;
      quit_ = quit;
      server_ = server;

      events_.addHandler(ProjectRVersionMismatchEvent.TYPE, this);
      events_.addHandler(RInstallCompletedEvent.TYPE, this);
   }

   @Override
   public void onProjectRVersionMismatch(ProjectRVersionMismatchEvent event)
   {
      ProjectRVersionMismatchEvent.Data data = event.getData();
      String requested = StringUtil.notNull(data.requested_version);
      String current = StringUtil.notNull(data.current_version);

      String message = StringUtil.equals(data.source, "project")
            ? constants_.projectRVersionMismatch(requested, current)
            : constants_.lockfileRVersionMismatch(requested, current);

      if (!data.supported)
      {
         showNotice(false, message + " " + constants_.rVersionUnsupported(requested));
         return;
      }

      // switching R is only possible on the desktop, where the session can be
      // relaunched with a different R
      if (!Desktop.isDesktop())
      {
         showNotice(false, message);
         return;
      }

      RVersionInstall installed = data.installed;
      if (StringUtil.equals(data.installing_version, data.requested_version))
      {
         // this page was loaded while R installs (e.g. after a refresh), so
         // it takes over waiting for the installation to finish
         pendingInstall_ = data.requested_version;
         showNotice(false, constants_.rInstallInProgress(requested));
      }
      else if (data.can_install &&
               (installed == null ||
                (!StringUtil.equals(installed.version, requested) &&
                 !installed.version.startsWith(requested + "."))))
      {
         // A nearby patch release can be used immediately, but also offer
         // the exact requested version. A major.minor request is already
         // satisfied by any installed patch release in that series.
         showNotice(
               false,
               message,
               constants_.installRVersion(requested),
               () -> installR(requested),
               installed == null ? null : constants_.switchToRVersion(installed.version),
               installed == null ? null : () -> switchToR(installed));
      }
      else if (installed != null)
      {
         showNotice(
               false,
               message,
               constants_.switchToRVersion(installed.version),
               () -> switchToR(installed));
      }
      else
      {
         showNotice(false, message);
      }
   }

   @Override
   public void onRInstallCompleted(RInstallCompletedEvent event)
   {
      RInstallCompletedEvent.Data data = event.getData();
      if (!StringUtil.equals(data.version, pendingInstall_))
         return;

      pendingInstall_ = null;

      if (data.success && data.installed != null)
      {
         // the offer stays up should the switch not go ahead
         RVersionInstall installed = data.installed;
         showNotice(
               false,
               constants_.rInstalled(StringUtil.notNull(installed.version)),
               constants_.switchToRVersion(installed.version),
               () -> switchToR(installed));

         switchToR(installed);
      }
      else
      {
         String version = StringUtil.notNull(data.version);
         String error = StringUtil.isNullOrEmpty(data.error)
               ? constants_.rInstallFailed(version)
               : constants_.rInstallFailedWithError(version, data.error);
         showNotice(true, error);
      }
   }

   private void installR(String version)
   {
      if (installDialogPending_)
         return;

      installDialogPending_ = true;
      server_.rigGetInstallDir(new ServerRequestCallback<String>()
      {
         @Override
         public void onResponseReceived(String directory)
         {
            installDialogPending_ = false;
            new InstallRDialog(version, directory,
                  (path, indicator) -> installR(version, path, indicator)).showModal();
         }

         @Override
         public void onError(ServerError error)
         {
            installDialogPending_ = false;
            globalDisplay_.showErrorMessage(constants_.installRCaption(version), error.getUserMessage());
         }
      });
   }

   private void installR(String version, String directory, ProgressIndicator indicator)
   {
      indicator.onProgress(constants_.rInstallStarting());

      // the job can finish (e.g. fail to start) before the response arrives,
      // so be ready for its completion event before asking
      pendingInstall_ = version;

      server_.rigInstallRVersion(version, directory, new ServerRequestCallback<String>()
      {
         @Override
         public void onResponseReceived(String jobId)
         {
            indicator.onCompleted();
            if (StringUtil.equals(pendingInstall_, version))
               showNotice(false, constants_.rInstallInProgress(StringUtil.notNull(version)));
         }

         @Override
         public void onError(ServerError error)
         {
            pendingInstall_ = null;
            indicator.onError(error.getUserMessage());
         }
      });
   }

   // Restart the session with the given R. A macOS framework version that
   // would run the default version of R instead of itself is updated first,
   // with the user's consent.
   private void switchToR(RVersionInstall installed)
   {
      if (installed.orthogonal)
      {
         restartWithR(installed);
         return;
      }

      globalDisplay_.showYesNoMessage(
            GlobalDisplay.MSG_QUESTION,
            constants_.updateRCaption(),
            constants_.updateRMessage(installed.version),
            () ->
            {
               Desktop.getFrame().makeROrthogonal(installed.home, error ->
               {
                  if (StringUtil.isNullOrEmpty(error))
                     restartWithR(installed);
                  else
                     onSwitchFailed(installed, error);
               });
            },
            true);
   }

   // Once the quit is going ahead, the desktop checks that the R runs and
   // holds on to it for the relaunch; a quit that then fails clears it again.
   // The Console notice (and its offer) stays up until then, as the user can
   // still back out of the quit.
   private void restartWithR(RVersionInstall installed)
   {
      String projectFile = session_.getSessionInfo().getActiveProjectFile();
      RVersionSpec spec = RVersionSpec.create(installed.version, installed.home, "");

      quit_.prepareForQuit(constants_.switchRVersionCaption(), saveChanges ->
      {
         Desktop.getFrame().setPendingRVersion(installed.binary, error ->
         {
            if (!StringUtil.isNullOrEmpty(error))
            {
               onSwitchFailed(installed, error);
               return;
            }

            hideNotice();
            quit_.performQuit(
                  null,
                  constants_.switchingRVersionProgress(installed.version),
                  saveChanges,
                  projectFile,
                  spec);
         });
      });
   }

   private void onSwitchFailed(RVersionInstall installed, String error)
   {
      showNotice(
            true,
            constants_.rSwitchFailed(StringUtil.notNull(installed.version), StringUtil.notNull(error)),
            constants_.switchToRVersion(installed.version),
            () -> switchToR(installed));
   }

   private void showNotice(boolean severe, String message)
   {
      showNotice(severe, message, null, null);
   }

   private void showNotice(boolean severe, String message, String actionLabel, Command action)
   {
      showNotice(severe, message, actionLabel, action, null, null);
   }

   private void showNotice(boolean severe, String message, String actionLabel, Command action,
                           String secondaryActionLabel, Command secondaryAction)
   {
      // The notice renders the message and action labels as plain text.
      events_.fireEvent(new RenvConsoleNoticeEvent(severe, message, actionLabel, action,
                                                  secondaryActionLabel, secondaryAction));
   }

   private void hideNotice()
   {
      events_.fireEvent(new RenvConsoleNoticeEvent(false, null, null, null));
   }

   private String pendingInstall_ = null;
   private boolean installDialogPending_ = false;

   private final EventBus events_;
   private final GlobalDisplay globalDisplay_;
   private final Session session_;
   private final ApplicationQuit quit_;
   private final RenvServerOperations server_;

   private static final RenvConstants constants_ = GWT.create(RenvConstants.class);
}
