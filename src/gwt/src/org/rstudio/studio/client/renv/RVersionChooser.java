/*
 * RVersionChooser.java
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

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.renv.model.RVersionInstall;
import org.rstudio.studio.client.renv.model.RenvServerOperations;
import org.rstudio.studio.client.renv.ui.ChooseRVersionDialog;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import jsinterop.base.JsArrayLike;

/**
 * Lets the user choose the installation of R that RStudio uses (on the
 * desktop): the default R, one of those found on this machine, or one the
 * user browses to.
 */
@Singleton
public class RVersionChooser
{
   /**
    * An installation of R to use. `useDefault` marks the default R, which
    * RStudio then follows from then on, rather than this installation.
    */
   public static class Choice
   {
      public Choice(RVersionInstall installation, boolean useDefault)
      {
         this.installation = installation;
         this.useDefault = useDefault;
      }

      public boolean sameAs(Choice other)
      {
         return other != null &&
                useDefault == other.useDefault &&
                sameExecutable(installation.binary, other.installation.binary);
      }

      public final RVersionInstall installation;
      public final boolean useDefault;
   }

   /**
    * Whether two paths name the same R. On Windows, where the installations
    * listed name bin\R.exe and the desktop keeps bin\x64\Rterm.exe, case,
    * separators and the executable's name don't matter, and bin\R.exe runs
    * the 64-bit R.
    */
   public static boolean sameExecutable(String a, String b)
   {
      return sameExecutable(a, b, BrowseCap.isWindowsDesktop());
   }

   // as above, with the platform given (for tests)
   static boolean sameExecutable(String a, String b, boolean windows)
   {
      return StringUtil.equals(executableIdentity(a, windows), executableIdentity(b, windows));
   }

   private static String executableIdentity(String binary, boolean windows)
   {
      String path = StringUtil.notNull(binary);
      if (!windows)
         return path;

      path = path.replace('\\', '/').toLowerCase();
      String dir = path.substring(0, Math.max(0, path.lastIndexOf('/')));
      return dir.endsWith("/bin") ? dir + "/x64" : dir;
   }

   @Inject
   public RVersionChooser(GlobalDisplay globalDisplay, RenvServerOperations server)
   {
      globalDisplay_ = globalDisplay;
      server_ = server;
   }

   /**
    * Ask the user to choose an installation of R. `onChosen` receives the
    * choice, and isn't run when the user cancels.
    *
    * @param selected The choice to start from; null for the R RStudio uses.
    */
   public void choose(String caption,
                      String okCaption,
                      Choice selected,
                      CommandWithArg<Choice> onChosen)
   {
      ProgressIndicator indicator = globalDisplay_.getProgressIndicator(caption);
      indicator.onProgress(constants_.findingRInstallations());

      // the desktop and the session each take a while to answer, and
      // neither needs the other's answer, so both are asked at once
      ChooserAnswers answers = new ChooserAnswers();
      Command showWhenAnswered = () ->
      {
         if (answers.failed || answers.rChoice == null || answers.installed == null)
            return;

         indicator.onCompleted();
         Choice start = selected != null ? selected : choiceFor(answers.rChoice);
         new ChooseRVersionDialog(
               caption,
               okCaption,
               defaultR(answers.rChoice),
               answers.installed,
               start,
               onBrowsed -> browse(caption, onBrowsed),
               onChosen::execute).showModal();
      };

      Desktop.getFrame().getRChoice(rChoice ->
      {
         answers.rChoice = rChoice != null ? rChoice : JsObject.createJsObject();
         showWhenAnswered.execute();
      });

      server_.rigListRInstallations(new ServerRequestCallback<JsArrayLike<RVersionInstall>>()
      {
         @Override
         public void onResponseReceived(JsArrayLike<RVersionInstall> response)
         {
            List<RVersionInstall> installed = new ArrayList<>();
            for (int i = 0; i < response.getLength(); i++)
               installed.add(response.getAt(i));

            answers.installed = installed;
            showWhenAnswered.execute();
         }

         @Override
         public void onError(ServerError error)
         {
            answers.failed = true;
            indicator.onError(error.getUserMessage());
         }
      });
   }

   // what choose() waits for, which arrives in either order
   private static class ChooserAnswers
   {
      JsObject rChoice;
      List<RVersionInstall> installed;
      boolean failed;
   }

   /**
    * The R RStudio uses from its next launch on, as the desktop describes
    * it: the R chosen in RStudio, or else the default R. The callback
    * receives null when neither is known.
    */
   public void choiceInUse(CommandWithArg<Choice> callback)
   {
      Desktop.getFrame().getRChoice(rChoice ->
      {
         callback.execute(rChoice == null ? null : choiceFor(rChoice));
      });
   }

   /**
    * How a choice of R reads in a list or a field: its version and where it
    * is, and whether it is the default R. `marks` adds whether it is the R in
    * use and whether it would run emulated.
    */
   public static String describe(Choice choice, boolean marks)
   {
      RVersionInstall installation = choice.installation;
      String location = StringUtil.isNullOrEmpty(installation.path)
            ? installation.binary
            : installation.path;

      String description = StringUtil.isNullOrEmpty(installation.version)
            ? constants_.rExecutableLabel(StringUtil.notNull(location))
            : constants_.rInstallationLabel(installation.version, StringUtil.notNull(location));

      if (choice.useDefault)
         description = constants_.defaultRLabel(description);

      if (marks && installation.current)
         description = constants_.rInUseLabel(description);

      if (marks && installation.emulated)
         description = constants_.rEmulatedLabel(description);

      return description;
   }

   // the chosen R, or else the default R
   private static Choice choiceFor(JsObject rChoice)
   {
      JsObject chosen = rChoice.getObject("chosen");
      if (chosen != null && !StringUtil.isNullOrEmpty(chosen.getString("path")))
      {
         RVersionInstall installation = installation(chosen);
         installation.current = sameR(chosen, rChoice.getObject("current"));
         return new Choice(installation, false);
      }

      RVersionInstall defaultR = defaultR(rChoice);
      return defaultR == null ? null : new Choice(defaultR, true);
   }

   // the default R as an installation, or null when there is none
   private static RVersionInstall defaultR(JsObject rChoice)
   {
      JsObject defaultR = rChoice.getObject("default");
      if (defaultR == null || StringUtil.isNullOrEmpty(defaultR.getString("path")))
         return null;

      RVersionInstall installation = installation(defaultR);
      installation.current = sameR(defaultR, rChoice.getObject("current"));
      return installation;
   }

   // an R the desktop described (an executable and its version); RStudio
   // checks that it runs as itself before using it
   private static RVersionInstall installation(JsObject description)
   {
      RVersionInstall installation = new RVersionInstall();
      installation.version = StringUtil.notNull(description.getString("version"));
      installation.binary = StringUtil.notNull(description.getString("path"));
      installation.path = "";
      installation.home = "";
      installation.orthogonal = true;
      return installation;
   }

   // as the desktop describes them; on Windows, its paths for the same R
   // can differ in their separators
   private static boolean sameR(JsObject a, JsObject b)
   {
      return a != null && b != null &&
             !StringUtil.isNullOrEmpty(a.getString("path")) &&
             sameExecutable(a.getString("path"), b.getString("path"));
   }

   private void browse(String caption, CommandWithArg<RVersionInstall> onChosen)
   {
      Desktop.getFrame().getOpenFileName(
            constants_.browseForRCaption(),
            constants_.browseForRButton(),
            "",
            "",
            false,
            true,
            path ->
            {
               if (StringUtil.isNullOrEmpty(path))
                  return;

               server_.rigDescribeRInstallation(path, new ServerRequestCallback<RVersionInstall>()
               {
                  @Override
                  public void onResponseReceived(RVersionInstall installation)
                  {
                     if (installation == null)
                        globalDisplay_.showErrorMessage(caption, constants_.notAnRInstallation(path));
                     else if (!installation.supported)
                        globalDisplay_.showErrorMessage(caption, constants_.rVersionUnsupported(installation.version));
                     else
                        onChosen.execute(installation);
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     globalDisplay_.showErrorMessage(caption, error.getUserMessage());
                  }
               });
            });
   }

   private final GlobalDisplay globalDisplay_;
   private final RenvServerOperations server_;

   private static final RenvConstants constants_ = GWT.create(RenvConstants.class);
}
