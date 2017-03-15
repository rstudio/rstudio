/*
 * ShinyGadgetDialog.java
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

package org.rstudio.studio.client.shiny.ui;

import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.DomMetrics;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.HasDirection.Direction;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ShinyGadgetDialog extends ModalDialogBase
{
   public ShinyGadgetDialog(String caption, String url, Size preferredSize)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      url_ = url;
      preferredSize_ = preferredSize;
      addCaptionWithCloseButton(caption);
      
      // one time initialization of static event handlers
      if (!initializedEvents_)
      {
         initializedEvents_ = true;
         initializeEvents();
      }
   }
   
   @Inject
   void initialize(Commands commands)
   {
      commands_ = commands;
   }
   
   @Override
   protected void onLoad()
   {
      super.onLoad();
      activeDialog_ = this;
   }
   
   @Override
   protected void onUnload()
   {
      super.onUnload();
      activeDialog_ = null;
   }
   
   @Override
   protected Widget createMainWidget()
   {
      frame_ = new RStudioFrame();
      frame_.addStyleName(ThemeStyles.INSTANCE.borderedIFrame());
      
      // compute the widget size and set it
      Size minimumSize = new Size(300, 300);
      Size size = DomMetrics.adjustedElementSize(preferredSize_, 
                                                 minimumSize, 
                                                 0,   // pad
                                                 100); // client margin
      frame_.setSize(size.width + "px", size.height + "px");
      
      if (Desktop.isDesktop())
         Desktop.getFrame().setShinyDialogUrl(url_);
      
      frame_.setUrl(url_);
      return frame_;
   }
   
   @Override
   protected void onDialogShown()
   {
      super.onDialogShown();
      frame_.getWindow().focus();
   }
   
   private native static void initializeEvents() /*-{  
      var handler = $entry(function(e) {
         if (typeof e.data != 'string')
            return;
         @org.rstudio.studio.client.shiny.ui.ShinyGadgetDialog::onMessage(Ljava/lang/String;Ljava/lang/String;)(e.data, e.origin);
      });
      $wnd.addEventListener("message", handler, true);
   }-*/;

   private static void onMessage(String data, String origin)
   {  
      if ("disconnected".equals(data))
      {
         // ensure the frame url starts with the specified origin
         if ((activeDialog_ != null) && 
             activeDialog_.getUrl().startsWith(origin))
         {
            activeDialog_.performClose();
         }
      }
   }
   
   private void addCaptionWithCloseButton(String caption)
   {
      final Image closeIcon = new Image(new ImageResource2x(ThemeResources.INSTANCE.closeDialog2x()));
      Style closeIconStyle = closeIcon.getElement().getStyle();
      closeIconStyle.setCursor(Style.Cursor.POINTER);
      closeIconStyle.setMarginTop(2, Unit.PX);

      FlexTable captionLayoutTable = new FlexTable();
      captionLayoutTable.setWidth("100%");
      captionLayoutTable.setText(0, 0, caption);
      captionLayoutTable.setWidget(0, 1, closeIcon);
      captionLayoutTable.getCellFormatter().setHorizontalAlignment(0, 1,
            HasHorizontalAlignment.HorizontalAlignmentConstant.endOf(Direction.LTR));

      HTML captionWidget = (HTML) getCaption();
      captionWidget.getElement().appendChild(captionLayoutTable.getElement());

      captionWidget.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event) {
            EventTarget target = event.getNativeEvent().getEventTarget();
            Element targetElement = (Element) target.cast();

            if (targetElement == closeIcon.getElement()) {
               closeIcon.fireEvent(event);
            }
         }
      });

      closeIcon.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event) {
            performClose();
         }
      });
   }
   
   private void performClose()
   {
      closeDialog();
      
      if (commands_.interruptR().isEnabled())
         commands_.interruptR().execute();
   }
   
   private String getUrl()
   {
      return frame_.getUrl();
   }

   private final String url_;
   private Size preferredSize_;
   private RStudioFrame frame_;
   private Commands commands_;
   private static boolean initializedEvents_ = false;
   private static ShinyGadgetDialog activeDialog_ = null;
}
