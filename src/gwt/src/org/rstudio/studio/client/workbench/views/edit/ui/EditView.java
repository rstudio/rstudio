/*
 * EditView.java
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
package org.rstudio.studio.client.workbench.views.edit.ui;

import com.google.inject.Inject;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.reditor.model.REditorServerOperations;
import org.rstudio.studio.client.workbench.views.edit.Edit.Display;

public class EditView implements Display
{
   @Inject
   public EditView(EventBus events)
   {
      events_ = events;
   }

   public void show(String text,
                    REditorServerOperations server,
                    ProgressOperationWithInput<String> operation)
   {
      new EditDialog(text, operation, events_, server).showModal();
   }

   private final EventBus events_;
}
