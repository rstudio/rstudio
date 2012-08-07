/*
 * CompileErrorList.java
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
package org.rstudio.studio.client.common.compile.errorlist;

import java.util.ArrayList;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.widget.DoubleClickState;
import org.rstudio.core.client.widget.FastSelectTable;
import org.rstudio.studio.client.common.compile.CompileError;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.TableColElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;

public class CompileErrorList extends Composite
                  implements HasSelectionCommitHandlers<CodeNavigationTarget>
{
   public CompileErrorList()
   {
      codec_ = new CompileErrorItemCodec(res_, false);
      
      errorTable_ = new FastSelectTable<CompileError, CodeNavigationTarget, CodeNavigationTarget>(
            codec_,
            res_.styles().selectedRow(),
            true,
            false);
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
                && el.getClassName().equals(res_.styles().disclosure()))
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
      scrollPanel.setSize("100%", "100%");
      initWidget(scrollPanel);
   }
   
   @Override
   public HandlerRegistration addSelectionCommitHandler(
                     SelectionCommitHandler<CodeNavigationTarget> handler)
   {
      return addHandler(handler, SelectionCommitEvent.getType());
   }
 
   public void showErrors(String targetFile, 
                          String basePath,
                          JsArray<CompileError> errors)
   {
      boolean showFileHeaders = false;
      ArrayList<CompileError> errorList = new ArrayList<CompileError>();
      for (CompileError error : JsUtil.asIterable(errors))
      {
         if (!error.getPath().equals(targetFile))
            showFileHeaders = true;
         errorList.add(error);
      }

      codec_.setShowFileHeaders(showFileHeaders);
      codec_.setFileHeaderBasePath(basePath);
      errorTable_.addItems(errorList, false);
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

   private final CompileErrorItemCodec codec_;
   private final FastSelectTable<CompileError, CodeNavigationTarget, CodeNavigationTarget> errorTable_;
   private final CompileErrorListResources res_ = CompileErrorListResources.INSTANCE;  
}
