/*
 * AceEditorCoverageHighlighter.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.inject.Inject;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.coverage.CodeCoverage;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;

import com.google.inject.Inject;

public class AceEditorCoverageHighlighter {
    
    public AceEditorCoverageHighlighter(AceEditor editor)
    {
        RStudioGinjector.INSTANCE.injectMembers(this);

        editor_ = editor;

        mockup();
    }

    @Inject
    private void initialize(CodeToolsServerOperations server)
    {
        server_ = server;
    }


    void mockup() 
    {
        server_.getCoverageInformation("R/summarise.R", new SimpleRequestCallback<CodeCoverage>()
        {
            @Override
            public void onResponseReceived(CodeCoverage information)
            {
                JsArrayInteger values = information.getValue();
                JsArrayInteger lines = information.getLine();

                int n = values.length();

                for (int i = 0; i < n; i++) 
                {
                    highlight(lines.get(i), values.get(i) > 0 ? "#00ff0040" : "#ff000040");
                }
            }
            
        });
    }

    void highlight(int line, String color) {
        Range range = Range.fromPoints(Position.create(line, 1), Position.create(line, Integer.MAX_VALUE));
        editor_.getSession().addMarker(range, RES.styles().marker(), "fullLine", false, "background-color: " + color + " !important; ");
    }

    interface Resources extends ClientBundle
    {
        @Source("AceEditorCoverageHighlighter.css")
        Styles styles();
    }

    interface Styles extends CssResource
    {
        String marker();
    }

    public static Resources RES = GWT.create(Resources.class);
    static {
        RES.styles().ensureInjected();
    }

    CodeToolsServerOperations server_;
    private AceEditor editor_;
}
