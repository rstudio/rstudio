/*
 * CommandPaletteEntrySource.java
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
package org.rstudio.studio.client.palette.model;

import com.google.gwt.user.client.ui.IsWidget;

public interface CommandPaletteItem extends IsWidget
{
   /**
    * Invoke the entry (execute the command, etc.)
    */
   public void invoke();
   
   /**
    * Does this item match the given search keywords?
    * 
    * @param keywords The keywords to match on
    * 
    * @return True if the palete item matches; false otherwise
    */
   boolean matchesSearch(String[] keywords);

   /**
    * Turns on search highlighting for the command.
    * 
    * @param keywords The search keywords to highlight.
    */
   public void setSearchHighlight(String[] keywords);

   /**
    * Dismiss after invoke?
    * 
    * @return Whether to dismiss the palette after invoking the entry.
    */
   public boolean dismissOnInvoke();
}
