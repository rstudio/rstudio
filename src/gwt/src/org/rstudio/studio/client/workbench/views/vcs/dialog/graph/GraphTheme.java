/*
 * GraphTheme.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs.dialog.graph;

import com.google.gwt.canvas.dom.client.CssColor;

import java.util.HashMap;

public class GraphTheme
{
   public int getColumnWidth()
   {
      return 12;
   }

   public int getRowHeight()
   {
      return 24;
   }

   public double getStrokeWidth()
   {
      return 2.0;
   }

   public double getCircleRadius()
   {
      return 3.0;
   }

   public CssColor getColorForId(int id)
   {
      if (!colors_.containsKey(id))
      {
         colors_.put(id, CssColor.make(
               randomColorValue(),
               randomColorValue(),
               randomColorValue()
         ));
      }
      return colors_.get(id);
   }

   private int randomColorValue()
   {
      return 60 + (int)(Math.random() * 140);
   }

   private static HashMap<Integer, CssColor> colors_ = new HashMap<Integer, CssColor>();
}
