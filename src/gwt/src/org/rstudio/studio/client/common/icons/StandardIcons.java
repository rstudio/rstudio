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

   @Source("stock_new_2x.png")
   ImageResource stock_new2x();

   @Source("chunk_menu_2x.png")
   ImageResource chunk_menu2x();

   @Source("go_up_2x.png")
   ImageResource go_up2x();

   @Source("right_arrow_2x.png")
   ImageResource right_arrow2x();

   @Source("click_feedback_2x.png")
   ImageResource click_feedback2x();

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

   @Source("svn_2x.png")
   ImageResource svn2x();

   @Source("viewer_window_2x.png")
   ImageResource viewer_window2x();

   @Source("run_2x.png")
   ImageResource run2x();

   @Source("mermaid_2x.png")
   ImageResource mermaid2x();

   @Source("export_menu_2x.png")
   ImageResource export_menu2x();

   @Source("functionLetter_2x.png")
   ImageResource functionLetter2x();

   @Source("methodLetter_2x.png")
   ImageResource methodLetter2x();

   @Source("lambdaLetter_2x.png")
   ImageResource lambdaLetter2x();

   @Source("outline_2x.png")
   ImageResource outline2x();

   @Source("options_2x.png")
   ImageResource options2x();
}
