/*
 * SupportPopupMenu.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.application.ui.support;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.PopupPanel;
import org.rstudio.core.client.widget.HyperlinkLabel;
import org.rstudio.core.client.widget.ThemedPopupPanel;
import org.rstudio.studio.client.common.GlobalDisplay;

public class SupportPopupMenu extends ThemedPopupPanel
{  
   public SupportPopupMenu(GlobalDisplay globalDisplay)
   {
      super(true,    // auto-hide
            false);  // non-modal
      
      setStylePrimaryName(RES.styles().supportPopupMenu());
      
      // setup table to contain menu items
      FlexTable supportTable = new FlexTable();
      supportTable.setHeight("0px");
      supportTable.getColumnFormatter().setWidth(0, "100%");
      
      // report a bug
      addMenuItem(supportTable,
                  "Report a Bug",
                  "bugs@rstudio.org",
                  globalDisplay);
      
      // suggest a feature
      addMenuItem(supportTable,
                  "Suggest a Feature",
                  "feedback@rstudio.org",
                  globalDisplay);
      
      // ask a question
      addMenuItem(supportTable,
                  "Ask a Question",
                  "support@rstudio.org",
                  globalDisplay);
                  
      // set widget
      this.setWidget(supportTable);
   }
   
   
   private void addMenuItem(FlexTable supportTable,        
                            String caption, 
                            final String email,
                            final GlobalDisplay globalDisplay)
   {
      // maintain reference to containing class for closing
      final PopupPanel popupPanel = this;
      
      // create a hyperlink label for this URL
      HyperlinkLabel link = new HyperlinkLabel(caption, new ClickHandler() {
         public void onClick(ClickEvent event)
         {
            globalDisplay.openEmailComposeWindow(email, null);
            popupPanel.hide();
         }
      });
      
      int row = supportTable.getRowCount();
      supportTable.setWidget(row, 0, link);
   }
   
   static Resources RES = (Resources)GWT.create(Resources.class);
   public static void ensureStylesInjected()
   {
      RES.styles().ensureInjected();
   }
   
   interface Resources extends ClientBundle
   {
      @Source("SupportPopupMenu.css")
      Styles styles();
   }

   interface Styles extends CssResource
   {
      String supportPopupMenu();
   }

}
