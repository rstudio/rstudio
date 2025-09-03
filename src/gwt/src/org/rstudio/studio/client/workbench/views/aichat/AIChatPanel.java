/*
 * AIChatPanel.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.aichat;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.PrimaryWindowFrame;
import org.rstudio.studio.client.workbench.views.aichat.ui.AIChatConstants;

public class AIChatPanel extends ResizeComposite implements AIChatDisplay
{
   public AIChatPanel()
   {
      constants_ = GWT.create(AIChatConstants.class);
      
      // Create main panel
      mainPanel_ = new DockLayoutPanel(Unit.PX);
      mainPanel_.setStyleName("rstudio-AIChatPanel");
      mainPanel_.getElement().getStyle().setBackgroundColor("#fafafa");
      
      // Create header
      FlowPanel header = new FlowPanel();
      header.setStyleName("rstudio-AIChatPanel-Header");
      header.getElement().getStyle().setPadding(8, Unit.PX);
      header.getElement().getStyle().setBackgroundColor("#f0f0f0");
      header.getElement().getStyle().setBorderWidth(0, Unit.PX);
      header.getElement().getStyle().setBorderStyle(Style.BorderStyle.SOLID);
      header.getElement().getStyle().setBorderColor("#d0d0d0");
      header.getElement().getStyle().setProperty("borderBottomWidth", "1px");
      
      Label headerLabel = new Label(constants_.aiChatTitle());
      headerLabel.setStyleName("rstudio-AIChatPanel-HeaderLabel");
      headerLabel.getElement().getStyle().setFontSize(14, Unit.PX);
      headerLabel.getElement().getStyle().setProperty("fontWeight", "500");
      header.add(headerLabel);
      
      mainPanel_.addNorth(header, 32);
      
      // Create content area with placeholder
      ScrollPanel contentScroll = new ScrollPanel();
      contentScroll.setStyleName("rstudio-AIChatPanel-Content");
      
      VerticalPanel contentPanel = new VerticalPanel();
      contentPanel.setStyleName("rstudio-AIChatPanel-ContentPanel");
      contentPanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
      contentPanel.setVerticalAlignment(VerticalPanel.ALIGN_MIDDLE);
      contentPanel.setSize("100%", "100%");
      
      // Add chat icon (placeholder for now)
      FlowPanel iconContainer = new FlowPanel();
      iconContainer.setStyleName("rstudio-AIChatPanel-IconContainer");
      
      // Create a simple text icon as placeholder
      Label iconLabel = new Label("\uD83E\uDD16"); // Robot emoji as placeholder
      iconLabel.setStyleName("rstudio-AIChatPanel-Icon");
      iconLabel.getElement().getStyle().setFontSize(48, Unit.PX);
      iconContainer.add(iconLabel);
      contentPanel.add(iconContainer);
      
      // Add placeholder text
      Label placeholderLabel = new Label(constants_.aiChatComingSoon());
      placeholderLabel.setStyleName("rstudio-AIChatPanel-Placeholder");
      placeholderLabel.getElement().getStyle().setFontSize(18, Unit.PX);
      placeholderLabel.getElement().getStyle().setColor("#666666");
      placeholderLabel.getElement().getStyle().setProperty("marginTop", "20px");
      contentPanel.add(placeholderLabel);
      
      contentScroll.add(contentPanel);
      mainPanel_.add(contentScroll);
      
      // Wrap in primary window frame
      PrimaryWindowFrame frame = new PrimaryWindowFrame(constants_.aiChatTitle(), mainPanel_);
      
      initWidget(frame);
   }
   
   @Override
   public void setVisible(boolean visible)
   {
      super.setVisible(visible);
   }
   
   @Override
   public void focus()
   {
      // No-op for now, will be implemented when we add input
   }
   
   @Override
   public void onResize()
   {
      super.onResize();
      if (mainPanel_ != null)
         mainPanel_.onResize();
   }
   
   @Override
   public int getWidth()
   {
      return getOffsetWidth();
   }
   
   @Override
   public void setWidth(int width)
   {
      setWidth(width + "px");
   }
   
   @Override
   public Widget asWidget()
   {
      return this;
   }
   
   private final DockLayoutPanel mainPanel_;
   private final AIChatConstants constants_;
}