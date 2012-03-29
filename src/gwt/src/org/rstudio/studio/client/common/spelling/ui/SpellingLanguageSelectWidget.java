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

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.common.prefs.PrefsWidgetHelper;
import org.rstudio.studio.client.common.spelling.model.SpellingLanguage;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.ListBox;

public class SpellingLanguageSelectWidget extends SelectWidget
{
   public SpellingLanguageSelectWidget(
                     final CommandWithArg<String> onInstallLanguages)
   {
      super("Spelling dictionary language:", 
            new String[0], 
            new String[0], 
            false, 
            true, 
            false);    
      
      PrefsWidgetHelper.addHelpButton(this, "spelling_dictionaries");
      
      getListBox().addChangeHandler(new ChangeHandler() {

         @Override
         public void onChange(ChangeEvent event)
         {
            if (getListBox().getSelectedIndex() == installIndex_)
            {
               setSelectedLanguage(currentLangId_);
               String progress = allLanguagesInstalled_ ?
                                    "Downloading dictionaries..." :
                                    "Downloading additional languages...";
               onInstallLanguages.execute(progress);
            }
            else
            {
               currentLangId_ = getSelectedLanguage();
            }
         }
         
      });
   }

   public void setLanguages(boolean allLanguagesInstalled,
                            JsArray<SpellingLanguage> languages)
   {
      languages_ = languages;
      installIndex_ = languages.length();
      allLanguagesInstalled_ = allLanguagesInstalled;
      String[] choices =  new String[languages.length()+1];
      String[] values = new String[languages.length()+1];
      for (int i=0; i<languages.length(); i++)
      {
         SpellingLanguage language = languages.get(i);
         choices[i] = language.getName();
         values[i] = language.getId();
      }
      if (allLanguagesInstalled)
         choices[installIndex_] = "Update Dictionaries...";
      else
         choices[installIndex_] = "Install More Languages...";
      values[installIndex_] = "";
      
      setChoices(choices, values);
   }
   
   public void setSelectedLanguage(String langId)
   {
      for (int i=0; i<languages_.length(); i++)
      {
         if (langId.equals(languages_.get(i).getId()))
         {
            currentLangId_ = langId;
            getListBox().setSelectedIndex(i);
            return;
         }
      }
      
      // if we couldn't find this lang id then reset
      getListBox().setSelectedIndex(0);
      currentLangId_ = getListBox().getValue(0);
   }
   
   public String getSelectedLanguage()
   {
      ListBox listBox = getListBox();
      return listBox.getValue(listBox.getSelectedIndex());
   }
   
   private String currentLangId_ = null;
   private int installIndex_ = -1;
   private boolean allLanguagesInstalled_ = false;
   private JsArray<SpellingLanguage> languages_;
  
}
