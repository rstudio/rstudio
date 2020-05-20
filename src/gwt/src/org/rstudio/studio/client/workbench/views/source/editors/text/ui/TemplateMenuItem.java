/*
 * TemplateMenuItem.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.VerticalAlign;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import org.rstudio.core.client.widget.DecorativeImage;

public class TemplateMenuItem extends Composite
{
   public TemplateMenuItem(String templateName)
   {
      super();
      wrapper_ = new FlowPanel();
      wrapper_.add(new InlineLabel(templateName));
      name_ = templateName;
      initWidget(wrapper_);
   }
   
   public void addIcon(ImageResource icon)
   {
      DecorativeImage iconImage = new DecorativeImage(icon);
      wrapper_.insert(iconImage, 0);
      Style imageStyle = iconImage.getElement().getStyle();
      imageStyle.setVerticalAlign(VerticalAlign.MIDDLE);
      imageStyle.setMarginRight(5, Unit.PX);
      imageStyle.setMarginBottom(2, Unit.PX);
   }
   
   public String getName()
   {
      return name_;
   }
   
   private FlowPanel wrapper_;
   private String name_;
}
