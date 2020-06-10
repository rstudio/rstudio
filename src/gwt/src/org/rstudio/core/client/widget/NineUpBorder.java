/*
 * NineUpBorder.java
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

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle.Source;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.user.client.ui.*;

public class NineUpBorder extends ResizeComposite implements AcceptsOneWidget
{
   // This needs to NOT extend ClientBundle, because doing so causes spurious
   // errors in Eclipse
   public interface Resources
   {
      ImageResource topLeft();
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource top();
      ImageResource topRight();
      @ImageOptions(repeatStyle = RepeatStyle.Vertical)
      ImageResource left();
      @ImageOptions(repeatStyle = RepeatStyle.Vertical)
      ImageResource right();
      ImageResource bottomLeft();
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource bottom();
      ImageResource bottomRight();

      @Source("NineUpBorder.css")
      Styles styles();
   }

   public interface Styles extends CssResource
   {
      String topLeftClass();
      String topClass();
      String topRightClass();
      String leftClass();
      String rightClass();
      String bottomLeftClass();
      String bottomClass();
      String bottomRightClass();
   }

   public NineUpBorder(Resources resources,
                       int marginTop,
                       int marginRight,
                       int marginBottom,
                       int marginLeft)
   {
      resources.styles().ensureInjected();

      panel_ = new LayoutPanel();

      addBgPanel(resources.styles().topLeftClass());
      addBgPanel(resources.styles().topClass());
      addBgPanel(resources.styles().topRightClass());
      addBgPanel(resources.styles().leftClass());
      addBgPanel(resources.styles().rightClass());
      addBgPanel(resources.styles().bottomLeftClass());
      addBgPanel(resources.styles().bottomClass());
      addBgPanel(resources.styles().bottomRightClass());

      inner_ = new SimplePanel();
      panel_.add(inner_);
      panel_.setWidgetTopBottom(inner_,
                                marginTop,
                                Unit.PX,
                                marginBottom,
                                Unit.PX);
      panel_.setWidgetLeftRight(inner_,
                                marginLeft,
                                Unit.PX,
                                marginRight,
                                Unit.PX);

      initWidget(panel_);
   }

   private void addBgPanel(String className)
   {
      DivElement div = Document.get().createDivElement();
      div.setClassName(className);
      panel_.getElement().appendChild(div);
   }

   @Override
   public void setWidget(IsWidget w)
   {
      inner_.setWidget(w);
   }

   public LayoutPanel getLayoutPanel()
   {
      return panel_;
   }

   public void setFillColor(String fillColor)
   {
      inner_.getElement().getStyle().setBackgroundColor(fillColor);
   }

   private final LayoutPanel panel_;
   private final SimplePanel inner_;
}
