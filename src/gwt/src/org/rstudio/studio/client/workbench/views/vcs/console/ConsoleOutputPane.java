/*
 * ConsoleOutputPane.java
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
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ResizeComposite;
import org.rstudio.core.client.events.EnsureHiddenEvent;
import org.rstudio.core.client.events.EnsureHiddenHandler;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.widget.BottomScrollPanel;
import org.rstudio.core.client.widget.ClickImage;
import org.rstudio.core.client.widget.NineUpBorder;
import org.rstudio.studio.client.workbench.views.vcs.console.ConsoleBarPresenter.OutputDisplay;

public class ConsoleOutputPane extends ResizeComposite implements OutputDisplay
{
   interface Resources extends NineUpBorder.Resources, ClientBundle
   {
      @Source("../../../../../../core/client/widget/NineUpBorder.css")
      BorderStyles styles();

      @Source("ConsoleOutputPane.css")
      Styles styles2();

      @Override
      @Source("GitCommandOutputTopLeft.png")
      ImageResource topLeft();

      @Override
      @Source("GitCommandOutputTop.png")
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource top();

      @Override
      @Source("GitCommandOutputTopRight.png")
      ImageResource topRight();

      @Override
      @Source("GitCommandOutputLeft.png")
      @ImageOptions(repeatStyle = RepeatStyle.Vertical)
      ImageResource left();

      @Override
      @Source("GitCommandOutputRight.png")
      @ImageOptions(repeatStyle = RepeatStyle.Vertical)
      ImageResource right();

      @Override
      @Source("GitCommandOutputLeft.png")
      ImageResource bottomLeft();

      @Override
      @Source("GitCommandOutputFill.png")
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource bottom();

      @Override
      @Source("GitCommandOutputRight.png")
      ImageResource bottomRight();

      @Source("GitCommandCloseIcon.png")
      ImageResource closeIcon();
   }

   interface BorderStyles extends NineUpBorder.Styles
   {}

   interface Styles extends CssResource
   {
      String outer();
      String command();
      String output();
   }

   public ConsoleOutputPane()
   {
      html_ = new HTML();
      html_.setStyleName(styles_.outer());

      scrollPanel_ = new BottomScrollPanel(html_);
      scrollPanel_.setSize("100%", "100%");

      NineUpBorder nineUpBorder = new NineUpBorder(res_, 15, 3, 0, 3);
      nineUpBorder.setWidget(scrollPanel_);
      nineUpBorder.setFillColor("#fff");

      Image closeIcon = new ClickImage(res_.closeIcon());
      LayoutPanel borderLayoutPanel = nineUpBorder.getLayoutPanel();
      borderLayoutPanel.add(closeIcon);
      borderLayoutPanel.setWidgetTopHeight(closeIcon,
                                           5, Unit.PX,
                                           closeIcon.getHeight(), Unit.PX);
      borderLayoutPanel.setWidgetRightWidth(closeIcon,
                                            7, Unit.PX,
                                            closeIcon.getWidth(), Unit.PX);
      closeIcon.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            ensureHidden();
         }
      });

      initWidget(nineUpBorder);
   }

   @Override
   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleHandler handler)
   {
      return addHandler(handler, EnsureVisibleEvent.TYPE);
   }

   @Override
   public HandlerRegistration addEnsureHiddenHandler(EnsureHiddenHandler handler)
   {
      return addHandler(handler, EnsureHiddenEvent.TYPE);
   }

   @Override
   public void addCommand(String command)
   {
      addText("> " + command).setClassName(styles_.command());
      scrollPanel_.onContentSizeChanged();
   }

   @Override
   public void addOutput(String output)
   {
      addText(output).setClassName(styles_.output());
      scrollPanel_.onContentSizeChanged();
   }

   @Override
   public void clearOutput()
   {
      html_.getElement().setInnerHTML("");
   }

   @Override
   public void onShow()
   {
      // Use scrollPos_ to restore scroll position (based on the bottom of the
      // scroll panel)

      if (scrollPos_ == null)
         scrollPanel_.scrollToBottom();
      else
      {
         int vscroll = Math.max(0, scrollPos_ - scrollPanel_.getOffsetHeight());
         scrollPanel_.setVerticalScrollPosition(vscroll);
      }
   }

   @Override
   public void onBeforeHide()
   {
      // Save scroll position to scrollPos_ (based on the bottom of the scroll
      // panel)

      if (scrollPanel_.isScrolledToBottom())
         scrollPos_ = null;
      else
         scrollPos_ = scrollPanel_.getVerticalScrollPosition() +
                      scrollPanel_.getOffsetHeight();
   }

   private Element addText(String command)
   {
      DivElement child = Document.get().createDivElement();
      child.setInnerText(command);
      html_.getElement().appendChild(child);
      scrollPanel_.onContentSizeChanged();
      ensureVisible();
      return child;
   }

   private void ensureVisible()
   {
      fireEvent(new EnsureVisibleEvent());
   }

   private void ensureHidden()
   {
      fireEvent(new EnsureHiddenEvent());
   }

   private final HTML html_;
   private final BottomScrollPanel scrollPanel_;
   private Integer scrollPos_ = null;

   private static final Resources res_ = GWT.create(Resources.class);
   private static final Styles styles_ = res_.styles2();
   static {
      styles_.ensureInjected();
   }
}
