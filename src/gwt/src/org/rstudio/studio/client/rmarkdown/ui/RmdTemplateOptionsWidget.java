/*
 * RmdTemplateOptionsWidget.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

import org.rstudio.studio.client.rmarkdown.model.RmdFrontMatter;
import org.rstudio.studio.client.rmarkdown.model.RmdFrontMatterOutputOptions;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplate;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateFormat;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateFormatOption;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class RmdTemplateOptionsWidget extends Composite
{

   private static RmdTemplateOptionsWidgetUiBinder uiBinder = GWT
         .create(RmdTemplateOptionsWidgetUiBinder.class);

   interface RmdTemplateOptionsWidgetUiBinder extends
         UiBinder<Widget, RmdTemplateOptionsWidget>
   {
   }

   public RmdTemplateOptionsWidget()
   {
      initWidget(uiBinder.createAndBindUi(this));
      listFormats_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            updateFormatOptions(getSelectedFormat());
         }
      });
   }
   
   public void setTemplate(RmdTemplate template, boolean forCreate)
   {
      setTemplate(template, forCreate, null);
   }

   public void setTemplate(RmdTemplate template, boolean forCreate, 
                           RmdFrontMatter frontMatter)
   {
      formats_ = template.getFormats();
      options_ = template.getOptions();
      if (frontMatter != null)
         applyFrontMatter(frontMatter);
      forCreate_ = forCreate;
      listFormats_.clear();
      for (int i = 0; i < formats_.length(); i++)
      {
         listFormats_.addItem(formats_.get(i).getUiName(), 
                              formats_.get(i).getName());
      }
      updateFormatOptions(getSelectedFormat());
   }

   public String getSelectedFormat()
   {
      return listFormats_.getValue(listFormats_.getSelectedIndex());
   }
   
   public void setSelectedFormat(String format)
   {
      for (int i = 0; i < listFormats_.getItemCount(); i++)
      {
         if (listFormats_.getValue(i).equals(format))
         {
            listFormats_.setSelectedIndex(i);
            updateFormatOptions(format);
         }
      }
   }
   
   public JavaScriptObject getOptionsJSON()
   {
      return RmdFormatOptionsHelper.optionsListToJson(optionWidgets_);
   }
   
   private void updateFormatOptions(String format)
   {
      panelOptions_.clear();
      for (int i = 0; i < formats_.length(); i++)
      {
         if (formats_.get(i).getName().equals(format))
         {
            addFormatOptions(formats_.get(i));
            break;
         }
      }
   }
   
   private void addFormatOptions(RmdTemplateFormat format)
   {
      if (format.getNotes().length() > 0)
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
         if (frontMatter_ != null)
         {
            String key = format.getName() + ":" + option.getName();
            if (frontMatter_.containsKey(key))
            {
               initialValue = frontMatter_.get(key);
            }
         }
         
         if (option.getType().equals(RmdTemplateFormatOption.TYPE_BOOLEAN))
         {
            optionWidget = new RmdBooleanOption(option, initialValue);
         } 
         else if (option.getType().equals(RmdTemplateFormatOption.TYPE_CHOICE))
         {
            optionWidget = new RmdChoiceOption(option, initialValue);
         }
         else if (option.getType().equals(RmdTemplateFormatOption.TYPE_FLOAT))
         {
            optionWidget = new RmdFloatOption(option, initialValue);
         }
         else
            continue;
         
         optionWidgets_.add(optionWidget);
         panelOptions_.add(optionWidget);
         Style optionStyle = optionWidget.asWidget().getElement().getStyle();
         optionStyle.setMarginTop(3, Unit.PX);
         optionStyle.setMarginBottom(5, Unit.PX);
      }
   }
   
   private RmdTemplateFormatOption findOption(String formatName, 
                                              String optionName)
   {
      RmdTemplateFormatOption result = null;
      for (int i = 0; i < options_.length(); i++)
      {
         RmdTemplateFormatOption option = options_.get(i);
         
         // Not the option we're looking for 
         if (!option.getName().equals(optionName))
            continue;

         if (forCreate_ && !option.showForCreate())
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
      frontMatter_ = new HashMap<String, String>();
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
            frontMatter_.put(format + ":" + option, 
                             options.getOptionValue(option));
         }
      }
   }

   private JsArray<RmdTemplateFormat> formats_;
   private JsArray<RmdTemplateFormatOption> options_;
   private List<RmdFormatOption> optionWidgets_;
   private boolean forCreate_ = false;
   private Map<String, String> frontMatter_;

   @UiField ListBox listFormats_;
   @UiField Label labelFormatNotes_;
   @UiField VerticalPanel panelOptions_;
}
