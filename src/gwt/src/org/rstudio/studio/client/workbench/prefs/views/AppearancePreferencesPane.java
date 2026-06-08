/*
 * AppearancePreferencesPane.java
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.ThemeFonts;
import org.rstudio.core.client.widget.FontDetector;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.LayoutGrid;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.NumericTextBox;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.core.client.widget.VerticalSpacer;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.DesktopInfo;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.projects.ui.prefs.events.ProjectOptionsChangedEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceTheme;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceThemes;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.model.ThemeServerOperations;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

public class AppearancePreferencesPane extends PreferencesPane
{
   static final String[] ZOOM_VALUES = new String[] {
           "0.25", "0.50", "0.75", "0.80", "0.90",
           "1.00", "1.10", "1.25", "1.50", "1.75",
           "2.00", "2.50", "3.00", "4.00", "5.00"
   };
   
   private static class NumericInput extends NumericTextBox
   {
      public NumericInput(Integer min, Integer max, Integer step)
      {
         super(min, max, step);
         setWidth("48px");
         getElement().addClassName(ModalDialogBase.ALLOW_ENTER_KEY_CLASS);
      }
   }

   // NOTE: this pane uses a custom layout (left panel + theme preview) and
   // intentionally does not call wrapWithPanel().
   @Inject
   public AppearancePreferencesPane(PreferencesDialogResources res,
                                    UserPrefs userPrefs,
                                    UserState userState,
                                    final AceThemes themes,
                                    WorkbenchContext workbenchContext,
                                    GlobalDisplay globalDisplay,
                                    DependencyManager dependencyManager,
                                    FileDialogs fileDialogs,
                                    ThemeServerOperations server,
                                    EventBus events,
                                    Session session,
                                    Commands commands)
   {
      res_ = res;
      userPrefs_ = userPrefs;
      userState_ = userState;
      globalDisplay_ = globalDisplay;
      dependencyManager_ = dependencyManager;
      server_ = server;
      events_ = events;
      commands_ = commands;
      hasActiveProject_ = session.getSessionInfo().getActiveProjectFile() != null;

      VerticalPanel leftPanel = new VerticalPanel();

      relaunchRequired_ = false;

      // dark-grey theme and classic themes no longer exist; map them to defaults
      if (StringUtil.equals(userPrefs_.globalTheme().getValue(), "dark-grey") ||
          StringUtil.equals(userPrefs_.globalTheme().getValue(), "classic"))
        userPrefs_.globalTheme().setGlobalValue(UserPrefs.GLOBAL_THEME_DEFAULT);

      @SuppressWarnings("unused")
      final String originalTheme = userPrefs_.globalTheme().getValue();

      flatTheme_ = new SelectWidget(
            constants_.appearanceRStudioThemeLabel(),
            new String[] {
                  constants_.modernThemeLabel(),
                  constants_.skyThemeLabel()
            },
            new String[] {
                  UserPrefs.GLOBAL_THEME_DEFAULT,
                  UserPrefs.GLOBAL_THEME_ALTERNATE
            },
            false);
      flatTheme_.addStyleName(res.styles().themeChooser());
      flatTheme_.getListBox().setWidth("95%");

      String themeAlias = userPrefs_.globalTheme().getGlobalValue();
      flatTheme_.setValue(themeAlias);

      leftPanel.add(flatTheme_);

      if (Desktop.hasDesktopFrame())
      {
         String[] zoomLabels = Arrays.stream(ZOOM_VALUES)
                 .map(zoomValue -> StringUtil.formatPercent(Double.parseDouble(zoomValue)))
                 .toArray(String[]::new);
         double currentZoomLevel = DesktopInfo.getZoomLevel();

         zoomLevel_ = new SelectWidget(constants_.appearanceZoomLabelZoom(),
                 zoomLabels,
                 ZOOM_VALUES,
                 false);
         zoomLevel_.getListBox().setWidth("95%");
         zoomLevel_.getListBox().addChangeHandler(event -> updatePreviewZoomLevel());

         if (BrowseCap.isElectron())
         {
            Desktop.getFrame().getZoomLevel(zoomLevel ->
            {
               int initialIndex = getInitialZoomIndex(zoomLevel);
               zoomLevel_.getListBox().setSelectedIndex(initialIndex);
            });
         }
         else
         {
            int initialIndex = getInitialZoomIndex(currentZoomLevel);
            zoomLevel_.getListBox().setSelectedIndex(initialIndex);
            initialZoomLevel_ = ZOOM_VALUES[initialIndex];
         }

         leftPanel.add(zoomLevel_);
      }

      String[] fonts = new String[] {};

      if (Desktop.isDesktop())
      {
         // In desktop mode, get the list of installed fonts from Electron
         String fontList = DesktopInfo.getFixedWidthFontList();

         if (fontList.isEmpty())
            registerFontListReadyCallback();
         else
            fonts = fontList.split("\\n");
      }
      else
      {
         // In server mode, get the installed set of fonts by querying the server
         getInstalledFontList();
      }

      String fontFaceLabel = fonts.length == 0
            ? constants_.fontFaceEditorFontLabel()
            : constants_.appearanceEditorFontLabel();

      fontFace_ = new SelectWidget(fontFaceLabel, fonts, fonts, false, false, false);
      fontFace_.getListBox().setWidth("95%");

      if (Desktop.isDesktop())
      {
         // Get the fixed width font set in desktop mode
         String value = DesktopInfo.getFixedWidthFont();
         String label = value.replaceAll("\\\"", "");
         if (!fontFace_.setValue(label))
         {
            fontFace_.insertValue(0, label, value);
            fontFace_.setValue(value);
         }
      }
      else
      {
         // In server mode, there's always a Default option which uses a
         // browser-specific font.
         fontFace_.insertValue(0, DEFAULT_FONT_NAME, DEFAULT_FONT_VALUE);
      }

      initialFontFace_ = StringUtil.notNull(fontFace_.getValue());

      fontFace_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            String font = fontFace_.getValue();
            if (font == null || StringUtil.equals(font, DEFAULT_FONT_VALUE))
            {
               preview_.setFont(ThemeFonts.getFixedWidthFont(), false);
            }
            else
            {
               preview_.setFont(font, !Desktop.hasDesktopFrame());
            }
         }
      });

      Double fontSize = userPrefs.fontSizePoints().getValue();
      if (fontSize == 0.0)
         fontSize = 10.0;
      
      initialEditorFontSize_ = fontSize;
      editorFontSize_ = new NumericInput(6, 72, null);
      editorFontSize_.setValue(String.valueOf(fontSize));
      editorFontSize_.addValueChangeHandler(new ValueChangeHandler<String>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            preview_.setFontSize(Double.parseDouble(editorFontSize_.getValue()));
         }
      });
      
      Double lineHeight = userPrefs.editorLineHeight().getValue();
      if (lineHeight == null || lineHeight == 0.0)
         lineHeight = (double) Math.round(FontSizer.getNormalLineHeight() * 100.0);
      
      initialEditorLineHeight_ = lineHeight;
      editorLineHeight_ = new NumericInput(20, 400, 5);
      editorLineHeight_.setValue(String.valueOf(lineHeight));
      editorLineHeight_.addValueChangeHandler(new ValueChangeHandler<String>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            preview_.setLineHeight(Double.parseDouble(editorLineHeight_.getValue()));
         }
      });
      
      Double helpFontSize = userPrefs.helpFontSizePoints().getValue();
      if (helpFontSize == null || helpFontSize == 0.0)
         helpFontSize = 10.0;
      
      initialHelpFontSize_ = helpFontSize;
      helpFontSize_ = new NumericInput(6, 72, null);
      helpFontSize_.setValue(String.valueOf(helpFontSize));
      
      LayoutGrid editorGrid = new LayoutGrid(3, 2);
      editorGrid.setWidth("100%");
      editorGrid.setWidget(0, 0, new Label(constants_.appearanceEditorFontSizeLabel()));
      editorGrid.setWidget(0, 1, editorFontSize_);
      editorGrid.setWidget(1, 0, new Label(constants_.appearanceEditorLineHeightLabel()));
      editorGrid.setWidget(1, 1, editorLineHeight_);
      editorGrid.setWidget(2, 0, new Label(constants_.helpFontSizeLabel()));
      editorGrid.setWidget(2, 1, helpFontSize_);
      
      textRendering_ = new SelectWidget(
            constants_.textRenderingLabel(),
            new String[] {
                  constants_.defaultInParentheses(),
                  constants_.geometricPrecision(),
            },
            new String[] {
                  UserPrefsAccessor.TEXT_RENDERING_AUTO,
                  UserPrefsAccessor.TEXT_RENDERING_GEOMETRICPRECISION
            },
            false);
      
      textRendering_.getListBox().setWidth("95%");
      textRendering_.setValue(userPrefs_.textRendering().getGlobalValue());
      textRendering_.getListBox().addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            preview_.getDocument().getBody().getStyle().setProperty(
                  "textRendering",
                  textRendering_.getValue());
         }
      });

      theme_ = new SelectWidget(
            constants_.appearanceEditorThemeLabel(),
            new String[0],
            new String[0],
            false);
      
      theme_.getListBox().getElement().<SelectElement>cast().setSize(7);
      theme_.getListBox().getElement().getStyle().setHeight(themeSelectorHeight(), Unit.PX);
      theme_.getListBox().addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            AceTheme aceTheme = themeList_.get(theme_.getValue());
            preview_.setTheme(aceTheme.getUrl());
            removeThemeButton_.setEnabled(!aceTheme.isDefaultTheme());
         }
      });
      theme_.addStyleName(res.styles().themeChooser());
      // SelectWidget.setElementId sets the id on the underlying <select> and
      // keeps its label associated (unlike assignElementId, which would land on
      // the composite wrapper).
      theme_.setElementId(ElementIds.getUniqueElementId(ElementIds.APPEARANCE_EDITOR_THEME));

      AceTheme currentTheme = userState_.theme().getGlobalValue().cast();
      addThemeButton_ = new ThemedButton(constants_.addThemeButtonLabel(), event ->
         fileDialogs.openFile(
            constants_.addThemeButtonCaption(),
            RStudioGinjector.INSTANCE.getRemoteFileSystemContext(),
            workbenchContext.getCurrentWorkingDir(), constants_.addThemeButtonCaption(),
            (input, indicator) ->
            {
               if (input == null)
                  return;

               String inputStem = input.getStem();
               String inputPath = input.getPath();
               boolean isTmTheme = StringUtil.equalsIgnoreCase(".tmTheme", input.getExtension());
               boolean found = false;
               for (AceTheme theme: themeList_.values())
               {
                  if (theme.isLocalCustomTheme() &&
                     StringUtil.equalsIgnoreCase(theme.getFileStem(), inputStem))
                  {
                     showThemeExistsDialog(inputStem, () -> addTheme(inputPath, themes, isTmTheme));
                     found = true;
                     break;
                  }
               }

               if (!found)
               {
                  addTheme(inputPath, themes, isTmTheme);
               }

               indicator.onCompleted();
            }));
      addThemeButton_.setLeftAligned(true);
      removeThemeButton_ = new ThemedButton(
         constants_.removeThemeButtonLabel(),
         event -> showRemoveThemeWarning(
            theme_.getValue(),
            () -> removeTheme(theme_.getValue(), themes)));
      removeThemeButton_.setLeftAligned(true);
      removeThemeButton_.setEnabled(!currentTheme.isDefaultTheme());

      themeButtonsPanel_ = new HorizontalPanel();
      themeButtonsPanel_.add(addThemeButton_);
      themeButtonsPanel_.add(removeThemeButton_);

      // Shown in place of the theme selector when the active project sets its
      // own editor theme. The global selection is ignored while that override
      // is in effect, so keep the "Editor theme:" label and, below it, explain
      // the override and point the user at the project options. Stacked
      // vertically because the left column is too narrow to fit the message and
      // button side by side.
      PreferencesDialogBaseResources baseRes = PreferencesDialogBaseResources.INSTANCE;
      projectThemeOverridePanel_ = new VerticalPanel();
      projectThemeOverridePanel_.setWidth("100%");
      ElementIds.assignElementId(projectThemeOverridePanel_,
         ElementIds.APPEARANCE_EDITOR_THEME_PROJECT_OVERRIDE);

      FormLabel projectOverrideThemeLabel =
         new FormLabel(constants_.appearanceEditorThemeLabel());
      projectThemeOverridePanel_.add(projectOverrideThemeLabel);

      Label projectOverrideMessage =
         new Label(constants_.appearanceEditorThemeProjectOverrideText());
      projectOverrideMessage.addStyleName(baseRes.styles().infoLabel());
      projectOverrideMessage.setWidth("100%");
      // Breathe a little between the "Editor theme:" heading and the message.
      projectOverrideMessage.getElement().getStyle().setMarginTop(4, Unit.PX);
      projectThemeOverridePanel_.add(projectOverrideMessage);

      SmallButton editProjectOptions =
         new SmallButton(constants_.editProjectPreferencesButtonLabel());
      editProjectOptions.getElement().getStyle().setMarginTop(6, Unit.PX);
      editProjectOptions.addClickHandler(event -> commands_.projectOptions().execute());
      projectThemeOverridePanel_.add(editProjectOptions);

      projectThemeOverridePanel_.setVisible(false);

      leftPanel.add(textRendering_);
      leftPanel.add(fontFace_);
      leftPanel.add(editorGrid);
      leftPanel.add(new VerticalSpacer("12px"));
      leftPanel.add(theme_);
      leftPanel.add(themeButtonsPanel_);
      leftPanel.add(projectThemeOverridePanel_);

      FlowPanel previewPanel = new FlowPanel();

      int previewWidth = PreferencesDialogConstants.PANEL_CONTAINER_WIDTH - 312;
      // Reserve room below the two columns for the "ignore project appearance"
      // checkbox so it fits without overflowing the pane.
      int previewHeight = PreferencesDialogConstants.PANEL_CONTAINER_HEIGHT - 38 - IGNORE_APPEARANCE_ROW_HEIGHT;
      previewPanel.setSize("100%", "100%");
      preview_ = new AceEditorPreview(RES.codeSample().getText());
      preview_.setWidth(previewWidth + "px");
      preview_.setHeight(previewHeight + "px");
      preview_.setFontSize(Double.parseDouble(editorFontSize_.getValue()));
      preview_.setLineHeight(Double.parseDouble(editorLineHeight_.getValue()));
      preview_.setTheme(currentTheme.getUrl());
      updatePreviewZoomLevel();
      previewPanel.add(preview_);

      HorizontalPanel hpanel = new HorizontalPanel();
      hpanel.setWidth("100%");
      hpanel.add(leftPanel);
      hpanel.setCellWidth(leftPanel, "160px");
      hpanel.add(previewPanel);

      add(hpanel);

      // "Ignore project-specific appearance settings" -- a global opt-out that
      // makes RStudio always use the global editor theme even when the active
      // project sets its own. checkboxPref persists the value in the base
      // onApply(); the visible effect (reverting the applied theme, restoring
      // the selector) is handled in this pane's onApply(), so toggling the box
      // alone changes nothing until OK/Apply.
      ignoreProjectAppearance_ = checkboxPref(
            constants_.ignoreProjectAppearanceLabel(),
            userPrefs_.ignoreProjectAppearance());
      initialIgnoreProjectAppearance_ = userPrefs_.ignoreProjectAppearance().getGlobalValue();
      // Breathe a little above the checkbox so it isn't cramped against the
      // two-column area.
      add(new VerticalSpacer("8px"));
      add(ignoreProjectAppearance_);

      // Themes are retrieved asynchronously, so we have to update the theme list and preview panel
      // asynchronously too. We also need to wait until the next event cycle so that the progress
      // indicator will be ready.
      Scheduler.get().scheduleDeferred(() -> setThemes(themes));
   }
   
   // It looks like theme components are larger in desktop, so we need
   // to adjust the height of certain UI elements to ensure everything
   // can fit.
   //
   // https://github.com/rstudio/rstudio/issues/13154
   private int themeSelectorHeight()
   {
      if (Desktop.isDesktop())
      {
         return 200;
      }
      else
      {
         return 250;
      }
   }

   private int getInitialZoomIndex(double currentZoomLevel)
   {
      int initialIndex = -1;
      int normalIndex = -1;

      for (int i = 0; i < ZOOM_VALUES.length; i++)
      {
         double zoomValue = Double.parseDouble(ZOOM_VALUES[i]);

         if (zoomValue == 1.0)
            normalIndex = i;

         if (zoomValue == currentZoomLevel)
            initialIndex = i;
      }

      return initialIndex == -1 ? normalIndex : initialIndex;
   }

   @Override
   protected void setPaneVisible(boolean visible)
   {
      super.setPaneVisible(visible);
      if (visible)
      {
         // When making the pane visible in desktop mode, add or remove a
         // meaningless transform to the iframe hosting the preview. This is
         // gross but necessary to work around a QtWebEngine bug which causes
         // the region to not paint at all (literally showing the previous
         // contents of the screen buffer) until invalidated in some way.
         //
         // Known to be an issue with Qt 5.12.8/Chromium 69; could be removed if
         // the bug is fixed in later releases.
         //
         // See https://github.com/rstudio/rstudio/issues/6268

         Scheduler.get().scheduleDeferred(() ->
         {
            Style style = preview_.getElement().getStyle();
            String translate = "translate(0px, 0px)";
            String transform = style.getProperty("transform");
            style.setProperty("transform",
                    StringUtil.isNullOrEmpty(transform) || !StringUtil.equals(translate, transform) ?
                        translate :
                        "");
         });
      }
   }

   private void removeTheme(String themeName, AceThemes themes, Operation afterOperation)
   {
      AceTheme currentTheme = userState_.theme().getGlobalValue().cast();
      if (StringUtil.equalsIgnoreCase(currentTheme.getName(), themeName))
      {
         showCantRemoveActiveThemeDialog(currentTheme.getName());
      }
      else
      {
         themes.removeTheme(
            themeName,
            errorMessage -> showCantRemoveThemeDialog(themeName, errorMessage),
            () ->
            {
               updateThemes(currentTheme.getName(), themes);
               afterOperation.execute();
            });
      }
   }

   private void removeTheme(String themeName, AceThemes themes)
   {
      // No after operation necessary.
      removeTheme(themeName, themes, () -> {});
   }

   private void doAddTheme(String inputPath, AceThemes themes, boolean isTmTheme)
   {
      if (isTmTheme)
         dependencyManager_.withThemes(
            constants_.addThemeUserActionLabel(),
            () -> themes.addTheme(
               inputPath,
               result -> updateThemes(result, themes),
               error -> showCantAddThemeDialog(inputPath, error)));
      else
         themes.addTheme(
            inputPath,
            result -> updateThemes(result, themes),
            error -> showCantAddThemeDialog(inputPath, error));

   }

   private void addTheme(String inputPath, AceThemes themes, boolean isTmTheme)
   {
      // Get the theme name and check if it's in the current list of themes.
      themes.getThemeName(
         inputPath,
         name ->
         {
            if (themeList_.containsKey(name))
            {
               if (themeList_.get(name).isLocalCustomTheme())
               {
                  showDuplicateThemeError(
                     name,
                     () -> removeTheme(
                        name,
                        themes,
                        () -> doAddTheme(inputPath, themes, isTmTheme)));
               }
               else
               {
                  showDuplicateThemeWarning(
                     name,
                     () -> doAddTheme(inputPath, themes, isTmTheme));
               }
            }
            else
            {
               doAddTheme(inputPath, themes, isTmTheme);
            }
         },
         error -> showCantAddThemeDialog(inputPath, error));
   }

   private void setThemes(AceThemes themes)
   {
      themes.getThemes(
         themeList ->
         {
            themeList_ = themeList;

            // Seed the selector from the user's stored global theme preference,
            // not from userState_.theme(), which may reflect a project override.
            String globalThemeName = userPrefs_.editorTheme().getGlobalValue();

            // It's possible the current theme was removed outside the context of
            // RStudio, so choose a default if it can't be found.
            if (!themeList_.containsKey(globalThemeName))
            {
               // Determine whether the missing theme was dark so we can fall
               // back to the matching default variant.
               AceTheme missingTheme = userState_.theme().getGlobalValue().cast();
               boolean wasDark = missingTheme != null && missingTheme.isDark();

               StringBuilder warningMsg = new StringBuilder();
               warningMsg.append(constants_.setThemeWarningMessage(globalThemeName, wasDark ? constants_.themeWarningMessageDarkLabel() : constants_.themeWarningMessageLightLabel()));

               AceTheme defaultTheme = AceTheme.createDefault(wasDark);
               // Do not write userState_.theme() here: seeding the pane must not mutate
               // the applied theme. syncThemePrefs() already resolves a valid theme on
               // startup, and a project override may currently be active in
               // userState_.theme(). The selector/preview below show the default; the
               // user's OK persists it.

               warningMsg.append(defaultTheme.getName())
                  .append("\".");
               Debug.logWarning(warningMsg.toString());

               globalThemeName = defaultTheme.getName();
            }

            theme_.setChoices(themeList_.keySet().toArray(new String[0]));
            theme_.setValue(globalThemeName);

            // Enable removal only for the (global) selection shown in the list.
            AceTheme globalTheme = themeList_.get(globalThemeName);
            removeThemeButton_.setEnabled(globalTheme != null && !globalTheme.isDefaultTheme());

            // Toggle the project-override UI and point the preview at the theme
            // that is actually in effect (the project's theme when overriding).
            updateProjectThemeOverride();

            // If the user changed a theme-affecting setting (e.g. toggled
            // "ignore project appearance") and applied it before this list
            // loaded, onApply couldn't resolve a live theme then; do it now.
            if (pendingThemeApply_)
            {
               pendingThemeApply_ = false;
               initialIgnoreProjectAppearance_ =
                  userPrefs_.ignoreProjectAppearance().getGlobalValue();
               applyEffectiveThemeLive();
            }
         },
         getProgressIndicator());
   }

   // The active project's editor theme overrides the global selection. The
   // project pref layer is only populated while a project is open, so a
   // non-empty project value implies a project is active; the explicit guard
   // keeps that intent clear.
   private boolean isProjectThemeOverrideActive()
   {
      if (!hasActiveProject_)
         return false;

      // When the user has opted to ignore project appearance settings, the
      // project's editor theme is not applied, so there is no override to show.
      // Read the persisted pref (not the checkbox) so that merely toggling the
      // checkbox doesn't change the pane until OK/Apply persists it.
      if (userPrefs_.ignoreProjectAppearance().getGlobalValue())
         return false;

      return userPrefs_.editorTheme().hasProjectValue() &&
             !StringUtil.isNullOrEmpty(userPrefs_.editorTheme().getProjectValue());
   }

   // When a project theme override is active, hide the theme selector (the
   // global choice is ignored) and show an indicator pointing at project
   // options; otherwise show the normal selector and Add/Remove buttons.
   private void updateProjectThemeOverride()
   {
      boolean overrideActive = isProjectThemeOverrideActive();
      theme_.setVisible(!overrideActive);
      themeButtonsPanel_.setVisible(!overrideActive);
      projectThemeOverridePanel_.setVisible(overrideActive);
      updatePreviewTheme();
   }

   // Point the preview at the theme that is actually applied: the effective
   // (project) theme when overriding, otherwise the global selection.
   private void updatePreviewTheme()
   {
      if (themeList_ == null)
         return;

      AceTheme theme;
      if (isProjectThemeOverrideActive())
      {
         // effective theme: project value if installed, else global, else default
         theme = AceTheme.resolveApplied(themeList_,
            userPrefs_.editorTheme().getValue(),
            userPrefs_.editorTheme().getGlobalValue());
      }
      else
      {
         theme = themeList_.get(theme_.getValue());
      }

      if (theme != null)
         preview_.setTheme(theme.getUrl());
   }

   // Resolve the effective editor theme (the project override when active, else
   // the global selection) and apply it to the live editor via user state.
   // Requires the theme list to be loaded; updating the preview and the
   // selector vs. override-indicator UI is the caller's job (via
   // updateProjectThemeOverride()).
   private void applyEffectiveThemeLive()
   {
      if (themeList_ == null)
         return;

      String preferred = isProjectThemeOverrideActive()
         ? userPrefs_.editorTheme().getValue()
         : userPrefs_.editorTheme().getGlobalValue();
      AceTheme applied = AceTheme.resolveApplied(themeList_, preferred,
         userPrefs_.editorTheme().getGlobalValue());
      if (applied != null)
         userState_.theme().setGlobalValue(applied);
   }

   private void updateThemes(String focusedThemeName, AceThemes themes)
   {
      themes.getThemes(
         themeList->
         {
            themeList_ = themeList;

            String themeName = focusedThemeName;
            if (!themeList.containsKey(themeName))
            {
               Debug.logWarning("The theme \"" + focusedThemeName + "\" does not exist. It may have been manually deleted outside the context of RStudio.");
               themeName = AceTheme.createDefault().getName();
            }
            AceTheme focusedTheme = themeList.get(themeName);

            theme_.setChoices(themeList_.keySet().toArray(new String[0]));
            theme_.setValue(focusedTheme.getName());
            preview_.setTheme(focusedTheme.getUrl());
            removeThemeButton_.setEnabled(!focusedTheme.isDefaultTheme());
         },
         getProgressIndicator());
   }

   private void updatePreviewZoomLevel()
   {
      // no zoom preview on desktop
      if (Desktop.hasDesktopFrame())
      {
         if (BrowseCap.isElectron())
         {
            Desktop.getFrame().getZoomLevel(currentZoomLevel ->
                    preview_.setZoomLevel(Double.parseDouble(zoomLevel_.getValue()) / currentZoomLevel)
            );
         }
         else
         {
            preview_.setZoomLevel(Double.parseDouble(zoomLevel_.getValue()) / DesktopInfo.getZoomLevel());
         }
      }
   }

   private void showThemeExistsDialog(String inputFileName, Operation continueOperation)
   {
      StringBuilder msg = new StringBuilder();
      msg.append(constants_.showThemeExistsDialogLabel(inputFileName));
      globalDisplay_.showYesNoMessage(
         GlobalDisplay.MSG_WARNING,
         constants_.globalDisplayThemeExistsCaption(),
         msg.toString(),
         continueOperation,
         false);
   }

   private void showCantAddThemeDialog(String themePath, String errorMessage)
   {
      StringBuilder msg = new StringBuilder();
      msg.append(constants_.cantAddThemeMessage())
         .append(themePath)
         .append(constants_.cantAddThemeErrorCaption())
         .append(errorMessage);

      globalDisplay_.showErrorMessage(constants_.cantAddThemeGlobalMessage(), msg.toString());
   }

   private void showCantRemoveThemeDialog(String themeName, String errorMessage)
   {
      StringBuilder msg = new StringBuilder();
      msg.append(constants_.showCantRemoveThemeDialogMessage(themeName, errorMessage));

      globalDisplay_.showErrorMessage(constants_.showCantRemoveErrorMessage(), msg.toString());
   }

   private void showCantRemoveActiveThemeDialog(String themeName)
   {
      StringBuilder msg = new StringBuilder();
      msg.append(constants_.showCantRemoveActiveThemeDialog(themeName));

      globalDisplay_.showErrorMessage(constants_.showCantRemoveThemeCaption(), msg.toString());
   }

   private void showRemoveThemeWarning(String themeName, Operation continueOperation)
   {
      StringBuilder msg = new StringBuilder();
      msg.append(constants_.showRemoveThemeWarningMessage(themeName));

      globalDisplay_.showYesNoMessage(
         GlobalDisplay.MSG_WARNING,
         constants_.showRemoveThemeGlobalMessage(),
         msg.toString(),
         continueOperation,
         false);
   }

   private void showDuplicateThemeError(String themeName, Operation continueOperation)
   {
      StringBuilder msg = new StringBuilder();
      msg.append(constants_.showDuplicateThemeErrorMessage(themeName));

      globalDisplay_.showYesNoMessage(
         GlobalDisplay.MSG_ERROR,
         constants_.showDuplicateThemeDuplicateGlobalMessage(),
         msg.toString(),
         continueOperation,
         false);
   }

   private void showDuplicateThemeWarning(String themeName, Operation continueOperation)
   {
      StringBuilder msg = new StringBuilder();
      msg.append(constants_.showDuplicateThemeWarningMessage(themeName));

      globalDisplay_.showYesNoMessage(
         GlobalDisplay.MSG_WARNING,
         constants_.showDuplicateThemeGlobalMessage(),
         msg.toString(),
         continueOperation,
         true);
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();
      // Re-evaluate the project-override UI whenever project options are saved
      // (e.g. via the "Edit Project Options..." button); the project pref layer
      // is updated before this event fires, so reading it here is accurate.
      // Registered on attach and torn down in onUnload so a detach/reattach
      // keeps the handler live.
      if (projectOptionsChangedHandler_ == null)
         projectOptionsChangedHandler_ = events_.addHandler(
            ProjectOptionsChangedEvent.TYPE, event -> updateProjectThemeOverride());
   }

   @Override
   public void onUnload()
   {
      if (projectOptionsChangedHandler_ != null)
      {
         projectOptionsChangedHandler_.removeHandler();
         projectOptionsChangedHandler_ = null;
      }
      super.onUnload();
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(res_.iconAppearance2x());
   }

   @Override
   protected void initialize(UserPrefs prefs)
   {
   }

   @Override
   public RestartRequirement onApply(UserPrefs rPrefs)
   {
      RestartRequirement restartRequirement = super.onApply(rPrefs);

      if (relaunchRequired_)
         restartRequirement.setUiReloadRequired(true);
      
      String textRendering = textRendering_.getValue();
      if (!StringUtil.equals(textRendering, userPrefs_.textRendering().getGlobalValue()))
      {
         userPrefs_.textRendering().setGlobalValue(textRendering);
         Document.get().getBody().getStyle().setProperty("textRendering", textRendering);
      }

      String themeName = flatTheme_.getValue();
      if (!StringUtil.equals(themeName, userPrefs_.globalTheme().getGlobalValue()))
         userPrefs_.globalTheme().setGlobalValue(themeName, false);

      double fontSize = Double.parseDouble(editorFontSize_.getValue());
      if (fontSize != initialEditorFontSize_)
      {
         userPrefs_.fontSizePoints().setGlobalValue(fontSize);
         initialEditorFontSize_ = fontSize;
      }
      
      double lineHeight = Double.parseDouble(editorLineHeight_.getValue());
      if (lineHeight != initialEditorLineHeight_)
      {
         userPrefs_.editorLineHeight().setGlobalValue(lineHeight);
         initialEditorLineHeight_ = lineHeight;
      }
      
      double helpFontSize = Double.parseDouble(helpFontSize_.getValue());
      if (helpFontSize != initialHelpFontSize_)
      {
         userPrefs_.helpFontSizePoints().setGlobalValue(helpFontSize);
         initialEditorFontSize_ = helpFontSize;
      }

      // The editor-theme selector is only meaningful when visible; an active
      // project override hides it. themeList_ is populated asynchronously from
      // setThemes(), so if the user clicks OK before that lands there's nothing
      // in the dropdown to apply.
      boolean globalThemeChanged =
         themeList_ != null &&
         theme_.isVisible() &&
         !StringUtil.equals(theme_.getValue(), userPrefs_.editorTheme().getGlobalValue());
      if (globalThemeChanged)
         userPrefs_.editorTheme().setGlobalValue(theme_.getValue(), false);

      // "Ignore project appearance settings" was already persisted by
      // super.onApply(); detect whether it changed since the pane was seeded so
      // we can re-resolve the applied theme (e.g. revert a project override back
      // to the global theme) and refresh the pane to match.
      boolean ignoreChanged =
         userPrefs_.ignoreProjectAppearance().getGlobalValue() != initialIgnoreProjectAppearance_;

      if (globalThemeChanged || ignoreChanged)
      {
         if (themeList_ != null)
         {
            initialIgnoreProjectAppearance_ = userPrefs_.ignoreProjectAppearance().getGlobalValue();

            // Reflect the (possibly new) override state in the pane -- show the
            // selector vs. the override indicator and point the preview at the
            // applied theme. On Apply this updates the still-visible pane
            // immediately; on OK it refreshes a pane that is about to close.
            updateProjectThemeOverride();
            applyEffectiveThemeLive();
         }
         else
         {
            // The theme list hasn't loaded, so the AceTheme (url/isDark) can't be
            // resolved yet. Defer the live re-resolution to setThemes(), which
            // runs once the list arrives. initialIgnoreProjectAppearance_ is left
            // unchanged so the change is still seen as pending.
            pendingThemeApply_ = true;
         }
      }

     if (!StringUtil.equals(initialFontFace_, fontFace_.getValue()))
     {
        String fontFace = fontFace_.getValue();
        initialFontFace_ = fontFace;
        if (Desktop.hasDesktopFrame())
        {
           // In desktop mode the font is stored in a per-machine file since
           // the font list varies between machines.
           Desktop.getFrame().setFixedWidthFont(fontFace);
        }
        else
        {
           if (StringUtil.equals(fontFace, DEFAULT_FONT_VALUE))
           {
              // User has chosen the default font face
              userPrefs_.serverEditorFontEnabled().setGlobalValue(false);
           }
           else
           {
              // User has chosen a specific font
              userPrefs_.serverEditorFontEnabled().setGlobalValue(true);
              userPrefs_.serverEditorFont().setGlobalValue(fontFace);
           }
        }
        restartRequirement.setUiReloadRequired(true);
     }

      if (Desktop.hasDesktopFrame())
      {
         if (!StringUtil.equals(initialZoomLevel_, zoomLevel_.getValue()))
         {
            double zoomLevel = Double.parseDouble(zoomLevel_.getValue());
            initialZoomLevel_ = zoomLevel_.getValue();
            Desktop.getFrame().setZoomLevel(zoomLevel);
         }
      }

      return restartRequirement;
   }

   @Override
   public String getName()
   {
      return constants_.appearanceLabel();
   }

   private final native void registerFontListReadyCallback()
   /*-{

      var self = this;
      $wnd.onFontListReady = $entry(function() {
         self.@org.rstudio.studio.client.workbench.prefs.views.AppearancePreferencesPane::onFontListReady()();
      });

   }-*/;

   private void onFontListReady()
   {
      // NOTE: we use a short poll as we might receive this notification
      // just before the Qt webchannel has been able to synchronize with
      // the front-end
      Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
      {
         private int retryCount_ = 0;

         @Override
         public boolean execute()
         {
            if (retryCount_++ > 20)
               return false;

            String fonts = DesktopInfo.getFixedWidthFontList();
            if (fonts.isEmpty())
               return true;

            String[] fontList = fonts.split("\\n");
            populateFontList(fontList);
            return false;
         }

      }, 100);
   }

   private void getInstalledFontList()
   {
      // Search for installed fixed-width fonts on this web browser.
      final Set<String> browserFonts = new TreeSet<>();
      JsArrayString candidates = userPrefs_.browserFixedWidthFonts().getGlobalValue();
      for (String candidate: JsUtil.asIterable(candidates))
      {
         if (FontDetector.isFontSupported(candidate))
         {
            browserFonts.add(candidate);
         }
      }

      server_.getInstalledFonts(new ServerRequestCallback<JsArrayString>()
      {
         @Override
         public void onResponseReceived(JsArrayString fonts)
         {
            browserFonts.addAll(JsUtil.toList(fonts));
            populateFontList(browserFonts.toArray(new String[browserFonts.size()]));
            fontFace_.insertValue(0, DEFAULT_FONT_NAME, DEFAULT_FONT_VALUE);

            String font = null;
            if (userPrefs_.serverEditorFontEnabled().getValue())
            {
               // Use the user's supplied font
               font = userPrefs_.serverEditorFont().getValue();
            }

            if (StringUtil.isNullOrEmpty(font))
            {
               // No font selected
               fontFace_.setValue(DEFAULT_FONT_VALUE);
            }
            else
            {
               // If there's a non-empty, enabled font, set it as the default
               fontFace_.setValue(font);
               preview_.setFont(font, true);
            }

            initialFontFace_ = StringUtil.notNull(fontFace_.getValue());
         }

         @Override
         public void onError(ServerError error)
         {
            // Change label so it doesn't load indefinitely
            fontFace_.setLabel(constants_.editorFontLabel());

            Debug.logError(error);
         }
      });
   }

   private void populateFontList(String[] fontList)
   {
      String value = fontFace_.getValue();
      if (!StringUtil.isNullOrEmpty(value))
         value = value.replaceAll("\\\"", "");
      fontFace_.setLabel(constants_.editorFontLabel());
      fontFace_.setChoices(fontList, fontList);
      fontFace_.setValue(value);
   }

   private final PreferencesDialogResources res_;
   private final UserPrefs userPrefs_;
   private final UserState userState_;
   private SelectWidget textRendering_;
   private NumericTextBox editorFontSize_;
   private Double initialEditorFontSize_;
   private NumericTextBox editorLineHeight_;
   private Double initialEditorLineHeight_;
   private NumericTextBox helpFontSize_;
   private Double initialHelpFontSize_;
   private SelectWidget theme_;
   private ThemedButton addThemeButton_;
   private ThemedButton removeThemeButton_;
   private HorizontalPanel themeButtonsPanel_;
   private VerticalPanel projectThemeOverridePanel_;
   private CheckBox ignoreProjectAppearance_;
   private boolean initialIgnoreProjectAppearance_;
   // Set when a theme-affecting setting is applied before the async theme list
   // loads; setThemes() resolves and applies the live theme once it arrives.
   private boolean pendingThemeApply_ = false;
   private final AceEditorPreview preview_;
   private SelectWidget fontFace_;
   private String initialFontFace_;
   private SelectWidget zoomLevel_;
   private String initialZoomLevel_;
   private final SelectWidget flatTheme_;
   private Boolean relaunchRequired_;
   private HashMap<String, AceTheme> themeList_;
   private final GlobalDisplay globalDisplay_;
   private final DependencyManager dependencyManager_;
   private final ThemeServerOperations server_;
   private final EventBus events_;
   private final Commands commands_;
   private final boolean hasActiveProject_;
   private HandlerRegistration projectOptionsChangedHandler_;
   private int renderPass_ = 1;

   private final static String DEFAULT_FONT_NAME = "(Default)";
   private final static String DEFAULT_FONT_VALUE = "__default__";

   // Vertical space (px) reserved below the two columns for the
   // "ignore project appearance" checkbox row, including the spacer above it.
   private final static int IGNORE_APPEARANCE_ROW_HEIGHT = 36;
   
   private final static PrefsConstants constants_ = GWT.create(PrefsConstants.class);
   
   public interface Resources extends ClientBundle
   {
      @Source("AppearancePreferencesPane.R")
      TextResource codeSample();
   }
   
   private static final Resources RES = GWT.create(Resources.class);
   
}
