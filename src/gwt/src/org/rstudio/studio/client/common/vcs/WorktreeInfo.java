/*
 * WorktreeInfo.java
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
package org.rstudio.studio.client.common.vcs;

import com.google.gwt.core.client.JavaScriptObject;

// One entry of 'git worktree list', plus the directory the project lives in
// within it (the worktree root, or the current project's subdirectory) and the
// .Rproj file found there (if any)
public class WorktreeInfo extends JavaScriptObject
{
   protected WorktreeInfo() {}

   public final native String getPath()        /*-{ return this["path"]; }-*/;
   public final native String getHead()        /*-{ return this["head"]; }-*/;
   public final native String getBranch()      /*-{ return this["branch"]; }-*/;
   public final native String getProjectDir()  /*-{ return this["project_dir"]; }-*/;
   public final native String getProjectFile() /*-{ return this["project_file"]; }-*/;
   public final native boolean isDetached()    /*-{ return this["detached"]; }-*/;
   public final native boolean isBare()        /*-{ return this["bare"]; }-*/;
   public final native boolean isLocked()      /*-{ return this["locked"]; }-*/;
   public final native boolean isPrunable()    /*-{ return this["prunable"]; }-*/;
   public final native boolean isMain()        /*-{ return this["is_main"]; }-*/;
   public final native boolean isCurrent()     /*-{ return this["is_current"]; }-*/;
}
