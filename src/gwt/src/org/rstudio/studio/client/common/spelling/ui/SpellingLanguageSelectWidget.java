/*
 * SpellingLanguageSelectWidget.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.spelling.ui;

import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.common.spelling.model.SpellingLanguage;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.ui.ListBox;

public class SpellingLanguageSelectWidget extends SelectWidget
{
   public SpellingLanguageSelectWidget()
   {
      super("Spell checking language:", 
            new String[0], 
            new String[0], 
            false, 
            true, 
            false);    
   }

   public void setLanguages(JsArray<SpellingLanguage> languages)
   {
      languages_ = languages;
      String[] choices =  new String[languages.length()];
      String[] values = new String[languages.length()];
      for (int i=0; i<languages.length(); i++)
      {
         SpellingLanguage language = languages.get(i);
         choices[i] = language.getName();
         values[i] = language.getId();
      }
      
      setChoices(choices, values);
   }
   
   public void setSelectedLanguage(String langId)
   {
      for (int i=0; i<languages_.length(); i++)
      {
         if (langId.equals(languages_.get(i).getId()))
         {
            getListBox().setSelectedIndex(i);
            break;
         }
      }
   }
   
   public String getSelectedLanguage()
   {
      ListBox listBox = getListBox();
      return listBox.getValue(listBox.getSelectedIndex());
   }
   
   private JsArray<SpellingLanguage> languages_;
  
}
