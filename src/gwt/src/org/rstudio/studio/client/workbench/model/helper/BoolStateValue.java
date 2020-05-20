/*
 * BoolStateValue.java
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

import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.workbench.model.ClientInitState;
import org.rstudio.studio.client.workbench.model.ClientState;

public abstract class BoolStateValue extends ClientStateValue<Boolean>
{
   public BoolStateValue(String group,
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
   protected final Boolean doGet(JsObject group, String name)
   {
      return group.getBoolean(name);
   }

   @Override
   protected final void doSet(ClientState state,
                        String group,
                        String name,
                        Boolean value,
                        int persist)
   {
      state.putBoolean(group, name, value, persist);
   }
}
