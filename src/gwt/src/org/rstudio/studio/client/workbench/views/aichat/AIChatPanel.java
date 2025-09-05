/*
 * AIChatPanel.java
 *
 * Copyright (C) 2025 by Posit Software, PBC
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
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.theme.PrimaryWindowFrame;
import org.rstudio.studio.client.workbench.views.aichat.ui.AIChatConstants;

public class AIChatPanel extends ResizeComposite implements AIChatDisplay
{
   public AIChatPanel()
   {
      constants_ = GWT.create(AIChatConstants.class);
      
      // Create content area with placeholder
      ScrollPanel contentScroll = new ScrollPanel();
      contentScroll.setStyleName("rstudio-AIChatPanel-Content");
      
      VerticalPanel contentPanel = new VerticalPanel();
      contentPanel.setStyleName("rstudio-AIChatPanel-ContentPanel");
      contentPanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
      contentPanel.setVerticalAlignment(VerticalPanel.ALIGN_MIDDLE);
      contentPanel.setSize("100%", "100%");
      
      // Add placeholder text
      Label placeholderLabel = new Label(constants_.aiChatComingSoon());
      placeholderLabel.setStyleName("rstudio-AIChatPanel-Placeholder");
      contentPanel.add(placeholderLabel);
      
      contentScroll.add(contentPanel);
      
      // Create main panel without the custom header
      mainPanel_ = new DockLayoutPanel(Unit.PX);
      mainPanel_.setStyleName("rstudio-AIChatPanel");
      mainPanel_.add(contentScroll);
      
      // Wrap in primary window frame - this provides the themed header
      PrimaryWindowFrame frame = new PrimaryWindowFrame(constants_.aiChatTitle(), mainPanel_);
      
      // Hide the separator and subtitle since we don't use them
      frame.getSubtitleWidget().setVisible(false);
      // The separator is the second widget in the panel
      com.google.gwt.dom.client.Element separator = (com.google.gwt.dom.client.Element) frame.getTitleWidget().getParent().getElement().getChild(1);
      separator.getStyle().setDisplay(Style.Display.NONE);
      
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