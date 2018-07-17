/*
 * AppearancePreferencesPane.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.ThemeFonts;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.DesktopInfo;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceTheme;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceThemes;

import java.util.HashMap;

public class AppearancePreferencesPane extends PreferencesPane
{
   
   @Inject
   public AppearancePreferencesPane(PreferencesDialogResources res,
                                    UIPrefs uiPrefs,
                                    final AceThemes themes,
                                    WorkbenchContext workbenchContext,
                                    GlobalDisplay globalDisplay,
                                    DependencyManager dependencyManager,
                                    FileDialogs fileDialogs)
   {
      res_ = res;
      uiPrefs_ = uiPrefs;
      globalDisplay_ = globalDisplay;
      dependencyManager_ = dependencyManager;
      
      VerticalPanel leftPanel = new VerticalPanel();
      
      relaunchRequired_ = false;

      // dark-grey theme used to be derived from default, now also applies to sky
      if (StringUtil.equals(uiPrefs_.getFlatTheme().getValue(), "dark-grey"))
        uiPrefs_.getFlatTheme().setGlobalValue("default");

      final String originalTheme = uiPrefs_.getFlatTheme().getValue();

      flatTheme_ = new SelectWidget("RStudio theme:",
                                new String[]{"Classic", "Modern", "Sky"},
                                new String[]{"classic", "default", "alternate"},
                                false);
      flatTheme_.addStyleName(res.styles().themeChooser());
      flatTheme_.getListBox().setWidth("95%");
      flatTheme_.getListBox().addChangeHandler(event ->
         relaunchRequired_ = (StringUtil.equals(originalTheme, "classic") && !StringUtil.equals(flatTheme_.getValue(), "classic")) ||
            (StringUtil.equals(flatTheme_.getValue(), "classic") && !StringUtil.equals(originalTheme, "classic")));

      String themeAlias = uiPrefs_.getFlatTheme().getGlobalValue();
      flatTheme_.setValue(themeAlias);

      leftPanel.add(flatTheme_);

      if (Desktop.isDesktop())
      {
         int initialIndex = -1;
         int normalIndex = -1;
         String[] zoomValues = new String[] {
               "0.25", "0.50", "0.75", "0.80", "0.90",
               "1.00", "1.10", "1.25", "1.50", "1.75",
               "2.00", "2.50", "3.00", "4.00", "5.00"
         };
         String[] zoomLabels = new String[zoomValues.length];
         double currentZoomLevel = DesktopInfo.getZoomLevel();
         for (int i = 0; i < zoomValues.length; i++)
         {
            double zoomValue = Double.parseDouble(zoomValues[i]);
            
            if (zoomValue == 1.0)
               normalIndex = i;
            
            if (zoomValue == currentZoomLevel)
               initialIndex = i;
            
            zoomLabels[i] = StringUtil.formatPercent(zoomValue);
         }
         
         if (initialIndex == -1)
            initialIndex = normalIndex;
         
         zoomLevel_ = new SelectWidget("Zoom:",
                                       zoomLabels,
                                       zoomValues,
                                       false);
         zoomLevel_.getListBox().setSelectedIndex(initialIndex);
         initialZoomLevel_ = zoomValues[initialIndex];
         
         leftPanel.add(zoomLevel_);
         
         zoomLevel_.getListBox().addChangeHandler(event -> updatePreviewZoomLevel());
      
         String[] fonts = DesktopInfo.getFixedWidthFontList().split("\\n");

         fontFace_ = new SelectWidget("Editor font:", fonts, fonts, false, false, false);
         fontFace_.getListBox().setWidth("95%");

         String value = DesktopInfo.getFixedWidthFont();
         String label = value.replaceAll("\\\"", "");
         if (!fontFace_.setValue(label))
         {
            fontFace_.insertValue(0, label, value);
            fontFace_.setValue(value);
         }
         initialFontFace_ = StringUtil.notNull(fontFace_.getValue());
         leftPanel.add(fontFace_);
         fontFace_.addChangeHandler(new ChangeHandler()
         {
            @Override
            public void onChange(ChangeEvent event)
            {
               String font = fontFace_.getValue();
               if (font != null)
                  preview_.setFont(font);
               else
                  preview_.setFont(ThemeFonts.getFixedWidthFont());
            }
         });
      }

      String[] labels = {"7", "8", "9", "10", "11", "12", "13", "14", "16", "18", "24", "36"};
      String[] values = new String[labels.length];
      for (int i = 0; i < labels.length; i++)
         values[i] = Double.parseDouble(labels[i]) + "";

      fontSize_ = new SelectWidget("Editor Font size:",
                                   labels,
                                   values,
                                   false);
      fontSize_.getListBox().setWidth("95%");
      if (!fontSize_.setValue(uiPrefs.fontSize().getGlobalValue() + ""))
         fontSize_.getListBox().setSelectedIndex(3);
      fontSize_.getListBox().addChangeHandler(new ChangeHandler()
      {
         public void onChange(ChangeEvent event)
         {
            preview_.setFontSize(Double.parseDouble(fontSize_.getValue()));
         }
      });
   
      theme_ = new SelectWidget("Editor theme:",
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
   
      AceTheme currentTheme = uiPrefs_.theme().getGlobalValue();
      addThemeButton_ = new ThemedButton("Add...", event ->
         fileDialogs.openFile(
            "Theme Files (*.tmTheme *.rstheme)",
            RStudioGinjector.INSTANCE.getRemoteFileSystemContext(),
            workbenchContext.getCurrentWorkingDir(),
            "Theme Files (*.tmTheme *.rstheme)",
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
                     showThemeExistsDialog(inputStem, () -> addTheme(inputPath, themes, indicator, isTmTheme));
                     found = true;
                     break;
                  }
               }
               
               if (!found)
               {
                  addTheme(inputPath, themes, indicator, isTmTheme);
               }
               
               indicator.onCompleted();
            }));
      addThemeButton_.setLeftAligned(true);
      removeThemeButton_ = new ThemedButton(
         "Remove...",
         event -> removeTheme(theme_.getValue(), themes));
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
      preview_.setTheme(currentTheme.getUrl());
      preview_.setFontSize(Double.parseDouble(fontSize_.getValue()));
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
   
   private void removeTheme(String themeName, AceThemes themes)
   {
      AceTheme currentTheme = uiPrefs_.theme().getGlobalValue();
      if (StringUtil.equalsIgnoreCase(currentTheme.getName(), themeName))
      {
         showCantRemoveActiveThemeDialog(currentTheme.getName());
      }
      else
      {
         themes.removeTheme(
            themeName,
            errorMessage -> showCantRemoveThemeDialog(themeName, errorMessage));
         updateThemes(currentTheme, themes, getProgressIndicator());
      }
   }
   
   private void addTheme(String inputPath, AceThemes themes, ProgressIndicator indicator, boolean isTmTheme)
   {
      if (isTmTheme)
         dependencyManager_.withThemes(
            "Converting a tmTheme to an rstheme",
            () -> themes.addTheme(
               inputPath,
               result -> updateThemes(result, themes, indicator),
               error -> showCantAddThemeDialog(inputPath, error)));
      else
         themes.addTheme(
            inputPath,
            result -> updateThemes(result, themes, indicator),
            error -> showCantAddThemeDialog(inputPath, error));
   }
   
   private void setThemes(AceThemes themes)
   {
      themes.getThemes(
         themeList ->
         {
            themeList_ = themeList;
         
            theme_.setChoices(themeList_.keySet().toArray(new String[0]));
            theme_.setValue(uiPrefs_.theme().getGlobalValue().getName());
         },
         getProgressIndicator());
   }
   
   private void updateThemes(AceTheme focusedTheme, AceThemes themes, ProgressIndicator indicator)
   {
      themes.getThemes(
         themeList->
         {
            themeList_ = themeList;
            
            theme_.setChoices(themeList_.keySet().toArray(new String[0]));
            theme_.setValue(focusedTheme.getName());
            preview_.setTheme(focusedTheme.getUrl());
            removeThemeButton_.setEnabled(!focusedTheme.isDefaultTheme());
         },
         indicator);
   }
   
   private void updateThemes(String focusedThemeName, AceThemes themes, ProgressIndicator indicator)
   {
      // The focused theme should come from the list at some point, so it shouldn't be possible to
      // not be in the list.
      assert(themeList_.containsKey(focusedThemeName));
      updateThemes(themeList_.get(focusedThemeName), themes, indicator);
   }
   
   private void updatePreviewZoomLevel()
   {
      // no zoom preview on desktop
      if (Desktop.isDesktop())
      {
         preview_.setZoomLevel(Double.parseDouble(zoomLevel_.getValue()) /
                               DesktopInfo.getZoomLevel());
      }
   }

   private void showThemeExistsDialog(String inputFileName, Operation continueOperation)
   {
      StringBuilder msg = new StringBuilder();
      msg.append("A theme file with the same name, '")
         .append(inputFileName)
         .append("', already exists. Adding the theme will cause the existing file to be ")
         .append("overwritten. Would you like to add the theme anyways?");
      globalDisplay_.showYesNoMessage(
         GlobalDisplay.MSG_WARNING,
         "Theme File Already Exists",
         msg.toString(),
         continueOperation,
         false);
   }
   
   private void showCantAddThemeDialog(String themePath, String errorMessage)
   {
      StringBuilder msg = new StringBuilder();
      msg.append("Unable to add the theme '")
         .append(themePath)
         .append("'. The following error occurred: ")
         .append(errorMessage);
      
      globalDisplay_.showErrorMessage("Failed to Add Theme", msg.toString());
   }
   
   private void showCantRemoveThemeDialog(String themeName, String errorMessage)
   {
      StringBuilder msg = new StringBuilder();
      msg.append("Unable to remove the theme '")
         .append(themeName)
         .append("': ")
         .append(errorMessage);
      
      globalDisplay_.showErrorMessage("Failed to Remove Theme", msg.toString());
   }
   
   private void showCantRemoveActiveThemeDialog(String themeName)
   {
      StringBuilder msg = new StringBuilder();
      msg.append("The theme \"")
         .append(themeName)
         .append("\" cannot be removed because it is currently in use. To delete this theme,")
         .append(" please change the active theme and retry.");
      
      globalDisplay_.showErrorMessage("Cannot Remove Active Theme", msg.toString());
   }
   
   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(res_.iconAppearance2x());
   }
   
   @Override
   protected void initialize(RPrefs prefs)
   { 
   }

   @Override
   public boolean onApply(RPrefs rPrefs)
   {
      boolean restartRequired = super.onApply(rPrefs);

      double fontSize = Double.parseDouble(fontSize_.getValue());
      uiPrefs_.fontSize().setGlobalValue(fontSize);
      if (!StringUtil.equals(theme_.getValue(), uiPrefs_.theme().getGlobalValue().getName()))
      {
         uiPrefs_.theme().setGlobalValue(themeList_.get(theme_.getValue()));
      }
      if (Desktop.isDesktop())
      {
         if (!StringUtil.equals(initialFontFace_, fontFace_.getValue()))
         {
            Desktop.getFrame().setFixedWidthFont(StringUtil.notNull(fontFace_.getValue()));
            restartRequired = true;
         }
         
         if (!StringUtil.equals(initialZoomLevel_, zoomLevel_.getValue()))
         {
            double zoomLevel = Double.parseDouble(zoomLevel_.getValue());
            Desktop.getFrame().setZoomLevel(zoomLevel);
            restartRequired = true;
         }
      }

      String themeName = flatTheme_.getValue();

      if (!StringUtil.equals(themeName, uiPrefs_.getFlatTheme().getGlobalValue()))
      {
         uiPrefs_.getFlatTheme().setGlobalValue(themeName);
      }
      
      return restartRequired || relaunchRequired_;
   }

   @Override
   public String getName()
   {
      return "Appearance";
   }

   private final PreferencesDialogResources res_;
   private final UIPrefs uiPrefs_;
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
   private static String previewDefaultHeight_ = "498px";
   private HashMap<String, AceTheme> themeList_;
   private final GlobalDisplay globalDisplay_;
   private final DependencyManager dependencyManager_;
   
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
