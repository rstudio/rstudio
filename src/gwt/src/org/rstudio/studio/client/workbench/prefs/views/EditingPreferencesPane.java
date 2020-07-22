/*
 * EditingPreferencesPane.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.DialogTabLayoutPanel;
import org.rstudio.core.client.theme.VerticalTabPanel;
import org.rstudio.core.client.widget.HelpButton;
import org.rstudio.core.client.widget.ModifyKeyboardShortcutsWidget;
import org.rstudio.core.client.widget.NumericValueWidget;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.common.DiagnosticsHelpLink;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.snippets.ui.EditSnippetsDialog;
import org.rstudio.studio.client.workbench.views.source.editors.text.IconvListResult;
import org.rstudio.studio.client.workbench.views.source.editors.text.ui.ChooseEncodingDialog;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

public class EditingPreferencesPane extends PreferencesPane
{
   @Inject
   public EditingPreferencesPane(UserPrefs prefs,
                                 SourceServerOperations server,
                                 PreferencesDialogResources res)
   {
      prefs_ = prefs;
      server_ = server;
      PreferencesDialogBaseResources baseRes = PreferencesDialogBaseResources.INSTANCE;

      VerticalTabPanel editingPanel = new VerticalTabPanel(ElementIds.EDIT_EDITING_PREFS);
      editingPanel.add(headerLabel("General"));
      editingPanel.add(tight(spacesForTab_ = checkboxPref("Insert spaces for tab", prefs.useSpacesForTab(),
            false /*defaultSpace*/)));
      editingPanel.add(indent(tabWidth_ = numericPref("Tab width", 1, UserPrefs.MAX_TAB_WIDTH,
            prefs.numSpacesForTab())));
      tabWidth_.setWidth("36px");
      editingPanel.add(checkboxPref(
            "Auto-detect code indentation",
            prefs_.autoDetectIndentation(),
            "When enabled, the indentation for documents not part of an RStudio project " +
            "will be automatically detected."));
      editingPanel.add(checkboxPref("Insert matching parens/quotes", prefs_.insertMatching()));
      editingPanel.add(checkboxPref("Auto-indent code after paste", prefs_.reindentOnPaste()));
      editingPanel.add(checkboxPref("Vertically align arguments in auto-indent", prefs_.verticallyAlignArgumentsIndent()));
      editingPanel.add(checkboxPref("Soft-wrap R source files", prefs_.softWrapRFiles()));
      editingPanel.add(checkboxPref(
            "Continue comment when inserting new line",
            prefs_.continueCommentsOnNewline(),
            "When enabled, pressing Enter will continue comments on new lines. " +
            "Press Shift + Enter to exit a comment."));

      delimiterSurroundWidget_ = new SelectWidget(
            "Surround selection on text insertion:",
            new String[] {
                  "Never",
                  "Quotes",
                  "Quotes & Brackets"
            },
            new String[] {
                  UserPrefs.SURROUND_SELECTION_NEVER,
                  UserPrefs.SURROUND_SELECTION_QUOTES,
                  UserPrefs.SURROUND_SELECTION_QUOTES_AND_BRACKETS
            },
            false,
            true,
            false);
      editingPanel.add(delimiterSurroundWidget_);

      HorizontalPanel keyboardPanel = new HorizontalPanel();
      editorMode_ = new SelectWidget(
            "Keybindings:",
            new String[] {
                  "Default",
                  "Vim",
                  "Emacs",
                  "Sublime Text"
            },
            new String[] {
                  UserPrefs.EDITOR_KEYBINDINGS_DEFAULT,
                  UserPrefs.EDITOR_KEYBINDINGS_VIM,
                  UserPrefs.EDITOR_KEYBINDINGS_EMACS,
                  UserPrefs.EDITOR_KEYBINDINGS_SUBLIME,
            },
            false,
            true,
            false);
      editorMode_.getElement().getStyle().setMarginBottom(0, Unit.PX);

      keyboardPanel.add(editorMode_);
      SmallButton editShortcuts = new SmallButton("Modify Keyboard Shortcuts...");
      editShortcuts.getElement().getStyle().setMarginLeft(15, Unit.PX);
      editShortcuts.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            new ModifyKeyboardShortcutsWidget().showModal();
         }
      });
      keyboardPanel.add(editShortcuts);


      lessSpaced(keyboardPanel);
      editingPanel.add(keyboardPanel);

      Label executionLabel = headerLabel("Execution");
      editingPanel.add(executionLabel);
      executionLabel.getElement().getStyle().setMarginTop(8, Unit.PX);
      editingPanel.add(checkboxPref("Always save R scripts before sourcing", prefs.saveBeforeSourcing()));
      editingPanel.add(checkboxPref("Focus console after executing from source", prefs_.focusConsoleAfterExec()));

      executionBehavior_ = new SelectWidget(
            "Ctrl+Enter executes: ",
            new String[] {
               "Current line",
               "Multi-line R statement",
               "Multiple consecutive R lines"
            },
            new String[] {
               UserPrefs.EXECUTION_BEHAVIOR_LINE,
               UserPrefs.EXECUTION_BEHAVIOR_STATEMENT,
               UserPrefs.EXECUTION_BEHAVIOR_PARAGRAPH
            },
            false,
            true,
            false);
      executionBehavior_.getElement().getStyle().setMarginBottom(0, Unit.PX);
      editingPanel.add(executionBehavior_);

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

      HelpButton snippetHelp = new HelpButton("code_snippets", "Help on code snippets");
      snippetHelp.getElement().getStyle().setMarginTop(2, Unit.PX);
      snippetHelp.getElement().getStyle().setMarginLeft(6, Unit.PX);
      panel.add(snippetHelp);

      editingPanel.add(panel);

      VerticalTabPanel displayPanel = new VerticalTabPanel(ElementIds.EDIT_DISPLAY_PREFS);
      displayPanel.add(headerLabel("General"));
      displayPanel.add(checkboxPref("Highlight selected word", prefs.highlightSelectedWord()));
      displayPanel.add(checkboxPref("Highlight selected line", prefs.highlightSelectedLine()));
      displayPanel.add(checkboxPref("Show line numbers", prefs.showLineNumbers()));
      displayPanel.add(tight(showMargin_ = checkboxPref("Show margin",
            prefs.showMargin(), false /*defaultSpace*/)));
      displayPanel.add(indent(marginCol_ = numericPref("Margin column", prefs.marginColumn())));
      displayPanel.add(checkboxPref("Show whitespace characters", prefs_.showInvisibles()));
      displayPanel.add(checkboxPref("Show indent guides", prefs_.showIndentGuides()));
      displayPanel.add(checkboxPref("Blinking cursor", prefs_.blinkingCursor()));
      displayPanel.add(checkboxPref("Allow scroll past end of document", prefs_.scrollPastEndOfDocument()));
      displayPanel.add(checkboxPref("Allow drag and drop of text", prefs_.enableTextDrag()));
      displayPanel.add(checkboxPref("Highlight R function calls", prefs_.highlightRFunctionCalls()));
      displayPanel.add(extraSpaced(
         checkboxPref("Rainbow parentheses", prefs_.rainbowParentheses(), false /* defaultSpace */)));

      foldMode_ = new SelectWidget(
            "Fold Style:",
            new String[] {
                  "Start Only",
                  "Start and End"
            },
            new String[] {
                  UserPrefs.FOLD_STYLE_BEGIN_ONLY,
                  UserPrefs.FOLD_STYLE_BEGIN_AND_END
            },
            false,
            true,
            false);

      displayPanel.add(foldMode_);

      VerticalTabPanel savePanel = new VerticalTabPanel(ElementIds.EDIT_SAVING_PREFS);

      savePanel.add(headerLabel("General"));
      savePanel.add(checkboxPref("Ensure that source files end with newline", prefs_.autoAppendNewline()));
      savePanel.add(checkboxPref("Strip trailing horizontal whitespace when saving", prefs_.stripTrailingWhitespace()));
      savePanel.add(checkboxPref("Restore last cursor position when opening file", prefs_.restoreSourceDocumentCursorPosition()));

      Label serializationLabel = headerLabel("Serialization");
      serializationLabel.getElement().getStyle().setPaddingTop(14, Unit.PX);
      savePanel.add(serializationLabel);


      lineEndings_ = new LineEndingsSelectWidget();
      spaced(lineEndings_);
      savePanel.add(lineEndings_);

      encodingValue_ = prefs_.defaultEncoding().getGlobalValue();
      savePanel.add(lessSpaced(encoding_ = new TextBoxWithButton(
            "Default text encoding:",
            "",
            "Change...",
            null,
            ElementIds.TextBoxButtonId.TEXT_ENCODING,
            true,
            new ClickHandler()
            {
               public void onClick(ClickEvent event)
               {
                  server_.iconvlist(new SimpleRequestCallback<IconvListResult>()
                  {
                     @Override
                     public void onResponseReceived(IconvListResult response)
                     {
                        new ChooseEncodingDialog(
                              response.getCommon(),
                              response.getAll(),
                              encodingValue_,
                              true,
                              false,
                              new OperationWithInput<String>()
                              {
                                 public void execute(String encoding)
                                 {
                                    if (encoding == null)
                                       return;

                                    setEncoding(encoding);
                                 }
                              }).showModal();
                     }
                  });

               }
            })));
      nudgeRight(encoding_);
      textBoxWithChooser(encoding_);
      spaced(encoding_);
      setEncoding(prefs.defaultEncoding().getGlobalValue());

      savePanel.add(spacedBefore(headerLabel("Auto-save")));
      savePanel.add(checkboxPref("Automatically save when editor loses focus", prefs_.autoSaveOnBlur()));
      autoSaveOnIdle_ = new SelectWidget(
            "When editor is idle: ",
            new String[] {
               "Backup unsaved changes",
               "Save and write changes",
               "Do nothing"
            },
            new String[] {
               UserPrefs.AUTO_SAVE_ON_IDLE_BACKUP,
               UserPrefs.AUTO_SAVE_ON_IDLE_COMMIT,
               UserPrefs.AUTO_SAVE_ON_IDLE_NONE
            },
            false,
            true,
            false);
      savePanel.add(autoSaveOnIdle_);
      autoSaveIdleMs_ = new SelectWidget(
            "Idle period: ",
            new String[] {
               "500ms",
               "1000ms",
               "1500ms",
               "2000ms",
               "3000ms",
               "4000ms",
               "5000ms",
               "10000ms",
            },
            new String[] {
                "500",
                "1000",
                "1500",
                "2000",
                "3000",
                "4000",
                "5000",
                "10000"
            },
            false,
            true,
            false);
      savePanel.add(autoSaveIdleMs_);

      VerticalTabPanel completionPanel = new VerticalTabPanel(ElementIds.EDIT_COMPLETION_PREFS);

      completionPanel.add(headerLabel("R and C/C++"));

      showCompletions_ = new SelectWidget(
            "Show code completions:",
            new String[] {
                  "Automatically",
                  "When Triggered ($, ::)",
                  "Manually (Tab)"
            },
            new String[] {
                  UserPrefs.CODE_COMPLETION_ALWAYS,
                  UserPrefs.CODE_COMPLETION_TRIGGERED,
                  UserPrefs.CODE_COMPLETION_MANUAL
            },
            false,
            true,
            false);

      spaced(showCompletions_);
      completionPanel.add(showCompletions_);

      final CheckBox alwaysCompleteInConsole = checkboxPref(
            "Allow automatic completions in console",
            prefs.consoleCodeCompletion());
      completionPanel.add(alwaysCompleteInConsole);

      showCompletions_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            alwaysCompleteInConsole.setVisible(
                   showCompletions_.getValue() == UserPrefs.CODE_COMPLETION_ALWAYS);

         }
      });

      final CheckBox insertParensAfterFunctionCompletionsCheckbox =
           checkboxPref("Insert parentheses after function completions",
                 prefs.insertParensAfterFunctionCompletion());

      final CheckBox showSignatureTooltipsCheckbox =
           checkboxPref("Show help tooltip after function completions",
                 prefs.showFunctionSignatureTooltips());

      addEnabledDependency(
            insertParensAfterFunctionCompletionsCheckbox,
            showSignatureTooltipsCheckbox);

      completionPanel.add(insertParensAfterFunctionCompletionsCheckbox);
      completionPanel.add(showSignatureTooltipsCheckbox);

      completionPanel.add(checkboxPref("Show help tooltip on cursor idle", prefs.showHelpTooltipOnIdle()));
      completionPanel.add(checkboxPref("Insert spaces around equals for argument completions", prefs.insertSpacesAroundEquals()));
      completionPanel.add(checkboxPref("Use tab for autocompletions", prefs.tabCompletion()));
      completionPanel.add(checkboxPref("Use tab for multiline autocompletions", prefs.tabMultilineCompletion()));


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
                  UserPrefs.CODE_COMPLETION_OTHER_ALWAYS,
                  UserPrefs.CODE_COMPLETION_OTHER_MANUAL
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
          numericPref("Show completions after characters entered:", 1, 99,
                      prefs.codeCompletionCharacters())));
      completionPanel.add(nudgeRightPlus(alwaysCompleteDelayMs_ =
          numericPref("Show completions after keyboard idle (ms):", 0, 9999,
                      prefs.codeCompletionDelay())));


      VerticalTabPanel diagnosticsPanel = new VerticalTabPanel(ElementIds.EDIT_DIAGNOSTICS_PREFS);
      diagnosticsPanel.add(headerLabel("R Diagnostics"));
      final CheckBox chkShowRDiagnostics = checkboxPref("Show diagnostics for R", prefs.showDiagnosticsR());
      diagnosticsPanel.add(chkShowRDiagnostics);

      final VerticalPanel rOptionsPanel = new VerticalPanel();
      rOptionsPanel.add(checkboxPref("Enable diagnostics within R function calls", prefs.diagnosticsInRFunctionCalls()));
      rOptionsPanel.add(checkboxPref("Check arguments to R function calls", prefs.checkArgumentsToRFunctionCalls()));
      rOptionsPanel.add(checkboxPref("Check usage of '<-' in function call", prefs.checkUnexpectedAssignmentInFunctionCall()));
      rOptionsPanel.add(checkboxPref("Warn if variable used has no definition in scope", prefs.warnIfNoSuchVariableInScope()));
      rOptionsPanel.add(checkboxPref("Warn if variable is defined but not used", prefs.warnVariableDefinedButNotUsed()));
      rOptionsPanel.add(checkboxPref("Provide R style diagnostics (e.g. whitespace)", prefs.styleDiagnostics()));
      rOptionsPanel.setVisible(prefs.showDiagnosticsR().getValue());
      chkShowRDiagnostics.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            rOptionsPanel.setVisible(event.getValue());
         }
      });

      diagnosticsPanel.add(rOptionsPanel);
      diagnosticsPanel.add(checkboxPref("Prompt to install missing R packages discovered in R source files", prefs.autoDiscoverPackageDependencies()));

      Label diagOtherLabel = headerLabel("Other Languages");
      diagnosticsPanel.add(spacedBefore(diagOtherLabel));
      diagnosticsPanel.add(checkboxPref("Show diagnostics for C/C++", prefs.showDiagnosticsCpp()));
      diagnosticsPanel.add(checkboxPref("Show diagnostics for JavaScript, HTML, and CSS", prefs.showDiagnosticsOther()));

      Label diagShowLabel = headerLabel("Show Diagnostics");
      diagnosticsPanel.add(spacedBefore(diagShowLabel));
      diagnosticsPanel.add(checkboxPref("Show diagnostics whenever source files are saved", prefs.diagnosticsOnSave()));
      diagnosticsPanel.add(tight(checkboxPref("Show diagnostics after keyboard is idle for a period of time",
            prefs.backgroundDiagnostics(), false /*defaultSpace*/)));
      diagnosticsPanel.add(indent(backgroundDiagnosticsDelayMs_ =
            numericPref("Keyboard idle time (ms):", 0, 9999, prefs.backgroundDiagnosticsDelayMs())));

      HelpLink diagnosticsHelpLink = new DiagnosticsHelpLink();
      diagnosticsHelpLink.getElement().getStyle().setMarginTop(12, Unit.PX);
      nudgeRight(diagnosticsHelpLink);
      diagnosticsPanel.add(diagnosticsHelpLink);


      DialogTabLayoutPanel tabPanel = new DialogTabLayoutPanel("Editing");
      tabPanel.setSize("435px", "533px");
      tabPanel.add(editingPanel, "Editing", editingPanel.getBasePanelId());
      tabPanel.add(displayPanel, "Display", displayPanel.getBasePanelId());
      tabPanel.add(savePanel, "Saving", savePanel.getBasePanelId());
      tabPanel.add(completionPanel, "Completion", completionPanel.getBasePanelId());
      tabPanel.add(diagnosticsPanel, "Diagnostics", diagnosticsPanel.getBasePanelId());
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
   protected void initialize(UserPrefs prefs)
   {
      lineEndings_.setValue(prefs.lineEndingConversion().getValue());

      showCompletions_.setValue(prefs_.codeCompletion().getValue());
      showCompletionsOther_.setValue(prefs_.codeCompletionOther().getValue());
      editorMode_.setValue(prefs_.editorKeybindings().getValue());
      foldMode_.setValue(prefs_.foldStyle().getValue());
      delimiterSurroundWidget_.setValue(prefs_.surroundSelection().getValue());
      executionBehavior_.setValue(prefs_.executionBehavior().getValue());
      autoSaveOnIdle_.setValue(prefs_.autoSaveOnIdle().getValue());

      // To prevent users from choosing nonsensical or pathological values for
      // the sensitive autosave idle option, act like they selected 1000ms (the
      // default) if they've managed to load something invalid.
      if (!autoSaveIdleMs_.setValue(prefs_.autoSaveIdleMs().getValue().toString()))
      {
         autoSaveIdleMs_.setValue("1000");
      }
   }

   @Override
   public RestartRequirement onApply(UserPrefs prefs)
   {
      RestartRequirement restartRequirement = super.onApply(prefs);

      // editing prefs
      prefs_.lineEndingConversion().setGlobalValue(lineEndings_.getValue());

      prefs_.defaultEncoding().setGlobalValue(encodingValue_);

      prefs_.codeCompletion().setGlobalValue(showCompletions_.getValue());
      prefs_.codeCompletionOther().setGlobalValue(showCompletionsOther_.getValue());

      String editorMode = editorMode_.getValue();

      prefs_.editorKeybindings().setGlobalValue(editorMode);
      boolean isVim = editorMode == UserPrefs.EDITOR_KEYBINDINGS_VIM;
      boolean isEmacs = editorMode == UserPrefs.EDITOR_KEYBINDINGS_EMACS;
      boolean isSublime = editorMode == UserPrefs.EDITOR_KEYBINDINGS_SUBLIME;

      if (isVim)
         ShortcutManager.INSTANCE.setEditorMode(KeyboardShortcut.MODE_VIM);
      else if (isEmacs)
         ShortcutManager.INSTANCE.setEditorMode(KeyboardShortcut.MODE_EMACS);
      else if (isSublime)
         ShortcutManager.INSTANCE.setEditorMode(KeyboardShortcut.MODE_SUBLIME);
      else
         ShortcutManager.INSTANCE.setEditorMode(KeyboardShortcut.MODE_DEFAULT);

      prefs_.foldStyle().setGlobalValue(foldMode_.getValue());
      prefs_.surroundSelection().setGlobalValue(delimiterSurroundWidget_.getValue());
      prefs_.executionBehavior().setGlobalValue(executionBehavior_.getValue());
      prefs_.autoSaveOnIdle().setGlobalValue(autoSaveOnIdle_.getValue());
      prefs_.autoSaveIdleMs().setGlobalValue(StringUtil.parseInt(autoSaveIdleMs_.getValue(), 1000));

      return restartRequirement;
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(PreferencesDialogBaseResources.INSTANCE.iconCodeEditing2x());
   }

   @Override
   public boolean validate()
   {
      return (!spacesForTab_.getValue() || tabWidth_.validate()) &&
             (!showMargin_.getValue() || marginCol_.validate()) &&
             alwaysCompleteChars_.validate() &&
             alwaysCompleteDelayMs_.validate() &&
             backgroundDiagnosticsDelayMs_.validate();
   }

   @Override
   public String getName()
   {
      return "Code";
   }

   private void setEncoding(String encoding)
   {
      encodingValue_ = encoding;
      if (StringUtil.isNullOrEmpty(encoding))
         encoding_.setText(ChooseEncodingDialog.ASK_LABEL);
      else
         encoding_.setText(encoding);
   }

   private final UserPrefs prefs_;
   private final SourceServerOperations server_;
   private final NumericValueWidget tabWidth_;
   private final NumericValueWidget marginCol_;
   private final LineEndingsSelectWidget lineEndings_;
   private final NumericValueWidget alwaysCompleteChars_;
   private final NumericValueWidget alwaysCompleteDelayMs_;
   private final NumericValueWidget backgroundDiagnosticsDelayMs_;
   private final CheckBox spacesForTab_;
   private final CheckBox showMargin_;
   private final SelectWidget showCompletions_;
   private final SelectWidget showCompletionsOther_;
   private final SelectWidget editorMode_;
   private final SelectWidget foldMode_;
   private final SelectWidget delimiterSurroundWidget_;
   private final SelectWidget executionBehavior_;
   private final SelectWidget autoSaveOnIdle_;
   private final SelectWidget autoSaveIdleMs_;
   private final TextBoxWithButton encoding_;
   private String encodingValue_;
}
