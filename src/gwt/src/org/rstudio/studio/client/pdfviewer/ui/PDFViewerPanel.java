/*
 * PDFViewerPanel.java
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
package org.rstudio.studio.client.pdfviewer.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.dom.client.*;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.WidgetHandlerRegistration;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.common.Value;
import org.rstudio.studio.client.common.synctex.model.PdfLocation;
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
      PDFView.toggleSidebar();
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
      return getBoundaryCoordinates(true);
   }
   
   @Override
   public SyncTexCoordinates getBottomCoordinates()
   {
      return getBoundaryCoordinates(false);
   }
   
   private SyncTexCoordinates getBoundaryCoordinates(boolean top)
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

               if (!top)
               {
                  final int kStatusBarHeight = 16;
                  pageY += Document.get().getClientHeight() - kStatusBarHeight;
               }
               
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
   public void navigateTo(final PdfLocation pdfLocation)
   {
      double factor = PDFView.currentScale() * 96 / 72;

      final double x = pdfLocation.getX() * factor;
      final double y = pdfLocation.getY() * factor;
      final double w = pdfLocation.getWidth() * factor;
      final double h = pdfLocation.getHeight() * factor;

      final Value<Integer> retries = new Value<Integer>(0);

      // Sometimes pageContainer is null during load, so retry every 100ms
      // until it's not, or we've tried 40 times.
      Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
      {
         @Override
         public boolean execute()
         {
            Element pageContainer = Document.get().getElementById(
                  "pageContainer" + pdfLocation.getPage());

            if (pageContainer == null)
            {
               retries.setValue(retries.getValue() + 1);
               return retries.getValue() < 40;
            }

            if (pdfLocation.isFromClick())
            {
               final DivElement div = Document.get().createDivElement();
               div.getStyle().setPosition(Style.Position.ABSOLUTE);
               div.getStyle().setTop(y, Unit.PX);
               div.getStyle().setLeft(x, Unit.PX);
               div.getStyle().setWidth(w, Unit.PX);
               div.getStyle().setHeight(h, Unit.PX);
               div.getStyle().setBackgroundColor("rgba(0, 126, 246, 0.1)");
               div.getStyle().setProperty("transition", "opacity 4s");
               // use DomUtils to set transition styles so gwt doesn't assert
               // an invalid style name (no camelCase) in debug mode
               DomUtils.setStyle(div, "-moz-transition", "opacity 4s");
               DomUtils.setStyle(div, "-webkit-transition", "opacity 4s");

               pageContainer.appendChild(div);

               Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand()
               {
                  @Override
                  public boolean execute()
                  {
                     div.getStyle().setOpacity(0.0);
                     return false;
                  }
               }, 2000);
            }

            // scroll to the page
            PDFView.goToPage(pdfLocation.getPage());

            // if the target isn't on-screen then scroll to it
            if (pdfLocation.getY() > getBottomCoordinates().getY())
            {
               Window.scrollTo(
                  Window.getScrollLeft(),
                  Math.max(0, pageContainer.getAbsoluteTop() + (int) y - 180));
            }

            return false;
         }
      }, 100);
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
