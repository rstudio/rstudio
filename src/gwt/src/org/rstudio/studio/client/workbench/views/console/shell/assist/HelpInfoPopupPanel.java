/*
 * HelpInfoPanel.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.VirtualConsole;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.ui.RStudioThemes;
import org.rstudio.studio.client.workbench.views.console.ConsoleConstants;
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
      
      f1prompt_ = new Label(constants_.f1prompt());
      f1prompt_.setStylePrimaryName(consoleStyles_.promptFullHelp());
      outer.add(f1prompt_);

      setWidget(outer);
      setVisible(false);
      setStylePrimaryName(RES.styles().helpPopup());
      
      if (RStudioThemes.usesScrollbars())
         addStyleName("rstudio-themes-scrollbars");
      
      // Normally, I'd apply this via CSS styles, but Safari refuses to
      // acknowledge my attempts to do so and so I must hack it in here
      // https://github.com/rstudio/rstudio/issues/10821
      if (BrowseCap.isSafari())
      {
         outer.getElement().getStyle().setProperty("transform", "translateZ(0)");
         getElement().getStyle().setProperty("transform", "translateZ(0)");
      }
      
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
      String signature = help.getFunctionSignature();
      if (StringUtil.isNullOrEmpty(signature))
      {
         lblSig = new Label(help.getTitle());
         lblSig.setStylePrimaryName(consoleStyles_.packageName());
         vpanel_.add(lblSig);
      }
      else if (!StringUtil.equals(signature, "-"))
      {
         lblSig = new Label(signature);
         lblSig.setStylePrimaryName(consoleStyles_.functionInfoSignature());
         vpanel_.add(lblSig);
      }
      
      String title = help.getTitle();
      if (!StringUtil.isNullOrEmpty(title))
      {
         Label lblTitle = new Label();
         lblTitle.setStylePrimaryName(RES.styles().helpTitleText());
         vpanel_.add(lblTitle);
      }
      
      String description = help.getDescription();
      if (!StringUtil.isNullOrEmpty(description))
      {
         HTML htmlDesc = new HTML(description);
         htmlDesc.setStylePrimaryName(RES.styles().helpBodyText());
         vpanel_.add(htmlDesc);
      }
      
      String glimpse = help.getGlimpse();
      if (!StringUtil.isNullOrEmpty(glimpse))
      {
         HTML htmlGlimpse = new HTML("<pre></pre>");
         htmlGlimpse.setStylePrimaryName(RES.styles().helpGlimpseText());
         
         VirtualConsole vc = RStudioGinjector.INSTANCE.getVirtualConsoleFactory().create(htmlGlimpse.getElement().getFirstChildElement());
         vc.submit(glimpse);
         vpanel_.add(htmlGlimpse);
      }

      doDisplay(help.hasHelp());
   }
   
   public void displayParameterHelp(String name, String description)
   {
      displayParameterHelp(name, description, true);
   }
   
   public void displayParameterHelp(String name, String description, boolean showF1Help)
   {
      timer_.cancel();
      vpanel_.clear();
      
      if (name != null)
      {
         Label lblSig = new Label(name);
         lblSig.setStylePrimaryName(consoleStyles_.paramInfoName());
         vpanel_.add(lblSig);
      }
      
      HTML htmlDesc = new HTML(description);
      htmlDesc.setStylePrimaryName(RES.styles().helpBodyText());
      vpanel_.add(htmlDesc);
      
      doDisplay(showF1Help);
   }
   
   public void displayParameterHelp(Map<String, String> help, String name)
   {
      String description = help.get(name);
      if (description == null)
      {
         clearHelp(false);
         return;
      }
      
      displayParameterHelp(name, description);
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

   public void displayRoxygenHelp(String title, String description, boolean hasVignette)
   {
      timer_.cancel();
      vpanel_.clear();
      
      Label label = new Label(title);
      label.setStylePrimaryName(RES.styles().roxygenTitle());
      vpanel_.add(label);
      
      HTML contents = new HTML(description);
      contents.addStyleName(RES.styles().roxygenText());
      vpanel_.add(contents);
      
      doDisplay(hasVignette);
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
   private static final ConsoleConstants constants_ = GWT.create(ConsoleConstants.class);
}
