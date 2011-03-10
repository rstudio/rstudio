package org.rstudio.studio.client.workbench.prefs.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HasValue;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.js.JsObject;

import java.util.HashMap;

public abstract class Prefs
{
   public interface PrefValue<T> extends HasValue<T>
   {
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
         if (!root_.hasKey(name_))
            return defaultValue_;
         return doGetValue();
      }

      public abstract T doGetValue();

      public void setValue(T value)
      {
         setValue(value, true);
      }

      public void setValue(T value, boolean fireEvents)
      {
         T val = doGetValue();

         if (value == null && val == null)
            return;
         if (value != null && val != null && value.equals(val))
            return;

         doSetValue(name_, value);
         if (fireEvents)
            ValueChangeEvent.fire(this, value);
      }

      protected abstract void doSetValue(String name, T value);

      public HandlerRegistration addValueChangeHandler(
            ValueChangeHandler<T> handler)
      {
         return handlerManager_.addHandler(ValueChangeEvent.getType(), handler);
      }

      public void fireEvent(GwtEvent<?> event)
      {
         handlerManager_.fireEvent(event);
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
      public Boolean doGetValue()
      {
         return root_.getBoolean(name_);
      }

      @Override
      protected void doSetValue(String name, Boolean value)
      {
         root_.setBoolean(name, value);
      }
   }

   private class IntValue extends JsonValue<Integer>
   {
      private IntValue(String name, Integer defaultValue)
      {
         super(name, defaultValue);
      }

      @Override
      public Integer doGetValue()
      {
         return root_.getInteger(name_);
      }

      @Override
      protected void doSetValue(String name, Integer value)
      {
         root_.setInteger(name, value);
      }
   }

   private class StringValue extends JsonValue<String>
   {
      private StringValue(String name, String defaultValue)
      {
         super(name, defaultValue);
      }

      @Override
      public String doGetValue()
      {
         return root_.getString(name_);
      }

      @Override
      protected void doSetValue(String name, String value)
      {
         root_.setString(name, value);
      }
   }

   private class ObjectValue<T extends JavaScriptObject> extends JsonValue<T>
   {
      private ObjectValue(String name)
      {
         super(name, null);
      }

      @Override
      public T doGetValue()
      {
         return root_.<T>getObject(name_);
      }

      @Override
      protected void doSetValue(String name, T value)
      {
         root_.setObject(name, value);
      }
   }

   public Prefs(JsObject root)
   {
      root_ = root;
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

   @SuppressWarnings("unchecked")
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

   private final JsObject root_;
   private final HashMap<String, PrefValue<?>> values_ =
         new HashMap<String, PrefValue<?>>();
}
