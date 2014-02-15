/*
 * RmdOutputPanel.java
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
package org.rstudio.studio.client.rmarkdown.ui;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.AnchorableFrame;
import org.rstudio.core.client.widget.SatelliteFramePanel;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarLabel;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.common.presentation.SlideNavigationMenu;
import org.rstudio.studio.client.common.presentation.SlideNavigationToolbarMenu;
import org.rstudio.studio.client.common.presentation.events.SlideIndexChangedEvent;
import org.rstudio.studio.client.common.presentation.events.SlideNavigationChangedEvent;
import org.rstudio.studio.client.common.presentation.events.SlideNavigationChangedEvent.Handler;
import org.rstudio.studio.client.common.presentation.model.SlideNavigation;
import org.rstudio.studio.client.common.presentation.model.SlideNavigationItem;

public class RmdOutputPanel extends SatelliteFramePanel<AnchorableFrame>
                            implements RmdOutputPresenter.Display
{
   @Inject
   public RmdOutputPanel(Commands commands, 
                         RMarkdownServerOperations server)
   {
      super(commands);
      server_ = server;
   }
   
   @Override
   public void showOutput(RmdPreviewParams params, boolean enablePublish, 
                          boolean refresh)
   {
      fileLabel_.setText(FileSystemItem.createFile(
                                       params.getOutputFile()).getName());
      
      // we can only publish self-contained HTML to RPubs
      boolean showPublish = enablePublish && 
                            params.getResult().isHtml() &&
                            params.getResult().isSelfContained();
      publishButton_.setText(params.getResult().getRpubsPublished() ? 
            "Republish" : "Publish");
      publishButton_.setVisible(showPublish);
      publishButtonSeparator_.setVisible(showPublish);
      // when refreshing, reapply the current scroll position and anchor
      scrollPosition_ = refresh ? 
            getScrollPosition() : params.getScrollPosition();
      String url;
      if (refresh)
      {
         url = getCurrentUrl();
      }
      else
      {
         url = server_.getApplicationURL(params.getOutputUrl());
         if (params.getAnchor().length() > 0)
         {
            url += "#" + params.getAnchor();
         }
      }
      showUrl(url);
   }
   
   @Override
   protected void initToolbar (Toolbar toolbar, Commands commands)
   {
      AppCommand presHome = commands.presentationHome();
      ToolbarButton homeButton = new ToolbarButton(
            presHome.getImageResource(),
            new ClickHandler() {
               @Override
               public void onClick(ClickEvent event)
               {
                  navigate(0);
               }
            });
      homeButton.setTitle(presHome.getTooltip());
      
      slideNavigationMenu_ = new SlideNavigationToolbarMenu(toolbar, 
                                                            homeButton,
                                                            400, 
                                                            100,
                                                            true);
      
      fileLabel_ = new ToolbarLabel();
      fileLabel_.addStyleName(ThemeStyles.INSTANCE.subtitle());
      fileLabel_.getElement().getStyle().setMarginRight(7, Unit.PX);
      toolbar.addLeftWidget(fileLabel_);
      toolbar.addLeftSeparator();
      ToolbarButton popoutButton = 
            commands.viewerPopout().createToolbarButton();
      popoutButton.setText("Open in Browser");
      toolbar.addLeftWidget(popoutButton);
      publishButtonSeparator_ = toolbar.addLeftSeparator();
      publishButton_ = commands.publishHTML().createToolbarButton(false);
      toolbar.addLeftWidget(publishButton_);

      toolbar.addRightWidget(commands.viewerRefresh().createToolbarButton());
   }
   
   @Override
   protected AnchorableFrame createFrame(String url)
   {
      AnchorableFrame frame = new AnchorableFrame();
      frame.navigate(url);
      frame.addLoadHandler(new LoadHandler()
      {
         @Override
         public void onLoad(LoadEvent event)
         {
            Document doc = getFrame().getIFrame().getContentDocument();
            if (scrollPosition_ > 0)
               doc.setScrollTop(scrollPosition_);
            String title = doc.getTitle();
            if (title != null && !title.isEmpty())
            {
               Window.setTitle(title);
               title_ = title;
            }
            
            // see if we can do ioslides navigation
            SlideNavigation slideNavigation = getIoslidesNavigationList(doc);  
            handlerManager_.fireEvent(new SlideNavigationChangedEvent(
                                                           slideNavigation));
            
            // slide change monitoring
            slideChangeMonitor_.cancel();
            if (slideNavigation != null)
            {
               fireSlideIndexChanged();
               slideChangeMonitor_.scheduleRepeating(250);
            }
         }
      });
      return frame;
   }
   
   private Timer slideChangeMonitor_ = new Timer() {

      @Override
      public void run()
      {
         String url = getCurrentUrl();
         if (!url.equals(lastUrl_))
         {
            lastUrl_ = url;
            fireSlideIndexChanged();
         }
      }
      
      private String lastUrl_ = null;  
   };
   
   @Override
   public void refresh()
   {
      // cache the scroll position, so we can re-apply it when the page loads
      scrollPosition_ = getScrollPosition();
      showUrl(getCurrentUrl());
   }

   @Override
   public int getScrollPosition()
   {
      return getFrame().getIFrame().getContentDocument().getScrollTop();
   }
   
   @Override
   public String getTitle()
   {
      return title_;
   }
   
   @Override
   public String getAnchor()
   {
      String url = getCurrentUrl();
      int anchorPos = url.lastIndexOf("#");
      return anchorPos > 0 ? url.substring(anchorPos + 1) : "";
   }
   
   @Override
   public void navigate(int index)
   {
      getFrame().getIFrame().focus();
      
      String url = getCurrentUrl();
      int anchorPos = url.lastIndexOf("#");
      if (anchorPos > 0)
         url = url.substring(0, anchorPos);
      
      showUrl(url + "#" + (index + 1));
   }

   @Override
   public SlideNavigationMenu getNavigationMenu()
   {
      return slideNavigationMenu_;
   }

   @Override
   public HandlerRegistration addSlideNavigationChangedHandler(Handler handler)
   {
      return handlerManager_.addHandler(SlideNavigationChangedEvent.TYPE, 
                                        handler);
   }
   
   @Override
   public HandlerRegistration addSlideIndexChangedHandler(
                                 SlideIndexChangedEvent.Handler handler)
   {
      return handlerManager_.addHandler(SlideIndexChangedEvent.TYPE, handler);
   }
   
   // the current URL is the one currently showing in the frame, which may 
   // reflect navigation occurring after initial load (e.g. anchor changes)
   private String getCurrentUrl()
   {
      return getFrame().getIFrame().getContentDocument().getURL();
   }
  
   private SlideNavigation getIoslidesNavigationList(Document doc)
   {
      // look for ioslides 
      JsArray<SlideNavigationItem> navItems = JsArray.createArray().cast();
      
      NodeList<Element> slides = doc.getElementsByTagName("slide");
      if (slides.getLength() <= 2) // just the title slide + the background
         return null;
      
      for (int i = 0; i<slides.getLength(); i++) 
      {     
         Element slide = slides.getItem(i);
         boolean segue = slide.getClassName().contains("segue");
         
         NodeList<Element> hgroups = slide.getElementsByTagName("hgroup");
         if (hgroups.getLength() != 0)
         {  
            // get the slide
            Element header = hgroups.getItem(0).getFirstChildElement();
            String h = header.getTagName();
            if (h.equalsIgnoreCase("h1") || h.equalsIgnoreCase("h2"))
            {
               String title = header.getInnerText();
               if (!StringUtil.isNullOrEmpty(title))
               {  
                  SlideNavigationItem navItem = SlideNavigationItem.create(
                                    title, segue ? 0 : 1, i, -1);
                  navItems.push(navItem);
                  
               }
            }
         }
      }
      
      if (navItems.length() > 0)
         return SlideNavigation.create(slides.getLength() - 1, navItems);  
      else
         return null;
   }
  
   private void fireSlideIndexChanged()
   {
      try
      {
         String anchor = getAnchor();
         if (anchor.length() == 0)
            anchor = "1";
         int index = Integer.parseInt(anchor);
         handlerManager_.fireEvent(new SlideIndexChangedEvent(index-1));
      }
      catch(NumberFormatException e)
      {
      }
   }
   
   private SlideNavigationToolbarMenu slideNavigationMenu_;

   private Label fileLabel_;
   private ToolbarButton publishButton_;
   private Widget publishButtonSeparator_;
   private String title_;
   
   private RMarkdownServerOperations server_;
   private int scrollPosition_ = 0;
   
   private HandlerManager handlerManager_ = new HandlerManager(this);
}
