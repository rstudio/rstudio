/*
 * RAddinCommandPaletteEntry.java
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
package org.rstudio.studio.client.palette.ui;

import java.util.List;

import org.rstudio.core.client.command.KeySequence;
import org.rstudio.studio.client.palette.model.CommandPaletteItem;
import org.rstudio.studio.client.workbench.addins.Addins.RAddin;

/**
 * RAddinCommandPaletteEntry is a widget that represents a command furnished by
 * an RStudio Addin in RStudio's command palette.
 */
public class RAddinCommandPaletteEntry extends CommandPaletteCommand
{
   public RAddinCommandPaletteEntry(RAddin addin, String label, List<KeySequence> keys,
                                    CommandPaletteItem item)
   {
      super(keys, item);
      addin_ = addin;
      label_ = label;

      initialize();
   }
   
   @Override
   public String getId()
   {
      return addin_.getId();
   }

   @Override
   public String getContext()
   {
      return addin_.getPackage();
   }

   @Override
   public boolean enabled()
   {
      // R Addins are always enabled.
      return true;
   }


   @Override
   public String getLabel()
   {
      return label_;
   }

   private final RAddin addin_;
   private final String label_;
}
