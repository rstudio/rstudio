/*
 * QuartoConfig.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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
package org.rstudio.studio.client.quarto.model;

import jsinterop.annotations.JsType;

@JsType
public class QuartoConfig
{
   public String user_installed;
   public boolean enabled;
   public String version;
   public boolean is_project;
   public String project_dir;
   public String project_type;
   public String project_output_dir;
   public String[] project_formats;
   public String project_editor;
}
