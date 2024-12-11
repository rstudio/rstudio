/*
 * FontSizeManager.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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
import org.rstudio.core.client.widget.FontSizer;
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
      events_ = events;
      prefs_ = prefs;
      
      commonInit();
   }
   
   private void commonInit()
   {
      fontSize_ = normalizeSize(prefs_.fontSizePoints().getValue());
      lineHeight_ = normalizeHeight(prefs_.editorLineHeight().getValue());
      
      prefs_.fontSizePoints().bind(new CommandWithArg<Double>()
      {
         public void execute(Double value)
         {
            fontSize_ = normalizeSize(value);
            events_.fireEvent(new ChangeFontSizeEvent(fontSize_, lineHeight_));
         }
      });
      
      prefs_.editorLineHeight().bind(new CommandWithArg<Double>()
      {
         @Override
         public void execute(Double value)
         {
            lineHeight_ = normalizeHeight(value);
         }
      });
   }
   
   private double normalizeSize(Double value)
   {
      return value == null ? FONT_SIZE_DEFAULT : value;
   }
   
   private double normalizeHeight(Double value)
   {
      if (value == null || value == 0.0)
      {
         return FontSizer.getNormalLineHeight();
      }
      else
      {
         return value;
      }
   }

   public double getFontSize()
   {
      return fontSize_;
   }
   
   public double getLineHeight()
   {
      return lineHeight_;
   }
   
   
   private double fontSize_;
   private double lineHeight_;
   
   private final EventBus events_;
   private final UserPrefs prefs_;
   
   private static final double FONT_SIZE_DEFAULT = 9.0;
   
}
