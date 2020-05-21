/*
 * RequestLogVisualization.java
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
package org.rstudio.studio.client.application.ui;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.CsvReader;
import org.rstudio.core.client.CsvWriter;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.jsonrpc.RequestLog;
import org.rstudio.core.client.jsonrpc.RequestLogEntry;
import org.rstudio.core.client.jsonrpc.RequestLogEntry.ResponseType;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ScrollPanelWithClick;

import java.util.ArrayList;
import java.util.Iterator;

public class RequestLogVisualization extends Composite
   implements HasCloseHandlers<RequestLogVisualization>, NativePreviewHandler
{
   private class TextBoxDialog extends ModalDialog<String>
   {
      private TextBoxDialog(String caption,
                            String initialValue,
                            OperationWithInput<String> operation)
      {
         super(caption, Roles.getDialogRole(), operation);
         textArea_ = new TextArea();
         textArea_.setSize("400px", "300px");
         textArea_.setText(initialValue);
      }

      @Override
      protected String collectInput()
      {
         return textArea_.getText(); 
      }

      @Override
      protected boolean validate(String input)
      {
         return true;
      }

      @Override
      protected Widget createMainWidget()
      {
         return textArea_;
      }

      private final TextArea textArea_;
   }

   public RequestLogVisualization()
   {
      overviewPanel_ = new LayoutPanel();
      overviewPanel_.getElement().getStyle().setProperty("borderRight",
                                                         "2px dashed #888");
      scrollPanel_ = new ScrollPanelWithClick(overviewPanel_);
      scrollPanel_.setSize("100%", "100%");
      scrollPanel_.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            detail_.setWidget(instructions_);
         }
      });


      SplitLayoutPanel outerPanel = new SplitLayoutPanel();
      outerPanel.getElement().getStyle().setBackgroundColor("white");
      outerPanel.getElement().getStyle().setZIndex(500);
      outerPanel.getElement().getStyle().setOpacity(0.9);

      detail_ = new SimplePanel();
      detail_.getElement().getStyle().setBackgroundColor("#FFE");

      instructions_ = new HTML();
      instructions_.setHTML("<p>Click on a request to see details. Click on the " +
                            "background to show these instructions again.</p>" +
                            "<h4>Available commands:</h4>" +
                            "<ul>" +
                            "<li>Esc: Close</li>" +
                            "<li>P: Play/pause</li>" +
                            "<li>E: Export</li>" +
                            "<li>I: Import</li>" +
                            "<li>+/-: Zoom in/out</li>" +
                            "</ul>");
      detail_.setWidget(instructions_);

      outerPanel.addSouth(detail_, 200);
      outerPanel.add(scrollPanel_);

      initWidget(outerPanel);

      handlerRegistration_ = Event.addNativePreviewHandler(this);

      timer_ = new Timer() {
         @Override
         public void run()
         {
            refresh(true, false);
         }
      };

      refresh(true, true);
   }

   @Override
   protected void onUnload()
   {
      timer_.cancel();
      super.onUnload();
   }

   private void refresh(boolean reloadEntries, boolean scrollToEnd)
   {
      if (reloadEntries)
      {
         entries_ = RequestLog.getEntries();
         now_ = System.currentTimeMillis();
      }

      overviewPanel_.clear();

      startTime_ = entries_[0].getRequestTime();
      long duration = now_ - startTime_;
      int totalWidth = (int) (duration * scaleMillisToPixels_);
      totalHeight_ = entries_.length * BAR_HEIGHT;

      overviewPanel_.setSize(totalWidth + "px", totalHeight_ + "px");

      for (int i = 0, entriesLength = entries_.length; i < entriesLength; i++)
      {
         RequestLogEntry entry = entries_[i];
         addEntry(i, entry);
      }

      if (scrollToEnd)
      {
         scrollPanel_.scrollToTop();
         scrollPanel_.scrollToRight();
      }
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         public void execute()
         {
            scrollPanel_.scrollToTop();
            scrollPanel_.scrollToRight();
         }
      });
   }

   private void addEntry(int i, final RequestLogEntry entry)
   {
      int top = totalHeight_ - (i+1) * BAR_HEIGHT;
      int left = (int) ((entry.getRequestTime() - startTime_) * scaleMillisToPixels_);
      long endTime = entry.getResponseTime() != null
                     ? entry.getResponseTime()
                     : now_;
      int right = Math.max(0, (int) ((now_ - endTime) * scaleMillisToPixels_) - 1);

      boolean active = entry.getResponseType() == ResponseType.None;

      HTML html = new HTML();
      html.getElement().getStyle().setOverflow(Overflow.VISIBLE);
      html.getElement().getStyle().setProperty("whiteSpace", "nowrap");
      
      String method = entry.getRequestMethodName();
      if (method == null)
         method = entry.getRequestId();
      
      html.setText(method + (active ? " (active)" : ""));
      if (active)
         html.getElement().getStyle().setFontWeight(FontWeight.BOLD);
      String color;
      switch (entry.getResponseType())
      {
         case ResponseType.Error:
            color = "red";
            break;
         case ResponseType.None:
            color = "#f99";
            break;
         case ResponseType.Normal:
            color = "#88f";
            break;
         case ResponseType.Cancelled:
            color = "#E0E0E0";
            break;
         case ResponseType.Unknown:
         default:
            color = "yellow";
            break;
      }
      html.getElement().getStyle().setBackgroundColor(color);
      html.getElement().getStyle().setCursor(Cursor.POINTER);

      html.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            event.stopPropagation();
            detail_.clear();
            RequestLogDetail entryDetail = new RequestLogDetail(entry);
            entryDetail.setSize("100%", "100%");
            detail_.setWidget(entryDetail);
         }
      });

      overviewPanel_.add(html);
      overviewPanel_.setWidgetTopHeight(html, top, Unit.PX, BAR_HEIGHT, Unit.PX);
      overviewPanel_.setWidgetLeftRight(html, left, Unit.PX, right, Unit.PX);
      overviewPanel_.getWidgetContainerElement(html).getStyle().setOverflow(Overflow.VISIBLE);
   }

   public HandlerRegistration addCloseHandler(CloseHandler<RequestLogVisualization> handler)
   {
      return addHandler(handler, CloseEvent.getType());
   }

   public void onPreviewNativeEvent(NativePreviewEvent event)
   {
      if (event.getTypeInt() == Event.ONKEYDOWN)
      {
         int keyCode = event.getNativeEvent().getKeyCode();
         if (keyCode == KeyCodes.KEY_ESCAPE)
         {
            CloseEvent.fire(RequestLogVisualization.this,
                            RequestLogVisualization.this);
            handlerRegistration_.removeHandler();
         }
         else if (keyCode == 'R'
                  && KeyboardShortcut.getModifierValue(event.getNativeEvent()) == 0)
         {
            refresh(true, true);
         }
         else if (keyCode == 'P')
         {
            if (timerIsRunning_)
               timer_.cancel();
            else
            {
               timer_.run();
               timer_.scheduleRepeating(PERIOD_MILLIS);
            }
            timerIsRunning_ = !timerIsRunning_;
         }
         else if (keyCode == 'E')
         {
            CsvWriter writer = new CsvWriter();
            writer.writeValue(now_ + "");
            writer.endLine();
            for (RequestLogEntry entry : entries_)
               entry.toCsv(writer);

            TextBoxDialog dialog = new TextBoxDialog("Export",
                                                     writer.getValue(),
                                                     null);
            dialog.showModal();
         }
         else if (keyCode == 'I')
         {
            TextBoxDialog dialog = new TextBoxDialog(
                  "Import",
                  "",
                  new OperationWithInput<String>()
                  {
                     public void execute(String input)
                     {
                        CsvReader reader = new CsvReader(input);
                        ArrayList<RequestLogEntry> entries = new ArrayList<RequestLogEntry>();
                        Iterator<String[]> it = reader.iterator();
                        String now = it.next()[0];
                        while (it.hasNext())
                        {
                           String[] line = it.next();
                           RequestLogEntry entry =
                                 RequestLogEntry.fromValues(line);
                           if (entry != null)
                              entries.add(entry);
                        }
                        now_ = Long.parseLong(now);
                        entries_ = entries.toArray(new RequestLogEntry[0]);
                        refresh(false, true);
                     }
                  });
            dialog.showModal();
         }
      }
      else if (event.getTypeInt() == Event.ONKEYPRESS)
      {
         if (event.getNativeEvent().getKeyCode() == '+')
         {
            scaleMillisToPixels_ *= 2.0;
            refresh(false, false);
         }
         else if (event.getNativeEvent().getKeyCode() == '-')
         {
            scaleMillisToPixels_ /= 2.0;
            refresh(false, false);
         }
      }
   }


   private static final int BAR_HEIGHT = 15;
   private double scaleMillisToPixels_ = 0.02;
   private long now_;
   private RequestLogEntry[] entries_;
   private int totalHeight_;
   private LayoutPanel overviewPanel_;
   private long startTime_;
   private ScrollPanelWithClick scrollPanel_;
   private HandlerRegistration handlerRegistration_;
   private Timer timer_;
   private boolean timerIsRunning_;
   private static final int PERIOD_MILLIS = 2000;
   private SimplePanel detail_;
   private HTML instructions_;
}
