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
package org.rstudio.studio.client.application.ui;

import java.util.ArrayList;
import java.util.List;

public interface CommandPaletteEntrySource
{ 
   // get the list of entries
   List<CommandPaletteEntry> getCommandPaletteEntries();
   
   // join multiple sources into one
   public static CommandPaletteEntrySource join(List<CommandPaletteEntrySource> sources)
   {
      return new CommandPaletteEntrySource() 
      {
         @Override
         public List<CommandPaletteEntry> getCommandPaletteEntries()
         {
            List<CommandPaletteEntry> entries = new ArrayList<CommandPaletteEntry>();
            sources.forEach((CommandPaletteEntrySource source) -> {
               List<CommandPaletteEntry> sourceEntries = source.getCommandPaletteEntries();
               if (sourceEntries != null)
                  entries.addAll(sourceEntries);
            });
            return entries;
         } 
      };
   }
}
