/*
 * RSConnectAuthWindow.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.rsconnect.ui;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Float;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.SatelliteWindow;
import org.rstudio.studio.client.rsconnect.model.RSConnectAuthParams;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;

@Singleton
public class RSConnectAuthWindow extends SatelliteWindow
                                 implements RSConnectAuthView
{

   @Inject
   public RSConnectAuthWindow(Provider<EventBus> pEventBus,
                              Provider<FontSizeManager> pFSManager, 
                              Provider<RSConnectAuthPresenter> pPresenter)
   {
      super(pEventBus, pFSManager);
      pPresenter_ = pPresenter;
   }

   @Override
   protected void onInitialize(LayoutPanel mainPanel, JavaScriptObject params)
   {
      // RSConnectServerInfo appParams = params.cast();
      RSConnectAuthPresenter appPresenter = pPresenter_.get();
      RSConnectAuthParams authParams = params.cast();
      
      appPresenter.showClaimUrl(authParams.getServerInfo().getName(),
            authParams.getPreAuthToken().getClaimUrl());
      
      // make it fill the containing layout panel
      Widget presWidget = appPresenter.asWidget();
      mainPanel.add(presWidget);
      mainPanel.setWidgetLeftRight(presWidget, 0, Unit.PX, 0, Unit.PX);
      mainPanel.setWidgetTopBottom(presWidget, 0, Unit.PX, 35, Unit.PX);
      
      LayoutPanel actionPanel = new LayoutPanel();
      Style style = actionPanel.getElement().getStyle();
      style.setBackgroundColor("#f3f4f4");
      style.setPaddingTop(5, Unit.PX);
      style.setBorderColor("#eaebee");
      style.setBorderStyle(BorderStyle.SOLID);
      style.setBorderWidth(1, Unit.PX);
      Label infoLabel = new Label("This window will close when you're finished.");
      infoLabel.getElement().getStyle().setFontSize(8, Unit.PT);
      actionPanel.add(infoLabel);
      actionPanel.setWidgetTopBottom(infoLabel, 10, Unit.PX, 10, Unit.PX);
      actionPanel.setWidgetLeftWidth(infoLabel, 5, Unit.PX, 50, Unit.PCT);
      HorizontalPanel buttonPanel = new HorizontalPanel();
      buttonPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
      ThemedButton cancelButton = new ThemedButton("Cancel", new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            onCancelAuth();
         }
      });
      buttonPanel.add(cancelButton);
      buttonPanel.setCellHorizontalAlignment(cancelButton, HasHorizontalAlignment.ALIGN_RIGHT);
      buttonPanel.setCellWidth(cancelButton, "100%");
      buttonPanel.setWidth("100%");
      actionPanel.add(buttonPanel);
      actionPanel.setWidgetTopBottom(buttonPanel, 5, Unit.PX, 0, Unit.PX);
      actionPanel.setWidgetRightWidth(buttonPanel, 5, Unit.PX, 50, Unit.PCT);
      mainPanel.add(actionPanel);
      mainPanel.setWidgetBottomHeight(actionPanel, 0, Unit.PX, 35, Unit.PX);
   }

   @Override
   public void reactivate(JavaScriptObject params)
   {
   }
   
   @Override 
   public Widget getWidget()
   {
      return this;
   }
   
   private final native void onCancelAuth() /*-{
      $wnd.close();
   }-*/;
   
   private Provider<RSConnectAuthPresenter> pPresenter_;
}