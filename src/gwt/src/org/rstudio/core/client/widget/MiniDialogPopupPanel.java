/*
 * MiniDialogPopupPanel.java
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
package org.rstudio.core.client.widget;

import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;

import com.google.gwt.dom.client.Document;
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
            ThemeResources.INSTANCE.closeChevron(),
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
      // be defensive since we are making this change right before v1.0
      try
      {
         originallyActiveElement_ = DomUtils.getActiveElement();
      }
      catch(Exception e)
      {
      }
   }
   
   protected void restorePreviouslyFocusedElement()
   { 
      try
      {
         if (originallyActiveElement_ != null
             && !originallyActiveElement_.getTagName().equalsIgnoreCase("body"))
         {
            Document doc = originallyActiveElement_.getOwnerDocument();
            if (doc != null)
            {
               originallyActiveElement_.focus();
            }
         }
      }
      catch (Exception e)
      {
         // focus() fail if the element is no longer visible. It's
         // easier to just catch this than try to detect it.
   
         // Also originallyActiveElement_.getTagName() can fail with:
         // "Permission denied to access property 'tagName' from a non-chrome context"
         // possibly due to Firefox "anonymous div" issue.
      }
      originallyActiveElement_ = null;
   }
   
   private VerticalPanel verticalPanel_;
   private Label captionLabel_ ;
   private com.google.gwt.dom.client.Element originallyActiveElement_ = null;

}
