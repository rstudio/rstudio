/*
 * SourceMarkerList.java
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
package org.rstudio.studio.client.common.sourcemarkers;

import java.util.ArrayList;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.widget.DoubleClickState;
import org.rstudio.core.client.widget.FastSelectTable;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.TableColElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;

public class SourceMarkerList extends Composite
                  implements HasSelectionCommitHandlers<CodeNavigationTarget>
{
   public static final int AUTO_SELECT_NONE = 0;
   public static final int AUTO_SELECT_FIRST = 1;
   public static final int AUTO_SELECT_FIRST_ERROR = 2;

   public SourceMarkerList()
   {
      codec_ = new SourceMarkerItemCodec(res_, false);

      errorTable_ = new FastSelectTable<SourceMarker, CodeNavigationTarget, CodeNavigationTarget>(
            codec_,
            res_.styles().selectedRow(),
            true,
            false,
            "Source Marker Item Table");
      setWidths();
      errorTable_.setStyleName(res_.styles().table());
      errorTable_.setSize("100%", "100%");
      errorTable_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            if (doubleClick_.checkForDoubleClick(event.getNativeEvent()))
            {
               fireSelectionCommittedEvent();
            }
         }
         private final DoubleClickState doubleClick_ = new DoubleClickState();
      });
      errorTable_.addKeyDownHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
               fireSelectionCommittedEvent();
         }
      });

      errorTable_.addMouseDownHandler(new MouseDownHandler()
      {
         public void onMouseDown(MouseDownEvent event)
         {
            Element el = DOM.eventGetTarget((Event) event.getNativeEvent());
            if (el != null
                && el.getTagName().equalsIgnoreCase("div")
                && el.getClassName().contains(res_.styles().disclosure()))
            {
               ArrayList<CodeNavigationTarget> values =
                                             errorTable_.getSelectedValues2();
               if (values.size() == 1)
               {
                  fireSelectionCommitedEvent(values.get(0));
               }
            }
         }
      });


      ScrollPanel scrollPanel = new ScrollPanel(errorTable_);
      scrollPanel.addStyleName("ace_editor_theme");
      scrollPanel.setSize("100%", "100%");
      initWidget(scrollPanel);
   }

   @Override
   public HandlerRegistration addSelectionCommitHandler(
                     SelectionCommitEvent.Handler<CodeNavigationTarget> handler)
   {
      return addHandler(handler, SelectionCommitEvent.getType());
   }

   public void showMarkers(String targetFile,
                           String basePath,
                           JsArray<SourceMarker> errors,
                           int autoSelect)
   {
      boolean showFileHeaders = false;
      ArrayList<SourceMarker> errorList = new ArrayList<SourceMarker>();
      int firstErrorIndex = -1;
      for (int i=0; i<errors.length(); i++)
      {
         SourceMarker error = errors.get(i);
         if (firstErrorIndex == -1 && error.getType() == SourceMarker.ERROR)
            firstErrorIndex = i;

         if (error.getPath() != targetFile)
            showFileHeaders = true;

         errorList.add(error);
      }

      codec_.setShowFileHeaders(showFileHeaders);
      codec_.setFileHeaderBasePath(basePath);
      errorTable_.addItems(errorList, false);

      if (autoSelect == AUTO_SELECT_FIRST)
      {
         selectFirstItem();
      }
      else if (autoSelect == AUTO_SELECT_FIRST_ERROR)
      {
         if (firstErrorIndex != -1)
            errorTable_.setSelected(firstErrorIndex, 1, true);
      }
   }

   public void ensureSelection()
   {
      if (errorTable_.getSelectedRowIndexes().isEmpty())
         selectFirstItem();
   }

   public void selectFirstItem()
   {
      if (errorTable_.getRowCount() > 0)
         errorTable_.setSelected(0, 1, true);
   }

   public void focus()
   {
      errorTable_.focus();
   }

   public void clear()
   {
      errorTable_.clear();
      setWidths();
   }

   private void setWidths()
   {
      setColumnClasses(errorTable_.getElement().<TableElement>cast(),
                       res_.styles().iconCell(),
                       res_.styles().lineCell(),
                       res_.styles().messageCell());
   }

   private void setColumnClasses(TableElement table,
                                 String... classes)
   {
      TableColElement colGroupElement = Document.get().createColGroupElement();
      for (String clazz : classes)
      {
         TableColElement colElement = Document.get().createColElement();
         colElement.setClassName(clazz);
         colGroupElement.appendChild(colElement);
      }
      table.appendChild(colGroupElement);
   }

   private void fireSelectionCommittedEvent()
   {
      ArrayList<CodeNavigationTarget> values = errorTable_.getSelectedValues();
      if (values.size() == 1)
         fireSelectionCommitedEvent(values.get(0));
   }

   private void fireSelectionCommitedEvent(CodeNavigationTarget target)
   {
      SelectionCommitEvent.fire(this, target);
   }

   private final SourceMarkerItemCodec codec_;
   private final FastSelectTable<SourceMarker, CodeNavigationTarget, CodeNavigationTarget> errorTable_;
   private final SourceMarkerListResources res_ = SourceMarkerListResources.INSTANCE;
}
