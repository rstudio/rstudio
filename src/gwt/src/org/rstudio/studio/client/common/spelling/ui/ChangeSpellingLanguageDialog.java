/*
 * ChangeSpellingLanguageDialog.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.spelling.ui;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.studio.client.common.StudioClientCommonConstants;
import org.rstudio.studio.client.common.spelling.SpellingService;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.PrefValue;
import org.rstudio.studio.client.workbench.prefs.model.SpellingPrefsContext;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;

/**
 * A one-step way to switch the spelling dictionary (Edit > Change Spelling
 * Language..., also in the command palette). The change is applied
 * immediately; open documents are re-checked. See #12223.
 */
public class ChangeSpellingLanguageDialog extends ModalDialog<String>
{
   public ChangeSpellingLanguageDialog(SpellingService spellingService, UserPrefs prefs)
   {
      super(constants_.changeSpellingLanguageCaption(),
            Roles.getDialogRole(),
            (langId) -> applyLanguage(prefs, langId));

      SpellingPrefsContext context = prefs.spellingPrefsContext().getValue();

      languageWidget_ = new SpellingLanguageSelectWidget(spellingService);
      languageWidget_.setProgressIndicator(addProgressIndicator(false));
      languageWidget_.setLanguages(context.getAllLanguagesInstalled(),
                                   context.getAvailableLanguages());
      languageWidget_.setSelectedLanguage(prefs.spellingDictionaryLanguage().getValue());
      ElementIds.assignElementId(languageWidget_.getListBox(), ElementIds.CHANGE_SPELLING_LANGUAGE_SELECT);
   }

   @Override
   protected Widget createMainWidget()
   {
      return languageWidget_;
   }

   @Override
   protected String collectInput()
   {
      return languageWidget_.getSelectedLanguage();
   }

   @Override
   protected boolean validate(String input)
   {
      // the "Install more languages..." entry has an empty value
      return !StringUtil.isNullOrEmpty(input);
   }

   private static void applyLanguage(UserPrefs prefs, String langId)
   {
      PrefValue<String> language = prefs.spellingDictionaryLanguage();
      if (StringUtil.equals(language.getValue(), langId))
         return;

      // the global value is what persists; a project-level dictionary (set in
      // Project Options) would otherwise keep shadowing it for this session
      language.setGlobalValue(langId);
      if (language.hasProjectValue())
         language.setProjectValue(langId);

      prefs.writeUserPrefs();
   }

   private final SpellingLanguageSelectWidget languageWidget_;

   private static final StudioClientCommonConstants constants_ = GWT.create(StudioClientCommonConstants.class);
}
