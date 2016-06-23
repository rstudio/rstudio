/*
 * ComponentsNotInstalledDialogs.java
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

public class ComponentsNotInstalledDialogs 
{
   public static void showSparkNotInstalled()
   {
      String message = 
        "<p>There are no versions of Spark currently installed on this system.</p>" +
        "<p>Please contact your system " +
        "administrator to request installation of Spark.</p>";
      showDialog("Spark Not Installed", message, null);
   }
   
   public static void showSparkHomeNotDefined()
   {
      String message = 
       "<p>Connecting with a Spark cluster requires that you are on a system " +
       "able to communicate with the cluster in both directions, and " +
       "requires that the SPARK_HOME environment variable refers to a  " +
       "locally installed version of Spark that is configured to " +
       "communicate with the cluster.</p>" +
       "<p>Your system doesn't currently have the SPARK_HOME environment " +
       "variable defined. Please contact your system administrator to " +
       "ensure that the server is properly configured to connect with " +
       "the cluster.<p>";
      showDialog("Connect to Spark", message, null);
   }
   
   public static void showServerRequiredForCluster()
   {
      String message = 
            "<p>Connecting with a remote Spark cluster requires " +
            "an RStudio Server instance that is either within the cluster " +
            "or has a high bandwidth connection to the cluster.</p>" +
            "<p>Please see the <strong>Using Spark with RStudio</strong> help " +
            "link below for additional details.</p>";
      showDialog("Connect to Spark", message, null);
   }
   
   public static void showJavaNotInstalled(String installUrl)
   {    
      StringBuilder builder = new StringBuilder();
      builder.append(
         "<p>In order to connect to Spark " +
         "your system needs to have Java installed (" +
          "no version of Java was detected).</p>");
      
      String url = null;
      if (Desktop.isDesktop())
      {
         builder.append(
           "<p>Click the link below to navigate to the Java website where " +
           "you will find instructions for installing Java (note that you " + 
           "may also need to restart RStudio after installing Java).</p>");
        
         url = installUrl;
      }
      else
      {
         builder.append(
            "<p>Please contact your server administrator to request the " + 
            "installation of Java on this system.</p>");
      }
      
      showDialog("Java Required for Spark Connections", 
                 builder.toString(), 
                 url);
   }
   
   private static void showDialog(String caption, 
                                  String message, 
                                  final String url)
   {
      NewSparkConnectionDialog.Styles styles = 
            NewSparkConnectionDialog.RES.styles();
      VerticalPanel verticalPanel = new VerticalPanel();
      verticalPanel.addStyleName(styles.componentNotInstalledWidget());

      HTML msg = new HTML(message);
      msg.setWidth("100%");
      verticalPanel.add(msg);
      
      if (url != null)
      {
         HyperlinkLabel link = new HyperlinkLabel(url, 
               new ClickHandler() {
            @Override
            public void onClick(ClickEvent event)
            {
               RStudioGinjector.INSTANCE.getGlobalDisplay()
               .openWindow(url);
            }

         });
         link.addStyleName(styles.componentInstallLink());
         verticalPanel.add(link);
      }
      
      MessageDialog dlg = new MessageDialog(MessageDialog.INFO,
            caption,
            verticalPanel);

      dlg.addButton("OK", (Operation)null, true, false);
      dlg.showModal();
   }
   
}
