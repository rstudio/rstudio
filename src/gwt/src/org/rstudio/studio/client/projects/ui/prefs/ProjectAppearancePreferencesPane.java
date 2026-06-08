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

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
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
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
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

      // When the user is globally ignoring project appearance settings, reserve
      // room above the two columns for a note explaining that anything chosen
      // here has no effect until that option is turned off.
      boolean ignoringAppearance = userPrefs_.ignoreProjectAppearance().getGlobalValue();

      int previewHeight = paneHeight - PREVIEW_VERTICAL_MARGIN
            - (ignoringAppearance ? IGNORED_NOTE_HEIGHT : 0);
      int previewWidth = paneWidth - LEFT_COLUMN_WIDTH;

      // Editor-theme list: a multi-row listbox (not a dropdown) that fills the
      // available vertical space, with "(Default)" as the first entry.
      theme_ = new ListBox();
      ElementIds.assignElementId(theme_, ElementIds.PROJECT_EDITOR_THEME);
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

      if (ignoringAppearance)
      {
         Label ignoredNote = new Label(constants_.appearanceIgnoredByGlobalText());
         ignoredNote.addStyleName(PreferencesDialogBaseResources.INSTANCE.styles().infoLabel());
         ignoredNote.setWidth("100%");
         // Separate the note from the theme controls below it.
         ignoredNote.getElement().getStyle().setMarginBottom(8, Unit.PX);
         add(ignoredNote);
      }

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

   @Override
   protected void setPaneVisible(boolean visible)
   {
      super.setPaneVisible(visible);
      if (visible)
      {
         // When making the pane visible, toggle a meaningless transform on the
         // preview iframe to work around a QtWebEngine bug that can leave the
         // region unpainted (showing stale content) until invalidated. Mirrors
         // the global AppearancePreferencesPane. See
         // https://github.com/rstudio/rstudio/issues/6268
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

   // Applied-theme resolution rule: effective if installed, else global, else the
   // built-in default. Returns null only when the theme list is unavailable
   // (not yet loaded or empty). When the user is ignoring project appearance
   // settings, the project override is excluded so the global theme is applied.
   public AceTheme resolveAppliedTheme(UserPrefs uiPrefs)
   {
      String preferred = uiPrefs.ignoreProjectAppearance().getGlobalValue()
         ? uiPrefs.editorTheme().getGlobalValue()
         : uiPrefs.editorTheme().getValue();
      return AceTheme.resolveApplied(themeList_, preferred,
         uiPrefs.editorTheme().getGlobalValue());
   }

   // Show the selected theme in the preview. "(Default)" (and any stored-but-
   // uninstalled theme) previews the user's global editor theme, since that is
   // what the project will actually use.
   private void updatePreview()
   {
      AceTheme theme = AceTheme.resolveApplied(themeList_,
         theme_.getSelectedValue(),
         userPrefs_.editorTheme().getGlobalValue());
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
   private static final int LEFT_COLUMN_WIDTH = 160;
   private static final int LEFT_COLUMN_PADDING = 12;
   private static final int PREVIEW_VERTICAL_MARGIN = 38;
   private static final int THEME_LABEL_HEIGHT = 24;
   // Vertical space (px) reserved for the "settings ignored" note when the user
   // is globally ignoring project appearance settings (the note wraps to ~2
   // lines, plus a little space below it).
   private static final int IGNORED_NOTE_HEIGHT = 56;
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
