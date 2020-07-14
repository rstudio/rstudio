/*
 * RAddinPaletteItem.java
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

package org.rstudio.studio.client.palette;

import java.util.List;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeySequence;
import org.rstudio.studio.client.palette.ui.RAddinCommandPaletteEntry;
import org.rstudio.studio.client.workbench.addins.Addins.AddinExecutor;
import org.rstudio.studio.client.workbench.addins.Addins.RAddin;

public class RAddinPaletteItem extends BasePaletteItem<RAddinCommandPaletteEntry>
{
   public RAddinPaletteItem(RAddin addin, AddinExecutor executor, List<KeySequence> keys)
   {
      addin_ = addin;
      executor_ = executor;
      keys_ = keys;
      label_ = addin_.getName();
      if (StringUtil.isNullOrEmpty(label_))
         label_ = addin_.getTitle();
      if (StringUtil.isNullOrEmpty(label_))
         label_ = addin_.getDescription();
      if (StringUtil.isNullOrEmpty(label_))
         label_ = "";
   }

   @Override
   public RAddinCommandPaletteEntry createWidget()
   {
      return new RAddinCommandPaletteEntry(addin_, label_, keys_, this);
   }

   @Override
   public void invoke(InvocationSource source)
   {
      executor_.execute(addin_);
   }

   @Override
   public boolean matchesSearch(String[] keywords)
   {
      return super.labelMatchesSearch(addin_.getPackage() + " " + label_, keywords);
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
   
   private String label_;
   
   private final RAddin addin_;
   private final AddinExecutor executor_;
   private final List<KeySequence> keys_;
}
