/*
 * Presentation2Pane.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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

package org.rstudio.studio.client.workbench.views.presentation2;

import org.rstudio.core.client.URIConstants;
import org.rstudio.core.client.URIUtils;
import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.common.AutoGlassPanel;
import org.rstudio.studio.client.quarto.model.QuartoNavigate;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.ui.RSConnectPublishButton;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.presentation2.events.PresentationHashChangeEvent;
import org.rstudio.studio.client.workbench.views.presentation2.events.PresentationInitEvent;
import org.rstudio.studio.client.workbench.views.presentation2.events.PresentationInitEvent.Handler;
import org.rstudio.studio.client.workbench.views.presentation2.events.PresentationSlideChangeEvent;
import org.rstudio.studio.client.workbench.views.presentation2.model.RevealMessage;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class Presentation2Pane extends WorkbenchPane implements Presentation2.Display
{
   @Inject
   public Presentation2Pane(Commands commands, ApplicationServerOperations server)
   {
      super("Presentation");
      commands_ = commands;
      server_ = server;
      ensureWidget();
      initializeEvents();
   }
   

   @Override
   protected Toolbar createMainToolbar()
   {
      toolbar_ = new Toolbar("Presentation Toolbar");
      
      // navigate
      toolbar_.addLeftWidget(commands_.presentation2Prev().createToolbarButton());
      toolbar_.addLeftWidget(commands_.presentation2Next().createToolbarButton());
      
      // TODO: standard slide navigation/edit widget
      
      toolbar_.addLeftSeparator();
      toolbar_.addLeftWidget(commands_.presentation2ViewInBrowser().createToolbarButton());
      
      // publish
      publishButton_ = new RSConnectPublishButton(
            RSConnectPublishButton.HOST_PRESENTATION2,
            RSConnect.CONTENT_TYPE_NONE, true, null);
      toolbar_.addRightWidget(publishButton_);

      // refresh
      toolbar_.addRightSeparator();
      toolbar_.addRightWidget(commands_.refreshPresentation2().createToolbarButton());
      
      return toolbar_;
   }

   @Override
   protected Widget createMainWidget()
   {
      frame_ = new RStudioFrame("Presentation Preview");
      frame_.addStyleName("ace_editor_theme");
      frame_.setSize("100%", "100%");
      frame_.setUrl(URIConstants.ABOUT_BLANK);
      frame_.getElement().setAttribute("webkitallowfullscreen", "");
      frame_.getElement().setAttribute("mozallowfullscreen", "");
      frame_.getElement().setAttribute("allowfullscreen", "");
      return new AutoGlassPanel(frame_);
   }
   
   @Override
   public void activate()
   {
      // bring tab to front
      bringToFront();
          
      // compute min height based on normal reveal presentation ratio and current width
      int frameWidth = frame_.getOffsetWidth();
      if (frameWidth > 0)
      {
         double ratio = (double)700 / 960;
         ensureHeight((int)(frameWidth * ratio));
      }
   }
   
   @Override
   public void navigate(String url, QuartoNavigate nav)
   {
      // in desktop mode we need to be careful about loading URLs which are
      // non-local; before changing the URL, set the iframe to be sandboxed
      // based on whether we're working with a local URL (note that prior to
      // RStudio 1.2 local URLs were forbidden entirely)
      if (Desktop.hasDesktopFrame())
      {
         if (URIUtils.isLocalUrl(url))
            frame_.getElement().removeAttribute("sandbox");
         else
            frame_.getElement().setAttribute("sandbox", "allow-scripts");
      }
      
      // update active presentation target
      activeUrl_ = url; // used for post-message
      
      // captured from init message and played back to reveal frame
      source_ = null;
      origin_ = null;
      
      // load content
      frame_.setUrl(activeUrl_);
      
      // set publish button
      if (nav.isWebsite())
         publishButton_.setQuartoSitePreview();
      else
         publishButton_.setQuartoDocPreview(nav.getSourceFile(), nav.getOutputFile());
      
      // redraw toolbar
      toolbar_.invalidateSeparators();      
   }
   
   @Override
   public void clear()
   {
      activeUrl_ = null;
      source_ = null;
      origin_ = null;
      publishButton_.setContentType(RSConnect.CONTENT_TYPE_NONE);
      frame_.setUrl("about:blank");
   }
   
   // TODO: use SlideNavigationToolbar re-usable class
   
   @Override
   public void home()
   {
      postRevealMessage("home");
   }

   @Override
   public void next()
   {
      postRevealMessage("next");
   }


   @Override
   public void prev()
   {
      postRevealMessage("previous");
   }

   @Override
   public void refresh()
   {
      postRevealMessage("reload");
   }
   
   @Override
   public HandlerRegistration addPresentationInitHandler(Handler handler)
   {
      return handlerManager_.addHandler(PresentationInitEvent.TYPE, handler);
   }

   @Override
   public HandlerRegistration addPresentationSlideChangeHandler(org.rstudio.studio.client.workbench.views.presentation2.events.PresentationSlideChangeEvent.Handler handler)
   {
      return handlerManager_.addHandler(PresentationSlideChangeEvent.TYPE, handler);
   }
   
   @Override
   public HandlerRegistration addPresentationHashChangeHandler(org.rstudio.studio.client.workbench.views.presentation2.events.PresentationHashChangeEvent.Handler handler)
   {
      return handlerManager_.addHandler(PresentationHashChangeEvent.TYPE, handler);
   }
   
   private native void initializeEvents() /*-{  
      var self = this;
      var callback = $entry(function(event) {
         self.@org.rstudio.studio.client.workbench.views.presentation2.Presentation2Pane::onMessage(*)(
            event.source,
            event.origin,
            event.data
         );
      });
      $wnd.addEventListener("message", callback, true);
      
   }-*/;

   private void onMessage(JavaScriptObject source, String origin, RevealMessage message) 
   {    
      // check to see if the message originated from the same origin
      if (activeUrl_ == null || !activeUrl_.startsWith(origin))
         return;
        
      // record source and origin for sending messages back
      source_ = source;
      origin_ = origin;
      
      final String kRevealMessagePrefix = "reveal-";
      if (message.getMessage() != null &&
          message.getMessage().startsWith(kRevealMessagePrefix))
      {
         String type = message.getMessage().replaceFirst(kRevealMessagePrefix, "");
         if (type == "init")
         {
            handlerManager_.fireEvent(new PresentationInitEvent(message.getData().cast()));
         }
         else if (type == "slidechange")
         {
            handlerManager_.fireEvent(new PresentationSlideChangeEvent(message.getData().cast()));
         }  
         else if (type == "hashchange")
         {
            handlerManager_.fireEvent(new PresentationHashChangeEvent(message.getData().cast()));
         }
      }
    
   }
   
   private void postRevealMessage(String type)
   {
      postRevealMessage(type, null);
   }
   
   private void postRevealMessage(String type, JavaScriptObject data)
   {
      postRevealMessage(source_, origin_, type, data);
   }
   
   private native static void postRevealMessage(JavaScriptObject source, String origin, 
                                                String message, JavaScriptObject data) /*-{
      source.postMessage({
         message: "reveal-" + message,
         data: data
      }, origin);
   }-*/;

   private String activeUrl_ = null;
   private JavaScriptObject source_ = null;
   private String origin_ = null;
     
   private final Commands commands_;
   private ApplicationServerOperations server_;
   
   private Toolbar toolbar_;
   private RSConnectPublishButton publishButton_;
   private RStudioFrame frame_;
  
   private HandlerManager handlerManager_ = new HandlerManager(this);
}
