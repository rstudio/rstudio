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
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.AnchorableFrame;
import org.rstudio.core.client.widget.SatelliteFramePanel;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarLabel;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.presentation.model.SlideNavigation;
import org.rstudio.studio.client.workbench.views.presentation.model.SlideNavigationItem;

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
      fileLabel_.setText(params.getOutputFile());
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
      toolbar.addLeftWidget(new ToolbarLabel("Viewing: "));
      fileLabel_ = new ToolbarLabel();
      fileLabel_.addStyleName(ThemeStyles.INSTANCE.subtitle());
      fileLabel_.getElement().getStyle().setMarginRight(7, Unit.PX);
      toolbar.addLeftWidget(fileLabel_);
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands.viewerRefresh().createToolbarButton());
      publishButtonSeparator_ = toolbar.addLeftSeparator();
      publishButton_ = commands.publishHTML().createToolbarButton(false);
      toolbar.addLeftWidget(publishButton_);
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
         }
      });
      return frame;
   }
   
   @Override
   public void refresh()
   {
      // cache the scroll position, so we can re-apply it when the page loads
      scrollPosition_ = getScrollPosition();
      showUrl(getCurrentUrl());
   }

   @Override
   protected String openCommandText()
   {
      return "Open in Browser";
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
   
   // the current URL is the one currently showing in the frame, which may 
   // reflect navigation occurring after initial load (e.g. anchor changes)
   private String getCurrentUrl()
   {
      return getFrame().getIFrame().getContentDocument().getURL();
   }
   
   @SuppressWarnings("unused")
   private SlideNavigation getIoslidesNavigationList(Document doc)
   {
      
      JsArray<SlideNavigationItem> navItems = JsArray.createArray().cast();
      
      NodeList<Element> slides = doc.getElementsByTagName("slide");
      for (int i = 0; i<slides.getLength(); i++) {
           
         Element slide = slides.getItem(i);
         boolean segue = slide.getClassName().contains("segue");
         
         NodeList<Element> hgroups = slide.getElementsByTagName("hgroup");
         if (hgroups.getLength() == 1)
         {
            Element header = hgroups.getItem(0).getFirstChildElement();
            if (header.getTagName().equalsIgnoreCase("h2"))
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
      
      return SlideNavigation.create(slides.getLength(), navItems);
      
   }

   private Label fileLabel_;
   private ToolbarButton publishButton_;
   private Widget publishButtonSeparator_;
   private String title_;
   
   private RMarkdownServerOperations server_;
   private int scrollPosition_ = 0;
}
