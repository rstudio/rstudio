/*
 * WarningBarList.java
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
package org.rstudio.studio.client.application.ui;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNull;
import com.google.gwt.json.client.JSONException;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.inject.Provider;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.application.AriaLiveService;

/** Application warnings, with independent actions and dismissals. */
public class WarningBarList extends Composite implements RequiresResize
{
   public WarningBarList(Provider<WarningBar> provider,
                         AriaLiveService ariaLive,
                         Command onChange,
                         Command onDismiss,
                         Command onEmptyFocus)
   {
      provider_ = provider;
      ariaLive_ = ariaLive;
      onChange_ = onChange;
      onDismiss_ = onDismiss;
      onEmptyFocus_ = onEmptyFocus;
      Roles.getStatusRole().set(status_.getElement());
      Roles.getAlertRole().set(alert_.getElement());
      announcement_.add(status_);
      announcement_.add(alert_);
      initWidget(scroll_);
      scroll_.getElement().getStyle().setOverflowX(Overflow.HIDDEN);
   }

   public void show(String id,
                    boolean severe,
                    String message,
                    boolean showLicenseButton,
                    String actionLabel,
                    Command action)
   {
      show(id, null, severe, message, showLicenseButton, actionLabel, action);
   }

   /**
    * Show a warning whose state, rather than its full text, decides whether
    * it was dismissed and whether an update is announced. Use this for
    * messages with live figures; a null state uses the full text.
    */
   public void show(String id,
                    String state,
                    boolean severe,
                    String message,
                    boolean showLicenseButton,
                    String actionLabel,
                    Command action)
   {
      String signature = signature(severe, message, showLicenseButton, actionLabel);
      if (state == null)
         state = signature;
      if (state.equals(dismissed_.get(id)))
         return;

      // A changed state is a new warning, even under the same id.
      dismissed_.remove(id);
      WarningBar bar = warnings_.get(id);
      boolean added = bar == null;
      if (added)
      {
         bar = provider_.get();
         bar.setWarningId(id);
         bar.setHeight(bar.getHeight() + "px");
         bar.addCloseHandler(event -> dismiss(id));
         bar.setDetailsReturnFocus(() ->
         {
            // The warning can resolve while its modal details dialog is open.
            WarningBar current = warnings_.get(id);
            if (current != null)
               current.focusDetailsButton();
            else
               focusWarning(0);
         });
         warnings_.put(id, bar);
         rows_.add(bar);
      }
      // Avoid announcing an identical warning again, but refresh its action.
      if (!signature.equals(signatures_.get(id)))
      {
         bar.setSeverity(severe);
         bar.setText(message);
         bar.showLicenseButton(showLicenseButton);
         if (!state.equals(states_.get(id)))
            queueAnnouncement(id, bar.getText(), severe);
      }
      bar.setAction(actionLabel, action);
      signatures_.put(id, signature);
      states_.put(id, state);

      // Relayout only when the row count changes; some warnings refresh on
      // every memory poll or keystroke.
      if (added)
      {
         rowsChanged();
         // Rows past the first few are out of view, and overlay scrollbars give
         // no sign of them. Don't scroll a focused row away, though.
         if (!getElement().isOrHasChild(DomUtils.getActiveElement()))
            scroll_.scrollToBottom();
      }
   }

   /** Let an explicit request show a warning again, even after its dismissal. */
   public void undismiss(String id)
   {
      dismissed_.remove(id);
   }

   @Override
   public void onResize()
   {
      scroll_.onResize();
   }

   public boolean contains(String id)
   {
      return warnings_.containsKey(id);
   }

   public int getPreferredHeight()
   {
      // Keep the workbench usable when several startup checks report at once.
      return Math.min(warnings_.size(), 3) * WarningBar.HEIGHT;
   }

   public void dismiss(String id)
   {
      if (!warnings_.containsKey(id))
         return;
      dismissed_.put(id, states_.get(id));
      remove(id);
      onDismiss_.execute();
   }

   /** Resolving a warning also allows it to be shown if it recurs later. */
   public void hide(String id)
   {
      dismissed_.remove(id);
      remove(id);
   }

   private void remove(String id)
   {
      WarningBar bar = warnings_.remove(id);
      signatures_.remove(id);
      states_.remove(id);
      pendingStatus_.remove(id);
      pendingAlerts_.remove(id);
      if (bar != null)
      {
         boolean hadFocus = bar.getElement().isOrHasChild(DomUtils.getActiveElement());
         int index = rows_.getWidgetIndex(bar);
         rows_.remove(bar);
         rowsChanged();
         if (hadFocus)
            focusWarning(index);
      }
   }

   private void rowsChanged()
   {
      // Number by position, so a lone warning is always "Warning 1".
      for (int i = 0; i < rows_.getWidgetCount(); i++)
         ((WarningBar) rows_.getWidget(i)).setWarningNumber(i + 1);
      onChange_.execute();
   }

   private void focusWarning(int index)
   {
      if (rows_.getWidgetCount() > 0)
         ((WarningBar) rows_.getWidget(Math.min(index, rows_.getWidgetCount() - 1))).focusDismissButton();
      else
         onEmptyFocus_.execute();
   }

   /** Kept outside the warning landmark so browsing a row reads its message once. */
   public Widget getAnnouncementWidget()
   {
      return announcement_;
   }

   private void queueAnnouncement(String id, String message, boolean severe)
   {
      if (ariaLive_.isDisabled(AriaLiveService.WARNING_BAR))
         return;
      // Severe warnings interrupt other speech, as their alert rows used to.
      (severe ? pendingAlerts_ : pendingStatus_).put(id, message);
      (severe ? pendingStatus_ : pendingAlerts_).remove(id);
      if (!announceTimer_.isRunning())
      {
         clearAnnouncementTimer_.cancel();
         clearAnnouncements();
         announceTimer_.schedule(AriaLiveService.UI_ANNOUNCEMENT_DELAY);
      }
   }

   private void clearAnnouncements()
   {
      status_.setText("");
      alert_.setText("");
   }

   private static String joinAnnouncements(Map<String, String> pending)
   {
      StringBuilder message = new StringBuilder();
      for (String text : pending.values())
      {
         if (message.length() > 0)
            message.append("\n");
         message.append(text);
      }
      return message.toString();
   }

   @Override
   protected void onUnload()
   {
      announceTimer_.cancel();
      clearAnnouncementTimer_.cancel();
      pendingStatus_.clear();
      pendingAlerts_.clear();
      clearAnnouncements();
      super.onUnload();
   }

   public String getDismissedState()
   {
      JSONObject state = new JSONObject();
      for (Map.Entry<String, String> entry : dismissed_.entrySet())
         state.put(entry.getKey(), new JSONString(entry.getValue()));
      return state.toString();
   }

   public void restoreDismissedState(String value)
   {
      if (value == null || value.isEmpty())
         return;
      JSONObject state;
      try
      {
         state = JSONParser.parseStrict(value).isObject();
      }
      catch (JSONException e)
      {
         return;
      }
      if (state == null)
         return;
      for (String id : state.keySet())
      {
         JSONString signature = state.get(id).isString();
         if (signature == null)
            continue;
         dismissed_.put(id, signature.stringValue());
         // Some warnings can arrive before session initialization completes.
         if (signature.stringValue().equals(states_.get(id)))
            remove(id);
      }
   }

   public static String messageId(String message, String actionLabel)
   {
      return "message:" + signature(false, message, false, actionLabel);
   }

   private static String signature(boolean severe, String message, boolean license, String actionLabel)
   {
      JSONArray parts = new JSONArray();
      parts.set(0, new JSONString(Boolean.toString(severe)));
      parts.set(1, new JSONString(message));
      parts.set(2, new JSONString(Boolean.toString(license)));
      parts.set(3, actionLabel == null ? JSONNull.getInstance() : new JSONString(actionLabel));
      return parts.toString();
   }

   // Coalesce updates to each row and announce a startup burst in one polite
   // update. Using the general status reporter here would replace earlier messages.
   private final Timer announceTimer_ = new Timer()
   {
      @Override
      public void run()
      {
         if (!ariaLive_.isDisabled(AriaLiveService.WARNING_BAR))
         {
            status_.setText(joinAnnouncements(pendingStatus_));
            alert_.setText(joinAnnouncements(pendingAlerts_));
            clearAnnouncementTimer_.schedule(4000);
         }
         pendingStatus_.clear();
         pendingAlerts_.clear();
      }
   };
   private final Timer clearAnnouncementTimer_ = new Timer()
   {
      @Override
      public void run()
      {
         clearAnnouncements();
      }
   };
   private final FlowPanel announcement_ = new FlowPanel();
   private final Label status_ = new Label();
   private final Label alert_ = new Label();
   private final Map<String, String> pendingStatus_ = new LinkedHashMap<>();
   private final Map<String, String> pendingAlerts_ = new LinkedHashMap<>();
   private final AriaLiveService ariaLive_;
   private final Command onEmptyFocus_;
   private final FlowPanel rows_ = new FlowPanel();
   private final ScrollPanel scroll_ = new ScrollPanel(rows_);
   private final Map<String, WarningBar> warnings_ = new LinkedHashMap<>();
   private final Map<String, String> signatures_ = new LinkedHashMap<>();
   private final Map<String, String> states_ = new LinkedHashMap<>();
   private final Map<String, String> dismissed_ = new LinkedHashMap<>();
   private final Provider<WarningBar> provider_;
   private final Command onChange_;
   private final Command onDismiss_;
}
