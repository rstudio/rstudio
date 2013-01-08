/*
 * SVNResolveDialog.java
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
package org.rstudio.studio.client.workbench.views.vcs.svn;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;

public class SVNResolveDialog extends ModalDialog<String>
{
   interface Binder extends UiBinder<HTMLPanel, SVNResolveDialog>
   {}

   public SVNResolveDialog(int fileCount,
                           String caption,
                           OperationWithInput<String> operation)
   {
      super(caption, operation);
      fileCount_ = fileCount;
   }

   @Override
   protected String collectInput()
   {
      for (InputElement el : inputElements_)
      {
         if (el.isChecked())
            return el.getValue();
      }

      return null;
   }

   @Override
   protected boolean validate(String input)
   {
      return input != null;
   }

   @Override
   protected Widget createMainWidget()
   {
      HTMLPanel widget = GWT.<Binder>create(Binder.class).createAndBindUi(this);

      inputElements_ = new InputElement[] {
            radioWorking_,
            radioMineConflict_,
            radioTheirsConflict_,
            radioMineAll_,
            radioTheirsAll_,
      };

      spanTargetNoun_.setInnerText(fileCount_ == 1 ? "path" : "paths");

      labelWorking_.setPropertyString("for", radioWorking_.getId());
      labelMineConflict_.setPropertyString("for", radioMineConflict_.getId());
      labelTheirsConflict_.setPropertyString("for", radioTheirsConflict_.getId());
      labelMineAll_.setPropertyString("for", radioMineAll_.getId());
      labelTheirsAll_.setPropertyString("for", radioTheirsAll_.getId());

      return widget;
   }

   @UiField
   InputElement radioWorking_;
   @UiField
   InputElement radioMineConflict_;
   @UiField
   InputElement radioTheirsConflict_;
   @UiField
   InputElement radioMineAll_;
   @UiField
   InputElement radioTheirsAll_;
   @UiField
   SpanElement spanTargetNoun_;
   @UiField
   LabelElement labelWorking_;
   @UiField
   LabelElement labelMineConflict_;
   @UiField
   LabelElement labelTheirsConflict_;
   @UiField
   LabelElement labelMineAll_;
   @UiField
   LabelElement labelTheirsAll_;
   private final int fileCount_;
   private InputElement[] inputElements_;
}
