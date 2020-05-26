/*
 * DataTableColumnWidget.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.event.dom.client.*;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;

import java.util.function.BiConsumer;

public class DataTableColumnWidget extends TextBox
{
   public DataTableColumnWidget(BiConsumer<Integer, Integer> onEnter)
   {
      onEnterFunction_ = onEnter;
      ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();
      setStylePrimaryName(styles.dataTableColumnWidget());
      DomUtils.disableSpellcheck(this);

      init();
   }

   /**
    * Parse the contents of getValue() and return an array with offset and max
    * Valid values of the textbox are either a single integer or two integers separated by a dash, spaces are trimmed
    * Any other values of the textbox are ignored
    * @return int[offset, max]
    */
   private int[] getValueOffsetAndMax()
   {
      int[] offsetAndMax = {-1, -1};

      String value = getValue();
      if (value == null || value.length() < 1) { return offsetAndMax; }

      String[] values = value.split("-");
      try
      {
         // starting at column 1 means no offset
         offsetAndMax[0] = Integer.parseInt(values[0].trim()) - 1;
      }
      catch (NumberFormatException e)
      {
         return offsetAndMax;
      }

      if (values.length > 1)
      {
         try
         {
            int endIndex = Integer.parseInt(values[1].trim());
            offsetAndMax[1] = endIndex - offsetAndMax[0];
         } catch (NumberFormatException e)
         {
            return offsetAndMax;
         }
      }

      return offsetAndMax;
   }

   private void init()
   {
      DataTableColumnWidget tb = this;
      addFocusHandler(new FocusHandler()
      {
         @Override
         public void onFocus(FocusEvent event)
         {
            selectAll();
         }
      });

      addKeyPressHandler(new KeyPressHandler()
      {
         @Override
         public void onKeyPress(KeyPressEvent event)
         {
            char charCode = event.getCharCode();
            if (charCode == KeyCodes.KEY_ENTER)
            {
               int[] values = tb.getValueOffsetAndMax();
               onEnterFunction_.accept(values[0], values[1]);

               tb.setFocus(false);
            }
         }
      });
   }

   private BiConsumer<Integer, Integer> onEnterFunction_;
}
