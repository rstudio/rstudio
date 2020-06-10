/*
 * EventBasedChangeTracker.java
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
package org.rstudio.studio.client.workbench.model;

import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;

public class EventBasedChangeTracker<T> implements ChangeTracker
{
   public EventBasedChangeTracker(HasValueChangeHandlers<T> source)
   {
      source_ = source;
      source.addValueChangeHandler(new ValueChangeHandler<T>()
      {
         public void onValueChange(ValueChangeEvent<T> valueChangeEvent)
         {
            changed_ = true;
         }
      });
   }

   public boolean hasChanged()
   {
      return changed_;
   }

   public void reset()
   {
      changed_ = false;
   }

   public ChangeTracker fork()
   {
      EventBasedChangeTracker<T> ebct = new EventBasedChangeTracker<T>(source_);
      ebct.changed_ = changed_;
      return ebct;
   }

   protected boolean changed_ = false;
   private final HasValueChangeHandlers<T> source_;
}
