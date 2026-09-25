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
import org.rstudio.studio.client.application.ApplicationQuit;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.model.RVersionSpec;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.renv.events.ProjectRVersionMismatchEvent;
import org.rstudio.studio.client.renv.events.RInstallCompletedEvent;
import org.rstudio.studio.client.renv.events.RenvRestorePromptEvent;
import org.rstudio.studio.client.renv.model.RVersionInstall;
import org.rstudio.studio.client.renv.model.RenvServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Warns when a project asks for a version of R other than the running one,
 * and offers to install it (via rig) and restart the session with it.
 */
@Singleton
public class RenvRVersionManager
      implements ProjectRVersionMismatchEvent.Handler,
                 RInstallCompletedEvent.Handler,
                 RenvRestorePromptEvent.Handler
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
      events_.addHandler(RenvRestorePromptEvent.TYPE, this);
   }

   // The warning bar renders its message as HTML, and the versions and errors
   // shown in it come from project files and processes, so everything
   // substituted into a message is escaped.
   @Override
   public void onProjectRVersionMismatch(ProjectRVersionMismatchEvent event)
   {
      ProjectRVersionMismatchEvent.Data data = event.getData();
      String requested = escape(data.requested_version);
      String current = escape(data.current_version);

      String message = StringUtil.equals(data.source, "project")
            ? constants_.projectRVersionMismatch(requested, current)
            : constants_.lockfileRVersionMismatch(requested, current);

      if (!data.supported)
      {
         globalDisplay_.showWarningBar(false, message + " " + constants_.rVersionUnsupported(requested));
         return;
      }

      // switching R is only possible on the desktop, where the session can be
      // relaunched with a different R
      if (!Desktop.isDesktop())
      {
         globalDisplay_.showWarningBar(false, message);
         return;
      }

      RVersionInstall installed = data.installed;
      if (installed != null)
      {
         globalDisplay_.showWarningBar(
               false,
               message,
               constants_.switchToRVersion(installed.version),
               () -> switchToR(installed));
      }
      else if (StringUtil.equals(data.installing_version, data.requested_version))
      {
         // this page was loaded while R installs (e.g. after a refresh), so
         // it takes over waiting for the installation to finish
         pendingInstall_ = data.requested_version;
         globalDisplay_.showWarningBar(false, constants_.rInstallInProgress(requested));
      }
      else if (data.can_install)
      {
         globalDisplay_.showWarningBar(
               false,
               message,
               constants_.installRVersion(data.requested_version),
               () -> installR(data.requested_version));
      }
      else
      {
         globalDisplay_.showWarningBar(false, message);
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
         globalDisplay_.showWarningBar(
               false,
               constants_.rInstalled(escape(installed.version)),
               constants_.switchToRVersion(installed.version),
               () -> switchToR(installed));

         switchToR(installed);
      }
      else
      {
         String version = escape(data.version);
         String error = StringUtil.isNullOrEmpty(data.error)
               ? constants_.rInstallFailed(version)
               : constants_.rInstallFailedWithError(version, escape(data.error));
         globalDisplay_.showWarningBar(true, error);
      }
   }

   @Override
   public void onRenvRestorePrompt(RenvRestorePromptEvent event)
   {
      globalDisplay_.showWarningBar(
            false,
            constants_.renvLibraryEmpty(),
            constants_.renvRestoreAction(),
            () ->
            {
               globalDisplay_.hideWarningBar();
               events_.fireEvent(new SendToConsoleEvent("renv::restore()", true));
            });
   }

   private void installR(String version)
   {
      globalDisplay_.hideWarningBar();

      // the job can finish (e.g. fail to start) before the response arrives,
      // so be ready for its completion event before asking
      pendingInstall_ = version;

      server_.rigInstallRVersion(version, new ServerRequestCallback<String>()
      {
         @Override
         public void onResponseReceived(String jobId)
         {
            if (StringUtil.equals(pendingInstall_, version))
               globalDisplay_.showWarningBar(false, constants_.rInstallInProgress(escape(version)));
         }

         @Override
         public void onError(ServerError error)
         {
            pendingInstall_ = null;
            globalDisplay_.showWarningBar(
                  true,
                  constants_.rInstallFailedWithError(escape(version), escape(error.getUserMessage())));
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
   // The warning bar (and its offer) stays up until then, as the user can
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

            globalDisplay_.hideWarningBar();
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
      globalDisplay_.showWarningBar(
            true,
            constants_.rSwitchFailed(escape(installed.version), escape(error)),
            constants_.switchToRVersion(installed.version),
            () -> switchToR(installed));
   }

   private static String escape(String text)
   {
      return SafeHtmlUtils.htmlEscape(StringUtil.notNull(text));
   }

   private String pendingInstall_ = null;

   private final EventBus events_;
   private final GlobalDisplay globalDisplay_;
   private final Session session_;
   private final ApplicationQuit quit_;
   private final RenvServerOperations server_;

   private static final RenvConstants constants_ = GWT.create(RenvConstants.class);
}
