/*
 * RawFormatSelect.java
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

package org.rstudio.studio.client.panmirror.dialogs;

import java.util.ArrayList;
import java.util.Arrays;

import org.rstudio.core.client.widget.SelectWidget;

public class PanmirrorRawFormatSelect extends SelectWidget
{
   public PanmirrorRawFormatSelect()
   {
      super("Format:", new String[] {}, new String[] {}, false);
   }
   
   public void setFormats(String[] formats, String value)
   {
      this.setChoices(
         getFormatList("(Choose Format)", formats, value),
         getFormatList("", formats, value)
      );
   }
   
   private static String[] getFormatList(String firstItem, String[] formats, String value)
   {
      ArrayList<String> options = new ArrayList<String>();
      options.add(firstItem);
      options.addAll(Arrays.asList(formats));
      if (!options.contains(value))
         options.add(value);
      return options.toArray(new String[]{});
   }

}
