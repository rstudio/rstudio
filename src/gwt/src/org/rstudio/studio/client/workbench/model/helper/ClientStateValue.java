/*
 * ClientStateValue.java
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
package org.rstudio.studio.client.workbench.model.helper;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.events.SaveClientStateEvent;
import org.rstudio.studio.client.workbench.model.ClientInitState;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.ValueChangeTracker;

/**
 * ClientStateValue makes it easy to hook a single value up to the ClientState
 * system. Subclass one of the type-specific abstract subclasses, and provide
 * implementations for onInit(T) and getValue().
 *
 * By default, getValue() will be called periodically to check for changes.
 * This will be called quite often so if it will be expensive and there's a
 * cheaper way to determine if the value changed, you can override hasChanged()
 * and have different logic.
 *
 * @param <T>
 */
public abstract class ClientStateValue<T> implements SaveClientStateEvent.Handler
{
   protected ClientStateValue(String group,
                              String name,
                              int persist,
                              ClientInitState state)
   {
      this(group, name, persist, state, false);
   }

   /**
    * If delayedInit == true, finishInit(ClientInitState) must be called
    * by the subclasser or caller, in order to complete object creation.
    */
   protected ClientStateValue(String group,
                              String name,
                              int persist,
                              ClientInitState state,
                              boolean delayedInit)
   {
      group_ = group;
      name_ = name;
      persist_ = persist;

      if (!delayedInit)
         finishInit(state);
   }

   protected void finishInit(ClientInitState state)
   {
      JsObject grp = state.peek(group_);
      T obj = doGet(grp, name_);
      valueTracker_ = new ValueChangeTracker<T>(obj);
      onInit(obj);

      EventBus evt = RStudioGinjector.INSTANCE.getEventBus();
      evt.addHandler(SaveClientStateEvent.TYPE, this);
   }

   protected abstract T doGet(JsObject group, String name);
   protected abstract void doSet(ClientState state,
                                 String group,
                                 String name,
                                 T value,
                                 int persist);

   protected abstract void onInit(T value);
   protected abstract T getValue();

   public final void onSaveClientState(SaveClientStateEvent event)
   {
      try
      {
         if (hasChanged())
         {
            ClientState clientState = event.getState();
            put(clientState, getValue());
         }
      }
      catch (Exception e)
      {
         Debug.log(e.toString());
      }
   }

   protected boolean hasChanged()
   {
      return valueTracker_.checkForChange(getValue());
   }

   protected void put(ClientState clientState, T newValue)
   {
      doSet(clientState, group_, name_, newValue, persist_);
   }

   private ValueChangeTracker<T> valueTracker_;
   private final String group_;
   private final String name_;
   private final int persist_;
}
