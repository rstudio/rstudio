/*
 * Prefs.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.prefs.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.js.JsObject;

import java.util.HashMap;

public abstract class Prefs
{
   public interface PrefValue<T> extends HasValueChangeHandlers<T>
   {
      // get accessor for prefs -- this automatically checks the project
      // prefs, then the global prefs, then returns the default. this should
      // be called by user code that wants to depend on prefs
      T getValue();
      
      // explicit get and set of global pref values -- these should be used by
      // preferences UI and be followed by a call to server.setUiPrefs to 
      // make sure they are persisted
      T getGlobalValue();
      void setGlobalValue(T value);
      void setGlobalValue(T value, boolean fireEvents);
      
      // explicit set for project values -- these are here so that the project
      // options dialog can notify other modules that preferences have changed
      // these values are not persisted by this module (rather, the project 
      // options dialog has its own codepath to read and write them along with
      // the other non-uipref project options)
      void setProjectValue(T value);
      void setProjectValue(T value, boolean fireEvents);
      
      HandlerRegistration bind(CommandWithArg<T> handler);
   }

   private abstract class JsonValue<T> implements PrefValue<T>
   {
      public JsonValue(String name, T defaultValue)
      {
         name_ = name;
         defaultValue_ = defaultValue;
      }

      public HandlerRegistration bind(final CommandWithArg<T> handler)
      {
         HandlerRegistration reg = addValueChangeHandler(new ValueChangeHandler<T>()
         {
            public void onValueChange(ValueChangeEvent<T> e)
            {
               handler.execute(e.getValue());
            }
         });
         handler.execute(getValue());
         return reg;
      }
      
      public T getValue()
      {
         if (projectRoot_.hasKey(name_))
            return doGetValue(projectRoot_);
         else
            return getGlobalValue();
      }

      public T getGlobalValue()
      {
         if (!globalRoot_.hasKey(name_))
            return defaultValue_;
         return doGetValue(globalRoot_);
      }

      public abstract T doGetValue(JsObject root);

      public void setGlobalValue(T value)
      {
         setGlobalValue(value, true);
      }

      public void setGlobalValue(T value, boolean fireEvents)
      {
         setValue(globalRoot_, value, fireEvents);
      }
      
      public void setProjectValue(T value)
      {
         setProjectValue(value, true);
      }
      
      public void setProjectValue(T value, boolean fireEvents)
      {
         setValue(projectRoot_, value, fireEvents);
      }

      protected abstract void doSetValue(JsObject root, String name, T value);

      public HandlerRegistration addValueChangeHandler(
            ValueChangeHandler<T> handler)
      {
         return handlerManager_.addHandler(ValueChangeEvent.getType(), handler);
      }

      public void fireEvent(GwtEvent<?> event)
      {
         handlerManager_.fireEvent(event);
      }
      
      private void setValue(JsObject root, T value, boolean fireEvents)
      {
         T val = doGetValue(root);

         if (value == null && val == null)
            return;
         if (value != null && val != null && value.equals(val))
            return;

         doSetValue(root, name_, value);
         if (fireEvents)
            ValueChangeEvent.fire(this, getValue());
         
      }

      protected final String name_;
      private final T defaultValue_;
      private final HandlerManager handlerManager_ = new HandlerManager(this);
   }

   private class BooleanValue extends JsonValue<Boolean>
   {
      private BooleanValue(String name, Boolean defaultValue)
      {
         super(name, defaultValue);
      }

      @Override
      public Boolean doGetValue(JsObject root)
      {
         return root.getBoolean(name_);
      }

      @Override
      protected void doSetValue(JsObject root, String name, Boolean value)
      {
         root.setBoolean(name, value);
      }
   }

   private class IntValue extends JsonValue<Integer>
   {
      private IntValue(String name, Integer defaultValue)
      {
         super(name, defaultValue);
      }

      @Override
      public Integer doGetValue(JsObject root)
      {
         return root.getInteger(name_);
      }

      @Override
      protected void doSetValue(JsObject root, String name, Integer value)
      {
         root.setInteger(name, value);
      }
   }

   private class DoubleValue extends JsonValue<Double>
   {
      private DoubleValue(String name, Double defaultValue)
      {
         super(name, defaultValue);
      }

      @Override
      public Double doGetValue(JsObject root)
      {
         return root.getDouble(name_);
      }

      @Override
      protected void doSetValue(JsObject root, String name, Double value)
      {
         root.setDouble(name, value);
      }
   }

   private class StringValue extends JsonValue<String>
   {
      private StringValue(String name, String defaultValue)
      {
         super(name, defaultValue);
      }

      @Override
      public String doGetValue(JsObject root)
      {
         return root.getString(name_);
      }

      @Override
      protected void doSetValue(JsObject root, String name, String value)
      {
         root.setString(name, value);
      }
   }

   private class ObjectValue<T extends JavaScriptObject> extends JsonValue<T>
   {
      private ObjectValue(String name)
      {
         super(name, null);
      }
      
      private ObjectValue(String name, T defaultValue)
      {
         super(name, defaultValue);
      }

      @Override
      public T doGetValue(JsObject root)
      {
         return root.<T>getObject(name_);
      }

      @Override
      protected void doSetValue(JsObject root, String name, T value)
      {
         root.setObject(name, value);
      }
   }

   public Prefs(JsObject root, JsObject projectRoot)
   {
      globalRoot_ = root;
      projectRoot_ = projectRoot;
   }

   @SuppressWarnings("unchecked")
   protected PrefValue<Boolean> bool(String name, boolean defaultValue)
   {
      PrefValue<Boolean> val = (PrefValue<Boolean>) values_.get(name);
      if (val == null)
      {
         val = new BooleanValue(name, defaultValue);
         values_.put(name, val);
      }
      return val;
   }

   @SuppressWarnings("unchecked")
   protected PrefValue<Integer> integer(String name, Integer defaultValue)
   {
      PrefValue<Integer> val = (PrefValue<Integer>) values_.get(name);
      if (val == null)
      {
         val = new IntValue(name, defaultValue);
         values_.put(name, val);
      }
      return val;
   }

   @SuppressWarnings("unchecked")
   protected PrefValue<Double> dbl(String name, Double defaultValue)
   {
      PrefValue<Double> val = (PrefValue<Double>) values_.get(name);
      if (val == null)
      {
         val = new DoubleValue(name, defaultValue);
         values_.put(name, val);
      }
      return val;
   }

   @SuppressWarnings("unchecked")
   protected PrefValue<String> string(String name, String defaultValue)
   {
      PrefValue<String> val = (PrefValue<String>) values_.get(name);
      if (val == null)
      {
         val = new StringValue(name, defaultValue);
         values_.put(name, val);
      }
      return val;
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   protected <T> PrefValue<T> object(String name)
   {
      PrefValue<T> val = (PrefValue<T>) values_.get(name);
      if (val == null)
      {
         val = new ObjectValue(name);
         values_.put(name, val);
      }
      return val;
   }
   
   @SuppressWarnings({ "unchecked" })
   protected <T extends JavaScriptObject> PrefValue<T> object(String name, 
                                                              T defaultValue)
   {
      PrefValue<T> val = (PrefValue<T>) values_.get(name);
      if (val == null)
      {
         val = new ObjectValue<T>(name, defaultValue);
         values_.put(name, val);
      }
      return val;
   }


   private final JsObject globalRoot_;
   private final JsObject projectRoot_;
   private final HashMap<String, PrefValue<?>> values_ =
         new HashMap<String, PrefValue<?>>();
}
