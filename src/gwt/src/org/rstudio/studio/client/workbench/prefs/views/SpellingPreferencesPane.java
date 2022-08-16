/*
 * SpellingPreferencesPane.java
 *
 * Copyright (C) 2022 by Posit, PBC
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

package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.studio.client.common.spelling.SpellingService;
import org.rstudio.studio.client.common.spelling.ui.SpellingCustomDictionariesWidget;
import org.rstudio.studio.client.common.spelling.ui.SpellingLanguageSelectWidget;
import org.rstudio.studio.client.workbench.WorkbenchList;
import org.rstudio.studio.client.workbench.WorkbenchListManager;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;
import org.rstudio.studio.client.workbench.prefs.model.SpellingPrefsContext;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.edit.ui.EditDialog;

public class SpellingPreferencesPane extends PreferencesPane
{
   @Inject
   public SpellingPreferencesPane(PreferencesDialogResources res,
                                  SpellingService spellingService,
                                  WorkbenchListManager workbenchListManager,
                                  UserPrefs prefs)
   {
      res_ = res;
      uiPrefs_ = prefs;
      
      add(headerLabel(constants_.spellingPreferencesPaneHeader()));

      languageWidget_ = new SpellingLanguageSelectWidget(spellingService);
      spaced(languageWidget_);
      add(languageWidget_);

      customDictsWidget_ =  new SpellingCustomDictionariesWidget();
      mediumSpaced(customDictsWidget_);
      nudgeRight(customDictsWidget_);
      add(customDictsWidget_);

      addUserDictionariesEditor(workbenchListManager);
      
      add(headerLabel(constants_.ignoreHeader()));
      
      add(checkboxPref(constants_.ignoreWordsUppercaseLabel(), prefs.ignoreUppercaseWords()));
      add(mediumSpaced(checkboxPref(constants_.ignoreWordsNumbersLabel(), prefs.ignoreWordsWithNumbers(), false)));

      
      add(headerLabel(constants_.checkingHeader()));
      
      realtimeSpellcheckingCheckbox_ = checkboxPref(constants_.realTimeSpellcheckingCheckboxLabel(), prefs.realTimeSpellchecking(), false);
      spaced(realtimeSpellcheckingCheckbox_);
      add(realtimeSpellcheckingCheckbox_);

      languageWidget_.addChangeHandler((event) ->
      {
         realtimeSpellcheckingCheckbox_.setValue(realtimeSpellcheckingCheckbox_.getValue());
      });
   }   
   
   private void addUserDictionariesEditor(WorkbenchListManager workbenchListManager)
   {
      final String kUserDictionary = constants_.kUserDictionaryLabel();
      final Label userDictLabel = new Label(kUserDictionary);
      final Consumer<Integer> setUserDictLabel = (Integer entries) -> {
         userDictLabel.setText(constants_.kUserDictionaryWordsLabel(kUserDictionary,StringUtil.formatGeneralNumber(entries)));
      };
      
      final ArrayList<String> userDictWords = new ArrayList<>();
      WorkbenchList userDict = workbenchListManager.getUserDictionaryList();
      userDict.addListChangedHandler((e) -> {
         userDictWords.clear();
         userDictWords.addAll(e.getList());
         Collections.sort(userDictWords, String.CASE_INSENSITIVE_ORDER);
         setUserDictLabel.accept(userDictWords.size());
      });
      
      HorizontalPanel userDictPanel = new HorizontalPanel();
      userDictPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
      userDictPanel.add(userDictLabel);
      SmallButton editUserDict = new SmallButton(constants_.editUserDictLabel());
      editUserDict.addStyleName(res_.styles().userDictEditButton());
      editUserDict.addClickHandler((e) -> {
         EditDialog editDialog = new EditDialog(
            constants_.editUserDictCaption(),
            constants_.editUserDictSaveCaption(),
            String.join("\n", userDictWords),
            Roles.getDialogRole(),
            false,
            true,
            new Size(400, 425),
            (dictionary, progress) -> {
               if (dictionary != null)
               {
                  List<String> dictSplitItems = Arrays.asList(dictionary.split("\n"));
                  ArrayList<String> dictWords = new ArrayList<>();
                  for (String item : dictSplitItems)
                  {
                     item = item.trim();
                     if (!item.isEmpty())
                        dictWords.add(item);
                  }
                  userDict.setContents(dictWords);
                  setUserDictLabel.accept(dictWords.size());
               }
               progress.onCompleted();
            }      
         );
         editDialog.showModal();
      });
      userDictPanel.add(editUserDict);
      
      mediumSpaced(userDictPanel);
      add(userDictPanel);
   }
   

   @Override
   protected void initialize(UserPrefs rPrefs)
   {
      SpellingPrefsContext context = uiPrefs_.spellingPrefsContext().getValue();
      languageWidget_.setProgressIndicator(getProgressIndicator());
      languageWidget_.setLanguages(context.getAllLanguagesInstalled(),
                                   context.getAvailableLanguages());

      languageWidget_.setSelectedLanguage(
                        uiPrefs_.spellingDictionaryLanguage().getValue());

      customDictsWidget_.setDictionaries(context.getCustomDictionaries());
      customDictsWidget_.setProgressIndicator(getProgressIndicator());
   }

   @Override
   public RestartRequirement onApply(UserPrefs rPrefs)
   {
      RestartRequirement restart = super.onApply(rPrefs);
      
      uiPrefs_.spellingDictionaryLanguage().setGlobalValue(
            languageWidget_.getSelectedLanguage());

      restart.setDesktopRestartRequired(
            restart.getDesktopRestartRequired() ||
            customDictsWidget_.getCustomDictsModified() ||
            !StringUtil.equals(
                  rPrefs.spellingDictionaryLanguage().getValue(),
                  languageWidget_.getSelectedLanguage()));
      
      return restart;
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(PreferencesDialogBaseResources.INSTANCE.iconSpelling2x());
   }

   @Override
   public boolean validate()
   {
      return true;
   }

   @Override
   public String getName()
   {
      return constants_.spellingPaneLabel();
   }


   private final PreferencesDialogResources res_;

   private final UserPrefs uiPrefs_;
   private final SpellingLanguageSelectWidget languageWidget_;
   private final SpellingCustomDictionariesWidget customDictsWidget_;
   private final CheckBox realtimeSpellcheckingCheckbox_;
   private static final PrefsConstants constants_ = GWT.create(PrefsConstants.class);
}
