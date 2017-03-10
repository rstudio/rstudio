/*
 * MessageDialogImages.java
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
package org.rstudio.core.client.widget.images;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface MessageDialogImages extends ClientBundle
{
   public static final MessageDialogImages INSTANCE =
                                       GWT.create(MessageDialogImages.class);
   @Source("dialog_info_2x.png")
   ImageResource dialog_info2x();

   @Source("dialog_error_2x.png")
   ImageResource dialog_error2x();

   @Source("dialog_warning_2x.png")
   ImageResource dialog_warning2x();

   @Source("dialog_question_2x.png")
   ImageResource dialog_question2x();

   @Source("dialog_popup_blocked_2x.png")
   ImageResource dialog_popup_blocked2x();
}
