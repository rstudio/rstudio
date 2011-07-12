/*
 * ConsoleBarView.java
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
package org.rstudio.studio.client.workbench.views.vcs.console;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.widget.ClickImage;
import org.rstudio.core.client.widget.LeftCenterRightBorder;

public class ConsoleBarView extends Composite
   implements ConsoleBarPresenter.Display,
              HasKeyDownHandlers,
              HasSelectionCommitHandlers<String>,
              HasText
{
   interface MyBinder extends UiBinder<Widget, ConsoleBarView>
   {}

   interface Resources extends LeftCenterRightBorder.Resources, ClientBundle
   {
      ImageResource chevronUp();
      ImageResource chevronDown();

      @Override
      @Source("GitCommandEntryLeft.png")
      ImageResource left();

      @Override
      @Source("GitCommandEntryTile.png")
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource center();

      @Override
      @Source("GitCommandEntryRight.png")
      ImageResource right();

      @Source("GitCommandBarTile.png")
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource barTile();
   }

   public ConsoleBarView()
   {
      Widget w = GWT.<MyBinder>create(MyBinder.class).createAndBindUi(this);
      w.setSize("100%", "100%");

      LeftCenterRightBorder border = new LeftCenterRightBorder(res_,
                                                               -1, 3, 0, 3);
      border.setWidget(w);

      LayoutPanel outer = new LayoutPanel();
      outer.getElement().getStyle().setBackgroundImage(
            "url(" + res_.barTile().getURL() + ")");
      outer.getElement().getStyle().setProperty("backgroundRepeat", "repeat-x");
      outer.add(border);
      outer.setWidgetTopHeight(border, 3, Unit.PX, 18, Unit.PX);
      outer.setWidgetLeftRight(border, 3, Unit.PX, 21, Unit.PX);

      expand_ = new ClickImage(res_.chevronUp());
      outer.add(expand_);
      outer.setWidgetTopHeight(expand_,
                               6,
                               Unit.PX,
                               expand_.getHeight(),
                               Unit.PX);
      outer.setWidgetRightWidth(expand_,
                                5,
                                Unit.PX,
                                expand_.getWidth(),
                                Unit.PX);

      initWidget(outer);

      expand_.setResource(res_.chevronUp());
   }

   @Override
   public HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
   {
      return input_.addKeyDownHandler(handler);
   }

   @Override
   public HandlerRegistration addSelectionCommitHandler(SelectionCommitHandler<String> handler)
   {
      return addHandler(handler, SelectionCommitEvent.getType());
   }

   @Override
   public String getText()
   {
      return input_.getText();
   }

   @Override
   public void setText(String text)
   {
      input_.setText(text);
   }

   @Override
   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return expand_.addClickHandler(handler);
   }

   @Override
   public void notifyOutputVisible(boolean visible)
   {
      if (visible)
         expand_.setResource(res_.chevronDown());
      else
         expand_.setResource(res_.chevronUp());
   }

   Image expand_;
   @UiField
   TextBox input_;

   private final Resources res_ = GWT.create(Resources.class);
}
