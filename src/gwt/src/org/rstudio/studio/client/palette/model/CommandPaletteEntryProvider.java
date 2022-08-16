/*
 * CommandPaletteEntryProvider.java
 *
 * Copyright (C) 2022 by Posit, PBC
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

import java.util.List;

public interface CommandPaletteEntryProvider
{
   /**
    * A list of all of the elements to be rendered as entries in the palette.
    *
    * @return A list of elements.
    */
   List<CommandPaletteItem> getCommandPaletteItems();

   /**
    * Retrieves a specific palette item by ID.
    *
    * @param id The ID to look up
    * @return A command palette item with the given ID, or null if no item
    *    with the given ID exists.
    */
   CommandPaletteItem getCommandPaletteItem(String id);

   /**
    * Gets the scope name supplied by this provider.
    *
    * @return The name of the scope
    */
   String getProviderScope();
}
