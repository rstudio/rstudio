/*
 * CommandPaletteCommand.java
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

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.command.KeyCombination;
import org.rstudio.core.client.command.KeySequence;
import org.rstudio.studio.client.palette.model.CommandPaletteItem;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public abstract class CommandPaletteCommand extends CommandPaletteEntry
{
   public CommandPaletteCommand(List<KeySequence> keys, CommandPaletteItem item)
   {
      super(item);
      keys_ = keys;
      shortcut_ = new Label();
      
      SafeHtmlBuilder b = new SafeHtmlBuilder();
      if (keys != null)
      {
         for (KeySequence k: keys_)
         {
            List<KeyCombination> combos = k.getData();
            for (int i = 0; i < combos.size(); i++)
            {
               KeyCombination combo = combos.get(i);
               if (combo.isCtrlPressed())
                  appendKey(b, "Ctrl");
               if (combo.isAltPressed())
                  appendKey(b, "Alt");
               if (combo.isShiftPressed())
                  appendKey(b, "Shift");
               if (combo.isMetaPressed())
                  appendKey(b, BrowseCap.hasMetaKey() ? "&#8984;" : "Cmd");
               appendKey(b, combo.key());
               
               // Is this a multi-key sequence?
               if (i < (combos.size() - 1))
               {
                  b.appendEscaped(",");
               }
            }
            break;
         }
         shortcut_.getElement().setInnerSafeHtml(b.toSafeHtml());
         shortcut_.addStyleName("rstudio-fixed-width-font");
      }
   }
   
   @Override
   public Widget getInvoker()
   {
      return shortcut_;
   }
   
   @Override
   public String getScope()
   {
      return "command";
   }
   
   @Override
   public boolean dismissOnInvoke()
   {
      // Dismiss the palette prior to invoking commands so that they don't act
      // on the palette itself.
      return true;
   }
   
   private void appendKey(SafeHtmlBuilder b, String key)
   {
      b.appendHtmlConstant("<span class=\"" + 
             styles_.keyboard() + "\">");
      b.appendHtmlConstant(key);
      b.appendHtmlConstant("</span>");
   }

   private final List<KeySequence> keys_;
   
   private Label shortcut_;
}
