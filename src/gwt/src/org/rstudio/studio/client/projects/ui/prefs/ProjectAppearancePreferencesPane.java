/*
 * ProjectAppearancePreferencesPane.java
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
package org.rstudio.studio.client.projects.ui.prefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.studio.client.projects.StudioClientProjectConstants;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.views.AceEditorPreview;
import org.rstudio.studio.client.workbench.prefs.views.PreferencesDialogConstants;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceTheme;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceThemes;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

public class ProjectAppearancePreferencesPane extends ProjectPreferencesPane
{
   // NOTE: like the global Appearance pane, this pane uses a custom two-column
   // layout (theme list + live preview) and intentionally does not call
   // wrapWithPanel(); it sizes its own widgets from the project dialog dimensions.
   @Inject
   public ProjectAppearancePreferencesPane(AceThemes themes, UserPrefs userPrefs)
   {
      themes_ = themes;
      userPrefs_ = userPrefs;

      int paneWidth = PreferencesDialogConstants.PROJECT_PANEL_CONTAINER_WIDTH
            - PreferencesDialogConstants.SECTION_CHOOSER_WIDTH
            - PreferencesDialogConstants.SECTION_CHOOSER_PADDING;
      int paneHeight = PreferencesDialogConstants.PROJECT_PANEL_CONTAINER_HEIGHT;

      int previewHeight = paneHeight - PREVIEW_VERTICAL_MARGIN;
      int previewWidth = paneWidth - LEFT_COLUMN_WIDTH;

      // Editor-theme list: a multi-row listbox (not a dropdown) that fills the
      // available vertical space, with "(Default)" as the first entry.
      theme_ = new ListBox();
      theme_.getElement().setId(THEME_LIST_ELEMENT_ID);
      theme_.setVisibleItemCount(VISIBLE_ITEM_COUNT);
      theme_.setWidth((LEFT_COLUMN_WIDTH - LEFT_COLUMN_PADDING) + "px");
      theme_.setHeight((previewHeight - THEME_LABEL_HEIGHT) + "px");
      theme_.addItem(constants_.projectTypeDefault(), DEFAULT_VALUE);
      theme_.addChangeHandler(event -> updatePreview());

      VerticalPanel leftPanel = new VerticalPanel();
      leftPanel.add(new FormLabel(constants_.editorThemeFormLabel(), theme_));
      leftPanel.add(theme_);

      FlowPanel previewPanel = new FlowPanel();
      preview_ = new AceEditorPreview(RES.codeSample().getText());
      preview_.setWidth(previewWidth + "px");
      preview_.setHeight(previewHeight + "px");
      preview_.setFontSize(userPrefs_.fontSizePoints().getValue());
      double lineHeight = userPrefs_.editorLineHeight().getValue();
      if (lineHeight > 0)
         preview_.setLineHeight(lineHeight);
      previewPanel.add(preview_);

      HorizontalPanel hpanel = new HorizontalPanel();
      hpanel.setWidth("100%");
      hpanel.add(leftPanel);
      hpanel.setCellWidth(leftPanel, LEFT_COLUMN_WIDTH + "px");
      hpanel.add(previewPanel);
      add(hpanel);
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(ProjectPreferencesDialogResources.INSTANCE.iconAppearance2x());
   }

   @Override
   public String getName()
   {
      return constants_.appearanceText();
   }

   @Override
   protected void initialize(RProjectOptions options)
   {
      final String storedTheme = options.getConfig().getEditorTheme();
      initialEditorTheme_ = storedTheme;

      themes_.getThemes(themeList ->
      {
         themeList_ = themeList;

         theme_.clear();
         theme_.addItem(constants_.projectTypeDefault(), DEFAULT_VALUE);

         ArrayList<String> names = new ArrayList<>(themeList.keySet());
         Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
         for (String name : names)
            theme_.addItem(name, name);

         // Preserve a stored-but-uninstalled theme so OK does not erase it.
         if (!StringUtil.isNullOrEmpty(storedTheme) && !themeList.containsKey(storedTheme))
            theme_.addItem(storedTheme, storedTheme);

         selectValue(StringUtil.isNullOrEmpty(storedTheme) ? DEFAULT_VALUE : storedTheme);
         updatePreview();
      },
      // A non-null ProgressIndicator is required: DelayedProgressRequestCallback
      // dereferences it in onResponseReceived/onError. The dialog sets it on every
      // pane before initialize() runs, so getProgressIndicator() is non-null here.
      getProgressIndicator());
   }

   @Override
   public RestartRequirement onApply(RProjectOptions options)
   {
      // If the theme list has not loaded yet, the selector only holds (Default);
      // preserve the stored value rather than erasing an existing override.
      String value = (themeList_ == null) ? initialEditorTheme_ : theme_.getSelectedValue();
      options.getConfig().setEditorTheme(value);
      return new RestartRequirement();
   }

   // Applied-theme resolution rule: effective if installed, else global, else the
   // built-in default. Returns null only when the theme list has not loaded.
   public AceTheme resolveAppliedTheme(UserPrefs uiPrefs)
   {
      if (themeList_ == null || themeList_.isEmpty())
         return null;

      AceTheme theme = themeList_.get(uiPrefs.editorTheme().getValue());
      if (theme == null)
         theme = themeList_.get(uiPrefs.editorTheme().getGlobalValue());
      if (theme == null)
         theme = themeList_.get(AceTheme.createDefault().getName());
      return theme;
   }

   // Show the selected theme in the preview. "(Default)" (and any stored-but-
   // uninstalled theme) previews the user's global editor theme, since that is
   // what the project will actually use.
   private void updatePreview()
   {
      if (themeList_ == null)
         return;

      AceTheme theme = themeList_.get(theme_.getSelectedValue());
      if (theme == null)
         theme = themeList_.get(userPrefs_.editorTheme().getGlobalValue());
      if (theme == null)
         theme = themeList_.get(AceTheme.createDefault().getName());
      if (theme != null)
         preview_.setTheme(theme.getUrl());
   }

   private void selectValue(String value)
   {
      for (int i = 0; i < theme_.getItemCount(); i++)
      {
         if (StringUtil.equals(theme_.getValue(i), value))
         {
            theme_.setSelectedIndex(i);
            return;
         }
      }
      theme_.setSelectedIndex(0);
   }

   private static final String DEFAULT_VALUE = "";
   private static final String THEME_LIST_ELEMENT_ID = "rstudio_project_editor_theme";
   private static final int LEFT_COLUMN_WIDTH = 160;
   private static final int LEFT_COLUMN_PADDING = 12;
   private static final int PREVIEW_VERTICAL_MARGIN = 38;
   private static final int THEME_LABEL_HEIGHT = 24;
   private static final int VISIBLE_ITEM_COUNT = 20;

   private String initialEditorTheme_ = "";
   private final AceThemes themes_;
   private final UserPrefs userPrefs_;
   private final ListBox theme_;
   private final AceEditorPreview preview_;
   private HashMap<String, AceTheme> themeList_;

   private static final StudioClientProjectConstants constants_ =
      GWT.create(StudioClientProjectConstants.class);

   interface Resources extends ClientBundle
   {
      // Reuse the global Appearance pane's preview code sample.
      @Source("org/rstudio/studio/client/workbench/prefs/views/AppearancePreferencesPane.R")
      TextResource codeSample();
   }

   private static final Resources RES = GWT.create(Resources.class);
}
