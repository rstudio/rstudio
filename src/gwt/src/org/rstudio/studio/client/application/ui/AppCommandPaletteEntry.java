/*
 * AppCommandPaletteEntry.java
 *
 * Copyright (C) 2009-19 by RStudio, PBC
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
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.KeySequence;

public class AppCommandPaletteEntry extends CommandPaletteEntry
{
   public AppCommandPaletteEntry(AppCommand command, List<KeySequence> keys)
   {
      super(keys);
      label_ = command.getLabel();
      if (StringUtil.isNullOrEmpty(label_))
         label_ = command.getButtonLabel();
      if (StringUtil.isNullOrEmpty(label_))
         label_ = command.getDesc();
      if (StringUtil.isNullOrEmpty(label_))
         label_ = command.getMenuLabel(false);
      if (StringUtil.isNullOrEmpty(label_))
         label_ = "";
      command_ = command;
      initialize();
   }
   
   public String getLabel()
   {
      return label_;
   }
   
   public void invoke()
   {
      command_.execute();
   }
   
   public String getId()
   {
      return command_.getId();
   }

   @Override
   public String getContext()
   {
      return null;
   }

   private String label_;
   private final AppCommand command_;
}
