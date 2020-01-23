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
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Severity;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Timing;
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
   public static final String INFO_BAR = "info_bar";
   public static final String PROGRESS_COMPLETION = "progress_completion";
   public static final String PROGRESS_LOG = "progress_log";
   public static final String SCREEN_READER_NOT_ENABLED = "screen_reader_not_enabled";
   public static final String SESSION_STATE = "session_state";
   public static final String TAB_KEY_MODE = "tab_key_mode";
   public static final String TOOLBAR_VISIBILITY = "toolbar_visibility";
   public static final String WARNING_BAR = "warning_bar";
   
   // Announcement requested by a user, not controlled by a preference since it is on-demand.
   // Do not include in the announcements_ map.
   public static final String ON_DEMAND = "on_demand";

   // Milliseconds to wait before making an announcement at session load
   public static final int STARTUP_ANNOUNCEMENT_DELAY = 3000;

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
      announcements_.put(INFO_BAR, "Announce info bars");
      announcements_.put(PROGRESS_COMPLETION, "Announce task completion");
      announcements_.put(PROGRESS_LOG, "Announce task progress details");
      announcements_.put(SCREEN_READER_NOT_ENABLED, "Announce screen reader not enabled");
      announcements_.put(SESSION_STATE, "Announce changes in session state");
      announcements_.put(TAB_KEY_MODE, "Announce tab key focus mode change");
      announcements_.put(TOOLBAR_VISIBILITY, "Announce toolbar visibility change");
      announcements_.put(WARNING_BAR, "Announce warning bars");
   }

   /**
    * Update live region so screen reader will announce.
    * @param announcementId unique identifier of this alert
    * @param message string to announce
    * @param timing IMMEDIATE or DEBOUNCED
    * @param severity STATUS (polite) or ALERT (assertive, use sparingly)
    */
   public void announce(String announcementId,
                        String message,
                        Timing timing,
                        Severity severity)
   {
      if (!announcements_.containsKey(announcementId))
         Debug.logWarning("Unregistered live announcement: " + announcementId);

      if (isDisabled(announcementId))
         return;

      eventBus_.fireEvent(new AriaLiveStatusEvent(message, timing, severity));
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


   private final Map<String, String> announcements_;

   // injected
   private final EventBus eventBus_;
   private final Provider<UserPrefs> pUserPrefs_;
}
