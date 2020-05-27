/*
 * RmdTemplateOptionsWidget.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.rmarkdown.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.DOM;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.rmarkdown.model.RmdFrontMatter;
import org.rstudio.studio.client.rmarkdown.model.RmdFrontMatterOutputOptions;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplate;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateFormat;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateFormatOption;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

public class RmdTemplateOptionsWidget extends Composite
{

   private static RmdTemplateOptionsWidgetUiBinder uiBinder = GWT
         .create(RmdTemplateOptionsWidgetUiBinder.class);

   interface RmdTemplateOptionsWidgetUiBinder extends
         UiBinder<Widget, RmdTemplateOptionsWidget>
   {
   }
   
   interface OptionsStyle extends CssResource
   {
      String optionWidget();
   }

   public RmdTemplateOptionsWidget(boolean allowFormatChange)
   {
      optionsTabs_ = new TabLayoutPanel(30, Style.Unit.PX, "R Markdown Options");
      initWidget(uiBinder.createAndBindUi(this));
      style.ensureInjected();
      allowFormatChange_ = allowFormatChange;
      if (allowFormatChange)
      {
         listFormats_.addChangeHandler(new ChangeHandler()
         {
            @Override
            public void onChange(ChangeEvent event)
            {
               updateFormatOptions(getSelectedFormat());
            }
         });
      }
      else
      {
         listFormats_.setVisible(false);
         listFormats_.setEnabled(false);
         labelFormatNotes_.setVisible(false);
         labelFormatName_.setVisible(true);
      }

      ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();
      optionsTabs_.addStyleName(styles.dialogTabPanel());
   }
   
   public void setTemplate(RmdTemplate template, boolean forCreate)
   {
      setTemplate(template, forCreate, null);
   }

   public void setTemplate(RmdTemplate template, boolean forCreate, 
                           RmdFrontMatter frontMatter)
   {
      template_ = template;
      formats_ = template.getFormats();
      options_ = template.getOptions();
      if (frontMatter != null)
         applyFrontMatter(frontMatter);
      listFormats_.clear();
      for (int i = 0; i < formats_.length(); i++)
      {
         listFormats_.addItem(formats_.get(i).getUiName(), 
                              formats_.get(i).getName());
      }
      updateFormatOptions(getSelectedFormat());
   }
   
   public void setDocument(FileSystemItem document)
   {
      document_ = document;
   }

   public String getSelectedFormat()
   {
      return listFormats_.getValue(listFormats_.getSelectedIndex());
   }
   
   public RmdFrontMatterOutputOptions getOutputOptions()
   {
      return RmdFormatOptionsHelper.optionsListToJson(
            optionWidgets_, 
            document_, 
            frontMatter_.getOutputOption(getSelectedFormat())).cast();
   }
   
   public void setSelectedFormat(String format)
   {
      if (allowFormatChange_)
      {
         for (int i = 0; i < listFormats_.getItemCount(); i++)
         {
            if (listFormats_.getValue(i) == format)
            {
               listFormats_.setSelectedIndex(i);
               updateFormatOptions(format);
            }
         }
      }
      else
      {
         RmdTemplateFormat selFormat = template_.getFormat(format);
         if (selFormat != null)
            labelFormatName_.setText("Shiny " + selFormat.getUiName());
      }
   }
   
   public JavaScriptObject getOptionsJSON()
   {
      return RmdFormatOptionsHelper.optionsListToJson(
            optionWidgets_,
            document_, 
            frontMatter_ == null ? 
                  null : frontMatter_.getOutputOption(getSelectedFormat()));
   }
   
   private void updateFormatOptions(String format)
   {
      tabs_ = new HashMap<String, FlowPanel>();
      optionsTabs_.clear();
      for (int i = 0; i < formats_.length(); i++)
      {
         if (formats_.get(i).getName() == format)
         {
            addFormatOptions(formats_.get(i));
            break;
         }
      }
   }
   
   private void addFormatOptions(RmdTemplateFormat format)
   {
      if (format.getNotes().length() > 0 && allowFormatChange_)
      {
         labelFormatNotes_.setText(format.getNotes());
         labelFormatNotes_.setVisible(true);
      }
      else
      {
         labelFormatNotes_.setVisible(false);
      }
      optionWidgets_ = new ArrayList<RmdFormatOption>();
      JsArrayString options = format.getOptions();
      for (int i = 0; i < options.length(); i++)
      {
         RmdFormatOption optionWidget;
         RmdTemplateFormatOption option = findOption(format.getName(),
                                                     options.get(i));
         if (option == null)
            continue;
         
         String initialValue = option.getDefaultValue();

         // check to see whether a value for this format and option were
         // specified in the front matter
         String frontMatterValue = getFrontMatterDefault(
               format.getName(), option.getName());
         if (frontMatterValue != null)
            initialValue = frontMatterValue;
         
         optionWidget = createWidgetForOption(option, initialValue);
         if (optionWidget == null)
            continue;
         
         optionWidget.asWidget().addStyleName(style.optionWidget());

         FlowPanel panel = null;
         String category = option.getCategory();
         if (tabs_.containsKey(category))
         {
            panel = tabs_.get(category);
         }
         else
         {
            ScrollPanel scrollPanel = new ScrollPanel();
            panel = new FlowPanel();
            scrollPanel.add(panel);
            optionsTabs_.add(scrollPanel, new Label(category));

            // associate tabpanel widget with controlling tab
            Roles.getTabpanelRole().set(scrollPanel.getElement());
            String tabId = DOM.createUniqueId();
            optionsTabs_.setTabId(scrollPanel, tabId);
            Roles.getTabpanelRole().setAriaLabelledbyProperty(scrollPanel.getElement(), Id.of(tabId));

            tabs_.put(category, panel);
         }

         panel.add(optionWidget);
         optionWidgets_.add(optionWidget);
      }
      
      // we need to center the tabs and overlay them on the top edge of the
      // content; to do this, it is necessary to nuke a couple of the inline
      // styles used by the default GWT tab panel. 
      Element tabOuter = (Element) optionsTabs_.getElement().getChild(1);
      tabOuter.getStyle().setOverflow(Overflow.VISIBLE);
      Element tabInner = (Element) tabOuter.getFirstChild();
      tabInner.getStyle().clearWidth();
   }
   
   private RmdFormatOption createWidgetForOption(RmdTemplateFormatOption option,
                                                 String initialValue)
   {
      RmdFormatOption optionWidget = null;
      if (option.getType() == RmdTemplateFormatOption.TYPE_BOOLEAN)
      {
         optionWidget = new RmdBooleanOption(option, initialValue);
      } 
      else if (option.getType() == RmdTemplateFormatOption.TYPE_CHOICE)
      {
         optionWidget = new RmdChoiceOption(option, initialValue);
      }
      else if (option.getType() == RmdTemplateFormatOption.TYPE_STRING)
      {
         optionWidget = new RmdStringOption(option, initialValue);
      }
      else if (option.getType() == RmdTemplateFormatOption.TYPE_FLOAT ||
               option.getType() == RmdTemplateFormatOption.TYPE_INTEGER)
      {
         optionWidget = new RmdFloatOption(option, initialValue);
      }
      else if (option.getType() == RmdTemplateFormatOption.TYPE_FILE)
      {
         // if we have a document and a relative path, resolve the path
         // relative to the document
         if (document_ != null && initialValue != "null" &&
             FilePathUtils.pathIsRelative(initialValue))
         {
            initialValue = 
                  document_.getParentPath().completePath(initialValue);
         }
         optionWidget = new RmdFileOption(option, initialValue);
      }
      return optionWidget;
   }
   
   private RmdTemplateFormatOption findOption(String formatName, 
                                              String optionName)
   {
      RmdTemplateFormatOption result = null;
      for (int i = 0; i < options_.length(); i++)
      {
         RmdTemplateFormatOption option = options_.get(i);
         
         // Not the option we're looking for 
         if (option.getName() != optionName)
            continue;

         String optionFormatName = option.getFormatName();
         if (optionFormatName.length() > 0)
         {
            // A format-specific option: if it's for this format we're done,
            // otherwise keep looking
            if (optionFormatName.equals(formatName))
               return option;
            else
               continue;
         }

         result = option;
      }
      return result;
   }
   
   private void applyFrontMatter(RmdFrontMatter frontMatter)
   {
      frontMatter_ = frontMatter;
      frontMatterCache_ = new HashMap<String, String>();
      ensureOptionsCache();
      JsArrayString formats = frontMatter.getFormatList();
      for (int i = 0; i < formats.length(); i++)
      {
         String format = formats.get(i);
         RmdFrontMatterOutputOptions options = 
               frontMatter.getOutputOption(format);
         JsArrayString optionList = options.getOptionList();
         for (int j = 0; j < optionList.length(); j++)
         {
            String option = optionList.get(j);
            String value = options.getOptionValue(option);
            frontMatterCache_.put(format + ":" + option, value);
            if (optionCache_.containsKey(option))
            {
               // If the option is specifically labeled as transferable
               // between formats, add a generic key to be applied to other
               // formats
               RmdTemplateFormatOption formatOption = optionCache_.get(option);
               if (formatOption.isTransferable())
               {
                  frontMatterCache_.put(option, value);
               }
            }
         }
      }
   }
   
   private String getFrontMatterDefault(String formatName, String optionName)
   {
      // if we have no front matter, we have no default
      if (frontMatterCache_ == null)
         return null;
      
      // is this value defined in the front matter?
      String key = formatName + ":" + optionName;
      if (frontMatterCache_.containsKey(key))
         return frontMatterCache_.get(key);
      else 
      {
         // is this value transferable from a format defined in the front
         // matter? (don't transfer options into formats explicitly defined
         // in the front matter)
         JsArrayString frontMatterFormats = frontMatter_.getFormatList();
         if ((!JsArrayUtil.jsArrayStringContains(frontMatterFormats, formatName)) 
               &&
             frontMatterCache_.containsKey(optionName))
         {
            return frontMatterCache_.get(optionName);
         }
      }
      return null;
   }
   
   private void ensureOptionsCache()
   {
      if (optionCache_ != null)
         return;
      optionCache_ = new HashMap<String, RmdTemplateFormatOption>();
      for (int i = 0; i < options_.length(); i++)
      {
         RmdTemplateFormatOption option = options_.get(i);
         if (option.getFormatName().length() > 0)
            continue;
         optionCache_.put(option.getName(), option);
      }
   }

   private RmdTemplate template_;
   private JsArray<RmdTemplateFormat> formats_;
   private JsArray<RmdTemplateFormatOption> options_;
   private List<RmdFormatOption> optionWidgets_;
   private RmdFrontMatter frontMatter_;
   private FileSystemItem document_;
   private boolean allowFormatChange_;
   
   // Cache of options present in the template (ignores those options that 
   // are specifically marked for a format)
   private Map<String, RmdTemplateFormatOption> optionCache_;

   // Cache of values set in the front matter, e.g.:
   // "html_document:fig_width" => "7.5"
   // In the case of options that are marked as transferable, maps directly
   // from an option name to its default, e.g.
   // "toc" => "true"
   private Map<String, String> frontMatterCache_;
   
   private Map<String, FlowPanel> tabs_;

   @UiField ListBox listFormats_;
   @UiField Label labelFormatNotes_;
   @UiField Label labelFormatName_;
   @UiField(provided=true) TabLayoutPanel optionsTabs_;
   @UiField OptionsStyle style;
}
