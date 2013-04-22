/*
 * EditingPreferencesPane.java
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
package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.inject.Inject;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.widget.NumericValueWidget;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

public class EditingPreferencesPane extends PreferencesPane
{
   @Inject
   public EditingPreferencesPane(UIPrefs prefs)
   {
      prefs_ = prefs;

      add(checkboxPref("Highlight selected word", prefs.highlightSelectedWord()));
      add(checkboxPref("Highlight selected line", prefs.highlightSelectedLine()));
      add(checkboxPref("Show line numbers", prefs.showLineNumbers()));
      add(tight(spacesForTab_ = checkboxPref("Insert spaces for tab", prefs.useSpacesForTab())));
      add(indent(tabWidth_ = numericPref("Tab width", prefs.numSpacesForTab())));
      add(tight(showMargin_ = checkboxPref("Show margin", prefs.showMargin())));
      add(indent(marginCol_ = numericPref("Margin column", prefs.printMarginColumn())));
      add(checkboxPref("Show whitespace characters", prefs_.showInvisibles()));
      add(checkboxPref("Show indent guides", prefs_.showIndentGuides()));
      add(checkboxPref("Blinking cursor", prefs_.blinkingCursor()));
      add(checkboxPref("Insert matching parens/quotes", prefs_.insertMatching()));
      add(checkboxPref("Auto-indent code after paste", prefs_.reindentOnPaste()));
      add(checkboxPref("Vertically align arguments in auto-indent", prefs_.verticallyAlignArgumentIndent()));
      add(checkboxPref("Soft-wrap R source files", prefs_.softWrapRFiles()));
      add(checkboxPref("Focus console after executing from source", prefs_.focusConsoleAfterExec()));
      add(checkboxPref("Show syntax highlighting in console input", prefs_.syntaxColorConsole()));
      add(checkboxPref("Enable vim editing mode", prefs_.useVimMode()));
   }

 
   @Override
   public ImageResource getIcon()
   {
      return PreferencesDialogBaseResources.INSTANCE.iconCodeEditing();
   }

   @Override
   public boolean validate()
   {
      return (!spacesForTab_.getValue() || tabWidth_.validatePositive("Tab width")) &&
             (!showMargin_.getValue() || marginCol_.validate("Margin column"));
   }

   @Override
   public String getName()
   {
      return "Code Editing";
   }

   @Override
   protected void initialize(RPrefs prefs)
   {
   }
   

   private final UIPrefs prefs_;
   private final NumericValueWidget tabWidth_;
   private final NumericValueWidget marginCol_;
   private final CheckBox spacesForTab_;
   private final CheckBox showMargin_;
   
}
