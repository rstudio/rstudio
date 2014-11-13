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
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.theme.DialogTabLayoutPanel;
import org.rstudio.core.client.widget.NumericValueWidget;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

public class EditingPreferencesPane extends PreferencesPane
{
   @Inject
   public EditingPreferencesPane(UIPrefs prefs)
   {
      prefs_ = prefs;
      
      VerticalPanel editingPanel = new VerticalPanel();
      editingPanel.add(tight(spacesForTab_ = checkboxPref("Insert spaces for tab", prefs.useSpacesForTab())));
      editingPanel.add(indent(tabWidth_ = numericPref("Tab width", prefs.numSpacesForTab())));   
      editingPanel.add(checkboxPref("Insert matching parens/quotes", prefs_.insertMatching()));
      editingPanel.add(checkboxPref("Auto-indent code after paste", prefs_.reindentOnPaste()));
      editingPanel.add(checkboxPref("Vertically align arguments in auto-indent", prefs_.verticallyAlignArgumentIndent()));
      editingPanel.add(checkboxPref("Soft-wrap R source files", prefs_.softWrapRFiles()));
      editingPanel.add(checkboxPref("Ensure that source files end with newline", prefs_.autoAppendNewline()));
      editingPanel.add(checkboxPref("Strip trailing horizontal whitespace when saving", prefs_.stripTrailingWhitespace()));
      editingPanel.add(checkboxPref("Focus console after executing from source", prefs_.focusConsoleAfterExec()));
      editingPanel.add(checkboxPref("Enable vim editing mode", prefs_.useVimMode())); 
     
      
      VerticalPanel displayPanel = new VerticalPanel();
      displayPanel.add(checkboxPref("Highlight selected word", prefs.highlightSelectedWord()));
      displayPanel.add(checkboxPref("Highlight selected line", prefs.highlightSelectedLine()));
      displayPanel.add(checkboxPref("Show line numbers", prefs.showLineNumbers()));
      displayPanel.add(tight(showMargin_ = checkboxPref("Show margin", prefs.showMargin())));
      displayPanel.add(indent(marginCol_ = numericPref("Margin column", prefs.printMarginColumn())));
      displayPanel.add(checkboxPref("Show whitespace characters", prefs_.showInvisibles()));
      displayPanel.add(checkboxPref("Show indent guides", prefs_.showIndentGuides()));
      displayPanel.add(checkboxPref("Blinking cursor", prefs_.blinkingCursor()));
      displayPanel.add(checkboxPref("Show syntax highlighting in console input", prefs_.syntaxColorConsole()));
      
      VerticalPanel completionPanel = new VerticalPanel();
      completionPanel.add(checkboxPref("Show tooltip after function completions", prefs.showSignatureTooltips()));    
      completionPanel.add(checkboxPref("Insert spaces around equals for argument completions", prefs.insertSpacesAroundEquals()));
      
      DialogTabLayoutPanel tabPanel = new DialogTabLayoutPanel();
      tabPanel.setSize("435px", "460px");     
      tabPanel.add(editingPanel, "Editing");
      tabPanel.add(displayPanel, "Display");
      tabPanel.add(completionPanel, "Completion");
      tabPanel.selectTab(0);
      add(tabPanel);
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
      return "Code";
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
