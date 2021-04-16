/*
 * EditingPreferencesPane.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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
import org.rstudio.studio.client.workbench.prefs.model.Prefs;
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
      editingPanel.add(tight(spacesForTab_ = checkboxPref(prefs.useSpacesForTab(),false /*defaultSpace*/)));
      editingPanel.add(indent(tabWidth_ = numericPref(1, UserPrefs.MAX_TAB_WIDTH, prefs.numSpacesForTab())));
      tabWidth_.setWidth("36px");
      editingPanel.add(checkboxPref(prefs_.autoDetectIndentation()));
      editingPanel.add(checkboxPref(prefs_.insertMatching()));
      editingPanel.add(checkboxPref(prefs_.insertNativePipeOperator()));
      editingPanel.add(checkboxPref(prefs_.reindentOnPaste()));
      editingPanel.add(checkboxPref(prefs_.verticallyAlignArgumentsIndent()));
      editingPanel.add(checkboxPref(prefs_.softWrapRFiles()));
      editingPanel.add(checkboxPref(prefs_.continueCommentsOnNewline()));
      editingPanel.add(checkboxPref(prefs_.highlightWebLink()));

      delimiterSurroundWidget_ = new SelectWidget((Prefs.EnumValue) prefs_.surroundSelection(),
         false,
         true,
         false);
      editingPanel.add(delimiterSurroundWidget_);

      HorizontalPanel keyboardPanel = new HorizontalPanel();
      editorMode_ = new SelectWidget((Prefs.EnumValue) prefs_.editorKeybindings(),
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
      editingPanel.add(checkboxPref(prefs_.focusConsoleAfterExec()));

      // DEBUG: Before i18n, this had ctrl+enter hard coded into the description as a way of indicating to reader what
      // this "execution" setting meant.  Current translation does not include that, but maybe we could map the
      // translated shortcut value here as well (ctrl+enter doesn't HAVE to be the shortcut, although it is probably a
      // good guess).  Alternative could be to add the text here or in the preference's title.
      executionBehavior_ = new SelectWidget((Prefs.EnumValue) prefs_.executionBehavior(),
            false,
            true,
            false);
      executionBehavior_.getElement().getStyle().setMarginBottom(0, Unit.PX);
      editingPanel.add(executionBehavior_);

      Label snippetsLabel = headerLabel("Snippets");
      snippetsLabel.getElement().getStyle().setMarginTop(8, Unit.PX);
      editingPanel.add(snippetsLabel);

      HorizontalPanel panel = new HorizontalPanel();
      CheckBox enableSnippets = checkboxPref(prefs_.enableSnippets());
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
      displayPanel.add(checkboxPref(prefs.highlightSelectedWord()));
      displayPanel.add(checkboxPref(prefs.highlightSelectedLine()));
      displayPanel.add(checkboxPref(prefs.showLineNumbers()));
      displayPanel.add(tight(showMargin_ = checkboxPref(prefs.showMargin(), false /*defaultSpace*/)));
      displayPanel.add(indent(marginCol_ = numericPref(prefs.marginColumn())));
      displayPanel.add(checkboxPref(prefs_.showInvisibles()));
      displayPanel.add(checkboxPref(prefs_.showIndentGuides()));
      displayPanel.add(checkboxPref(prefs_.blinkingCursor()));
      displayPanel.add(checkboxPref(prefs_.scrollPastEndOfDocument()));
      displayPanel.add(checkboxPref(prefs_.enableTextDrag()));
      displayPanel.add(checkboxPref(prefs_.highlightRFunctionCalls()));
      displayPanel.add(extraSpaced(
         checkboxPref(prefs_.rainbowParentheses(), false /* defaultSpace */)));

      foldMode_ = new SelectWidget((Prefs.EnumValue) prefs_.foldStyle(),
            false,
            true,
            false);

      displayPanel.add(foldMode_);

      VerticalTabPanel savePanel = new VerticalTabPanel(ElementIds.EDIT_SAVING_PREFS);

      savePanel.add(headerLabel("General"));
      savePanel.add(checkboxPref(prefs_.autoAppendNewline()));
      savePanel.add(checkboxPref(prefs_.stripTrailingWhitespace()));
      savePanel.add(checkboxPref(prefs_.restoreSourceDocumentCursorPosition()));

      Label serializationLabel = headerLabel("Serialization");
      serializationLabel.getElement().getStyle().setPaddingTop(14, Unit.PX);
      savePanel.add(serializationLabel);


      lineEndings_ = new LineEndingsSelectWidget();
      spaced(lineEndings_);
      savePanel.add(lineEndings_);

      encodingValue_ = prefs_.defaultEncoding().getGlobalValue();
      savePanel.add(lessSpaced(encoding_ = new TextBoxWithButton(
            prefs_.defaultEncoding().getTitle(),
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
      savePanel.add(checkboxPref(prefs.saveBeforeSourcing()));
      savePanel.add(checkboxPref(prefs_.autoSaveOnBlur()));
      autoSaveOnIdle_ = new SelectWidget((Prefs.EnumValue) prefs_.autoSaveOnIdle(),
            false,
            true,
            false);
      savePanel.add(autoSaveOnIdle_);
      autoSaveIdleMs_ = new SelectWidget(
         prefs.autoSaveIdleMs().getTitle(),
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

      showCompletions_ = new SelectWidget((Prefs.EnumValue) prefs_.codeCompletion(),
            false,
            true,
            false);

      spaced(showCompletions_);
      completionPanel.add(showCompletions_);

      final CheckBox alwaysCompleteInConsole = checkboxPref(prefs.consoleCodeCompletion());
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
           checkboxPref(prefs.insertParensAfterFunctionCompletion());

      final CheckBox showSignatureTooltipsCheckbox =
           checkboxPref(prefs.showFunctionSignatureTooltips());

      addEnabledDependency(
            insertParensAfterFunctionCompletionsCheckbox,
            showSignatureTooltipsCheckbox);

      completionPanel.add(insertParensAfterFunctionCompletionsCheckbox);
      completionPanel.add(showSignatureTooltipsCheckbox);

      completionPanel.add(checkboxPref(prefs.showHelpTooltipOnIdle()));
      completionPanel.add(checkboxPref(prefs.insertSpacesAroundEquals()));
      completionPanel.add(checkboxPref(prefs.tabCompletion()));
      completionPanel.add(checkboxPref(prefs.tabMultilineCompletion()));


      Label otherLabel = headerLabel("Other Languages");
      otherLabel.getElement().getStyle().setMarginTop(8, Unit.PX);
      completionPanel.add(otherLabel);

      showCompletionsOther_ = new SelectWidget((Prefs.EnumValue) prefs_.codeCompletionOther(),
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
          numericPref(1, 99, prefs.codeCompletionCharacters())));
      completionPanel.add(nudgeRightPlus(alwaysCompleteDelayMs_ =
          numericPref(0, 9999, prefs.codeCompletionDelay())));


      VerticalTabPanel diagnosticsPanel = new VerticalTabPanel(ElementIds.EDIT_DIAGNOSTICS_PREFS);
      diagnosticsPanel.add(headerLabel("R Diagnostics"));
      final CheckBox chkShowRDiagnostics = checkboxPref(prefs.showDiagnosticsR());
      diagnosticsPanel.add(chkShowRDiagnostics);

      final VerticalPanel rOptionsPanel = new VerticalPanel();
      rOptionsPanel.add(checkboxPref(prefs.diagnosticsInRFunctionCalls()));
      rOptionsPanel.add(checkboxPref(prefs.checkArgumentsToRFunctionCalls()));
      rOptionsPanel.add(checkboxPref(prefs.checkUnexpectedAssignmentInFunctionCall()));
      rOptionsPanel.add(checkboxPref(prefs.warnIfNoSuchVariableInScope()));
      rOptionsPanel.add(checkboxPref(prefs.warnVariableDefinedButNotUsed()));
      rOptionsPanel.add(checkboxPref(prefs.styleDiagnostics()));
      rOptionsPanel.setVisible(prefs.showDiagnosticsR().getValue());
      chkShowRDiagnostics.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            rOptionsPanel.setVisible(event.getValue());
         }
      });

      diagnosticsPanel.add(rOptionsPanel);
      diagnosticsPanel.add(checkboxPref(prefs.autoDiscoverPackageDependencies()));

      Label diagOtherLabel = headerLabel("Other Languages");
      diagnosticsPanel.add(spacedBefore(diagOtherLabel));
      diagnosticsPanel.add(checkboxPref(prefs.showDiagnosticsCpp()));
      diagnosticsPanel.add(checkboxPref(prefs.showDiagnosticsOther()));

      Label diagShowLabel = headerLabel("Show Diagnostics");
      diagnosticsPanel.add(spacedBefore(diagShowLabel));
      diagnosticsPanel.add(checkboxPref(prefs.diagnosticsOnSave()));
      diagnosticsPanel.add(tight(checkboxPref(prefs.backgroundDiagnostics(), false /*defaultSpace*/)));
      diagnosticsPanel.add(indent(backgroundDiagnosticsDelayMs_ =
            numericPref(0, 9999, prefs.backgroundDiagnosticsDelayMs())));

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
