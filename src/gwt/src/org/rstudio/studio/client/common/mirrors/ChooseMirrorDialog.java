/*
 * ChooseMirrorDialog.java
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
package org.rstudio.studio.client.common.mirrors;

import java.util.ArrayList;

import org.rstudio.core.client.widget.FocusHelper;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.SimplePanelWithProgress;
import org.rstudio.core.client.widget.images.ProgressImages;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.server.ServerDataSource;
import org.rstudio.studio.client.server.ServerError;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

public class ChooseMirrorDialog<T extends JavaScriptObject> extends ModalDialog<T>
{
   public interface Source<T extends JavaScriptObject> 
                    extends ServerDataSource<JsArray<T>>
   {
      String getType();
      String getLabel(T mirror);
      String getURL(T mirror);
   }
   
   public ChooseMirrorDialog(GlobalDisplay globalDisplay,
                             Source<T> mirrorSource,
                             OperationWithInput<T> inputOperation)
   {
      super("Choose " + mirrorSource.getType() + " Mirror", inputOperation);
      globalDisplay_ = globalDisplay;
      mirrorSource_ = mirrorSource;
      enableOkButton(false);
   }

   @Override
   protected T collectInput()
   {
      if (listBox_ != null && listBox_.getSelectedIndex() >= 0)
      {
         return mirrors_.get(listBox_.getSelectedIndex());
      }
      else
      {
         return null;
      }
   }

   @Override
   protected boolean validate(T input)
   {
      if (input == null)
      {
         globalDisplay_.showErrorMessage("Error", 
                                         "Please select a CRAN Mirror");
         return false;
      }
      else
      {
         return true;
      }
   }

   @Override
   protected Widget createMainWidget()
   {
      // create progress container
      final SimplePanelWithProgress panel = new SimplePanelWithProgress(
                                          ProgressImages.createLargeGray());
      panel.setStylePrimaryName(RESOURCES.styles().mainWidget());
         
      // show progress (with delay)
      panel.showProgress(200);
      
      // query data source for packages
      mirrorSource_.requestData(new SimpleRequestCallback<JsArray<T>>() {

         @Override 
         public void onResponseReceived(JsArray<T> mirrors)
         {   
            // keep internal list of mirrors 
            mirrors_ = new ArrayList<T>(mirrors.length());
            
            // create list box and select default item
            listBox_ = new ListBox(false);
            listBox_.setVisibleItemCount(18); // all
            listBox_.setWidth("100%");
            if (mirrors.length() > 0)
            {
               for(int i=0; i<mirrors.length(); i++)
               {
                  T mirror = mirrors.get(i);
                  mirrors_.add(mirror);
                  String item = mirrorSource_.getLabel(mirror);
                  String value = mirrorSource_.getURL(mirror);
                  listBox_.addItem(item, value);
               }
               
               listBox_.setSelectedIndex(0);
               enableOkButton(true);
            }
            
            // set it into the panel
            panel.setWidget(listBox_);
            
            // update ok button on changed
            listBox_.addDoubleClickHandler(new DoubleClickHandler() {
               @Override
               public void onDoubleClick(DoubleClickEvent event)
               {
                  clickOkButton();              
               }
            });
            
            
            // if the list box is larger than the space we initially allocated
            // then increase the panel height
            final int kDefaultPanelHeight = 285;
            if (listBox_.getOffsetHeight() > kDefaultPanelHeight)
               panel.setHeight(listBox_.getOffsetHeight() + "px");
            
            // set focus   
            FocusHelper.setFocusDeferred(listBox_);
         }
         
         @Override
         public void onError(ServerError error)
         {
            closeDialog();
            super.onError(error);
         }
      });
      
      return panel;
   }
   
   static interface Styles extends CssResource
   {
      String mainWidget();
   }
  
   static interface Resources extends ClientBundle
   {
      @Source("ChooseMirrorDialog.css")
      Styles styles();
   }
   
   static Resources RESOURCES = (Resources)GWT.create(Resources.class) ;
   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }
   
   private final GlobalDisplay globalDisplay_ ;
   private final Source<T> mirrorSource_;
   private ArrayList<T> mirrors_ = null;
   private ListBox listBox_ = null;

}
