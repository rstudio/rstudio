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

import org.rstudio.studio.client.palette.BasePaletteItem;

public class PanmirrorCommandPaletteItem extends BasePaletteItem<PanmirrorCommandPaletteEntry>
{
   public PanmirrorCommandPaletteItem(PanmirrorCommandUI cmd)
   {
      cmd_ = cmd;
   }
   
   @Override
   public PanmirrorCommandPaletteEntry createWidget()
   {
     return new PanmirrorCommandPaletteEntry(cmd_, this);
   }

   @Override
   public void invoke(InvocationSource source)
   {
      cmd_.execute();
   }

   @Override
   public boolean matchesSearch(String[] keywords)
   {
      return super.labelMatchesSearch("visual editor " + cmd_.getFullMenuText(), keywords);
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
}
