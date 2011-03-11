/*
 * FontSizeManager.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.ui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.FontSizer.Size;
import org.rstudio.studio.client.application.events.ChangeFontSizeEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

@Singleton
public class FontSizeManager
{
   @Inject
   public FontSizeManager(final EventBus events,
                          UIPrefs prefs)
   {
      prefs.fontSize().bind(new CommandWithArg<String>()
      {
         public void execute(String value)
         {
            final Size DEFAULT_SIZE = Size.Pt12;
            try
            {
               if (value != null)
                  size_ = FontSizer.Size.valueOf(value);
               else
                  size_ = DEFAULT_SIZE;
            }
            catch (Exception e)
            {
               size_ = DEFAULT_SIZE;
            }
            events.fireEvent(new ChangeFontSizeEvent(size_));
         }
      });
   }

   public Size getSize()
   {
      return size_;
   }

   private Size size_;
}
