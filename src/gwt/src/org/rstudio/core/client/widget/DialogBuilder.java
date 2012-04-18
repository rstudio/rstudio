/*
 * DialogBuilder.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

public interface DialogBuilder
{
   DialogBuilder addButton(String label);
   DialogBuilder addButton(String label,
                           Operation operation);
   DialogBuilder addButton(String label,
                           ProgressOperation operation);

   DialogBuilder setDefaultButton(int index);

   void showModal();
}
