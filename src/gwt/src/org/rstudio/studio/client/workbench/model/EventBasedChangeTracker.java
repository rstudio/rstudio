/*
 * EventBasedChangeTracker.java
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
package org.rstudio.studio.client.workbench.model;

import com.google.gwt.event.logical.shared.HasValueChangeHandlers;

public class EventBasedChangeTracker<T> implements ChangeTracker
{
   public EventBasedChangeTracker(HasValueChangeHandlers<T> source)
   {
      source.addValueChangeHandler(event -> markChanged());
   }

   public boolean hasChanged()
   {
      return changed_;
   }

   public void reset()
   {
      changed_ = false;
   }

   public int getVersion()
   {
      return version_;
   }

   protected void markChanged()
   {
      changed_ = true;
      version_++;
   }

   private boolean changed_ = false;
   private int version_ = 0;
}
