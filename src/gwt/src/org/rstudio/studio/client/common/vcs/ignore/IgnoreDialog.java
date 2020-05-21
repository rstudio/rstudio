/*
 * IgnoreDialog.java
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
package org.rstudio.studio.client.common.vcs.ignore;

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.widget.CaptionWithHelp;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class IgnoreDialog extends ModalDialogBase 
                          implements Ignore.Display
{    
   @Inject
   public IgnoreDialog()
   {
      super(Roles.getDialogRole());
      dirChooser_ = new DirectoryChooserTextBox("Directory:", 
                                                "", 
                                                ElementIds.TextBoxButtonId.VCS_IGNORE,
                                                null);
      dirChooser_.addStyleName(RES.styles().dirChooser());
      
      editor_ = new AceEditor();
      editor_.setUseWrapMode(false);
      editor_.setShowLineNumbers(false);
      editor_.setTabAlwaysMovesFocus();
      editor_.setTextInputAriaLabel("Ignored files");
      
      ignoresCaption_ = new CaptionWithHelp("Ignore:",
                                             "Specifying ignored files",
                                             editor_.getWidget());
      ignoresCaption_.setIncludeVersionInfo(false);
      ignoresCaption_.addStyleName(RES.styles().ignoresCaption());

      saveButton_ = new ThemedButton("Save", (ClickHandler)null); 
      addButton(saveButton_, ElementIds.DIALOG_OK_BUTTON);
      addCancelButton();
      setButtonAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
      
      progressIndicator_ = addProgressIndicator();
   }
   
   @Override
   public void setDialogCaption(String caption)
   {
      setText(caption);
   }
   
   @Override
   public void setIgnoresCaption(String caption)
   {
      ignoresCaption_.setCaption(caption);
   }
   
   @Override
   public void setHelpLinkName(String helpLinkName)
   {
      ignoresCaption_.setRStudioLinkName(helpLinkName);
   }
   
   @Override
   public ProgressIndicator progressIndicator()
   {
      return progressIndicator_;
   }
   
   @Override
   public HasClickHandlers saveButton()
   {
      return saveButton_;
   }
   
   @Override
   public void setCurrentPath(String path)
   {
      dirChooser_.setText(path);
   }
   
   @Override
   public String getCurrentPath()
   {
      return dirChooser_.getText();
   }
   
   @Override
   public HandlerRegistration addPathChangedHandler(
                                 ValueChangeHandler<String> handler)
   {
      return dirChooser_.addValueChangeHandler(handler);
   }
   
   
   @Override
   public void setIgnored(String ignored)
   {
      editor_.setCode(ignored.trim(), false);
   }
   
   @Override
   public void focusIgnored()
   {
      editor_.focus();
   }
   
   @Override
   public String getIgnored()
   {
      String ignored = editor_.getCode();
      if (ignored.length() > 0)
         ignored = ignored.trim() + "\n";
      return ignored;
   }
   
   @Override
   public void scrollToBottom()
   {
      editor_.scrollToBottom();
   }
   
   @Override
   protected Widget createMainWidget()
   {
      VerticalPanel verticalPanel = new VerticalPanel();
      
      verticalPanel.add(dirChooser_);
      
      verticalPanel.add(ignoresCaption_);
      
      final String aceWidth = "400px";
      final String aceHeight = "300px";
      
      Widget editorWidget = editor_.getWidget();
      editorWidget.setSize(aceWidth, aceHeight);
     
      SimplePanel panel = new SimplePanel();
      panel.addStyleName(RES.styles().editorFrame());
      panel.setSize(aceWidth, aceHeight);
      panel.setWidget(editor_.getWidget());
      verticalPanel.add(panel);
      
      return verticalPanel;
   }
   
   @Override
   protected void focusInitialControl()
   {
      editor_.focus();
   }
   
   static interface Styles extends CssResource
   {
      String dirChooser();
      String ignoresCaption();
      String editorFrame();
   }
  
   static interface Resources extends ClientBundle
   {
      @Source("IgnoreDialog.css")
      Styles styles();
   }
   
   static Resources RES = (Resources)GWT.create(Resources.class);
   public static void ensureStylesInjected()
   {
      RES.styles().ensureInjected();
   }

   private final DirectoryChooserTextBox dirChooser_;
   private final CaptionWithHelp ignoresCaption_;
   private final AceEditor editor_;
   private final ThemedButton saveButton_;
   private final ProgressIndicator progressIndicator_;
  
  
  
}
