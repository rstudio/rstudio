/*
 * MiniPieWidget.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import org.rstudio.core.client.ElementIds;

/**
 * A mini pie chart widget that shows a simple percentage in the form of a pie chart.
 */
public class MiniPieWidget extends Composite
{
   /**
    * Create a new mini pie widget
    *
    * @param foreColor The color to use for the foreground (filled) portion of the chart
    * @param backColor The color to use for the background (unfilled) portion of the chart
    * @param percent The percentage of the chart to fill with the foreground color
    */
   public MiniPieWidget(String title, String description, String foreColor, String backColor, int percent)
   {
      Element svg = Document.get().createElement("svg");
      svg.setAttribute("viewBox", "0 0 " + (PIE_CENTER * 2) + " " + (PIE_CENTER * 2));
      svg.setAttribute("style", "width: 100%; height: 100%;");

      // Create accessibility title and description
      title_ = Document.get().createTitleElement();
      title_.setInnerText(title);
      title_.setId(ElementIds.getUniqueElementId("pie-title"));
      svg.appendChild(title_);
      Roles.getImgRole().setAriaLabelledbyProperty(svg, Id.of(title_));

      description_ = Document.get().createElement("desc");
      description_.setInnerText(description);
      svg.appendChild(description_);

      // Create background segment
      back_ = Document.get().createElement("circle");
      back_.setAttribute("cx", PIE_CENTER + "");
      back_.setAttribute("cy", PIE_CENTER + "");
      back_.setAttribute("r", PIE_RADIUS + "");
      back_.setAttribute("fill", "transparent");
      back_.setAttribute("stroke-width", PIE_STROKE + "");
      setBackColor(backColor);
      svg.appendChild(back_);

      // Create foreground segment
      fore_ = Document.get().createElement("circle");
      fore_.setAttribute("cx", PIE_CENTER + "");
      fore_.setAttribute("cy", PIE_CENTER + "");
      fore_.setAttribute("r", PIE_RADIUS + "");
      fore_.setAttribute("fill", "transparent");
      fore_.setAttribute("stroke-width", PIE_STROKE + "");
      fore_.setAttribute("stroke-dashoffset", "25");
      setForeColor(foreColor);
      svg.appendChild(fore_);

      setPercent(percent);

      initWidget(new ElementPanel(svg));
   }

   /**
    * Sets the color of the pie chart for the empty (unfilled) portion of the chart
    *
    * @param color The color to set
    */
   public void setBackColor(String color)
   {
      back_.setAttribute("stroke", color);
   }

   /**
    * Sets the color of the pie chart for the used (filled) portion of the chart
    *
    * @param color The color to set
    */
   public void setForeColor(String color)
   {
      fore_.setAttribute("stroke", color);
   }

   /**
    * Sets the percentage of the pie chart to be filled
    *
    * @param percent The fill percent (0 - 100)
    */
   public void setPercent(int percent)
   {
      fore_.setAttribute("stroke-dasharray", percent + " " + (100 - percent));
   }

   /**
    * Sets the accessibility title of the pie chart (not displayed)
    *
    * @param title The new title
    */
   public void setTitle(String title)
   {
      title_.setInnerText(title);
   }

   /**
    * Sets the accessibility description of the pie chart (not displayed)
    *
    * @param description The new description
    */
   public void setDescription(String description)
   {
      description_.setInnerText(description);
   }

   private class ElementPanel extends SimplePanel
   {
      ElementPanel(Element ele)
      {
         super(ele);
      }
   }

   // These values create a circle that can use 0 - 100 values for stroke offsets
   private final static int PIE_CENTER = 27;
   private final static double PIE_RADIUS = 15.915494;

   private final static int PIE_STROKE = 20;

   private final Element back_;
   private final Element fore_;
   private final Element title_;
   private final Element description_;
}
