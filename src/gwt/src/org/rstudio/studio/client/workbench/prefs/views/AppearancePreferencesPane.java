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

import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.ThemeFonts;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.DesktopInfo;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.ThemeChangedEvent;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceThemes;

public class AppearancePreferencesPane extends PreferencesPane
{
   @Inject
   public AppearancePreferencesPane(PreferencesDialogResources res,
                                    UIPrefs uiPrefs,
                                    final AceThemes themes,
                                    EventBus eventBus)
   {
      res_ = res;
      uiPrefs_ = uiPrefs;
      eventBus_ = eventBus;

      VerticalPanel leftPanel = new VerticalPanel();
      
      relaunchRequired_ = false;

      // dark-grey theme used to be derived from default, now also applies to sky
      if (uiPrefs_.getFlatTheme().getValue() == "dark-grey")
        uiPrefs_.getFlatTheme().setGlobalValue("default");

      final String originalTheme = uiPrefs_.getFlatTheme().getValue();

      flatTheme_ = new SelectWidget("RStudio theme:",
                                new String[]{"Classic", "Modern", "Sky"},
                                new String[]{"classic", "default", "alternate"},
                                false);
      flatTheme_.addStyleName(res.styles().themeChooser());
      flatTheme_.getListBox().setWidth("95%");
      flatTheme_.getListBox().addChangeHandler(new ChangeHandler()
      {
         public void onChange(ChangeEvent event)
         {
            relaunchRequired_ = false;
            
            if (originalTheme == "classic" && flatTheme_.getValue() != "classic" ||
                flatTheme_.getValue() == "classic" && originalTheme != "classic")
            {
               relaunchRequired_ = true;
            }
         }
      });

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

      
      String[] themeOptions = themes.getThemeNames();
      for (int idxTheme = 0; idxTheme < themeOptions.length; idxTheme++) {
         if (themeOptions[idxTheme] == "TextMate") {
            themeOptions[idxTheme] = "TextMate (default)";
         }
      }
      
      theme_ = new SelectWidget("Editor theme:",
                                themeOptions,
                                themes.getThemeNames(),
                                false);
      theme_.getListBox().addChangeHandler(new ChangeHandler()
      {
         public void onChange(ChangeEvent event)
         {
            preview_.setTheme(themes.getThemeUrl(theme_.getValue()));
         }
      });
      theme_.getListBox().getElement().<SelectElement>cast().setSize(7);
      theme_.getListBox().getElement().getStyle().setHeight(225, Unit.PX);
      theme_.addStyleName(res.styles().themeChooser());
      theme_.setValue(themes.getEffectiveThemeName(uiPrefs_.theme().getGlobalValue()));
      
      leftPanel.add(fontSize_);
      leftPanel.add(theme_);

      FlowPanel previewPanel = new FlowPanel();
      
      previewPanel.setSize("100%", "100%");
      preview_ = new AceEditorPreview(CODE_SAMPLE);
      preview_.setHeight(previewDefaultHeight_);
      preview_.setWidth("278px");
      preview_.setTheme(themes.getThemeUrl(uiPrefs_.theme().getGlobalValue()));
      preview_.setFontSize(Double.parseDouble(fontSize_.getValue()));
      updatePreviewZoomLevel();
      previewPanel.add(preview_);

      HorizontalPanel hpanel = new HorizontalPanel();
      hpanel.setWidth("100%");
      hpanel.add(leftPanel);
      hpanel.setCellWidth(leftPanel, "160px");
      hpanel.add(previewPanel);

      add(hpanel);
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
      uiPrefs_.theme().setGlobalValue(theme_.getValue());
      if (Desktop.isDesktop())
      {
         if (initialFontFace_ != fontFace_.getValue())
         {
            Desktop.getFrame().setFixedWidthFont(StringUtil.notNull(fontFace_.getValue()));
            restartRequired = true;
         }
         
         if (initialZoomLevel_ != zoomLevel_.getValue())
         {
            double zoomLevel = Double.parseDouble(zoomLevel_.getValue());
            Desktop.getFrame().setZoomLevel(zoomLevel);
            restartRequired = true;
         }
      }

      String themeName = flatTheme_.getValue();

      uiPrefs_.getFlatTheme().setGlobalValue(themeName);  
      ThemeChangedEvent themeChangedEvent = new ThemeChangedEvent(flatTheme_.getValue());
      eventBus_.fireEvent(themeChangedEvent);
      eventBus_.fireEventToAllSatellites(themeChangedEvent);
      
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
   private final AceEditorPreview preview_;
   private SelectWidget fontFace_;
   private String initialFontFace_;
   private SelectWidget zoomLevel_;
   private String initialZoomLevel_;
   private final SelectWidget flatTheme_;
   private EventBus eventBus_;
   private Boolean relaunchRequired_;
   private static String previewDefaultHeight_ = "498px";

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
