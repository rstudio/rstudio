/*
 * StringStateValue.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.model.helper;

import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.workbench.model.ClientInitState;
import org.rstudio.studio.client.workbench.model.ClientState;

public abstract class StringStateValue extends ClientStateValue<String>
{
   public StringStateValue(String group,
                           String name,
                           int persist,
                           ClientInitState state)
   {
      super(group,
            name,
            persist,
            state);
   }

   @Override
   protected final String doGet(JsObject group, String name)
   {
      return group.getString(name);
   }

   @Override
   protected final void doSet(ClientState state,
                        String group,
                        String name,
                        String value,
                        int persist)
   {
      state.putString(group, name, value, persist);
   }
}
