/*
 * RawFormatSelect.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
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

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.panmirror.PanmirrorConstants;

public class PanmirrorRawFormatSelect extends SelectWidget
{
   public PanmirrorRawFormatSelect()
   {
      super(constants_.formatLabel(), new String[] {}, new String[] {}, false);
   }
   
   public void setFormats(String[] formats, String value)
   {
      this.setChoices(
         getFormatList(constants_.chooseFormatLabel(), formats, value),
         getFormatList("", formats, value)
      );
   }
   
   private static String[] getFormatList(String firstItem, String[] formats, String value)
   {
      ArrayList<String> options = new ArrayList<>();
      options.add(firstItem);
      options.addAll(Arrays.asList(formats));
      if (!options.contains(value))
         options.add(value);
      return options.toArray(new String[]{});
   }
   private static final PanmirrorConstants constants_ = GWT.create(PanmirrorConstants.class);

}
