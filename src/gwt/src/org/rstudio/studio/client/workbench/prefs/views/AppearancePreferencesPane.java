package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.ListBox;
import com.google.inject.Inject;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceThemes;

public class AppearancePreferencesPane extends PreferencesPane
{
   @Inject
   public AppearancePreferencesPane(PreferencesDialogResources res,
                                    UIPrefs uiPrefs,
                                    final AceThemes themes)
   {
      res_ = res;
      uiPrefs_ = uiPrefs;

      String[] labels = {"10", "12", "14", "16", "18"};
      String[] values = {"Pt10", "Pt12", "Pt14", "Pt16", "Pt18"};

      fontSize_ = new SelectWidget("Console/Source Font Size",
                                             labels,
                                             values);
      String value = uiPrefs.fontSize().getValue();
      boolean matched = false;
      for (int i = 0; i < values.length; i++)
         if (values[i].equals(value))
         {
            fontSize_.getListBox().setSelectedIndex(i);
            matched = true;
            break;
         }
      if (!matched)
         fontSize_.getListBox().setSelectedIndex(1);

      add(fontSize_);

      theme_ = new SelectWidget("Editor Theme",
                                themes.getThemeNames(),
                                themes.getThemeNames());
      theme_.getListBox().addChangeHandler(new ChangeHandler()
      {
         public void onChange(ChangeEvent event)
         {
            ListBox list = theme_.getListBox();
            preview_.setTheme(
                  themes.getThemeUrl(list.getValue(list.getSelectedIndex())));
         }
      });
      add(theme_);
      theme_.setValue(themes.getEffectiveThemeName(uiPrefs_.theme().getValue()));

      preview_ = new AceEditorPreview();
      preview_.setTheme(themes.getThemeUrl(uiPrefs_.theme().getValue()));
      add(preview_);
   }

   @Override
   public ImageResource getIcon()
   {
      return res_.iconAppearance();
   }

   @Override
   public void onApply()
   {
      super.onApply();
      ListBox list = fontSize_.getListBox();
      uiPrefs_.fontSize().setValue(list.getValue(list.getSelectedIndex()));
      uiPrefs_.theme().setValue(theme_.getListBox().getValue(
            theme_.getListBox().getSelectedIndex()));
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
   private AceEditorPreview preview_;
}
