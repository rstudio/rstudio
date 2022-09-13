/*
 * CommandPaletteMruEntru.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.palette.model;

import com.google.gwt.core.client.JsArrayString;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.palette.ui.CommandPalette;

/**
 * Represents an entry in the list of most recently used (MRU) commands
 */
public class CommandPaletteMruEntry
{
   public CommandPaletteMruEntry(String scope, String id)
   {
      scope_ = scope;
      id_ = id;
   }

   /**
    * Returns the scope (source) of this MRU entry
    *
    * @return The entry's scope
    */
   public String getScope()
   {
      return scope_;
   }

   /**
    * Returns the ID of this MRU entry; only unique when combined with the scope
    *
    * @return The entry's ID
    */
   public String getId()
   {
      return id_;
   }

   /**
    * Constructs a new MRU entry from a raw string
    *
    * @param entry The entry as a string; must be in the format "scope|id"
    *
    * @return An MRU entry, or null if the string is not correctly formatted
    */
   public static CommandPaletteMruEntry fromString(String entry)
   {
      if (entry == null)
      {
         return null;
      }
      // Ensure entry is properly formatted
      JsArrayString parts = StringUtil.split(entry, CommandPalette.SCOPE_MRU_DELIMITER);
      if (parts.length() != 2)
      {
         Debug.logWarning("Unexpected Command Palette MRU entry format: '" + entry + "'");
         return null;
      }

      return new CommandPaletteMruEntry(parts.get(0), parts.get(1));
   }

   public boolean equals(CommandPaletteMruEntry other)
   {
      return StringUtil.equals(scope_, other.getScope()) &&
             StringUtil.equals(id_, other.getId());
   }

   public String toString()
   {
      return scope_ + CommandPalette.SCOPE_MRU_DELIMITER + id_;
   }

   private final String scope_;
   private final String id_;
}
