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
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.studio.client.projects.StudioClientProjectConstants;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceTheme;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceThemes;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

public class ProjectAppearancePreferencesPane extends ProjectPreferencesPane
{
   @Inject
   public ProjectAppearancePreferencesPane(AceThemes themes)
   {
      themes_ = themes;

      addHeader(constants_.appearanceText());

      theme_ = new ListBox();
      theme_.addItem(constants_.projectTypeDefault(), DEFAULT_VALUE);

      VerticalPanel panel = new VerticalPanel();
      panel.add(new FormLabel(constants_.editorThemeFormLabel(), theme_));
      panel.add(theme_);
      add(panel);

      wrapWithPanel("project_appearance_prefs");
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

   private String initialEditorTheme_ = "";
   private final AceThemes themes_;
   private final ListBox theme_;
   private HashMap<String, AceTheme> themeList_;
   private static final StudioClientProjectConstants constants_ =
      GWT.create(StudioClientProjectConstants.class);
}
