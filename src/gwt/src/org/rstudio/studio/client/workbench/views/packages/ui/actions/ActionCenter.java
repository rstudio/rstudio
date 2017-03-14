/*
 * ActionCenter.java
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
package org.rstudio.studio.client.workbench.views.packages.ui.actions;

import java.util.ArrayList;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.workbench.views.packages.Packages;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ActionCenter extends Composite 
{
   public ActionCenter(final Command onSizeChanged)
   { 
      icon_ = new Image(new ImageResource2x(RESOURCES.packratIcon2x()));
      chevronUp_ = new Image(new ImageResource2x(RESOURCES.chevronUp2x()));
      chevronUp_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            collapsed_ = true;
            manageUI();
            onSizeChanged.execute();;
         } 
      });
      chevronDown_ = new Image(new ImageResource2x(RESOURCES.chevronDown2x()));
      chevronDown_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            collapsed_ = false;
            manageUI();
            onSizeChanged.execute();
         }
      });
      
      initWidget(binder.createAndBindUi(this));
      
      collapsedLabel_ = new Label("Packrat library out of sync");
      collapsedLabel_.addStyleName(RESOURCES.styles().actionLabel());
      actions_.add(collapsedLabel_);
      
      collapsed_ = false;
      manageUI();
   }
   
   public void setActions(ArrayList<Packages.Action> actions)
   {
      // remove existing action widgets
      for (ActionWidget actionWidget : actionWidgets_)
         actionWidget.removeFromParent();
      actionWidgets_.clear();
      
      // repopulate the list
      for (Packages.Action action : actions)
      {
         ActionWidget actionWidget = new ActionWidget(action);
         actionWidgets_.add(actionWidget);
         actions_.add(actionWidget);
      }
          
      manageUI();
   }
   
   public int getHeight()
   {
      return container_.getOffsetHeight() + 1;
   }
   
   public int getActionCount()
   {
      return actionWidgets_.size();
   }
   
   public boolean collapsed()
   {
      return collapsed_;
   }
   
   private void manageUI()
   {
      // action label visibility
      collapsedLabel_.setVisible(collapsed_);
      for (ActionWidget actionWidget : actionWidgets_)
         actionWidget.setVisible(!collapsed_);
      
      // chevron visiblity
      if (collapsed_)
      {
         chevron_.setVisible(true);
         chevron_.setWidget(chevronDown_);
      }
      else
      {
         chevron_.setVisible(actionWidgets_.size() > 1);
         chevron_.setWidget(chevronUp_);
      }
    
      // height
      final int kPad = 4;
      final int kActionHeight = 25;
      int containerHeight = kPad;
      if (collapsed_)
         containerHeight += kActionHeight;
      else
         containerHeight += actionWidgets_.size() * kActionHeight;
      containerHeight += kPad;
      container_.setHeight(containerHeight + "px");
   }

   @UiField
   protected DockLayoutPanel container_;
   @UiField(provided = true)
   protected Image icon_;
   @UiField
   protected VerticalPanel actions_;
   @UiField
   SimplePanel chevron_;
  
   interface MyBinder extends UiBinder<Widget, ActionCenter>{}
   private static MyBinder binder = GWT.create(MyBinder.class);
   
   static interface Styles extends CssResource
   {
      String actionLabel();
      String actionWidget();
   }
   
   static interface Resources extends ClientBundle
   {
      @Source("chevronDown_2x.png")
      ImageResource chevronDown2x();

      @Source("chevronUp_2x.png")
      ImageResource chevronUp2x();

      @Source("packratIcon_2x.png")
      ImageResource packratIcon2x();

      @Source("packratIconSmall_2x.png")
      ImageResource packratIconSmall2x();
      
      @Source("ActionCenter.css")
      Styles styles();
   }
   
   static Resources RESOURCES = (Resources)GWT.create(Resources.class) ;
   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }
   
   private boolean collapsed_ = false;
   
   private Label collapsedLabel_;
   private ArrayList<ActionWidget> actionWidgets_ = new ArrayList<ActionWidget>();
   private Image chevronUp_;
   private Image chevronDown_;
   
}
