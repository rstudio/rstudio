/*
 * ConnectionExplorer.java
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

package org.rstudio.studio.client.workbench.views.connections.ui;

import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.workbench.views.connections.model.Connection;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.RequiresResize;

public class ConnectionExplorer extends Composite implements RequiresResize
{
   public ConnectionExplorer()
   {
     
      
      int codePanelHeight = 125;
      codePanel_ = new ConnectionCodePanel();
      codePanel_.addStyleName(ThemeStyles.INSTANCE.secondaryToolbarPanel());
      codePanel_.getElement().getStyle().setPadding(8, Unit.PX);
      codePanel_.setHeight(codePanelHeight + "px");
      codePanel_.setWidth("100%");
     
     
      setConnected(false);
      
      initWidget(codePanel_);
   }
   
   public void setConnection(Connection connection, String connectVia)
   {
      codePanel_.setCode(connection.getConnectCode(), connectVia);
   }
   
   public void setConnected(boolean connected)
   {
      codePanel_.setVisible(!connected);
   }
   
   public String getConnectCode()
   {
      return codePanel_.getCode();
   }
   
   public String getConnectVia()
   {
      return codePanel_.getConnectVia();
   }
  
   @Override
   public void onResize()
   {
      codePanel_.onResize();
      
   }
   
  
   
   private final ConnectionCodePanel codePanel_;
      
  
   
   
}
