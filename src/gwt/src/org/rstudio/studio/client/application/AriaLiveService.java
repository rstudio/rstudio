/*
 * AriaLiveService.java
 *
 * Copyright (C) 2020 by RStudio, Inc.
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
package org.rstudio.studio.client.application;

import com.google.gwt.core.client.JsArrayString;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class AriaLiveService
{
   // Unique identifiers for aria-live announcements. Use the same text for the 
   // value as the constant name to ensure uniqueness. Add here and in the constructor below to
   // associate description for preferences UI.
   public static final String CONSOLE_CLEARED = "console_cleared";
   public static final String CONSOLE_LOG = "console_log";
   public static final String FILTERED_LIST = "filtered_list";
   public static final String GIT_MESSAGE_LENGTH = "git_message_length";
   public static final String PROGRESS_COMPLETION = "progress_completion";
   public static final String PROGRESS_LOG = "progress_log";
   public static final String TAB_KEY_MODE = "tab_key_mode";
   public static final String TOOLBAR_VISIBILITY = "toolbar_visibility";
   
   // Announcement requested by a user, not controlled by a preference since it is on-demand.
   // Do not include in the announcements_ map.
   public static final String ON_DEMAND = "on_demand";

   @Inject
   public AriaLiveService(EventBus eventBus, Provider<UserPrefs> pUserPrefs)
   {
      eventBus_ = eventBus;
      pUserPrefs_ = pUserPrefs;

      announcements_ = new HashMap<>();
      announcements_.put(CONSOLE_CLEARED, "Announce console cleared");
      announcements_.put(CONSOLE_LOG, "Announce console output (requires restart)");
      announcements_.put(FILTERED_LIST, "Announce filtered result count");
      announcements_.put(GIT_MESSAGE_LENGTH, "Announce commit message length");
      announcements_.put(PROGRESS_COMPLETION, "Announce task completion");
      announcements_.put(PROGRESS_LOG, "Announce task progress details");
      announcements_.put(TAB_KEY_MODE, "Announce tab key focus mode change");
      announcements_.put(TOOLBAR_VISIBILITY, "Announce toolbar visibility change");
   }

   /**
    * Report a message to screen reader after a debounce delay
    * @param announcementId unique identifier of this announcement
    * @param message string to announce
    */
   public void reportStatusDebounced(String announcementId, String message)
   {
      reportStatus(announcementId, message, false);
   }

   /**
    * Report a message to screen reader.
    * @param announcementId unique identifier of this announcement
    * @param message string to announce
    */
   public void reportStatus(String announcementId, String message)
   {
      reportStatus(announcementId, message, true);
   }

   public Map<String, String> getAnnouncements()
   {
      return announcements_;
   }

   public boolean isDisabled(String announcementId)
   {
      JsArrayString disabledItems = pUserPrefs_.get().disabledAriaLiveAnnouncements().getValue();
      for (int i = 0; i < disabledItems.length(); i++)
      {
         if (StringUtil.equals(announcementId, disabledItems.get(i)))
            return true;
      }
      return false;
   }

   private void reportStatus(String announcementId, String message, boolean immediate)
   {
      if (!announcements_.containsKey(announcementId))
         Debug.logWarning("Unregistered live announcement: " + announcementId);

      if (isDisabled(announcementId))
         return;

      eventBus_.fireEvent(new AriaLiveStatusEvent(message, immediate));
   }

   private final Map<String, String> announcements_;

   // injected
   private final EventBus eventBus_;
   private final Provider<UserPrefs> pUserPrefs_;
}
