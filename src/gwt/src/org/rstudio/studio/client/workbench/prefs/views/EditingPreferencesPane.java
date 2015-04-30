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

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.theme.DialogTabLayoutPanel;
import org.rstudio.core.client.widget.HelpButton;
import org.rstudio.core.client.widget.NumericValueWidget;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.studio.client.common.DiagnosticsHelpLink;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefsAccessor;
import org.rstudio.studio.client.workbench.snippets.ui.EditSnippetsDialog;

public class EditingPreferencesPane extends PreferencesPane
{
   @Inject
   public EditingPreferencesPane(UIPrefs prefs,
                                 PreferencesDialogResources res)
   {
      prefs_ = prefs;
      PreferencesDialogBaseResources baseRes = PreferencesDialogBaseResources.INSTANCE;
      
      VerticalPanel editingPanel = new VerticalPanel();
      editingPanel.add(headerLabel("General"));
      editingPanel.add(tight(spacesForTab_ = checkboxPref("Insert spaces for tab", prefs.useSpacesForTab())));
      editingPanel.add(indent(tabWidth_ = numericPref("Tab width", prefs.numSpacesForTab())));   
      editingPanel.add(checkboxPref("Insert matching parens/quotes", prefs_.insertMatching()));
      editingPanel.add(checkboxPref("Auto-indent code after paste", prefs_.reindentOnPaste()));
      editingPanel.add(checkboxPref("Vertically align arguments in auto-indent", prefs_.verticallyAlignArgumentIndent()));
      editingPanel.add(checkboxPref("Soft-wrap R source files", prefs_.softWrapRFiles()));
      editingPanel.add(checkboxPref("Ensure that source files end with newline", prefs_.autoAppendNewline()));
      editingPanel.add(checkboxPref("Strip trailing horizontal whitespace when saving", prefs_.stripTrailingWhitespace()));
      editingPanel.add(checkboxPref(
            "Continue comment when inserting new line",
            prefs_.continueCommentsOnNewline(),
            "When enabled, pressing enter will continue comments on new lines. Press Shift + Enter to exit a comment."));
      editingPanel.add(checkboxPref("Enable vim editing mode", prefs_.useVimMode()));
     
      Label executionLabel = headerLabel("Execution");
      editingPanel.add(executionLabel);
      executionLabel.getElement().getStyle().setMarginTop(8, Unit.PX);
      editingPanel.add(checkboxPref("Always save R scripts before sourcing", prefs.saveBeforeSourcing()));
      editingPanel.add(checkboxPref("Focus console after executing from source", prefs_.focusConsoleAfterExec()));
      
    
      
      Label snippetsLabel = headerLabel("Snippets");
      snippetsLabel.getElement().getStyle().setMarginTop(8, Unit.PX);
      editingPanel.add(snippetsLabel);
      
      HorizontalPanel panel = new HorizontalPanel();
      CheckBox enableSnippets = checkboxPref("Enable code snippets", prefs_.enableSnippets());
      panel.add(enableSnippets);
     
      SmallButton editSnippets = new SmallButton("Edit Snippets...");
      editSnippets.getElement().getStyle().setMarginTop(1, Unit.PX);
      editSnippets.getElement().getStyle().setMarginLeft(5, Unit.PX);
      editSnippets.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            new EditSnippetsDialog().showModal();
         }
      });
      panel.add(editSnippets);
      
      HelpButton snippetHelp = new HelpButton("code_snippets");
      snippetHelp.getElement().getStyle().setMarginTop(2, Unit.PX);
      snippetHelp.getElement().getStyle().setMarginLeft(6, Unit.PX);
      panel.add(snippetHelp);
      
      editingPanel.add(panel);
      
      
      
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
      
      completionPanel.add(headerLabel("R and C/C++"));
     
      showCompletions_ = new SelectWidget(
            "Show code completions:",
            new String[] {
                  "Automatically",
                  "When Triggered ($, ::)",
                  "Manually (Tab)"
            },
            new String[] {
                  UIPrefsAccessor.COMPLETION_ALWAYS,
                  UIPrefsAccessor.COMPLETION_WHEN_TRIGGERED,
                  UIPrefsAccessor.COMPLETION_MANUAL
            },
            false, 
            true, 
            false);
      
      spaced(showCompletions_);
      completionPanel.add(showCompletions_);    
      
      final CheckBox alwaysCompleteInConsole = checkboxPref(
            "Allow automatic completions in console",
            prefs.alwaysCompleteInConsole());
      completionPanel.add(alwaysCompleteInConsole);
      
      showCompletions_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            alwaysCompleteInConsole.setVisible(
                   showCompletions_.getValue().equals(
                                        UIPrefsAccessor.COMPLETION_ALWAYS));
            
         }
      });
    
      final CheckBox insertParensAfterFunctionCompletionsCheckbox =
           checkboxPref("Insert parentheses after function completions",
                 prefs.insertParensAfterFunctionCompletion());
      
      final CheckBox showSignatureTooltipsCheckbox =
           checkboxPref("Show help tooltip after function completions",
                 prefs.showSignatureTooltips());
      
      addEnabledDependency(
            insertParensAfterFunctionCompletionsCheckbox,
            showSignatureTooltipsCheckbox);
      
      completionPanel.add(insertParensAfterFunctionCompletionsCheckbox);
      completionPanel.add(showSignatureTooltipsCheckbox);
      
      completionPanel.add(checkboxPref("Insert spaces around equals for argument completions", prefs.insertSpacesAroundEquals()));
      completionPanel.add(checkboxPref("Use tab for multiline autocompletions", prefs.allowTabMultilineCompletion()));
      
      
      Label otherLabel = headerLabel("Other Languages");
      otherLabel.getElement().getStyle().setMarginTop(8, Unit.PX);
      completionPanel.add(otherLabel);
      
      showCompletionsOther_ = new SelectWidget(
            "Show code completions:",
            new String[] {
                  "Automatically",
                  "Manually (Ctrl+Space) "
            },
            new String[] {
                  UIPrefsAccessor.COMPLETION_ALWAYS,
                  UIPrefsAccessor.COMPLETION_MANUAL
            },
            false, 
            true, 
            false);
      completionPanel.add(showCompletionsOther_);
      
      Label otherTip = new Label(
        "Keyword and text-based completions are supported for several other " +
        "languages including JavaScript, HTML, CSS, Python, and SQL.");
      otherTip.addStyleName(baseRes.styles().infoLabel());
      completionPanel.add(nudgeRightPlus(otherTip));
      
      
      Label delayLabel = headerLabel("Completion Delay");
      delayLabel.getElement().getStyle().setMarginTop(14, Unit.PX);
      completionPanel.add(delayLabel);
      
      completionPanel.add(nudgeRightPlus(alwaysCompleteChars_ =
          numericPref("Show completions after characters entered:",
                      prefs.alwaysCompleteCharacters())));
      completionPanel.add(nudgeRightPlus(alwaysCompleteDelayMs_ = 
          numericPref("Show completions after keyboard idle (ms):",
                      prefs.alwaysCompleteDelayMs())));
        
      
      VerticalPanel diagnosticsPanel = new VerticalPanel();
      Label rLabel = headerLabel("R Diagnostics");
      diagnosticsPanel.add(spacedBefore(rLabel));
      final CheckBox chkShowRDiagnostics = checkboxPref("Show diagnostics for R", prefs.showDiagnosticsR());
      diagnosticsPanel.add(chkShowRDiagnostics);
      
      final VerticalPanel rOptionsPanel = new VerticalPanel();
      rOptionsPanel.add(checkboxPref("Enable diagnostics within R function calls", prefs.diagnosticsInRFunctionCalls()));
      rOptionsPanel.add(spaced(checkboxPref("Check arguments to R function calls", prefs.checkArgumentsToRFunctionCalls())));
      rOptionsPanel.add(spaced(checkboxPref("Warn if variable used has no definition in scope", prefs.warnIfNoSuchVariableInScope())));
      rOptionsPanel.add(spaced(checkboxPref("Warn if variable is defined but not used", prefs.warnIfVariableDefinedButNotUsed())));
      rOptionsPanel.add(spaced(checkboxPref("Provide R style diagnostics (e.g. whitespace)", prefs.enableStyleDiagnostics())));
      rOptionsPanel.setVisible(prefs.showDiagnosticsR().getValue());
      chkShowRDiagnostics.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            rOptionsPanel.setVisible(event.getValue());
         }
      });
      
      diagnosticsPanel.add(rOptionsPanel);
      
      
      Label diagOtherLabel = headerLabel("Other Languages");
      diagnosticsPanel.add(spacedBefore(diagOtherLabel));
      diagnosticsPanel.add(checkboxPref("Show diagnostics for C/C++", prefs.showDiagnosticsCpp()));
      diagnosticsPanel.add(checkboxPref("Show diagnostics for JavaScript, HTML, and CSS", prefs.showDiagnosticsOther()));
    
      Label diagShowLabel = headerLabel("Show Diagnostics");
      diagnosticsPanel.add(spacedBefore(diagShowLabel));
      diagnosticsPanel.add(checkboxPref("Show diagnostics whenever source files are saved", prefs.diagnosticsOnSave()));
      diagnosticsPanel.add(tight(checkboxPref("Show diagnostics after keyboard is idle for a period of time", prefs.enableBackgroundDiagnostics())));
      diagnosticsPanel.add(indent(backgroundDiagnosticsDelayMs_ =
            numericPref("Keyboard idle time (ms):", prefs.backgroundDiagnosticsDelayMs())));
      
      HelpLink diagnosticsHelpLink = new DiagnosticsHelpLink();
      diagnosticsHelpLink.getElement().getStyle().setMarginTop(12, Unit.PX);
      nudgeRight(diagnosticsHelpLink); 
      diagnosticsPanel.add(diagnosticsHelpLink);
      
      
      DialogTabLayoutPanel tabPanel = new DialogTabLayoutPanel();
      tabPanel.setSize("435px", "498px");
      tabPanel.add(editingPanel, "Editing");
      tabPanel.add(displayPanel, "Display");
      tabPanel.add(completionPanel, "Completion");
      tabPanel.add(diagnosticsPanel, "Diagnostics");
      tabPanel.selectTab(0);
      add(tabPanel);
   }
   
   private void disable(CheckBox checkBox)
   {
      checkBox.setValue(false);
      checkBox.setEnabled(false);
      checkBox.setVisible(false);
   }
   
   private void enable(CheckBox checkBox)
   {
      checkBox.setValue(true);
      checkBox.setEnabled(true);
      checkBox.setVisible(true);
   }
   
   private void addEnabledDependency(final CheckBox speaker,
                                     final CheckBox listener)
   {
      if (speaker.getValue() == false)
         disable(listener);
      
      speaker.addValueChangeHandler(
            new ValueChangeHandler<Boolean>()
            {
               @Override
               public void onValueChange(ValueChangeEvent<Boolean> event)
               {
                  if (event.getValue() == false)
                     disable(listener);
                  else
                     enable(listener);
               }
            });
   }

   @Override
   protected void initialize(RPrefs prefs)
   {
      showCompletions_.setValue(prefs_.codeComplete().getValue());
      showCompletionsOther_.setValue(prefs_.codeCompleteOther().getValue());
   }
   
   @Override
   public boolean onApply(RPrefs prefs)
   {
      boolean reload = super.onApply(prefs);
      
      prefs_.codeComplete().setGlobalValue(showCompletions_.getValue());
      prefs_.codeCompleteOther().setGlobalValue(showCompletionsOther_.getValue());
      
      return reload;
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
             (!showMargin_.getValue() || marginCol_.validate("Margin column")) &&
             alwaysCompleteChars_.validateRange("Characters entered", 1, 100) &&
             alwaysCompleteDelayMs_.validateRange("Completion keyboard idle (ms)", 0, 10000) &&
             backgroundDiagnosticsDelayMs_.validateRange("Diagnostics keyboard idle (ms):", 0, 10000);
   }

   @Override
   public String getName()
   {
      return "Code";
   }

   private final UIPrefs prefs_;
   private final NumericValueWidget tabWidth_;
   private final NumericValueWidget marginCol_;
   private final NumericValueWidget alwaysCompleteChars_;
   private final NumericValueWidget alwaysCompleteDelayMs_;
   private final NumericValueWidget backgroundDiagnosticsDelayMs_;
   private final CheckBox spacesForTab_;
   private final CheckBox showMargin_;
   private final SelectWidget showCompletions_;
   private final SelectWidget showCompletionsOther_;
   
   
}
