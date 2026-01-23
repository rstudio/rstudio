/*
 * PaiUtil.java
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
package org.rstudio.studio.client.workbench.views.chat;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.projects.model.RProjectAssistantOptions;
import org.rstudio.studio.client.projects.ui.prefs.events.ProjectOptionsChangedEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Utility class for Posit AI (PAI) feature availability checks.
 */
@Singleton
public class PaiUtil
{
   @Inject
   public PaiUtil(Session session, UserPrefs userPrefs, EventBus events)
   {
      session_ = session;
      userPrefs_ = userPrefs;

      // Initialize cached project options from session info
      projectOptions_ = session_.getSessionInfo().getAssistantProjectOptions();

      // Listen for project options changes to keep cache updated
      events.addHandler(ProjectOptionsChangedEvent.TYPE, (event) ->
      {
         projectOptions_ = event.getData().getAssistantOptions();
      });
   }

   /**
    * Returns true if the Posit AI feature is enabled.
    *
    * @return true if PAI is enabled, false otherwise
    */
   public boolean isPaiEnabled()
   {
      return session_.getSessionInfo().getPositAssistantEnabled();
   }

   /**
    * Returns true if the user has selected Posit AI as their assistant.
    * Use this to gate features that should only be active when PAI is selected.
    *
    * @return true if user has selected Posit AI, false otherwise
    */
   public boolean isPaiSelected()
   {
      return userPrefs_.assistant().getGlobalValue()
            .equals(UserPrefsAccessor.ASSISTANT_POSIT);
   }

   /**
    * Returns true if Posit AI is the configured chat provider, checking:
    * 1. Project-level chat provider setting (if set and not "default")
    * 2. Global user preference
    *
    * @return true if Posit AI is the effective chat provider, false otherwise
    */
   public boolean isChatProviderPosit()
   {
      return getConfiguredChatProvider().equals(UserPrefsAccessor.CHAT_PROVIDER_POSIT);
   }

   /**
    * Returns true if chat is disabled (provider set to "none"), checking:
    * 1. Project-level chat provider setting (if set and not "default")
    * 2. Global user preference
    *
    * @return true if chat is disabled, false otherwise
    */
   public boolean isChatProviderNone()
   {
      return getConfiguredChatProvider().equals(UserPrefsAccessor.CHAT_PROVIDER_NONE);
   }

   /**
    * Returns the configured chat provider, checking:
    * 1. Project-level chat provider setting (if set and not "default")
    * 2. Global user preference
    *
    * @return The effective chat provider value
    */
   public String getConfiguredChatProvider()
   {
      // Check for project-level override
      if (projectOptions_ != null)
      {
         String projectChatProvider = projectOptions_.chat_provider;
         if (projectChatProvider != null &&
             !projectChatProvider.isEmpty() &&
             !projectChatProvider.equals("default"))
         {
            return projectChatProvider;
         }
      }

      // Fall back to global preference
      return userPrefs_.chatProvider().getGlobalValue();
   }

   private final Session session_;
   private final UserPrefs userPrefs_;
   private RProjectAssistantOptions projectOptions_;
}
