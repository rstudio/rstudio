/*
 * ImageButtonColumn.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.core.client.cellview;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.OperationWithInput;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;

public class ImageButtonColumn<T> extends Column<T, String>
{
   public ImageButtonColumn(final ImageResource2x image,
                            final OperationWithInput<T> onClick,
                            final String title)
   {
      super(new ButtonCell(){
         @Override
         public void render(Context context, 
                            SafeHtml value, 
                            SafeHtmlBuilder sb) 
         {   
            if (value != null)
            {
               sb.appendHtmlConstant("<span title=\"" + title + "\" " +
                                     "style=\"cursor: pointer;\">");
               sb.append(image.getSafeHtml());
               sb.appendHtmlConstant("</span>");
            }
         }                                
      });

      setFieldUpdater(new FieldUpdater<T,String>() {
         public void update(int index, T object, String value)
         {
            if (value != null)
               onClick.execute(object);
         }
      });
   }


   @Override
   public String getValue(T object)
   {
      if (showButton(object))
         return new String();
      else
         return null;
   }
   
   protected boolean showButton(T object)
   {
      return true;
   }
}