/*
 * AddRemoteDialog.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.a11y.A11y;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.VerticalSpacer;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class AddRemoteDialog extends ModalDialog<AddRemoteDialog.Input>
                             implements KeyDownHandler
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
      super(caption, Roles.getDialogRole(), operation);
      setOkButtonCaption("Add");

      final String nameLabel = "Remote Name:";
      final String urlLabel = "Remote URL:";
      final String NAME_ID = ElementIds.idFromLabel(nameLabel);
      final String URL_ID = ElementIds.idFromLabel(urlLabel);
      
      container_ = new VerticalPanel();
      lblName_ = label(nameLabel, NAME_ID);
      lblUrl_ = label(urlLabel, URL_ID);
      tbName_ = textBox(NAME_ID);
      tbUrl_ = textBox(URL_ID);
      
      tbName_.addKeyDownHandler(this);
      tbUrl_.addKeyDownHandler(this);
      
      tbUrl_.setValue(StringUtil.notNull(defaultUrl));
      
      container_.add(lblName_);
      container_.add(tbName_);
      container_.add(new VerticalSpacer("6px"));
      container_.add(lblUrl_);
      container_.add(tbUrl_);
      container_.add(new VerticalSpacer("6px"));
      
      synchronize();
   }

   @Override
   protected Widget createMainWidget()
   {
      return container_;
   }
   
   @Override
   public void onKeyDown(KeyDownEvent event)
   {
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            synchronize();
         }
      });
   }

   @Override
   public void focusFirstControl()
   {
      tbName_.setFocus(true);
      tbName_.selectAll();
   }

   private void synchronize()
   {
      boolean isValidState =
            !StringUtil.isNullOrEmpty(tbName_.getValue()) &&
            !StringUtil.isNullOrEmpty(tbUrl_.getValue());
      
      enableOkButton(isValidState);
   }
   
   private TextBox textBox(String id)
   {
      TextBox textBox = new TextBox();
      textBox.setWidth("260px");
      textBox.getElement().getStyle().setMarginBottom(6, Unit.PX);
      textBox.getElement().setAttribute("spellcheck", "false");
      textBox.getElement().setId(id);
      A11y.setARIARequired(textBox.getElement());
      return textBox;
   }
   
   private FormLabel label(String text, String forId)
   {
      FormLabel label = new FormLabel(text, forId);
      label.getElement().getStyle().setMarginBottom(4, Unit.PX);
      return label;
   }
   
   private final FormLabel lblName_;
   private final FormLabel lblUrl_;
   private final TextBox tbName_;
   private final TextBox tbUrl_;
   private final VerticalPanel container_;
}
