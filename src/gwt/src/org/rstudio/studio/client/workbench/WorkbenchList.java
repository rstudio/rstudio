/*
 * WorkbenchList.java
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
package org.rstudio.studio.client.workbench;

import java.util.ArrayList;

import org.rstudio.studio.client.workbench.events.ListChangedEvent;

import com.google.gwt.event.shared.HandlerRegistration;

/*
 * Interface to workbench lists. The contract is that the mutating functions
 * call the server and then an updated copy of the list is (eventually)
 * returned via the ListChangedEvent.Handler
 */
public interface WorkbenchList
{
   // mutating operations
   void setContents(ArrayList<String> list);
   void append(String item);
   void prepend(String item);
   void remove(String item);
   void clear();

   // change handler
   HandlerRegistration addListChangedHandler(ListChangedEvent.Handler handler);
}
