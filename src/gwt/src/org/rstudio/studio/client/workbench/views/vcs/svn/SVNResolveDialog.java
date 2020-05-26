/*
 * SVNResolveDialog.java
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
package org.rstudio.studio.client.workbench.views.vcs.svn;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.ElementIds;
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
      super(caption, Roles.getDialogRole(), operation);
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

      ElementIds.assignElementId(groupLabel_, ElementIds.SVN_RESOLVE_GROUP);
      Roles.getGroupRole().set(layoutTable_);
      Roles.getGroupRole().setAriaLabelledbyProperty(layoutTable_, Id.of(groupLabel_));

      inputElements_ = new InputElement[] {
            radioWorking_,
            radioMineConflict_,
            radioTheirsConflict_,
            radioMineAll_,
            radioTheirsAll_,
      };

      spanTargetNoun_.setInnerText(fileCount_ == 1 ? "path" : "paths");

      ElementIds.assignElementId(radioWorking_, ElementIds.SVN_RESOLVE_MINE);
      ElementIds.assignElementId(descriptionWorking_, ElementIds.SVN_RESOLVE_MINE_DESC);
      Roles.getRadioRole().setAriaDescribedbyProperty(radioWorking_, Id.of(descriptionWorking_));

      ElementIds.assignElementId(radioMineConflict_, ElementIds.SVN_RESOLVE_MINE_CONFLICT);
      ElementIds.assignElementId(descriptionMineConflict_, ElementIds.SVN_RESOLVE_MINE_CONFLICT_DESC);
      Roles.getRadioRole().setAriaDescribedbyProperty(radioMineConflict_, Id.of(descriptionMineConflict_));

      ElementIds.assignElementId(radioTheirsConflict_, ElementIds.SVN_RESOLVE_THEIRS_CONFLICT);
      ElementIds.assignElementId(descriptionTheirsConflict_, ElementIds.SVN_RESOLVE_THEIRS_CONFLICT_DESC);
      Roles.getRadioRole().setAriaDescribedbyProperty(radioTheirsConflict_, Id.of(descriptionTheirsConflict_));

      ElementIds.assignElementId(radioMineAll_, ElementIds.SVN_RESOLVE_MINE_ALL);
      ElementIds.assignElementId(descriptionMineAll_, ElementIds.SVN_RESOLVE_MINE_ALL_DESC);
      Roles.getRadioRole().setAriaDescribedbyProperty(radioMineAll_, Id.of(descriptionMineAll_));

      ElementIds.assignElementId(radioTheirsAll_, ElementIds.SVN_RESOLVE_THEIRS_ALL);
      ElementIds.assignElementId(descriptionTheirsAll_, ElementIds.SVN_RESOLVE_THEIRS_ALL_DESC);
      Roles.getRadioRole().setAriaDescribedbyProperty(radioTheirsAll_, Id.of(descriptionTheirsAll_));

      labelWorking_.setAttribute("for", radioWorking_.getId());
      labelMineConflict_.setAttribute("for", radioMineConflict_.getId());
      labelTheirsConflict_.setAttribute("for", radioTheirsConflict_.getId());
      labelMineAll_.setAttribute("for", radioMineAll_.getId());
      labelTheirsAll_.setAttribute("for", radioTheirsAll_.getId());

      return widget;
   }

   @UiField DivElement groupLabel_;
   @UiField SpanElement spanTargetNoun_;
   @UiField TableElement layoutTable_;

   @UiField InputElement radioWorking_;
   @UiField LabelElement labelWorking_;
   @UiField TableCellElement descriptionWorking_;

   @UiField InputElement radioMineConflict_;
   @UiField LabelElement labelMineConflict_;
   @UiField TableCellElement descriptionMineConflict_;

   @UiField InputElement radioTheirsConflict_;
   @UiField LabelElement labelTheirsConflict_;
   @UiField TableCellElement descriptionTheirsConflict_;

   @UiField InputElement radioMineAll_;
   @UiField LabelElement labelMineAll_;
   @UiField TableCellElement descriptionMineAll_;

   @UiField InputElement radioTheirsAll_;
   @UiField LabelElement labelTheirsAll_;
   @UiField TableCellElement descriptionTheirsAll_;

   private final int fileCount_;
   private InputElement[] inputElements_;
}
