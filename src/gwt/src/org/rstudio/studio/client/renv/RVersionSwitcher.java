/*
 * RVersionSwitcher.java
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

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.ApplicationQuit;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.model.RVersionSpec;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.projects.Projects;
import org.rstudio.studio.client.renv.model.RVersionInstall;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Restarts the R session with another installation of R (on the desktop,
 * where the session can be relaunched with a different R). Once a session
 * has started with it, the desktop keeps that R for later launches too.
 */
@Singleton
public class RVersionSwitcher
{
   public interface Observer
   {
      // the quit for the switch is going ahead
      void onSwitchStarted();

      // the switch can't go ahead, and the session keeps its R
      void onSwitchFailed(String error);
   }

   interface Binder extends CommandBinder<Commands, RVersionSwitcher> {}

   @Inject
   public RVersionSwitcher(GlobalDisplay globalDisplay,
                           Session session,
                           ApplicationQuit quit,
                           RVersionChooser chooser,
                           EventBus events,
                           Commands commands,
                           Binder binder)
   {
      globalDisplay_ = globalDisplay;
      session_ = session;
      quit_ = quit;
      chooser_ = chooser;

      binder.bind(commands, this);

      // only a desktop session runs the R the desktop launches it with
      events.addHandler(SessionInitEvent.TYPE, event ->
      {
         commands.switchRVersion().setVisible(isLocalDesktopSession(session));
      });
   }

   /** Whether the session is one the desktop launched, and can switch R. */
   public static boolean isLocalDesktopSession(Session session)
   {
      return Desktop.isDesktop() &&
             session.getSessionInfo() != null &&
             StringUtil.equals(session.getSessionInfo().getMode(), SessionInfo.DESKTOP_MODE);
   }

   @Handler
   void onSwitchRVersion()
   {
      chooser_.choose(
            constants_.switchRVersionCaption(),
            constants_.switchRVersionButton(),
            null,
            choice ->
            {
               // the R in use needs no restart, only keeping from now on
               if (choice.installation.current)
               {
                  Desktop.getFrame().setDefaultRVersion(choice.installation.binary, error ->
                  {
                     if (!StringUtil.isNullOrEmpty(error))
                        showSwitchFailed(choice, error);
                  });
                  return;
               }

               switchTo(choice.installation, new Observer()
               {
                  @Override
                  public void onSwitchStarted()
                  {
                  }

                  @Override
                  public void onSwitchFailed(String error)
                  {
                     showSwitchFailed(choice, error);
                  }
               });
            });
   }

   /**
    * Offer to restart the session with the given R, which RStudio was just
    * told to use from now on.
    */
   public void offerRestartWith(RVersionChooser.Choice choice)
   {
      globalDisplay_.showYesNoMessage(
            GlobalDisplay.MSG_QUESTION,
            constants_.restartWithChosenRCaption(),
            constants_.restartWithChosenRMessage(RVersionChooser.describe(choice, false)),
            () -> switchTo(choice.installation, new Observer()
            {
               @Override
               public void onSwitchStarted()
               {
               }

               @Override
               public void onSwitchFailed(String error)
               {
                  showSwitchFailed(choice, error);
               }
            }),
            true);
   }

   private void showSwitchFailed(RVersionChooser.Choice choice, String error)
   {
      globalDisplay_.showErrorMessage(
            constants_.switchRVersionCaption(),
            constants_.rSwitchFailed(StringUtil.notNull(choice.installation.version), StringUtil.notNull(error)));
   }

   /**
    * Restart the session with the given R. A macOS framework version that
    * would run the default version of R instead of itself is updated first,
    * with the user's consent; declining leaves everything as it was.
    */
   public void switchTo(RVersionInstall installed, Observer observer)
   {
      makeOrthogonal(installed, () -> restartWithR(installed, observer), observer::onSwitchFailed);
   }

   /**
    * Make sure the given R runs as itself, then run `onReady`. A macOS
    * framework version that would run the default version of R instead is
    * updated first, with the user's consent; `onFailed` receives the error
    * when updating fails, and neither runs when the user declines.
    */
   public void makeOrthogonal(RVersionInstall installed,
                              Command onReady,
                              CommandWithArg<String> onFailed)
   {
      makeOrthogonal(installed, onReady, onFailed, () -> {});
   }

   /** As above, running `onDeclined` when the user declines the update. */
   public void makeOrthogonal(RVersionInstall installed,
                              Command onReady,
                              CommandWithArg<String> onFailed,
                              Command onDeclined)
   {
      if (installed.orthogonal)
      {
         onReady.execute();
         return;
      }

      globalDisplay_.showYesNoMessage(
            GlobalDisplay.MSG_QUESTION,
            constants_.updateRCaption(),
            constants_.updateRMessage(installed.version),
            false,
            () ->
            {
               Desktop.getFrame().makeROrthogonal(installed.home, error ->
               {
                  if (StringUtil.isNullOrEmpty(error))
                  {
                     // a switch to it next (Global Options offers one) needn't ask again
                     installed.orthogonal = true;
                     onReady.execute();
                  }
                  else
                  {
                     onFailed.execute(error);
                  }
               });
            },
            () -> onDeclined.execute(),
            true);
   }

   // Once the quit is going ahead, the desktop checks that the R runs and
   // holds on to it for the relaunch; a quit that then fails clears it again.
   private void restartWithR(RVersionInstall installed, Observer observer)
   {
      // the quit restarts the session (rather than exiting RStudio) only
      // when it names a project to switch to, which may be none
      String activeProject = session_.getSessionInfo().getActiveProjectFile();
      String projectFile = activeProject == null ? Projects.NONE : activeProject;

      RVersionSpec spec = RVersionSpec.create(installed.version, installed.home, "");

      quit_.prepareForQuit(constants_.switchRVersionCaption(), saveChanges ->
      {
         // claim the quit before the desktop holds the R: held for a quit
         // that cannot start, it would be used by the quit already under way
         ApplicationQuit.QuitClaim claim = quit_.claimQuit();
         if (claim == null)
         {
            observer.onSwitchFailed(constants_.quitAlreadyInProgress());
            return;
         }

         Desktop.getFrame().setPendingRVersion(installed.binary, error ->
         {
            if (!StringUtil.isNullOrEmpty(error))
            {
               claim.release();
               observer.onSwitchFailed(error);
               return;
            }

            // the claim may have been superseded while the R was checked (a
            // stalled quit admits the next one, see QuitGate); the prepared
            // R is used only by a quit that names it, so nothing is left for
            // that other quit
            if (!claim.performQuit(
                  constants_.switchingRVersionProgress(installed.version),
                  saveChanges,
                  projectFile,
                  spec,
                  installed.binary))
            {
               observer.onSwitchFailed(constants_.quitAlreadyInProgress());
               return;
            }

            observer.onSwitchStarted();
         });
      });
   }

   private final GlobalDisplay globalDisplay_;
   private final Session session_;
   private final ApplicationQuit quit_;
   private final RVersionChooser chooser_;

   private static final RenvConstants constants_ = GWT.create(RenvConstants.class);
}
