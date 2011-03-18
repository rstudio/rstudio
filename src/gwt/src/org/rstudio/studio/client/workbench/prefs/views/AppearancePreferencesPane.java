package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import org.rstudio.core.client.widget.FontSizer.Size;
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

      VerticalPanel leftPanel = new VerticalPanel();

      String[] labels = {"10", "12", "14", "16", "18"};
      String[] values = {"Pt10", "Pt12", "Pt14", "Pt16", "Pt18"};

      fontSize_ = new SelectWidget("Font Size",
                                   labels,
                                   values,
                                   false);
      if (!fontSize_.setValue(uiPrefs.fontSize().getValue()))
         fontSize_.getListBox().setSelectedIndex(1);
      fontSize_.getListBox().addChangeHandler(new ChangeHandler()
      {
         public void onChange(ChangeEvent event)
         {
            preview_.setFontSize(Size.valueOf(fontSize_.getValue()));
         }
      });

      leftPanel.add(fontSize_);

      theme_ = new SelectWidget("Editor Theme",
                                themes.getThemeNames(),
                                themes.getThemeNames(),
                                true);
      theme_.getListBox().addChangeHandler(new ChangeHandler()
      {
         public void onChange(ChangeEvent event)
         {
            preview_.setTheme(themes.getThemeUrl(theme_.getValue()));
         }
      });
      theme_.addStyleName(res.styles().themeChooser());
      leftPanel.add(theme_);
      theme_.setValue(themes.getEffectiveThemeName(uiPrefs_.theme().getValue()));

      FlowPanel previewPanel = new FlowPanel();
      previewPanel.setSize("100%", "100%");
      previewPanel.add(new Label("Preview"));
      preview_ = new AceEditorPreview();
      preview_.setHeight("300px");
      preview_.setWidth("288px");
      preview_.setTheme(themes.getThemeUrl(uiPrefs_.theme().getValue()));
      preview_.setFontSize(Size.valueOf(fontSize_.getValue()));
      previewPanel.add(preview_);

      HorizontalPanel hpanel = new HorizontalPanel();
      hpanel.setWidth("100%");
      hpanel.add(leftPanel);
      hpanel.setCellWidth(leftPanel, "150px");
      hpanel.add(previewPanel);

      add(spaced(new Label("Console/Editor Appearance")));
      add(hpanel);
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
      uiPrefs_.fontSize().setValue(fontSize_.getValue());
      uiPrefs_.theme().setValue(theme_.getValue());
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
