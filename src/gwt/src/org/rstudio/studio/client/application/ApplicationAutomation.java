/*
 * ApplicationAutomation.java
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

import java.util.Map;
import java.util.Set;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.server.model.DocumentCloseAllNoSaveEvent;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.Prefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.gwt.core.client.JsArrayString;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ApplicationAutomation
{
   static interface NullaryCallback<R>
   {
      public R execute();
   }

   static interface UnaryCallback<R, T>
   {
      public R execute(T value);
   }

   static interface BinaryCallback<R, T1, T2>
   {
      public R execute(T1 value1, T2 value2);
   }

   @Inject
   public ApplicationAutomation(Commands commands,
                                EventBus eventBus,
                                UserPrefs userPrefs)
   {
      commands_ = commands;
      eventBus_ = eventBus;
      userPrefs_ = userPrefs;
   }

   public final boolean isAutomationAgent()
   {
      return isAutomationAgent_;
   }

   public final void initializeAgent()
   {
      isAutomationAgent_ = true;

      initializeCallbacks();

      exportCallback("commandExecute", new UnaryCallback<Void, String>()
      {
         @Override
         public Void execute(String commandId)
         {
            AppCommand command = commands_.getCommandById(commandId);
            command.execute();
            return null;
         }
      });

      exportCallback("commandList", new NullaryCallback<JsArrayString>()
      {
         @Override
         public JsArrayString execute()
         {
            Map<String, AppCommand> allCommands = commands_.getCommands();
            Set<String> commandIds = allCommands.keySet();
            return JsUtil.toJsArrayString(commandIds);
         }
      });

      exportCallback("commandIsChecked", new UnaryCallback<Boolean, String>()
      {
         @Override
         public Boolean execute(String commandId)
         {
            AppCommand command = commands_.getCommandById(commandId);
            return command.isChecked();
         }
      });

      exportCallback("commandIsEnabled", new UnaryCallback<Boolean, String>()
      {
         @Override
         public Boolean execute(String commandId)
         {
            AppCommand command = commands_.getCommandById(commandId);
            return command.isEnabled();
         }
      });

      // Close every open source document, discarding unsaved changes. The
      // .rs.api.closeAllSourceBuffersWithoutSaving R API fires the same
      // DocumentCloseAllNoSave client event via a server roundtrip; this
      // callback lets tests skip that round trip so the R session isn't busy
      // executing R code while the close runs (which would surface the
      // "session is busy, are you sure?" confirmation dialog and hang).
      exportCallback("documentCloseAllNoSave", new NullaryCallback<Void>()
      {
         @Override
         public Void execute()
         {
            eventBus_.dispatchEvent(new DocumentCloseAllNoSaveEvent());
            return null;
         }
      });

      // Read a user preference by name. Returns the live (project-then-user-
      // then-default) value as a JS-marshalable Object: Boolean, String,
      // Integer/Double, or null when the pref is unknown.
      exportCallback("prefGet", new UnaryCallback<Object, String>()
      {
         @Override
         public Object execute(String name)
         {
            Prefs.PrefValue<?> pref = userPrefs_.getPrefValue(name);
            return pref == null ? null : pref.getValue();
         }
      });

      // Set a user preference and persist via the same RPC the Options dialog
      // uses. Dispatches by the pref's declared type rather than the JS value's
      // type so a JS number passed for a Boolean pref is a hard error, not a
      // silent coercion.
      exportCallback("prefSet", new BinaryCallback<Void, String, Object>()
      {
         @Override
         public Void execute(String name, Object value)
         {
            setPref(name, value);
            userPrefs_.writeUserPrefs();
            return null;
         }
      });

      // Remove a user-layer preference value, falling back to the default.
      // Mirrors .rs.uiPrefs$<name>$clear().
      exportCallback("prefClear", new UnaryCallback<Void, String>()
      {
         @Override
         public Void execute(String name)
         {
            Prefs.PrefValue<?> pref = userPrefs_.getPrefValue(name);
            if (pref == null)
               throw new RuntimeException("Unknown user preference: " + name);
            pref.removeGlobalValue(true);
            userPrefs_.writeUserPrefs();
            return null;
         }
      });
   }

   @SuppressWarnings("unchecked")
   private void setPref(String name, Object value)
   {
      Prefs.PrefValue<?> pref = userPrefs_.getPrefValue(name);
      if (pref == null)
         throw new RuntimeException("Unknown user preference: " + name);

      if (pref instanceof Prefs.BooleanValue)
      {
         ((Prefs.PrefValue<Boolean>) pref).setGlobalValue((Boolean) value);
      }
      else if (pref instanceof Prefs.IntValue)
      {
         // JS numbers arrive as java.lang.Double; narrow before assigning.
         ((Prefs.PrefValue<Integer>) pref).setGlobalValue(((Number) value).intValue());
      }
      else if (pref instanceof Prefs.DoubleValue)
      {
         ((Prefs.PrefValue<Double>) pref).setGlobalValue(((Number) value).doubleValue());
      }
      else if (pref instanceof Prefs.StringValue || pref instanceof Prefs.EnumValue)
      {
         ((Prefs.PrefValue<String>) pref).setGlobalValue((String) value);
      }
      else
      {
         throw new RuntimeException(
            "Unsupported preference type for " + name + ": " + pref.getClass().getSimpleName());
      }
   }

   private native final void initializeCallbacks()
   /*-{
      $wnd.rstudioCallbacks = $wnd.rstudioCallbacks || {};
   }-*/;

   private native final <R> void exportCallback(String name, NullaryCallback<R> callback)
   /*-{
      $wnd.rstudioCallbacks[name] = $entry(function() {
         return callback.@org.rstudio.studio.client.application.ApplicationAutomation.NullaryCallback::execute(*)();
      });
   }-*/;

   private native final <R, T> void exportCallback(String name, UnaryCallback<R, T> callback)
   /*-{
      $wnd.rstudioCallbacks[name] = $entry(function(value) {
         return callback.@org.rstudio.studio.client.application.ApplicationAutomation.UnaryCallback::execute(*)(value);
      });
   }-*/;

   private native final <R, T1, T2> void exportCallback(String name, BinaryCallback<R, T1, T2> callback)
   /*-{
      $wnd.rstudioCallbacks[name] = $entry(function(value1, value2) {
         return callback.@org.rstudio.studio.client.application.ApplicationAutomation.BinaryCallback::execute(*)(value1, value2);
      });
   }-*/;

   private final Commands commands_;
   private final EventBus eventBus_;
   private final UserPrefs userPrefs_;
   private boolean isAutomationAgent_ = false;
}
