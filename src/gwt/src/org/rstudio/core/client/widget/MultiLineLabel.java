/*
 * MultiLineLabel.java
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

import com.google.gwt.user.client.ui.Label;
import org.rstudio.core.client.dom.DomUtils;

public class MultiLineLabel extends Label
{
   public MultiLineLabel()
   {
   }

   public MultiLineLabel(String text)
   {
      super(text);
   }

   public MultiLineLabel(String text, boolean wordWrap)
   {
      super(text, wordWrap);
   }

   @Override
   public void setText(String text)
   {
      getElement().setInnerHTML(DomUtils.textToHtml(text));
   }

   public void setHtml(String html)
   {
      getElement().setInnerHTML(html);
   }
}
