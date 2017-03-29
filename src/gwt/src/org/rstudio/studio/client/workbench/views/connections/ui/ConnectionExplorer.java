/*
 * ConnectionExplorer.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.connections.ui;

import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.ProgressSpinner;
import org.rstudio.core.client.widget.SimplePanelWithProgress;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.views.connections.model.Connection;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleBusyEvent;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.TextAlign;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ConnectionExplorer extends Composite implements RequiresResize
{
   public ConnectionExplorer()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      // code/connection panel
      int codePanelHeight = 80;
      disconnectedUI_ = new VerticalPanel();
      disconnectedUI_.setWidth("100%");
      disconnectedUI_.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
      codePanel_ = new ConnectionCodePanel(false);
      codePanel_.addStyleName(ThemeStyles.INSTANCE.secondaryToolbarPanel());
      codePanel_.getElement().getStyle().setPadding(8, Unit.PX);
      codePanel_.setHeight((codePanelHeight-5) + "px");
      codePanel_.setWidth("100%");
      disconnectedUI_.add(codePanel_);
      Label label = new Label("(Not connected)");
      Style labelStyle = label.getElement().getStyle();
      labelStyle.setColor("#888");
      labelStyle.setMarginTop(25, Unit.PX);
      labelStyle.setTextAlign(TextAlign.CENTER);
      disconnectedUI_.add(label);
      disconnectedUI_.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
      
      // object browser panel
      objectBrowser_ = new ObjectBrowser();
      
      // container panel to enable switching between connected/disconnected
      ProgressSpinner spin = new ProgressSpinner(ProgressSpinner.COLOR_BLACK);
      spin.getElement().getStyle().setWidth(32, Unit.PX);
      spin.getElement().getStyle().setHeight(32, Unit.PX);
      containerPanel_ = new SimplePanelWithProgress(spin, 50);
      
      setConnected(false);
      
      initWidget(containerPanel_);
      
      eventBus_.addHandler(ConsoleBusyEvent.TYPE, new ConsoleBusyEvent.Handler()
      {
         @Override
         public void onConsoleBusy(ConsoleBusyEvent event)
         {
            // clear progress on console becoming unblocked
            if (!event.isBusy() && containerPanel_.isProgressShowing())
            {
               showActivePanel();
               updateObjectBrowser();
            }
         }
      });
   }
   
   @Inject
   public void initialize(EventBus eventBus)
   {
      eventBus_ = eventBus;
   }
   
   public void showConnectionProgress()
   {
      containerPanel_.showProgress(50); 
   }
   
   public void setConnection(Connection connection, String connectVia)
   {
      connection_ = connection;
      codePanel_.setCode(connection.getConnectCode(), connectVia);
      updateObjectBrowser();
   }
   
   public void setConnected(boolean connected)
   {
      activePanel_ = connected ? objectBrowser_ : disconnectedUI_;
      showActivePanel();
      if (!connected)
         objectBrowser_.clear();
   }
   
   public String getConnectCode()
   {
      return codePanel_.getCode();
   }
   
   public String getConnectVia()
   {
      return codePanel_.getConnectVia();
   }
   
   public void updateObjectBrowser()
   {
      updateObjectBrowser("");
   }
   
   public void updateObjectBrowser(String hint)
   {   
      objectBrowser_.update(connection_, hint);
   }
  
   @Override
   public void onResize()
   {
      containerPanel_.onResize();
      codePanel_.onResize();
      
   }
   
   private void showActivePanel()
   {
      containerPanel_.setWidget(activePanel_);
      containerPanel_.onResize();
   }
   
   private final ConnectionCodePanel codePanel_;
   
   private final VerticalPanel disconnectedUI_;
   private final ObjectBrowser objectBrowser_;
  
   private Widget activePanel_;
   
   private final SimplePanelWithProgress containerPanel_;
   
   private Connection connection_ = null;
   
   private EventBus eventBus_;
}
