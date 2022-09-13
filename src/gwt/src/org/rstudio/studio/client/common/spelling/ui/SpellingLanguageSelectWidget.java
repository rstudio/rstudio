/*
 * SpellingLanguageSelectWidget.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.spelling.ui;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.widget.HelpButton;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.StudioClientCommonConstants;
import org.rstudio.studio.client.common.spelling.SpellingService;
import org.rstudio.studio.client.common.spelling.model.SpellingLanguage;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.SpellingPrefsContext;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.ui.ListBox;

public class SpellingLanguageSelectWidget extends SelectWidget
{
   public SpellingLanguageSelectWidget(SpellingService spellingService)
   {
      this(spellingService, false);
   }
   
   public SpellingLanguageSelectWidget(SpellingService spellingService, 
                                       boolean includeDefaultOption)
   {
      super(constants_.spellingLanguageSelectWidgetLabel(),
            new String[0], 
            new String[0], 
            false, 
            true, 
            false);    
      
      includeDefaultOption_ = includeDefaultOption;
      languageOffset_ = includeDefaultOption_ ? 1 : 0;
      
      getLabel().getElement().getStyle().setMarginBottom(4, Unit.PX);
      
      HelpButton.addHelpButton(this, "spelling_dictionaries", constants_.addHelpButtonLabel(), 0);
      
      getListBox().addChangeHandler(new ChangeHandler() {

         @Override
         public void onChange(ChangeEvent event)
         {
            int selectedIndex = getListBox().getSelectedIndex();
            if (selectedIndex == installIndex_)
            {
               setSelectedLanguage(currentLangId_);
               String progress = allLanguagesInstalled_ ?
                                    constants_.progressDownloadingLabel() :
                                    constants_.progressDownloadingLanguagesLabel();
               
               // show progress
               progressIndicator_.onProgress(progress);

               // save current selection for restoring
               final String currentLang = getSelectedLanguage();

               spellingService.installAllDictionaries(
                  new ServerRequestCallback<SpellingPrefsContext> () {

                     @Override
                     public void onResponseReceived(SpellingPrefsContext context)
                     {
                        progressIndicator_.onCompleted();
                        setLanguages(context.getAllLanguagesInstalled(),
                                     context.getAvailableLanguages());
                        setSelectedLanguage(currentLang);
                     }

                     @Override
                     public void onError(ServerError error)
                     {
                        JSONString userMessage = error.getClientInfo().isString();
                        if (userMessage != null)
                        {
                           progressIndicator_.onCompleted();
                           RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
                              constants_.onErrorDownloadingCaption(), userMessage.stringValue());
                        }
                        else
                        {
                           progressIndicator_.onError(error.getUserMessage());
                        }
                     }

               });
               
            }
            else
            {
               currentLangId_ = getSelectedLanguage();
            }
         }
         
      });
   }
   
   public void setProgressIndicator(ProgressIndicator progressIndicator)
   {
      progressIndicator_ = progressIndicator;
   }

   public void setLanguages(boolean allLanguagesInstalled,
                            JsArray<SpellingLanguage> languages)
   {
      languages_ = languages;
      installIndex_ = languages.length() + languageOffset_;
      allLanguagesInstalled_ = allLanguagesInstalled;
      String[] choices =  new String[languages.length()+1 + languageOffset_];
      String[] values = new String[languages.length()+1 + languageOffset_];
      if (includeDefaultOption_)
      {
         choices[0] = constants_.includeDefaultOption();
         values[0] = "";
      }
      for (int i=0; i<languages.length(); i++)
      {
         SpellingLanguage language = languages.get(i);
         choices[i + languageOffset_] = language.getName();
         values[i + languageOffset_] = language.getId();
      }
      if (allLanguagesInstalled)
         choices[installIndex_] = constants_.allLanguagesInstalledOption();
      else
         choices[installIndex_] = constants_.installIndexOption();
      values[installIndex_] = "";
      
      setChoices(choices, values);
   }
   
   public void setSelectedLanguage(String langId)
   {
      if (includeDefaultOption_ && langId.isEmpty())
      {
         getListBox().setSelectedIndex(0);
         currentLangId_ = "";
      }
      else
      {
         for (int i=0; i<languages_.length(); i++)
         {
            if (langId == languages_.get(i).getId())
            {
               currentLangId_ = langId;
               getListBox().setSelectedIndex(i + languageOffset_);
               return;
            }
         }
         
         // if we couldn't find this lang id then reset
         getListBox().setSelectedIndex(0);
         currentLangId_ = getListBox().getValue(0);
      }
   }
   
   public String getSelectedLanguage()
   {
      ListBox listBox = getListBox();
      return listBox.getValue(listBox.getSelectedIndex());
   }
   
   private final boolean includeDefaultOption_;
   private final int languageOffset_;
   private String currentLangId_ = null;
   private int installIndex_ = -1;
   private boolean allLanguagesInstalled_ = false;
   private JsArray<SpellingLanguage> languages_;
   private ProgressIndicator progressIndicator_;
   private static final StudioClientCommonConstants constants_ = GWT.create(StudioClientCommonConstants.class);

}
