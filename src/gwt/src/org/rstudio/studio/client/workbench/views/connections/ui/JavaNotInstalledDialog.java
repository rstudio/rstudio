/*
 * JavaNotInstalledDialog.java
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

import org.rstudio.core.client.widget.HyperlinkLabel;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;

public class JavaNotInstalledDialog 
{
   public static void show(final String installUrl)
   {
      NewSparkConnectionDialog.Styles styles = 
                              NewSparkConnectionDialog.RES.styles();
      VerticalPanel verticalPanel = new VerticalPanel();
      verticalPanel.addStyleName(styles.javaNotInstalledWidget());
      
     
      HTML msg = new HTML();
      msg.setWidth("100%");
      
      StringBuilder builder = new StringBuilder();
      builder.append(
         "<p>In order to connect to a local or remote Spark " +
         "cluster your system needs to have Java installed. " +
          "No version of Java was detected on this system.</p>");
      
      if (Desktop.isDesktop())
      {
         builder.append(
           "<p>Click the link below to navigate to the Java website where " +
           "you will find instructions for installing Java (note that you " + 
           "may also need to restart RStudio after installing Java).</p>");
         
         msg.setHTML(builder.toString());
         verticalPanel.add(msg);
         
         HyperlinkLabel link = new HyperlinkLabel(installUrl, 
                                                  new ClickHandler() {
            @Override
            public void onClick(ClickEvent event)
            {
               RStudioGinjector.INSTANCE.getGlobalDisplay()
                                             .openWindow(installUrl);
            }
            
         });
         link.addStyleName(styles.javaInstallLink());
         verticalPanel.add(link);
      }
      else
      {
         builder.append(
            "<p>You should request that your " +
            "server administrator install Java on this system.</p>");
             
         msg.setHTML(builder.toString());
         verticalPanel.add(msg);
      }
      
      MessageDialog dlg = new MessageDialog(MessageDialog.INFO,
                                            "Java Required for Spark Connections",
                                            verticalPanel);
      
      dlg.addButton("OK", (Operation)null, true, false);
      dlg.showModal();
   }
   
}
