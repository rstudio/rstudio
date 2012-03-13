/*
 * PDFViewerPanel.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.pdfviewer.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.*;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.WidgetHandlerRegistration;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.pdfviewer.PDFViewerPresenter;
import org.rstudio.studio.client.pdfviewer.events.InitCompleteEvent;
import org.rstudio.studio.client.pdfviewer.events.PageClickEvent;
import org.rstudio.studio.client.pdfviewer.model.SyncTexCoordinates;
import org.rstudio.studio.client.pdfviewer.pdfjs.PDFView;
import org.rstudio.studio.client.pdfviewer.pdfjs.PdfJs;

public class PDFViewerPanel extends Composite
                            implements PDFViewerPresenter.Display
{
   interface Binder extends UiBinder<Widget, PDFViewerPanel>
   {}

   @Inject
   public PDFViewerPanel(PDFViewerToolbar toolbar)
   {
      toolbar_ = toolbar;

      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));
      viewer_.getElement().setId("viewer");
      Document.get().getBody().getStyle().setMarginLeft(200, Style.Unit.PX);
      
      // tweak font baseline for ubuntu mono on chrome
      if (BrowseCap.hasUbuntuFonts() && BrowseCap.isChrome())
         lblStatus_.getElement().getStyle().setTop(-1, Unit.PX);

      new WidgetHandlerRegistration(this)
      {
         @Override
         protected HandlerRegistration doRegister()
         {
            return Event.addNativePreviewHandler(new Event.NativePreviewHandler()
            {
               @Override
               public void onPreviewNativeEvent(Event.NativePreviewEvent event)
               {
                  if ( (event.getTypeInt() == Event.ONCLICK) &&
                        DomUtils.isCommandClick(event.getNativeEvent()))
                  { 
                     EventTarget target =
                                        event.getNativeEvent().getEventTarget();

                     if (Element.is(target))
                     {
                        Element el = Element.as(target);
                        if (viewer_.getElement().isOrHasChild(el))
                        {
                           fireClickEvent(event.getNativeEvent(), el);
                        }
                     }
                  }
               }
            });
         }
      };
   }

   private void fireClickEvent(NativeEvent nativeEvent, Element el)
   {
      Element pageEl = el;
      while (pageEl != null)
      {
         if (pageEl.getId().matches("^pageContainer([\\d]+)$"))
         {
            break;
         }

         pageEl = pageEl.getParentElement();
      }

      if (pageEl == null)
         return;

      int page = getContainerPageNum(pageEl);

      int pageX = nativeEvent.getClientX() +
                  Document.get().getDocumentElement().getScrollLeft() +
                  Document.get().getBody().getScrollLeft() -
                  pageEl.getAbsoluteLeft();
      int pageY = nativeEvent.getClientY() +
                  Document.get().getDocumentElement().getScrollTop() +
                  Document.get().getBody().getScrollTop() -
                  pageEl.getAbsoluteTop();

      fireEvent(new PageClickEvent(new SyncTexCoordinates(
            page,
            (int) ((pageX / PDFView.currentScale() / 96) * 72),
            (int) ((pageY / PDFView.currentScale() / 96) * 72))));
   }

   private int getContainerPageNum(Element container)
   {
      return Integer.parseInt(
            container.getId().substring("pageContainer".length()));
   }

   @Override
   public PDFViewerToolbarDisplay getToolbarDisplay()
   {
      return toolbar_;
   }

   @Override
   public void toggleThumbnails()
   {
      BodyElement body = Document.get().getBody();
      if (body.getClassName().contains("nosidebar"))
         body.removeClassName("nosidebar");
      else
         body.addClassName("nosidebar");
   }

   @Override
   public void updateSelectedPage(int pageNumber)
   {
      Element pageLabel =
                   Document.get().getElementById("thumbnailLabel" + pageNumber);
      if (pageLabel != null && pageLabel.getClassName().contains("selected"))
         return;

      if (selectedPageLabel_ != null)
         selectedPageLabel_.removeClassName("selected");

      selectedPageLabel_ = pageLabel;

      if (selectedPageLabel_ != null)
      {
         selectedPageLabel_.addClassName("selected");

         Element scroller = Document.get().getElementById("sidebarScrollView");
         Element page =
               Document.get().getElementById("thumbnailContainer" + pageNumber);
         DomUtils.ensureVisibleVert(scroller, page, 30);
      }
   }

   @Override
   public void setStatusText(String text)
   {
      lblStatus_.setText(text);
   }
   
   @Override
   public SyncTexCoordinates getTopCoordinates()
   {
      int scrollY = Document.get().getScrollTop() + toolbar_.getOffsetHeight();

      // linear probe our way to the current page
      Element viewerEl = viewer_.getElement();
      for (int i = 1; i < viewerEl.getChildCount(); i+=2)
      {
         Node childNode = viewerEl.getChild(i);
         if (Element.is(childNode))
         {
            Element el = Element.as(childNode);

            if (el.getAbsoluteBottom() > scrollY)
            {
               int pageNum = getContainerPageNum(el);
               int pageY = scrollY - el.getAbsoluteTop();
               if (pageY < 0)
                  pageY = 0;

               return new SyncTexCoordinates(
                     pageNum,
                     0,
                     (int) ((pageY / PDFView.currentScale() / 96) * 72));
            }
         }
      }
      return null;
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();

      if (!once_)
      {
         once_ = true;
         PdfJs.load(new Command()
         {
            @Override
            public void execute()
            {
               loaded_ = true;
               if (initialUrl_ != null)
                  open(initialUrl_);

               fireEvent(new InitCompleteEvent());
            }
         });
      }
   }

   @Override
   public void setURL(String url)
   {
      PDFView.setLoadingVisible(true);

      if (loaded_)
         open(url);
      else
         initialUrl_ = url;
   }

   @Override
   public HandlerRegistration addInitCompleteHandler(
                                              InitCompleteEvent.Handler handler)
   {
      return addHandler(handler, InitCompleteEvent.TYPE);
   }
   
   @Override
   public void closeWindow()
   {
      WindowEx.get().close();
   }

   private native void open(String url) /*-{
      $wnd.PDFView.open(url, 0);
   }-*/;

   @Override
   public HandlerRegistration addPageClickHandler(PageClickEvent.Handler handler)
   {
      return addHandler(handler, PageClickEvent.TYPE);
   }

   private boolean loaded_;
   private String initialUrl_;
   private boolean once_;

   @UiField(provided = true)
   PDFViewerToolbar toolbar_;
   @UiField
   FlowPanel viewer_;
   @UiField
   Label lblStatus_;

   private Element selectedPageLabel_;
}
