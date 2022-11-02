/*
 * ConsolePreferencesPane.java
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
package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.NumericValueWidget;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;


public class ConsolePreferencesPane extends PreferencesPane
{
   @Inject
   public ConsolePreferencesPane(UserPrefs prefs,
                                 Session session,
                                 PreferencesDialogResources res)
   {
      prefs_ = prefs;
      res_ = res;

      Label displayLabel = headerLabel(constants_.consoleDisplayLabel());
      add(displayLabel);
      add(checkboxPref(constants_.consoleSyntaxHighlightingLabel(), prefs_.syntaxColorConsole()));
      add(checkboxPref(constants_.consoleDifferentColorLabel(), prefs_.highlightConsoleErrors()));
      add(checkboxPref(constants_.consoleLimitVariableLabel(), prefs_.limitVisibleConsole()));
      NumericValueWidget limitLengthPref =
         numericPref(constants_.consoleLimitOutputLengthLabel(), prefs_.consoleLineLengthLimit());
      add(nudgeRightPlus(limitLengthPref));

      consoleColorMode_ = new SelectWidget(
         constants_.consoleANSIEscapeCodesLabel(),
         new String[] {
            constants_.consoleColorModeANSIOption(),
            constants_.consoleColorModeRemoveANSIOption(),
            constants_.consoleColorModeIgnoreANSIOption()
         },
         new String[] {
            UserPrefs.ANSI_CONSOLE_MODE_ON,
            UserPrefs.ANSI_CONSOLE_MODE_STRIP,
            UserPrefs.ANSI_CONSOLE_MODE_OFF
         },
         false,
         true,
         false);
      add(consoleColorMode_);

      Label executionLabel = headerLabel(constants_.consoleExecutionLabel());
      add(spacedBefore(executionLabel));
      add(checkboxPref(constants_.consoleDiscardPendingConsoleInputOnErrorLabel(), prefs_.discardPendingConsoleInputOnError()));
      
      Label debuggingLabel = headerLabel(constants_.debuggingHeaderLabel());
      add(spacedBefore(debuggingLabel));
      add(debuggingLabel);
      add(spaced(checkboxPref(
         constants_.debuggingExpandTracebacksLabel(),
         prefs_.autoExpandErrorTracebacks(),
         true /*defaultSpaced*/)));

      Label otherLabel = headerLabel(constants_.otherHeaderCaption());
      add(spacedBefore(otherLabel));
      add(spaced(checkboxPref(constants_.otherDoubleClickLabel(), prefs_.consoleDoubleClickSelect())));
      add(spaced(checkboxPref(constants_.warnAutoSuspendPausedLabel(), prefs_.consoleSuspendBlockedNotice())));
      add(indent(numericPref(constants_.numSecondsToDelayWarningLabel(), prefs_.consoleSuspendBlockedNoticeDelay())));
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(res_.iconConsole2x());
   }

   @Override
   public String getName()
   {
      return constants_.consoleLabel();
   }

   @Override
   protected void initialize(UserPrefs prefs)
   {
      consoleColorMode_.setValue(prefs_.ansiConsoleMode().getValue());
      initialHighlightConsoleErrors_ = prefs.highlightConsoleErrors().getValue();
      initialLimitVisibleConsole_ = prefs.limitVisibleConsole().getValue();
   }

   @Override
   public RestartRequirement onApply(UserPrefs prefs)
   {
      RestartRequirement restartRequirement = super.onApply(prefs);

      prefs_.ansiConsoleMode().setGlobalValue(consoleColorMode_.getValue());
      if (prefs_.highlightConsoleErrors().getValue() != initialHighlightConsoleErrors_)
      {
         initialHighlightConsoleErrors_ = prefs_.highlightConsoleErrors().getValue();
         restartRequirement.setRestartRequired();
      }
      if (!restartRequirement.getDesktopRestartRequired() && !restartRequirement.getUiReloadRequired())
      {
         if (prefs_.limitVisibleConsole().getValue() != initialLimitVisibleConsole_)
         {
            initialLimitVisibleConsole_ = prefs_.limitVisibleConsole().getValue();
            restartRequirement.setRestartRequired();
         }
      }
      return restartRequirement;
   }

   @Override
   public boolean validate()
   {
      return true;
   }

   private boolean initialHighlightConsoleErrors_;
   private boolean initialLimitVisibleConsole_;
   private final SelectWidget consoleColorMode_;

   // Injected
   private final UserPrefs prefs_;
   private final PreferencesDialogResources res_;
   private final static PrefsConstants constants_ = GWT.create(PrefsConstants.class);
}
