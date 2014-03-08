/*
 * RmdFormatOptionsHelper.java
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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.rmarkdown.model.RmdFrontMatterOutputOptions;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateFormatOption;

import com.google.gwt.core.client.JavaScriptObject;

public class RmdFormatOptionsHelper
{
   public final static RmdFrontMatterOutputOptions optionsListToJson(
         Map<RmdTemplateFormatOption, String> optionVals, 
         FileSystemItem document)
   {
      Set<RmdTemplateFormatOption> options = optionVals.keySet();
      RmdFrontMatterOutputOptions optionList = 
            RmdFrontMatterOutputOptions.create();
      for (RmdTemplateFormatOption option: options)
      {
         addOption(document, optionList, option, optionVals.get(option));
      }
      return optionList;
   }

   public final static RmdFrontMatterOutputOptions optionsListToJson(
         List<RmdFormatOption> options, 
         FileSystemItem document, 
         RmdFrontMatterOutputOptions optionList)
   {
      if (optionList == null) 
         optionList = RmdFrontMatterOutputOptions.create();
      for (RmdFormatOption option: options)
      {
         if (option.valueIsDefault())
         {
            optionList.removeOption(option.getOption().getName());
         }
         else
         {
            addOption(document, optionList, option.getOption(), 
                      option.getValue());
         }
      }
      return optionList;
   }
  
   private static void addOption(FileSystemItem document, 
                          JavaScriptObject optionList, 
                          RmdTemplateFormatOption option, String value)
   {
      String type = option.getType();
      if (value == null)
      {
         // all nulls are written identically
         addOption(optionList, option, null);
      }
      else if (type.equals(RmdTemplateFormatOption.TYPE_BOOLEAN))
      {
         addOption(optionList, option, Boolean.parseBoolean(value));
      }
      else if (type.equals(RmdTemplateFormatOption.TYPE_FLOAT))
      {
         addOption(optionList, option, Float.parseFloat(value));
      }
      else if (type.equals(RmdTemplateFormatOption.TYPE_INTEGER))
      {
         addOption(optionList, option, Integer.parseInt(value));
      }
      else if (type.equals(RmdTemplateFormatOption.TYPE_CHOICE) ||
               type.equals(RmdTemplateFormatOption.TYPE_STRING))
      {
         addOption(optionList, option, value);
      }
      else if (type.equals(RmdTemplateFormatOption.TYPE_FILE))
      {
         // For file options, compute the path relative to the document
         // if we're starting with an absolute path
         if (document != null && value != null && 
             FilePathUtils.pathIsAbsolute(value))
         {
            FileSystemItem selFile = 
                  FileSystemItem.createFile(value);
            // this will be null if no relative path can be found; if
            // this is the case, we'll use the absolute path as-is
            String relativePath = 
                  selFile.getPathRelativeTo(document.getParentPath());
            addOption(optionList, option,
                      relativePath == null ? value : relativePath);
         }
         else
         {
            addOption(optionList, option, value);
         }
      }
   }
   
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

   private final native static void addOption (JavaScriptObject optionList, 
         RmdTemplateFormatOption option, int value) /*-{
      optionList[option.option_name] = value;
   }-*/;
}
