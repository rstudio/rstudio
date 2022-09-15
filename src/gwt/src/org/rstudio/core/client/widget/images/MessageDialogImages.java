/*
 * MessageDialogImages.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget.images;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import org.rstudio.core.client.CoreClientConstants;

public interface MessageDialogImages extends ClientBundle
{
    MessageDialogImages INSTANCE = GWT.create(MessageDialogImages.class);
    static final CoreClientConstants constants_ = GWT.create(CoreClientConstants.class);

   @Source("dialog_info_2x.png")
   ImageResource dialog_info2x();
   String DIALOG_INFO_TEXT = constants_.dialogInfoText();

   @Source("dialog_error_2x.png")
   ImageResource dialog_error2x();
   String DIALOG_ERROR_TEXT = constants_.dialogErrorText();

   @Source("dialog_warning_2x.png")
   ImageResource dialog_warning2x();
   String DIALOG_WARNING_TEXT = constants_.dialogWarningText();

   @Source("dialog_question_2x.png")
   ImageResource dialog_question2x();
   String DIALOG_QUESTION_TEXT = constants_.dialogQuestionText();

   @Source("dialog_popup_blocked_2x.png")
   ImageResource dialog_popup_blocked2x();
   String DIALOG_POPUP_BLOCKED_TEXT = constants_.dialogPopupBlockedText();
}
