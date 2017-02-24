/*
 * AddRemoteDialog.java
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

package org.rstudio.studio.client.workbench.views.vcs;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.VerticalSpacer;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class AddRemoteDialog extends ModalDialog<AddRemoteDialog.Input>
{
   public static class Input
   {
      public Input(String name, String url)
      {
         name_ = name;
         url_ = url;
      }
      
      public final String getName() { return name_; }
      public final String getUrl() { return url_; }
      
      private final String name_;
      private final String url_;
   }

   @Override
   protected Input collectInput()
   {
      return new Input(
            tbName_.getValue().trim(),
            tbUrl_.getValue().trim());
   }
   
   public AddRemoteDialog(String caption,
                          String defaultUrl,
                          OperationWithInput<Input> operation)
   {
      super(caption, operation);
      setOkButtonCaption("Add");
      
      container_ = new VerticalPanel();
      lblName_ = label("Remote Name:");
      lblUrl_ = label("Remote URL:");
      tbName_ = textBox();
      tbUrl_ = textBox();
      
      tbUrl_.setValue(StringUtil.notNull(defaultUrl));
      
      container_.add(lblName_);
      container_.add(tbName_);
      container_.add(new VerticalSpacer("6px"));
      container_.add(lblUrl_);
      container_.add(tbUrl_);
      container_.add(new VerticalSpacer("6px"));
   }

   @Override
   protected Widget createMainWidget()
   {
      return container_;
   }
   
   @Override
   public void showModal()
   {
      super.showModal();
      tbName_.setFocus(true);
   }
   
   private TextBox textBox()
   {
      TextBox textBox = new TextBox();
      textBox.setWidth("260px");
      textBox.getElement().getStyle().setMarginBottom(6, Unit.PX);
      return textBox;
   }
   
   private Label label(String text)
   {
      Label label = new Label(text);
      label.getElement().getStyle().setMarginBottom(4, Unit.PX);
      return label;
   }
   
   private final Label lblName_;
   private final Label lblUrl_;
   private final TextBox tbName_;
   private final TextBox tbUrl_;
   private final VerticalPanel container_;
}
