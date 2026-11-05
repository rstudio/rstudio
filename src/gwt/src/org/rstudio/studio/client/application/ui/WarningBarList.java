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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gwt.dom.client.Style.Overflow;
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

/** Application warnings, with independent actions and dismissals. */
public class WarningBarList extends Composite implements RequiresResize
{
   public WarningBarList(Provider<WarningBar> provider, Command onChange, Command onDismiss)
   {
      provider_ = provider;
      onChange_ = onChange;
      onDismiss_ = onDismiss;
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
      String signature = signature(severe, message, showLicenseButton, actionLabel);
      if (signature.equals(dismissed_.get(id)))
         return;

      // A changed message or severity is a new warning, even at the same key.
      dismissed_.remove(id);
      WarningBar bar = warnings_.get(id);
      if (bar == null)
      {
         bar = provider_.get();
         bar.setWarningId(id);
         bar.setHeight(bar.getHeight() + "px");
         bar.addCloseHandler(event -> dismiss(id));
         warnings_.put(id, bar);
         rows_.add(bar);
      }
      // Avoid announcing an identical warning again, but refresh its action.
      if (!signature.equals(signatures_.get(id)))
      {
         bar.setSeverity(severe);
         bar.setText(message);
         bar.showLicenseButton(showLicenseButton);
      }
      bar.setAction(actionLabel, action);
      signatures_.put(id, signature);
      onChange_.execute();
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
      dismissed_.put(id, signatures_.get(id));
      remove(id);
      onDismiss_.execute();
   }

   /** Resolving a warning also allows it to be shown if it recurs later. */
   public void hide(String id)
   {
      dismissed_.remove(id);
      remove(id);
   }

   public void clear()
   {
      dismissed_.clear();
      for (String id : new ArrayList<>(warnings_.keySet()))
         remove(id);
   }

   private void remove(String id)
   {
      WarningBar bar = warnings_.remove(id);
      signatures_.remove(id);
      if (bar != null)
      {
         rows_.remove(bar);
         onChange_.execute();
      }
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
         if (signature.stringValue().equals(signatures_.get(id)))
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

   private final FlowPanel rows_ = new FlowPanel();
   private final ScrollPanel scroll_ = new ScrollPanel(rows_);
   private final Map<String, WarningBar> warnings_ = new LinkedHashMap<>();
   private final Map<String, String> signatures_ = new LinkedHashMap<>();
   private final Map<String, String> dismissed_ = new LinkedHashMap<>();
   private final Provider<WarningBar> provider_;
   private final Command onChange_;
   private final Command onDismiss_;
}
