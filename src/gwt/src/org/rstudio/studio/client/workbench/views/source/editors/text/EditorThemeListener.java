/*
 * EditorThemeListener.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

public interface EditorThemeListener
{
   class Colors
   {
      public Colors(String front, String back, String outline, String high,
            String raised)
      {
         foreground = front;
         background = back;
         border = outline;
         highlight = high;
         surface = raised;
      }
      public final String foreground;
      public final String background;
      public final String border;
      public final String highlight;
      public final String surface;
   }
   void onEditorThemeChanged(Colors colors);
}
