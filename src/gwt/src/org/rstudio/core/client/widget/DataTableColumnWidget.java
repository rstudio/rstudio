/*
 * DataTableColumnWidget.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.event.dom.client.*;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;

import java.util.function.Consumer;

/**
 * "Go to column" jump box for the data viewer toolbar. Pressing Enter parses
 * the contents as a 1-based column index and invokes the supplied callback;
 * non-numeric input is ignored. (Pre column-virtualization this widget showed
 * and edited the paginated column window's range; with the grid scrolling
 * continuously through every column, jumping is the part still worth a
 * dedicated control.)
 */
public class DataTableColumnWidget extends TextBox
{
   public DataTableColumnWidget(Consumer<Integer> onEnter)
   {
      onEnterFunction_ = onEnter;
      ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();
      setStylePrimaryName(styles.dataTableColumnWidget());
      DomUtils.disableSpellcheck(this);

      init();
   }

   /**
    * Parse the contents of getValue() as a 1-based column index.
    * @return the column index, or -1 when the contents aren't a number
    */
   private int getValueColumn()
   {
      String value = getValue();
      if (value == null)
         return -1;

      try
      {
         return Integer.parseInt(value.trim());
      }
      catch (NumberFormatException e)
      {
         return -1;
      }
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
               int column = tb.getValueColumn();
               if (column >= 1)
                  onEnterFunction_.accept(column);

               tb.setFocus(false);
            }
         }
      });
   }

   private Consumer<Integer> onEnterFunction_;
}
