/*
 * PackageManagerRepository.java
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
package org.rstudio.studio.client.workbench.views.packages.model;

import com.google.gwt.core.client.JavaScriptObject;

public class PackageManagerRepository extends JavaScriptObject
{
    public static final String TYPE_R      = "R";
    public static final String TYPE_PYTHON = "Python";

    protected PackageManagerRepository()
    {
    }

    public final native int getId()             /*-{ return this.id || -1; }-*/;
    public final native String getName()        /*-{ return this.name || ""; }-*/;
    public final native String getCreated()     /*-{ return this.created || ""; }-*/;
    public final native String getDescription() /*-{ return this.description || ""; }-*/;
    public final native String getType()        /*-{ return this.type || ""; }-*/;
    public final native boolean isHidden()      /*-{ return this.hidden || false; }-*/;

    public final native String setSnapshot(String snapshot) /*-{ this.snapshot = snapshot; }-*/;
    public final native String getSnapshot()                /*-{ return this.snapshot || ""; }-*/;
}
