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

import org.rstudio.core.client.CoreClientConstants;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.common.StudioClientCommonConstants;
import org.rstudio.studio.client.common.spelling.SpellingService;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidResponse;
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
   public ChangeSpellingLanguageDialog(
         SpellingService spellingService,
         UserPrefs prefs,
         ProjectsServerOperations server)
   {
      super(constants_.changeSpellingLanguageCaption(),
            Roles.getDialogRole(),
            (langId, indicator) -> applyLanguage(spellingService, prefs, server, langId, indicator));

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

   private static void applyLanguage(
         SpellingService spellingService,
         UserPrefs prefs,
         ProjectsServerOperations server,
         String langId,
         ProgressIndicator indicator)
   {
      PrefValue<String> language = prefs.spellingDictionaryLanguage();
      if (StringUtil.equals(language.getValue(), langId))
      {
         indicator.onCompleted();
         return;
      }

      indicator.onProgress(coreConstants_.progressIndicatorTitle());

      // Persist the effective layer so the server and the picker agree, and
      // leave the global default alone when this project overrides it.
      if (language.hasProjectValue())
      {
         server.readProjectOptions(new ServerRequestCallback<RProjectOptions>()
         {
            @Override
            public void onResponseReceived(RProjectOptions options)
            {
               RProjectConfig config = options.getConfig();
               config.setSpellingDictionary(langId);
               server.writeProjectConfig(config, new ServerRequestCallback<VoidResponse>()
               {
                  @Override
                  public void onResponseReceived(VoidResponse response)
                  {
                     language.setProjectValue(langId);
                     spellingService.onDictionaryLanguageChanged();
                     indicator.onCompleted();
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     indicator.onError(error.getUserMessage());
                  }
               });
            }

            @Override
            public void onError(ServerError error)
            {
               indicator.onError(error.getUserMessage());
            }
         });
      }
      else
      {
         boolean hadGlobalValue = prefs.getUserLayer().hasKey(language.getId());
         String previousLanguage = language.getGlobalValue();
         language.setGlobalValue(langId, false);
         prefs.writeUserPrefsWithDetail((succeeded, errorMessage) ->
         {
            if (succeeded)
            {
               indicator.onCompleted();
            }
            else
            {
               if (hadGlobalValue)
                  language.setGlobalValue(previousLanguage, false);
               else
                  prefs.getUserLayer().unset(language.getId());

               indicator.onError(errorMessage);
            }
         });
      }
   }

   private final SpellingLanguageSelectWidget languageWidget_;

   private static final StudioClientCommonConstants constants_ = GWT.create(StudioClientCommonConstants.class);
   private static final CoreClientConstants coreConstants_ = GWT.create(CoreClientConstants.class);
}
