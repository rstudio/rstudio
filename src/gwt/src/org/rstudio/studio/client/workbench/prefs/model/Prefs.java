/*
 * Prefs.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.js.JsUtil;

import java.util.HashMap;

public abstract class Prefs
{
   public interface PrefValue<T> extends HasValueChangeHandlers<T>
   {
      // get accessor for prefs -- this automatically checks the project
      // prefs, then the global prefs, then returns the default. this should
      // be called by user code that wants to depend on prefs
      boolean hasValue();
      T getValue();
      
      // explicit get and set of global pref values -- these should be used by
      // preferences UI and be followed by a call to server.setUiPrefs to 
      // make sure they are persisted
      T getGlobalValue();
      void setGlobalValue(T value);
      void setGlobalValue(T value, boolean fireEvents);
      void removeGlobalValue(boolean fireEvents);
      
      // explicit set for project values -- these are here so that the project
      // options dialog can notify other modules that preferences have changed
      // these values are not persisted by this module (rather, the project 
      // options dialog has its own codepath to read and write them along with
      // the other non-uipref project options)
      T getProjectValue();
      boolean hasProjectValue();
      void setProjectValue(T value);
      void setProjectValue(T value, boolean fireEvents);
      void removeProjectValue(boolean fireEvents);
      
      // generic set for any layer
      void setValue(String layer, T value);
      
      /**
       * Gets the title of the preference (a short description in the imperative
       * mood)
       * 
       * @return The preference's title
       */
      String getTitle();
      
      /**
       * Gets the description of the preference
       * 
       * @return The preference's description
       */
      String getDescription();
      
      /**
       * Gets the identifier of the preference
       * 
       * @return The preference ID
       */
      String getId();
      
      HandlerRegistration bind(CommandWithArg<T> handler);
   }

   public abstract class JsonValue<T> implements PrefValue<T>
   {
      public JsonValue(String name, String title, String description, T defaultValue)
      {
         name_ = name;
         title_ = title;
         description_ = description;
         defaultValue_ = defaultValue;
      }
      
      public String getId()
      {
         return name_;
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

      public void setValue(String layerName, T value)
      {
         setValue(layerName, value, true);
      }
      
      public void setValue(String layerName, T value, boolean fireEvents)
      {
         for (PrefLayer layer: JsUtil.asIterable(layers_))
         {
            if (layer.getName() == layerName)
            {
               setValue(layer.getValues(), value, fireEvents);
               break;
            }
         }
      }
      
      public boolean hasValue()
      {
         for (PrefLayer layer: JsUtil.asReverseIterable(layers_))
         {
            if (layer.getValues().hasKey(name_))
            {
               return true;
            }
         }
         return false;
      }
      
      public T getValue()
      {
         // Work backwards through all layers, starting with the most specific
         // and working towards the most general.
         for (PrefLayer layer: JsUtil.asReverseIterable(layers_))
         {
            if (layer.getValues().hasKey(name_))
            {
               return doGetValue(layer.getValues());
            }
         }
         return defaultValue_;
      }

      public T getGlobalValue()
      {
         // Skip the project layer if it exists by starting at the user layer.
         for (int i = userLayer(); i >= 0; i--)
         {
            if (layers_.get(i).getValues().hasKey(name_))
            {
               return doGetValue(layers_.get(i).getValues());
            }
         }
         return defaultValue_;
      }

      public abstract T doGetValue(JsObject root);

      public void setGlobalValue(T value)
      {
         setGlobalValue(value, true);
      }

      public void setGlobalValue(T value, boolean fireEvents)
      {
         setValue(layers_.get(userLayer()).getValues(), value, fireEvents);
      }
      
      public void removeGlobalValue(boolean fireEvents)
      {
         boolean wasUnset = false;
         
         for (int i = userLayer(); i >= 0; i--)
         {
            JsObject layer = layers_.get(i).getValues();
            if (layer.hasKey(name_))
            {
               layer.unset(name_);
               wasUnset = true;
            }
         }
         
         if (fireEvents && wasUnset)
            ValueChangeEvent.fire(this, getValue());
      }
      
      public T getProjectValue()
      {
         JsObject projValues = layers_.get(projectLayer()).getValues();
         return doGetValue(projValues);
      }

      public boolean hasProjectValue()
      {
         JsObject projValues = layers_.get(projectLayer()).getValues();
         return projValues.hasKey(name_);
      }
      
      public void setProjectValue(T value)
      {
         setProjectValue(value, true);
      }
      
      public void setProjectValue(T value, boolean fireEvents)
      {
         setValue(layers_.get(projectLayer()).getValues(), value, fireEvents);
      }
      
      public void removeProjectValue(boolean fireEvents)
      {
         JsObject projValues = layers_.get(projectLayer()).getValues();
         if (projValues.hasKey(name_))
         {
            projValues.unset(name_);
            if (fireEvents)
               ValueChangeEvent.fire(this, getValue());
         }
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
      
      public String getTitle()
      {
         return title_;
      }
      
      public String getDescription()
      {
         return description_;
      }
      protected final String name_;
      private final String title_;
      private final String description_;
      private final T defaultValue_;
      private final HandlerManager handlerManager_ = new HandlerManager(this);
   }

   public class BooleanValue extends JsonValue<Boolean>
   {
      private BooleanValue(String name, String title, String description, Boolean defaultValue)
      {
         super(name, title, description, defaultValue);
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

   public class IntValue extends JsonValue<Integer>
   {
      private IntValue(String name, String title, String description, Integer defaultValue)
      {
         super(name, title, description, defaultValue);
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

   public class DoubleValue extends JsonValue<Double>
   {
      private DoubleValue(String name, String title, String description, Double defaultValue)
      {
         super(name, title, description, defaultValue);
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

   public class StringValue extends JsonValue<String>
   {
      private StringValue(String name, String title, String description, String defaultValue)
      {
         super(name, title, description, defaultValue);
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
   
   public class EnumValue extends JsonValue<String>
   {
      private EnumValue(String name, String title, String description, 
                        String[] values, String defaultValue)
      {
         super(name, title, description, defaultValue);
         allowedValues_ = values;
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
      
      public String[] getAllowedValues()
      {
         return allowedValues_;
      }
      
      private final String[] allowedValues_;
   }

   public class ObjectValue<T extends JavaScriptObject> extends JsonValue<T>
   {
      private ObjectValue(String name, String title, String description)
      {
         super(name, title, description, null);
      }
      
      private ObjectValue(String name, String title, String description, T defaultValue)
      {
         super(name, title, description, defaultValue);
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

   public Prefs(JsArray<PrefLayer> layers)
   {
      layers_ = layers;
   }
   
   public JsObject getUserLayer()
   {
      return layers_.get(userLayer()).getValues();
   }
   
   public PrefValue<?> getPrefValue(String name)
   {
      return values_.get(name);
   }
   
   public abstract int userLayer();
   public abstract int projectLayer();

   @SuppressWarnings("unchecked")
   protected PrefValue<Boolean> bool(
      String name, String title, String description, boolean defaultValue)
   {
      PrefValue<Boolean> val = (PrefValue<Boolean>) values_.get(name);
      if (val == null)
      {
         val = new BooleanValue(name, title, description, defaultValue);
         values_.put(name, val);
      }
      return val;
   }

   @SuppressWarnings("unchecked")
   protected PrefValue<Integer> integer(
      String name, String title, String description, Integer defaultValue)
   {
      PrefValue<Integer> val = (PrefValue<Integer>) values_.get(name);
      if (val == null)
      {
         val = new IntValue(name, title, description, defaultValue);
         values_.put(name, val);
      }
      return val;
   }

   @SuppressWarnings("unchecked")
   protected PrefValue<Double> dbl(
      String name, String title, String description, Double defaultValue)
   {
      PrefValue<Double> val = (PrefValue<Double>) values_.get(name);
      if (val == null)
      {
         val = new DoubleValue(name, title, description, defaultValue);
         values_.put(name, val);
      }
      return val;
   }

   @SuppressWarnings("unchecked")
   protected PrefValue<String> string(
      String name, String title, String description, String defaultValue)
   {
      PrefValue<String> val = (PrefValue<String>) values_.get(name);
      if (val == null)
      {
         val = new StringValue(name, title, description, defaultValue);
         values_.put(name, val);
      }
      return val;
   }

   @SuppressWarnings("unchecked")
   protected PrefValue<String> enumeration(
      String name, String title, String description, String[] values, String defaultValue)
   {
      PrefValue<String> val = (PrefValue<String>) values_.get(name);
      if (val == null)
      {
         val = new EnumValue(name, title, description, values, defaultValue);
         values_.put(name, val);
      }
      return val;
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   protected <T> PrefValue<T> object(String name, String title, String description)
   {
      PrefValue<T> val = (PrefValue<T>) values_.get(name);
      if (val == null)
      {
         val = new ObjectValue(name, title, description);
         values_.put(name, val);
      }
      return val;
   }
   
   @SuppressWarnings({ "unchecked" })
   protected <T extends JavaScriptObject> PrefValue<T> object(String name, 
           String title, String description, T defaultValue)
   {
      PrefValue<T> val = (PrefValue<T>) values_.get(name);
      if (val == null)
      {
         val = new ObjectValue<T>(name, title, description, defaultValue);
         values_.put(name, val);
      }
      return val;
   }
   
   // Meant to be called when the satellite window receives the sessionInfo.
   protected void updatePrefs(JsArray<PrefLayer> layers)
   {
      layers_ = layers;
   }
   
   private JsArray<PrefLayer> layers_;
   private final HashMap<String, PrefValue<?>> values_ =
         new HashMap<String, PrefValue<?>>();
}
