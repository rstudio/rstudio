/*
 * FontSizeManager.java
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
package org.rstudio.studio.client.workbench.ui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.studio.client.application.events.ChangeFontSizeEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

@Singleton
public class FontSizeManager
{
   @Inject
   public FontSizeManager(final EventBus events,
                          UserPrefs prefs)
   {
      prefs.fontSizePoints().bind(new CommandWithArg<Double>()
      {
         public void execute(Double value)
         {
            final int DEFAULT_SIZE = 9;
            try
            {
               if (value != null)
                  size_ = value;
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

   public double getSize()
   {
      return size_;
   }

   private double size_;
}
