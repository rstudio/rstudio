/*
 * StandardIcons.java
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
package org.rstudio.studio.client.common.icons;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface StandardIcons extends ClientBundle
{
   public static final StandardIcons INSTANCE = GWT.create(StandardIcons.class);
   ImageResource stock_new();

   @Source("chunk_menu_2x.png")
   ImageResource chunk_menu2x();

   ImageResource go_up();
   ImageResource right_arrow();
   ImageResource click_feedback();

   @Source("more_actions_2x.png")
   ImageResource more_actions2x();

   @Source("import_dataset_2x.png")
   ImageResource import_dataset2x();

   ImageResource empty_command();

   @Source("show_log_2x.png")
   ImageResource show_log2x();

   @Source("help_2x.png")
   ImageResource help2x();

   @Source("git_2x.png")
   ImageResource git2x();

   ImageResource svn();
   ImageResource viewer_window();

   @Source("run_2x.png")
   ImageResource run2x();

   @Source("mermaid_2x.png")
   ImageResource mermaid2x();

   ImageResource export_menu();
   ImageResource functionLetter();
   ImageResource methodLetter();
   ImageResource lambdaLetter();
   ImageResource outline();

   @Source("options_2x.png")
   ImageResource options2x();
}
