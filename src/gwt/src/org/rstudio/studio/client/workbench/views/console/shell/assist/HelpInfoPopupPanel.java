/*
 * HelpInfoPane.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import java.util.Map;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.application.ui.RStudioThemes;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.help.model.HelpInfo;

public class HelpInfoPopupPanel extends PopupPanel
{
   public HelpInfoPopupPanel()
   {
      super();
      consoleStyles_ = ConsoleResources.INSTANCE.consoleStyles();

      FlowPanel outer = new FlowPanel();

      scrollPanel_.add(vpanel_);
      vpanel_.setStylePrimaryName(consoleStyles_.functionInfo());
      vpanel_.setWidth("100%");
      outer.add(scrollPanel_);
      
      f1prompt_ = new Label("Press F1 for additional help");
      f1prompt_.setStylePrimaryName(consoleStyles_.promptFullHelp());
      outer.add(f1prompt_);

      setWidget(outer);
      setVisible(false);
      setStylePrimaryName(RES.styles().helpPopup());
      
      if (RStudioThemes.usesScrollbars())
         addStyleName("rstudio-themes-scrollbars");
      
      timer_ = new Timer() {
         public void run()
         {
            scrollPanel_.setVisible(false);
            f1prompt_.setVisible(false);
            vpanel_.clear();
            setVisible(false);
         }
      };
   }

   public void displayHelp(HelpInfo.ParsedInfo help)
   {
      timer_.cancel();
      vpanel_.clear();

      Label lblSig;
      if (StringUtil.isNullOrEmpty(help.getFunctionSignature()))
      {
         lblSig = new Label(help.getTitle());
         lblSig.setStylePrimaryName(consoleStyles_.packageName());
      }
      else
      {
         lblSig = new Label(help.getFunctionSignature());
         lblSig.setStylePrimaryName(consoleStyles_.functionInfoSignature());
      }
      vpanel_.add(lblSig);
      
      HTML htmlDesc = new HTML(help.getDescription());
      htmlDesc.setStylePrimaryName(RES.styles().helpBodyText());
      vpanel_.add(htmlDesc);
      
      doDisplay();

   }
   
   public void displayParameterHelp(Map<String, String> help, String paramName)
   {
      String desc = help.get(paramName);
      if (desc == null)
      {
         clearHelp(false);
         return;
      }

      timer_.cancel();
      vpanel_.clear();

      if (paramName != null)
      {
         Label lblSig = new Label(paramName);
         lblSig.setStylePrimaryName(consoleStyles_.paramInfoName());
         vpanel_.add(lblSig);
      }
      
      HTML htmlDesc = new HTML(desc);
      htmlDesc.setStylePrimaryName(RES.styles().helpBodyText());
      vpanel_.add(htmlDesc);
      
      doDisplay();
   }
   
   public void displayPackageHelp(HelpInfo.ParsedInfo help)
   {
      timer_.cancel();
      vpanel_.clear();

      String title = help.getTitle();
      if (title != null)
      {
         Label label = new Label(title);
         label.setStylePrimaryName(consoleStyles_.packageName());
         vpanel_.add(label);
      }
      
      HTML htmlDesc = new HTML(help.getDescription());
      htmlDesc.setStylePrimaryName(RES.styles().helpBodyText());
      vpanel_.add(htmlDesc);

      doDisplay();
   }
   
   public void displaySnippetHelp(String contents)
   {
      timer_.cancel();
      vpanel_.clear();
      
      Label contentsLabel = new Label(contents.replace("\t", "  "));
      contentsLabel.addStyleName(RES.styles().snippetText());
      vpanel_.add(contentsLabel);
      
      doDisplay(false);
   }

   public void clearHelp(boolean downloadOperationPending)
   {
      f1prompt_.setVisible(false);
      timer_.cancel();
      if (downloadOperationPending)
         timer_.schedule(170);
      else
         timer_.run();
   }
   
   private void doDisplay()
   {
      doDisplay(true);
   }
   
   private void doDisplay(boolean showF1Prompt)
   {
      vpanel_.setVisible(true);
      f1prompt_.setVisible(showF1Prompt);
      scrollPanel_.setVisible(true);
      
      String newHeight = Math.min(135, vpanel_.getOffsetHeight()) + "px";
      scrollPanel_.setHeight(newHeight);
   }
   
   private final ScrollPanel scrollPanel_ = new ScrollPanel();
   private final VerticalPanel vpanel_ = new VerticalPanel();
   private final Timer timer_;
   private final ConsoleResources.ConsoleStyles consoleStyles_;
   private Label f1prompt_;
   
   private static HelpInfoPopupPanelResources RES =
         HelpInfoPopupPanelResources.INSTANCE;
   
   static {
      RES.styles().ensureInjected();
   }
   
}
