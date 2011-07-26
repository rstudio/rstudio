/*
 * NavGutter.java
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
package org.rstudio.studio.client.workbench.views.vcs.diff;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import org.rstudio.core.client.ValueSink;
import org.rstudio.studio.client.workbench.views.vcs.diff.Line.Type;

import java.util.ArrayList;

public class NavGutter extends Composite implements ValueSink<ArrayList<ChunkOrLine>>
{
   public NavGutter()
   {
      container_ = new SimplePanel();
      initWidget(container_);
   }

   public void setData(CssColor background, ArrayList<CssColor> lines)
   {
      Canvas newCanvas = Canvas.createIfSupported();
      newCanvas.setSize("100%", "100%");
      newCanvas.setCoordinateSpaceWidth(10);
      newCanvas.setCoordinateSpaceHeight(lines.size());

      Context2d ctx = newCanvas.getContext2d();
      ctx.translate(0.5, 0.5);

      ctx.setFillStyle(background.value());
      ctx.fillRect(0, 0, 10, lines.size());

      for (int i = 0; i < lines.size(); i++)
      {
         CssColor color = lines.get(i);
         if (color != null)
         {
            ctx.setFillStyle(color.value());
            ctx.fillRect(0, i, 10, 1);
         }
      }

      container_.setWidget(newCanvas);
   }

   @Override
   public void setValue(ArrayList<ChunkOrLine> value)
   {
      ArrayList<CssColor> colors = new ArrayList<CssColor>(value.size());
      for (ChunkOrLine item : value)
      {
         Line line = item.getLine();
         Type type = line == null ? Type.Same : line.getType();
         switch (type)
         {
            case Insertion:  colors.add(CssColor.make("#6F6")); break;
            case Deletion:   colors.add(CssColor.make("pink")); break;
            default:
            case Same:       colors.add(null); break;
         }
      }
      setData(CssColor.make("#e1e2e5"), colors);
   }

   private final SimplePanel container_;
}
