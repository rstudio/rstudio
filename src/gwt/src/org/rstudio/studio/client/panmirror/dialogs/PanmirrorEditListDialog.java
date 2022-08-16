/*
 * PanmirrorEditListDialog.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


package org.rstudio.studio.client.panmirror.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.NumericTextBox;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.panmirror.PanmirrorConstants;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorListCapabilities;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorListIncremental;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorListProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorListType;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;


public class PanmirrorEditListDialog extends ModalDialog<PanmirrorListProps>
{
   public PanmirrorEditListDialog(PanmirrorListProps props,
                                  PanmirrorListCapabilities capabilities,
                                  OperationWithInput<PanmirrorListProps> operation)
   {
      super(constants_.listLabel(), Roles.getDialogRole(), operation, () -> {
         // cancel returns null
         operation.execute(null);
      });
   
      capabilities_ = capabilities;
      
      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      
      listType_.setChoices(new String[] {
         PanmirrorListType.Ordered,
         PanmirrorListType.Bullet
      });
      orderedOptionsPanel_.setVisible(props.type.equals(PanmirrorListType.Ordered));
      listType_.addChangeHandler((event) -> {
         orderedOptionsPanel_.setVisible(listType_.getValue().equals(PanmirrorListType.Ordered));
      });
      
      
      listIncremental_.setChoices(new String[] {
          constants_.defaultChoiceList(),
          constants_.incrementalChoiceList(),
          constants_.nonIncrementalChoiceList()
      }, new String[] {
         PanmirrorListIncremental.Default,
         PanmirrorListIncremental.Incremental,
         PanmirrorListIncremental.Nonincremental
      });
      labelIncremental_.setVisible(capabilities.incremental);
      listIncremental_.setVisible(capabilities.incremental);
      
      
      tight_.getElement().setId(ElementIds.VISUAL_MD_LIST_TIGHT);
      tight_.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
      tight_.getElement().getStyle().setMarginTop(6, Unit.PX);
      
      startingNumber_.setMin(1);
      
      startingNumber_.setVisible(capabilities.order);
      numberStyle_.setVisible(capabilities.fancy);
      numberDelimiter_.setVisible(capabilities.fancy);
      
      List<String> numberStyleChoices = new ArrayList<>();
      numberStyleChoices.add("DefaultStyle");
      numberStyleChoices.add(constants_.decimalChoice());
      numberStyleChoices.add("LowerRoman");
      numberStyleChoices.add("UpperRoman");
      numberStyleChoices.add("LowerAlpha");
      numberStyleChoices.add("UpperAlpha");
      if (capabilities.example) {
         numberStyleChoices.add(constants_.exampleChoice());
      }
      numberStyle_.setChoices(numberStyleChoices.toArray(new String[] {}));
      
      numberDelimiter_.setChoices(new String[] {
         "DefaultDelim",
         "Period",
         "OneParen",
         "TwoParens",
      });

      
      listType_.setValue(props.type);
      listIncremental_.setValue(props.incremental);
      tight_.setValue(props.tight);
      startingNumber_.setValue(props.order + "");
      numberStyle_.setValue(props.number_style);
      numberDelimiter_.setValue(props.number_delim);
      numberDelimiter_.setDescribedBy(
         ElementIds.getElementId(ElementIds.VISUAL_MD_LIST_NUMBER_DELIM_NOTE));
      
   }
   
   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }
   
   @Override
   protected PanmirrorListProps collectInput()
   {
      PanmirrorListProps result = new PanmirrorListProps();
      result.type = listType_.getValue();
      result.incremental = listIncremental_.getValue();
      result.tight = tight_.getValue();
      result.order = StringUtil.parseInt(startingNumber_.getValue(), 1);
      result.number_style = capabilities_.fancy ? numberStyle_.getValue() : "DefaultStyle";
      result.number_delim = capabilities_.fancy ? numberDelimiter_.getValue() : "DefaultDelim";
      
      return result;
   }
   
   @Override
   protected boolean validate(PanmirrorListProps result)
   {
      return true;
   }
   
   
   interface Binder extends UiBinder<Widget, PanmirrorEditListDialog> {}
   
   private final PanmirrorListCapabilities capabilities_;
   
   private Widget mainWidget_;

   private static final PanmirrorConstants constants_ = GWT.create(PanmirrorConstants.class);

   @UiField SelectWidget listType_;
   @UiField FormLabel labelIncremental_;
   @UiField SelectWidget listIncremental_;
   @UiField CheckBox tight_;
   @UiField VerticalPanel orderedOptionsPanel_;
   @UiField NumericTextBox startingNumber_;
   @UiField SelectWidget numberStyle_;
   @UiField SelectWidget numberDelimiter_;
}
