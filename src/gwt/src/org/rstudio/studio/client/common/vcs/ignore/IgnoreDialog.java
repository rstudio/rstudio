/*
 * IgnoreDialog.java
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
package org.rstudio.studio.client.common.vcs.ignore;

import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class IgnoreDialog extends ModalDialogBase 
                          implements Ignore.Display
{    
   @Inject
   public IgnoreDialog()
   {
      editor_ = new AceEditor();
      editor_.setUseWrapMode(false);
      editor_.setShowLineNumbers(false);
      
      saveButton_ = new ThemedButton("Save", (ClickHandler)null); 
      addButton(saveButton_);
      addCancelButton();
      setButtonAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
      
      progressIndicator_ = addProgressIndicator();
   }
   
   @Override
   public HasClickHandlers saveButton()
   {
      return saveButton_;
   }
   
   @Override
   public ProgressIndicator progressIndicator()
   {
      return progressIndicator_;
   }
   
   @Override
   public void showDialog(String caption, String ignores)
   {
      setText(caption);
      editor_.setCode(ignores, false);
      showModal();
   }
   
   @Override
   public String getIgnored()
   {
      return editor_.getCode();
   }

   @Override
   protected Widget createMainWidget()
   {
      final String width = "300px";
      final String height = "300px";
      
      Widget editorWidget = editor_.getWidget();
      editorWidget.setSize(width, height);
      
      SimplePanel panel = new SimplePanel();
      panel.addStyleName(RES.styles().editorFrame());
      panel.setSize(width, height);
      panel.setWidget(editor_.getWidget());
      return panel;
   }
   
   static interface Styles extends CssResource
   {
      String editorFrame();
   }
  
   static interface Resources extends ClientBundle
   {
      @Source("IgnoreDialog.css")
      Styles styles();
   }
   
   static Resources RES = (Resources)GWT.create(Resources.class) ;
   public static void ensureStylesInjected()
   {
      RES.styles().ensureInjected();
   }

   private final AceEditor editor_ ;
   private final ThemedButton saveButton_;
   private final ProgressIndicator progressIndicator_;
  
  
  
}
