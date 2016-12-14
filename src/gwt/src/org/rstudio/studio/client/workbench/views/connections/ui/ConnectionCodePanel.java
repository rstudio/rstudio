/*
 * ConnectionCodePanel.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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


package org.rstudio.studio.client.workbench.views.connections.ui;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.reditor.EditorLanguage;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditorWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.EditSession;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

public class ConnectionCodePanel extends Composite implements RequiresResize
{
   public ConnectionCodePanel()
   {
      this(true);
   }
   
   public ConnectionCodePanel(boolean connectViaUI)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      container_ = new VerticalPanel();
      container_.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);      
      HorizontalPanel codeHeaderPanel = new HorizontalPanel();
      codeHeaderPanel.addStyleName(RES.styles().codePanelHeader());
      codeHeaderPanel.setWidth("100%");
      Label codeLabel = new Label("Connection:");
      codeHeaderPanel.add(codeLabel);
      codeHeaderPanel.setCellHorizontalAlignment(
               codeLabel, HasHorizontalAlignment.ALIGN_LEFT);
      HorizontalPanel connectPanel = new HorizontalPanel();
      Label connectLabel = new Label("Connect from:");
      connectLabel.addStyleName(RES.styles().leftLabel());
      connectPanel.add(connectLabel);
      connectVia_ = new ListBox();
      updateConnectViaUI_ = new Command() {
         @Override
         public void execute()
         {
            if (connectVia_.getSelectedValue().equals(ConnectionOptions.CONNECT_COPY_TO_CLIPBOARD))
            {
               if (codeViewer_ != null)
               {
                  codeViewer_.getEditor().getSession().getSelection().selectAll();
                  codeViewer_.getEditor().focus();
               }
            }
            else
            {
               if (codeViewer_ != null)
                  codeViewer_.getEditor().getSession().getSelection().moveCursorTo(0, 0, false);
            }
         }
      };
      connectVia_.addItem("R Console", ConnectionOptions.CONNECT_R_CONSOLE);
      connectVia_.addItem("New R Script", ConnectionOptions.CONNECT_NEW_R_SCRIPT);
      connectVia_.addItem("New R Notebook", ConnectionOptions.CONNECT_NEW_R_NOTEBOOK);
      if (BrowseCap.INSTANCE.canCopyToClipboard())
      {
         connectVia_.addItem("Copy to Clipboard", 
                             ConnectionOptions.CONNECT_COPY_TO_CLIPBOARD);
      }
      updateConnectViaUI_.execute();
      addConnectViaChangeHandler(new ChangeHandler() {
         @Override
         public void onChange(ChangeEvent event)
         {
            updateConnectViaUI_.execute();
         }
      });
      connectPanel.add(connectVia_);
      codeHeaderPanel.add(connectPanel);
      codeHeaderPanel.setCellHorizontalAlignment(
            connectPanel, HasHorizontalAlignment.ALIGN_RIGHT);
      if (connectViaUI)
         container_.add(codeHeaderPanel);
     
      initWidget(container_);
   }
   
   @Inject
   public void initialize()
   {
   }
   
   public boolean setConnectVia(String connectVia)
   {
      for (int i = 0; i < connectVia_.getItemCount(); i++)
      {
         if (connectVia_.getValue(i).equals(connectVia))
         {
            connectVia_.setSelectedIndex(i);
            return true;
         }
      }
      return false;
   }
   
   public String getConnectVia()
   {
      return connectVia_.getSelectedValue();
   }
   
   public HandlerRegistration addConnectViaChangeHandler(ChangeHandler handler)
   {
      return connectVia_.addChangeHandler(handler);
   }
   
   public void setCode(String code, String connectVia)
   {
      settingCode_ = true;
      
      // clear existing code viewer
      if (codeViewer_ != null)
      {
         codeViewer_.removeFromParent();
         codeViewer_ = null;
      }
      
      // create new code viewer
      codeViewer_ = new AceEditorWidget(false);
      codeViewer_.addStyleName(RES.styles().codeViewer());
      codeViewer_.getEditor().getSession().setEditorMode(
            EditorLanguage.LANG_R.getParserName(), false);
      codeViewer_.getEditor().getSession().setUseWrapMode(true);
      codeViewer_.getEditor().getRenderer().setShowGutter(false);
      codeViewer_.getEditor().setReadOnly(false);
      codeViewer_.addCursorChangedHandler(new CursorChangedHandler() {
         @Override
         public void onCursorChanged(CursorChangedEvent event)
         {
            EditSession session = codeViewer_.getEditor().getSession();
            String selectedCode = session.getTextRange(session.getSelection().getRange());
            if (!settingCode_ && selectedCode.trim().equals(session.getValue().trim())) 
            {
               setConnectVia(ConnectionOptions.CONNECT_COPY_TO_CLIPBOARD);
            }
         }
      });
      codeViewer_.setCode(code);
      container_.add(codeViewer_);
      
      // update connectVia
      if (connectVia != null)
      {
         setConnectVia(connectVia);
         updateConnectViaUI_.execute();
      }
      onResize();
      settingCode_ = false;
   }
   
   @Override
   public void onResize()
   {
      if (codeViewer_ != null)
      {
         codeViewer_.forceResize();
         codeViewer_.getEditor().resize();
         codeViewer_.forceCursorChange();
      }
   }
   
   public String getCode()
   {
      if (codeViewer_ != null)
         return codeViewer_.getEditor().getSession().getValue();
      else
         return "";
   }
   
   
   private VerticalPanel container_;
   private ListBox connectVia_;
   private AceEditorWidget codeViewer_;
   private boolean settingCode_ = false;
   private final Command updateConnectViaUI_;
   
   private static NewConnectionShinyHost.Resources RES = NewConnectionShinyHost.RES;
}
