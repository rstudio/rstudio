/*
 * ImageElementEx.java
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
package org.rstudio.core.client.dom;

import com.google.gwt.dom.client.ImageElement;

public class ImageElementEx extends ImageElement
{
   protected ImageElementEx()
   {
   }

   public final native int naturalWidth() /*-{
      return this.naturalWidth;
   }-*/;

   public final native int naturalHeight() /*-{
      return this.naturalHeight;
   }-*/;
}
