/*
 * DiffLineWidget.java
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
package org.rstudio.studio.client.workbench.views.vcs.git;

import com.google.gwt.user.client.ui.HTML;

import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.PinnedLineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.DiffChunk;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.Line;

public class DiffLineWidget implements PinnedLineWidget.Host {

    public DiffLineWidget(AceEditor editor) 
    {
        editor_ = editor;
    } 

    public void show(int row, DiffChunk chunk)
    {
        if (lineWidget_ != null)
            lineWidget_.detach();

        String diffText = "";
        for (Line line: chunk.getLines()) {
            diffText += line.getType().getValue() + " " + line.getText() + "\n";
        }

        lineWidget_ = new PinnedLineWidget(
            "GitDiff", editor_, new HTML("<pre>" + diffText + "</pre>"), row - 1, null, this
        );
    }

    @Override
    public void onLineWidgetAdded(LineWidget widget) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onLineWidgetRemoved(LineWidget widget) {
        // TODO Auto-generated method stub
    }
    private AceEditor editor_;
    private PinnedLineWidget lineWidget_;

    
}
