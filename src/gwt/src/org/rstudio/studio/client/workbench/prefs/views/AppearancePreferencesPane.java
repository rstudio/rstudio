/*
 * AppearancePreferencesPane.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.ThemeFonts;
import org.rstudio.core.client.widget.FontDetector;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.DesktopInfo;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceTheme;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceThemes;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.model.ThemeServerOperations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

public class AppearancePreferencesPane extends PreferencesPane
{
   static final String[] ZOOM_VALUES = new String[] {
           "0.25", "0.50", "0.75", "0.80", "0.90",
           "1.00", "1.10", "1.25", "1.50", "1.75",
           "2.00", "2.50", "3.00", "4.00", "5.00"
   };

   @Inject
   public AppearancePreferencesPane(PreferencesDialogResources res,
                                    UserPrefs userPrefs,
                                    UserState userState,
                                    final AceThemes themes,
                                    WorkbenchContext workbenchContext,
                                    GlobalDisplay globalDisplay,
                                    DependencyManager dependencyManager,
                                    FileDialogs fileDialogs,
                                    ThemeServerOperations server)
   {
      res_ = res;
      userPrefs_ = userPrefs;
      userState_ = userState;
      globalDisplay_ = globalDisplay;
      dependencyManager_ = dependencyManager;
      server_ = server;

      VerticalPanel leftPanel = new VerticalPanel();

      relaunchRequired_ = false;

      // dark-grey theme and classic themes no longer exist; map them to defaults
      if (StringUtil.equals(userPrefs_.globalTheme().getValue(), "dark-grey") ||
          StringUtil.equals(userPrefs_.globalTheme().getValue(), "classic"))
        userPrefs_.globalTheme().setGlobalValue(UserPrefs.GLOBAL_THEME_DEFAULT);

      @SuppressWarnings("unused")
      final String originalTheme = userPrefs_.globalTheme().getValue();

      flatTheme_ = new SelectWidget(constants_.appearanceRStudioThemeLabel(),
                                new String[]{constants_.modernThemeLabel(), constants_.skyThemeLabel()},
                                new String[]{
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
         zoomLevel_.getListBox().addChangeHandler(event -> updatePreviewZoomLevel());

         if (BrowseCap.isElectron()) {
            Desktop.getFrame().getZoomLevel(zoomLevel ->
            {
               int initialIndex = getInitialZoomIndex(zoomLevel);
               zoomLevel_.getListBox().setSelectedIndex(initialIndex);
            });
         } else {
            int initialIndex = getInitialZoomIndex(currentZoomLevel);
            zoomLevel_.getListBox().setSelectedIndex(initialIndex);
            initialZoomLevel_ = ZOOM_VALUES[initialIndex];
         }

         leftPanel.add(zoomLevel_);
      }

      String[] fonts = new String[] {};

      if (Desktop.isDesktop())
      {
         // In desktop mode, get the list of installed fonts from Qt
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

      leftPanel.add(fontFace_);
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

      String[] labels = {"7", "8", "9", "10", "11", "12", "13", "14", "16", "18", "24", "36"};
      String[] values = new String[labels.length];
      for (int i = 0; i < labels.length; i++)
         values[i] = Double.parseDouble(labels[i]) + "";

      fontSize_ = new SelectWidget(constants_.appearanceEditorFontSizeLabel(),
                                   labels,
                                   values,
                                   false);
      fontSize_.getListBox().setWidth("95%");
      if (!fontSize_.setValue(userPrefs.fontSizePoints().getGlobalValue() + ""))
         fontSize_.getListBox().setSelectedIndex(3);
      fontSize_.getListBox().addChangeHandler(new ChangeHandler()
      {
         public void onChange(ChangeEvent event)
         {
            preview_.setFontSize(Double.parseDouble(fontSize_.getValue()));
         }
      });

      theme_ = new SelectWidget(constants_.appearanceEditorThemeLabel(),
                                new String[0],
                                new String[0],
                                false);
      theme_.getListBox().getElement().<SelectElement>cast().setSize(7);
      theme_.getListBox().getElement().getStyle().setHeight(225, Unit.PX);
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

      HorizontalPanel buttonPanel = new HorizontalPanel();
      buttonPanel.add(addThemeButton_);
      buttonPanel.add(removeThemeButton_);

      leftPanel.add(fontSize_);
      leftPanel.add(theme_);
      leftPanel.add(buttonPanel);

      FlowPanel previewPanel = new FlowPanel();

      previewPanel.setSize("100%", "100%");
      preview_ = new AceEditorPreview(CODE_SAMPLE);
      preview_.setHeight(previewDefaultHeight_);
      preview_.setWidth("278px");
      preview_.setFontSize(Double.parseDouble(fontSize_.getValue()));
      preview_.setTheme(currentTheme.getUrl());
      updatePreviewZoomLevel();
      previewPanel.add(preview_);

      HorizontalPanel hpanel = new HorizontalPanel();
      hpanel.setWidth("100%");
      hpanel.add(leftPanel);
      hpanel.setCellWidth(leftPanel, "160px");
      hpanel.add(previewPanel);

      add(hpanel);

      // Themes are retrieved asynchronously, so we have to update the theme list and preview panel
      // asynchronously too. We also need to wait until the next event cycle so that the progress
      // indicator will be ready.
      Scheduler.get().scheduleDeferred(() -> setThemes(themes));
   }

   private int getInitialZoomIndex(double currentZoomLevel) {
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

            // It's possible the current theme was removed outside the context of
            // RStudio, so choose a default if it can't be found.
            AceTheme currentTheme = userState_.theme().getGlobalValue().cast();
            if (!themeList_.containsKey(currentTheme.getName()))
            {
               StringBuilder warningMsg = new StringBuilder();
               warningMsg.append(constants_.setThemeWarningMessage(currentTheme.getName(), currentTheme.isDark() ? constants_.themeWarningMessageDarkLabel() : constants_.themeWarningMessageLightLabel()));

               currentTheme = AceTheme.createDefault(currentTheme.isDark());
               userState_.theme().setGlobalValue(currentTheme);
               preview_.setTheme(currentTheme.getUrl());

               warningMsg.append(currentTheme.getName())
                  .append("\".");
               Debug.logWarning(warningMsg.toString());
            }

            theme_.setChoices(themeList_.keySet().toArray(new String[0]));
            theme_.setValue(currentTheme.getName());
            removeThemeButton_.setEnabled(!currentTheme.isDefaultTheme());
         },
         getProgressIndicator());
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

      String themeName = flatTheme_.getValue();
      if (!StringUtil.equals(themeName, userPrefs_.globalTheme().getGlobalValue()))
      {
         userPrefs_.globalTheme().setGlobalValue(themeName, false);
      }

      double fontSize = Double.parseDouble(fontSize_.getValue());
      userPrefs_.fontSizePoints().setGlobalValue(fontSize);
      if (!StringUtil.equals(theme_.getValue(), userPrefs_.editorTheme().getGlobalValue()))
      {
         userState_.theme().setGlobalValue(themeList_.get(theme_.getValue()));
         userPrefs_.editorTheme().setGlobalValue(theme_.getValue(), false);
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
   private SelectWidget helpFontSize_;
   private SelectWidget fontSize_;
   private SelectWidget theme_;
   private ThemedButton addThemeButton_;
   private ThemedButton removeThemeButton_;
   private final AceEditorPreview preview_;
   private SelectWidget fontFace_;
   private String initialFontFace_;
   private SelectWidget zoomLevel_;
   private String initialZoomLevel_;
   private final SelectWidget flatTheme_;
   private Boolean relaunchRequired_;
   private static String previewDefaultHeight_ = "533px";
   private HashMap<String, AceTheme> themeList_;
   private final GlobalDisplay globalDisplay_;
   private final DependencyManager dependencyManager_;
   private final ThemeServerOperations server_;
   private int renderPass_ = 1;

   private final static String DEFAULT_FONT_NAME = "(Default)";
   private final static String DEFAULT_FONT_VALUE = "__default__";
   private final static PrefsConstants constants_ = GWT.create(PrefsConstants.class);
   private static final String CODE_SAMPLE =
         "# plotting of R objects\n" +
         "plot <- function (x, y, ...)\n" +
         "{\n" +
         "  if (is.function(x) && \n" +
         "      is.null(attr(x, \"class\")))\n" +
         "  {\n" +
         "    if (missing(y))\n" +
         "      y <- NULL\n" +
         "    \n" +
         "    # check for ylab argument\n" +
         "    hasylab <- function(...) \n" +
         "      !all(is.na(\n" +
         "        pmatch(names(list(...)),\n" +
         "              \"ylab\")))\n" +
         "    \n" +
         "    if (hasylab(...))\n" +
         "      plot.function(x, y, ...)\n" +
         "    \n" +
         "    else \n" +
         "      plot.function(\n" +
         "        x, y, \n" +
         "        ylab = paste(\n" +
         "          deparse(substitute(x)),\n" +
         "          \"(x)\"), \n" +
         "        ...)\n" +
         "  }\n" +
         "  else \n" +
         "    UseMethod(\"plot\")\n" +
         "}\n";
}
