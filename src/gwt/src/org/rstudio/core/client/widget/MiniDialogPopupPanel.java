/*
 * MiniDialogPopupPanel.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public abstract class MiniDialogPopupPanel extends DecoratedPopupPanel
{
   public MiniDialogPopupPanel()
   {
      super();
      commonInit();
   }

   public MiniDialogPopupPanel(boolean autoHide)
   {
      super(autoHide);
      commonInit();
   }

   public MiniDialogPopupPanel(boolean autoHide, boolean modal)
   {
      super(autoHide, modal);
      commonInit();
   }

   private void commonInit()
   {
      addStyleName(ThemeStyles.INSTANCE.miniDialogPopupPanel());
      
      verticalPanel_ = new VerticalPanel();
      verticalPanel_.setStyleName(ThemeStyles.INSTANCE.miniDialogContainer());
      
      // title bar
      HorizontalPanel titleBar = new HorizontalPanel();
      titleBar.setWidth("100%");
      
      captionLabel_ = new Label();
      captionLabel_.setStyleName(ThemeStyles.INSTANCE.miniDialogCaption());
      titleBar.add(captionLabel_);
      titleBar.setCellHorizontalAlignment(captionLabel_, 
                                          HasHorizontalAlignment.ALIGN_LEFT);
     
      HorizontalPanel toolsPanel = new HorizontalPanel();
      toolsPanel.setStyleName(ThemeStyles.INSTANCE.miniDialogTools());
      ToolbarButton hideButton = new ToolbarButton(
            new ImageResource2x(ThemeResources.INSTANCE.closeChevron2x()),
            new ClickHandler() { 
               public void onClick(ClickEvent event)
               {
                  MiniDialogPopupPanel.this.hideMiniDialog();
               }
            });
      hideButton.setTitle("Close");
      toolsPanel.add(hideButton);
      titleBar.add(toolsPanel);
      titleBar.setCellHorizontalAlignment(toolsPanel,
                                          HasHorizontalAlignment.ALIGN_RIGHT);
      
      verticalPanel_.add(titleBar);
      
      // main widget
      verticalPanel_.add(createMainWidget());
      
      setWidget(verticalPanel_);
   }
   
   public void setCaption(String caption)
   {
      captionLabel_.setText(caption);
   }
   
   protected abstract Widget createMainWidget();
   
   protected void hideMiniDialog()
   {
      hide();
      restorePreviouslyFocusedElement();
   }
   
   // TODO: refactor so the originally active element code is
   // shared between MiniDialogPopupPanel and ModalDialogBase 
   
   public void recordPreviouslyFocusedElement()
   {
      focusContext_.record();
   }
   
   protected void restorePreviouslyFocusedElement()
   { 
      focusContext_.restore();
   }
   
   private VerticalPanel verticalPanel_;
   private Label captionLabel_ ;
   private FocusContext focusContext_ = new FocusContext();
}
