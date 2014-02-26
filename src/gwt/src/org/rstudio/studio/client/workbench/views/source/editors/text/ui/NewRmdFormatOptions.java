/*
 * NewRmdFormatOptions.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ui;

import java.util.List;

import org.rstudio.studio.client.rmarkdown.model.RmdTemplateFormatOption;

import com.google.gwt.core.client.JavaScriptObject;

public class NewRmdFormatOptions
{
   public final static JavaScriptObject optionsListToJson(
         List<NewRmdFormatOption> options)
   {
      JavaScriptObject optionList = createOptionList();
      for (NewRmdFormatOption option: options)
      {
         if (!option.valueIsDefault())
         {
            if (option.getOption().getType().equals(
                  RmdTemplateFormatOption.TYPE_BOOLEAN))
            {
               addOption(optionList, option.getOption(), 
                         Boolean.parseBoolean(option.getValue()));
            }
            if (option.getOption().getType().equals(
                  RmdTemplateFormatOption.TYPE_FLOAT))
            {
               addOption(optionList, option.getOption(), 
                         Float.parseFloat(option.getValue()));
            }
            else if (option.getOption().getType().equals(
                  RmdTemplateFormatOption.TYPE_CHOICE))
            {
               addOption(optionList, option.getOption(), option.getValue());
            }
         }
      }
      return optionList;
   }
   
   private final native static JavaScriptObject createOptionList() /*-{
      return {};
   }-*/;
   
   // We need one of these per type since JSNI doesn't unbox templated types
   // for us
   private final native static void addOption (JavaScriptObject optionList, 
         RmdTemplateFormatOption option, boolean value) /*-{
      optionList[option.option_name] = value;
   }-*/;

   private final native static void addOption (JavaScriptObject optionList, 
         RmdTemplateFormatOption option, String value) /*-{
      optionList[option.option_name] = value;
   }-*/;

   private final native static void addOption (JavaScriptObject optionList, 
         RmdTemplateFormatOption option, float value) /*-{
      optionList[option.option_name] = value;
   }-*/;
}
