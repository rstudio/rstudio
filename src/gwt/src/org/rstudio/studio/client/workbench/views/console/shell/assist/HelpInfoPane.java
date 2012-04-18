/*
 * HelpInfoPane.java
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
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.help.model.HelpInfo;

public class HelpInfoPane extends Composite
{
   public HelpInfoPane()
   {
      styles_ = ConsoleResources.INSTANCE.consoleStyles();

      DockLayoutPanel outer = new DockLayoutPanel(Unit.PX);

      f1prompt_ = new Label("Press F1 for additional help");
      f1prompt_.setStylePrimaryName(styles_.promptFullHelp());
      outer.addSouth(f1prompt_, 16);

      scrollPanel_.add(vpanel_) ;
      vpanel_.setStylePrimaryName(styles_.functionInfo()) ;
      vpanel_.setWidth("100%") ;
      outer.add(scrollPanel_);

      initWidget(outer) ;
      
      timer_ = new Timer() {
         public void run()
         {
            scrollPanel_.setVisible(false) ;
            vpanel_.clear() ;
         }
      };
   }

   public void displayFunctionHelp(HelpInfo.ParsedInfo help)
   {
      timer_.cancel() ;
      vpanel_.clear() ;

      f1prompt_.setVisible(true);
      
      if (help.getFunctionSignature() != null)
      {
         Label lblSig = new Label(help.getFunctionSignature()) ;
         lblSig.setStylePrimaryName(styles_.functionInfoSignature()) ;
         vpanel_.add(lblSig);
      }
      
      HTML htmlDesc = new HTML(help.getDescription()) ;
      htmlDesc.setStylePrimaryName(styles_.functionInfoSummary()) ;
      vpanel_.add(htmlDesc) ;

      scrollPanel_.setVisible(true) ;
   }
   
   public void displayParameterHelp(HelpInfo.ParsedInfo help, String paramName)
   {
      String desc = help.getArgs().get(paramName) ;
      if (desc == null)
      {
         clearHelp(false) ;
         return ;
      }

      timer_.cancel() ;
      vpanel_.clear() ;

      f1prompt_.setVisible(true);

      if (paramName != null)
      {
         Label lblSig = new Label(paramName) ;
         lblSig.setStylePrimaryName(styles_.paramInfoName()) ;
         vpanel_.add(lblSig);
      }
      
      HTML htmlDesc = new HTML(desc) ;
      htmlDesc.setStylePrimaryName(styles_.paramInfoDesc()) ;
      vpanel_.add(htmlDesc) ;
      
      scrollPanel_.setVisible(true) ;
   }

   public void clearHelp(boolean downloadOperationPending)
   {
      f1prompt_.setVisible(false);

      timer_.cancel() ;
      if (downloadOperationPending)
         timer_.schedule(170) ;
      else
         timer_.run() ;
   }
   
   private final ScrollPanel scrollPanel_ = new ScrollPanel() ;
   private final VerticalPanel vpanel_ = new VerticalPanel() ;
   private final Timer timer_;
   private final ConsoleResources.ConsoleStyles styles_;
   private Label f1prompt_;
}
