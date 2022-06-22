/*
 * MiniPieWidget.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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
import com.google.gwt.user.client.ui.SimplePanel;
import org.rstudio.core.client.ElementIds;


/**
 * A mini pie chart widget that shows a set of percentages in the form of a pie chart.
 */
public class MiniPieWidget extends Composite
{
   /**
    * Create a new mini pie widget (zero-arg constructor for use in UiBinder)
    */
   public MiniPieWidget()
   {
      this("", "", "#000000");
   }

   /**
    * Create a new mini pie widget.
    *
    * @param title The title of the chart (for accessibility, not shown)
    * @param description The description of the chart (for accessibility, not shown)
    * @param backColor The color to use for the background (unfilled) portion of the chart
    */
   public MiniPieWidget(String title, String description, String backColor)
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
    * Adds a segment to the pie chart.
    *
    * @param percent The percentage of the chart to be consumed by the segment.
    * @param color The color of the segment.
    */
   public void addSegment(int percent, String color)
   {
      // Create the new segment of the chart
      Element ele = Document.get().createElement("circle");
      ele.setAttribute("cx", PIE_CENTER + "");
      ele.setAttribute("cy", PIE_CENTER + "");
      ele.setAttribute("r", PIE_RADIUS + "");
      ele.setAttribute("fill", "transparent");
      ele.setAttribute("stroke-width", PIE_STROKE + "");

      // Create one big dash that consumes the requested percentage
      ele.setAttribute("stroke-dasharray", percent + " " + (100 - percent));

      // Start at 25 ("noon") plus the current running total
      ele.setAttribute("stroke-dashoffset", "25");
      ele.setAttribute("stroke", color);

      // Append to the SVG host element
      getElement().appendChild(ele);
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

   /**
    * Extend SimplePanel so we can use the <svg> element without a superfluous <div> wrapping it.
    */
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
   private final Element title_;
   private final Element description_;
}
