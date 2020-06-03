/*
 * PanmirrorCommandPaletteItem.java
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

import org.rstudio.studio.client.palette.model.CommandPaletteItem;

import com.google.gwt.user.client.ui.Widget;

public class PanmirrorCommandPaletteItem implements CommandPaletteItem
{
   public PanmirrorCommandPaletteItem(PanmirrorCommandUI cmd)
   {
      cmd_ = cmd;
   }
   
   @Override
   public Widget asWidget()
   {
      if (widget_ == null)
      {
         widget_ = new PanmirrorCommandPaletteEntry(cmd_, this);
      }

      return widget_;
   }

   @Override
   public void invoke()
   {
      cmd_.execute();
   }

   @Override
   public boolean matchesSearch(String[] keywords)
   {
      String hay = widget_.getLabel();
      for (String needle: keywords)
      {
         if (hay.contains(needle))
         {
            return true;
         }
      }
      return false;
   }

   @Override
   public void setSearchHighlight(String[] keywords)
   {
      widget_.setSearchHighlight(keywords);
   }

   @Override
   public boolean dismissOnInvoke()
   {
      return true;
   }

   @Override
   public void setSelected(boolean selected)
   {
      widget_.setSelected(selected);
   }

   private final PanmirrorCommandUI cmd_;
   private PanmirrorCommandPaletteEntry widget_;
}
