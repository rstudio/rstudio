/*
 * NewConnectionSnippetDialog.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

import java.util.ArrayList;
import java.util.HashMap;

import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext.NewConnectionInfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class NewConnectionSnippetDialog extends ModalDialog<HashMap<String, String>>
{
   public interface NewConnectionSnippetDialogStyle extends CssResource
   {
   }

   @Inject
   private void initialize()
   {
   }
   
   public NewConnectionSnippetDialog(
      OperationWithInput<HashMap<String, String>> operation,
      ArrayList<NewConnectionSnippetParts> config,
      NewConnectionInfo newConnectionInfo)
   {
      super("Advanced Options", operation);
      initialConfig_ = config;
      newConnectionInfo_ = newConnectionInfo;

      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Override
   protected void onDialogShown()
   {
      super.onDialogShown();

      setOkButtonCaption("Configure");

      if (newConnectionInfo_.getHelp() != null) {
         HelpLink helpLink = new HelpLink(
            "Using " + newConnectionInfo_.getName(),
            newConnectionInfo_.getHelp(),
            false,
            false);

         addLeftWidget(helpLink);
      }
   }
   
   @Override
   protected Widget createMainWidget()
   {
      SimplePanel wrapper = new SimplePanel();
      wrapper.setStyleName(RES.styles().wrapper());
      
      Grid connGrid = new Grid(initialConfig_.size(), 2);
      connGrid.addStyleName(RES.styles().grid());

      for (int idxParams = 0; idxParams < initialConfig_.size(); idxParams++) {
         final String key = initialConfig_.get(idxParams).getKey();
         Label label = new Label(key + ":");
         label.addStyleName(RES.styles().label());
         connGrid.setWidget(idxParams, 0, label);
         connGrid.getRowFormatter().setVerticalAlign(idxParams, HasVerticalAlignment.ALIGN_TOP);
         
         final TextBox textbox = new TextBox();
         textbox.setText(initialConfig_.get(idxParams).getValue());
         textbox.addStyleName(RES.styles().textbox());
         connGrid.setWidget(idxParams, 1, textbox);
         
         textbox.addChangeHandler(new ChangeHandler()
         {
            @Override
            public void onChange(ChangeEvent arg0)
            {
               partsKeyValues_.put(key, textbox.getValue());
            }
         });
      }
      
      wrapper.add(connGrid);
      
      return wrapper;
   }
   
   @Override
   protected HashMap<String, String> collectInput()
   {
      return partsKeyValues_;
   }

   public interface Styles extends CssResource
   {
      String grid();
      String label();
      String textbox();
      String wrapper();
   }

   public interface Resources extends ClientBundle
   {
      @Source("NewConnectionSnippetDialog.css")
      Styles styles();
   }
   
   public static Resources RES = GWT.create(Resources.class);
   public static void ensureStylesInjected() 
   {
      RES.styles().ensureInjected();
   }
   
   private NewConnectionInfo newConnectionInfo_;
   private ArrayList<NewConnectionSnippetParts> initialConfig_;
   HashMap<String, String> partsKeyValues_ = new HashMap<String, String>();
}
