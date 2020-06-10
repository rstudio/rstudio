/*
 * GraphLine.java
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
package org.rstudio.studio.client.workbench.views.vcs.dialog.graph;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.Context2d.LineJoin;
import com.google.gwt.safehtml.shared.SafeHtml;
import org.rstudio.core.client.SafeHtmlUtil;

public class GraphLine
{
   public GraphLine(String value)
   {
      String[] vals = value.length() == 0 ? new String[] {} : value.split(" ");
      columns_ = new GraphColumn[vals.length];
      for (int i = 0; i < columns_.length; i++)
         columns_[i] = new GraphColumn(vals[i]);
      altText_ = "";
   }

   public GraphColumn[] getColumns()
   {
      return columns_;
   }

   public int getTotalWidth(GraphTheme theme)
   {
      int startColumns = 0;
      int endColumns = 0;
      for (GraphColumn c : columns_)
      {
         if (!c.start)
            startColumns++;
         if (!c.end)
            endColumns++;
      }
      return Math.max(startColumns, endColumns) * theme.getColumnWidth();
   }

   public SafeHtml render(GraphTheme theme)
   {
      draw(s_canvas, theme);
      return SafeHtmlUtil.createOpenTag("img",
                                        "alt", altText_,
                                        "class", theme.getImgClassName(),
                                        "src", s_canvas.toDataUrl());
   }

   private void draw(Canvas canvas, GraphTheme theme)
   {
      int height = theme.getRowHeight();
      int colWidth = theme.getColumnWidth();
      double pad = theme.getVerticalLinePadding();

      canvas.setCoordinateSpaceHeight(height);
      canvas.setCoordinateSpaceWidth(colWidth * getTotalWidth(theme));
      Context2d ctx = canvas.getContext2d();

      //ctx.clearRect(0, 0, colWidth * columns_.length, height);

      ctx.translate(colWidth / 2.0, 0);

      int startPos = -1;
      int endPos = -1;
      int nexusColumn = -1;
      for (int i = 0; i < columns_.length; i++)
      {
         GraphColumn c = columns_[i];

         if (!c.start)
            startPos++;
         if (!c.end)
            endPos++;

         ctx.setStrokeStyle(theme.getColorForId(c.id));
         ctx.setLineWidth(theme.getStrokeWidth());
         ctx.setLineJoin(LineJoin.ROUND);

         if (!c.nexus && !c.start && !c.end)
         {
            // Just draw a line from start to end position

            ctx.beginPath();
            ctx.moveTo(startPos * colWidth, 0);
            ctx.lineTo(startPos * colWidth, pad);
            // This next lineTo helps ensure that the shape of the line looks
            // congruous to any specials on the same line
            ctx.lineTo(Math.min(startPos, endPos) * colWidth, height / 2.0);
            ctx.lineTo(endPos * colWidth, height - pad);
            ctx.lineTo(endPos * colWidth, height);
            ctx.stroke();
         }
         else
         {
            // something special

            if (c.nexus)
            {
               nexusColumn = i;
               altText_ = "commit depth " + nexusColumn;
               ctx.setFillStyle(theme.getColorForId(c.id));
            }

            if (!c.start)
            {
               // draw from i to nexusColumn;
               ctx.beginPath();
               ctx.moveTo(startPos * colWidth, 0);
               ctx.lineTo(startPos * colWidth, pad);
               ctx.lineTo(nexusColumn * colWidth, height / 2.0);
               ctx.stroke();
            }

            if (!c.end)
            {
               // draw from nexusColumn to endPosition
               ctx.beginPath();
               ctx.moveTo(nexusColumn * colWidth, height / 2.0);
               ctx.lineTo(endPos * colWidth, height - pad);
               ctx.lineTo(endPos * colWidth, height);
               ctx.stroke();
            }

         }
      }

      // draw a circle on the nexus
      ctx.beginPath();
      ctx.arc(nexusColumn * colWidth, height / 2.0,
              theme.getCircleRadius() + theme.getStrokeWidth(), 0, Math.PI * 2);
      ctx.closePath();
      ctx.fill();

      ctx.beginPath();
      ctx.arc(nexusColumn * colWidth, height / 2.0,
              theme.getCircleRadius(), 0, Math.PI * 2);
      ctx.closePath();
      ctx.setFillStyle("white");
      ctx.fill();

   }

   private GraphColumn[] columns_;
   private String altText_;

   // Use a static canvas to avoid the overhead of continually recreating them
   private static final Canvas s_canvas = Canvas.createIfSupported();
}

