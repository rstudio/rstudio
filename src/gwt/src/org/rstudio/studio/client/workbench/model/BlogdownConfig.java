/*
 * BlogdownConfig.java
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
package org.rstudio.studio.client.workbench.model;

import org.rstudio.core.client.files.FileSystemItem;

import jsinterop.annotations.JsType;

@JsType
public class BlogdownConfig
{
   public boolean is_blogdown_project;
   public boolean is_hugo_project;
   public FileSystemItem site_dir;
   public String[] static_dirs;
   public String markdown_engine;
   public String markdown_extensions;
   public String rmd_extensions;
}
