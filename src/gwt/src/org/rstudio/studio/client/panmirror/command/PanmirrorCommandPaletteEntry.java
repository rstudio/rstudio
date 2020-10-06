/*
 * PanmirrorCommandPaletteEntry.java
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

package org.rstudio.studio.client.panmirror.command;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.command.KeySequence;
import org.rstudio.studio.client.palette.ui.CommandPaletteCommand;

public class PanmirrorCommandPaletteEntry extends CommandPaletteCommand
{
   public PanmirrorCommandPaletteEntry(PanmirrorCommandUI command, 
                                       PanmirrorCommandPaletteItem item)
   {
      super(keySequence(command), item);
      command_ = command;
      initialize();
   }

   @Override
   public String getLabel()
   {
      return command_.getFullMenuText();
   }

   @Override
   public String getId()
   {
      return command_.getId();
   }

   @Override
   public String getContext()
   {
      return "Visual Editor";
   }

   @Override
   public boolean enabled()
   {
      return command_.isEnabled();
   }
   
   private PanmirrorCommandUI command_;

   private static List<KeySequence> keySequence(PanmirrorCommandUI command)
   {
      List<KeySequence> keys = new ArrayList<KeySequence>();
      KeySequence keySequence = command.getKeySequence();
      if (keySequence != null)
         keys.add(keySequence);
      return keys;
   }
}
