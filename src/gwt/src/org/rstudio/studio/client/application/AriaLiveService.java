/*
 * AriaLiveService.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.studio.client.application;

import com.google.gwt.core.client.GWT;
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
import java.util.HashSet;
import java.util.Map;

@Singleton
public class AriaLiveService
{
   // Unique identifiers for aria-live announcements. Use the same text for the
   // value as the constant name to ensure uniqueness. Add here and in the constructor below to
   // associate description for preferences UI.
   public static final String CONSOLE_CLEARED = "console_cleared";
   public static final String CONSOLE_LOG = "console_log";
   public static final String CONSOLE_COMMAND = "console_command";
   public static final String FILTERED_LIST = "filtered_list";
   public static final String GIT_MESSAGE_LENGTH = "git_message_length";
   public static final String INACCESSIBLE_FEATURE = "inaccessible_feature";
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

   // Milliseconds to wait before making an announcement after significant UI change
   public static final int UI_ANNOUNCEMENT_DELAY = 1000;

   @Inject
   public AriaLiveService(EventBus eventBus, Provider<UserPrefs> pUserPrefs)
   {
      eventBus_ = eventBus;
      pUserPrefs_ = pUserPrefs;

      announcements_ = new HashMap<>();
      announcements_.put(CONSOLE_CLEARED, constants_.consoleClearedAnnouncement());
      announcements_.put(CONSOLE_LOG, constants_.consoleOutputAnnouncement());
      announcements_.put(CONSOLE_COMMAND, constants_.consoleCommandAnnouncement());
      announcements_.put(FILTERED_LIST, constants_.filterResultCountAnnouncement());
      announcements_.put(GIT_MESSAGE_LENGTH, constants_.commitMessageLengthAnnouncement());
      announcements_.put(INACCESSIBLE_FEATURE, constants_.inaccessibleWarningAnnouncement());
      announcements_.put(INFO_BAR, constants_.infoBarsAnnouncement());
      announcements_.put(PROGRESS_COMPLETION, constants_.taskCompletionAnnouncement());
      announcements_.put(PROGRESS_LOG, constants_.taskProgressAnnouncement());
      announcements_.put(SCREEN_READER_NOT_ENABLED, constants_.screenReaderAnnouncement());
      announcements_.put(SESSION_STATE, constants_.sessionStateAnnouncement());
      announcements_.put(TAB_KEY_MODE, constants_.tabKeyFocusAnnouncement());
      announcements_.put(TOOLBAR_VISIBILITY, constants_.toolBarVisibilityAnnouncement());
      announcements_.put(WARNING_BAR, constants_.warningBarsAnnouncement());

      alwaysEnabledAnnouncements_ = new HashSet<>();
      alwaysEnabledAnnouncements_.add(ON_DEMAND);
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
      if (!announcements_.containsKey(announcementId) &&
         !alwaysEnabledAnnouncements_.contains(announcementId))
      {
         Debug.logWarning(constants_.unregisteredLiveAnnouncementMessage() + announcementId);
      }

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
   private final HashSet<String> alwaysEnabledAnnouncements_;

   // injected
   private final EventBus eventBus_;
   private final Provider<UserPrefs> pUserPrefs_;
   private static final StudioClientApplicationConstants constants_ = GWT.create(StudioClientApplicationConstants.class);
}
