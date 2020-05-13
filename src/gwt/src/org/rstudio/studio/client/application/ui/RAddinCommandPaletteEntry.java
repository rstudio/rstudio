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
package org.rstudio.studio.client.application.ui;

import java.util.List;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeySequence;
import org.rstudio.studio.client.workbench.addins.Addins.AddinExecutor;
import org.rstudio.studio.client.workbench.addins.Addins.RAddin;

/**
 * RAddinCommandPaletteEntry is a widget that represents a command furnished by
 * an RStudio Addin in RStudio's command palette.
 */
public class RAddinCommandPaletteEntry extends CommandPaletteEntry
{
   public RAddinCommandPaletteEntry(RAddin addin, AddinExecutor executor, 
                                    List<KeySequence> keys)
   {
      super(keys);
      addin_ = addin;
      executor_ = executor;
      label_ = addin_.getName();
      if (StringUtil.isNullOrEmpty(label_))
         label_ = addin_.getTitle();
      if (StringUtil.isNullOrEmpty(label_))
         label_ = addin_.getDescription();
      if (StringUtil.isNullOrEmpty(label_))
         label_ = "";

      initialize();
   }
   
   @Override
   public String getLabel()
   {
      return label_;
   }

   @Override
   public void invoke()
   {
      executor_.execute(addin_);
   }

   @Override
   public String getId()
   {
      return addin_.getId();
   }

   @Override
   public String getContext()
   {
      return "&#9865; " + addin_.getPackage();
   }

   private String label_;
   private final RAddin addin_;
   private final AddinExecutor executor_;
}
