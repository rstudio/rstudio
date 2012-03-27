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

import com.google.gwt.resources.client.ImageResource;
import com.google.inject.Inject;

import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.studio.client.common.spelling.ui.SpellingLanguageSelectWidget;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

public class SpellingPreferencesPane extends PreferencesPane
{
   @Inject
   public SpellingPreferencesPane(PreferencesDialogResources res,
                                  UIPrefs prefs)
   {
      res_ = res;
      uiPrefs_ = prefs;
      
      languageWidget_ = new SpellingLanguageSelectWidget();
      spaced(languageWidget_);
      add(languageWidget_);
      

      add(checkboxPref("Check spelling before compiling PDF",
                       prefs.checkSpellingBeforeCompile()));
      
      add(checkboxPref("Ignore words in UPPERCASE",
                        prefs.ignoreWordsInUppercase()));
      
      add(checkboxPref("Ignore words with numbers",
                       prefs.ignoreWordsInUppercase()));
   }

   @Override
   protected void initialize(RPrefs rPrefs)
   {
      languageWidget_.setLanguages(
                  rPrefs.getSpellingPrefsContext().getAvailableLanguages());
      
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
   
   private final UIPrefs uiPrefs_;
   private final SpellingLanguageSelectWidget languageWidget_;
}
