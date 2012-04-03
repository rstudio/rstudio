/*
 * SpellingPreferencesPane.java
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

package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.json.client.JSONString;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.spelling.model.SpellingServerOperations;
import org.rstudio.studio.client.common.spelling.ui.MacSpellingLanguageWidget;
import org.rstudio.studio.client.common.spelling.ui.SpellingLanguageSelectWidget;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.SpellingPrefsContext;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

public class SpellingPreferencesPane extends PreferencesPane
{
   @Inject
   public SpellingPreferencesPane(GlobalDisplay globalDisplay,
                                  PreferencesDialogResources res,
                                  SpellingServerOperations server,
                                  UIPrefs prefs)
   {
      globalDisplay_ = globalDisplay;
      res_ = res;
      server_ = server;
      uiPrefs_ = prefs;
      
      languageWidget_ = new SpellingLanguageSelectWidget(onInstallLanguages_);
      spaced(languageWidget_);
      add(languageWidget_);
      
      // for Mac desktop we hide the language widget and show an alternate
      // widget indicating we are using the system default default language
      if (BrowseCap.isMacintoshDesktop())
      {
         languageWidget_.setVisible(false);
         Widget macLanguageWidget = new MacSpellingLanguageWidget();
         spaced(macLanguageWidget);
         add(macLanguageWidget);
      }
         
      add(checkboxPref("Check spelling as you type",
                       prefs.checkSpellingAsYouType()));
      
      add(checkboxPref("Ignore words in UPPERCASE",
                        prefs.ignoreWordsInUppercase()));
      
      add(checkboxPref("Ignore words with numbers",
                       prefs.ignoreWordsInUppercase()));
      
      add(checkboxPref("Check spelling before compiling PDF",
                       prefs.checkSpellingBeforeCompile()));

   }

   
   private CommandWithArg<String> onInstallLanguages_ 
                                                = new CommandWithArg<String>()
   {
      @Override
      public void execute(String progress)
      {
         // show progress
         final ProgressIndicator indicator = getProgressIndicator();
         indicator.onProgress(progress);
         
         // save current selection for restoring
         final String currentLang = languageWidget_.getSelectedLanguage();
         
         server_.installAllDictionaries(
            new ServerRequestCallback<SpellingPrefsContext> () {

               @Override
               public void onResponseReceived(SpellingPrefsContext context)
               {
                  indicator.onCompleted();
                  languageWidget_.setLanguages(
                                       context.getAllLanguagesInstalled(),
                                       context.getAvailableLanguages());
                  languageWidget_.setSelectedLanguage(currentLang);
               }
               
               @Override
               public void onError(ServerError error)
               {
                  JSONString userMessage = error.getClientInfo().isString();
                  if (userMessage != null)
                  {
                     indicator.onCompleted();
                     globalDisplay_.showErrorMessage(
                                             "Error Downloading Dictionaries", 
                                              userMessage.stringValue());
                  }
                  else
                  {
                     indicator.onError(error.getUserMessage());
                  }
               }
            
         });
      }
      
   };
   
   @Override
   protected void initialize(RPrefs rPrefs)
   {
      SpellingPrefsContext context = rPrefs.getSpellingPrefsContext();
      languageWidget_.setLanguages(context.getAllLanguagesInstalled(),
                                   context.getAvailableLanguages());
      
      languageWidget_.setSelectedLanguage(
                        uiPrefs_.spellingDictionaryLanguage().getValue());
   }

   @Override
   public boolean onApply(RPrefs rPrefs)
   {
      uiPrefs_.spellingDictionaryLanguage().setGlobalValue(
                                       languageWidget_.getSelectedLanguage());
      
      return super.onApply(rPrefs);
   }

   
   @Override
   public ImageResource getIcon()
   {
      return PreferencesDialogBaseResources.INSTANCE.iconSpelling();
   }

   @Override
   public boolean validate()
   {
      return true;
   }

   @Override
   public String getName()
   {
      return "Spelling";
   }

  
   @SuppressWarnings("unused")
   private final PreferencesDialogResources res_;
   
   private final GlobalDisplay globalDisplay_;
   private final UIPrefs uiPrefs_;
   private final SpellingServerOperations server_;
   private final SpellingLanguageSelectWidget languageWidget_;
}
