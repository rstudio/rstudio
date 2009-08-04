/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.js;

import com.google.gwt.core.ext.soyc.Range;
import com.google.gwt.dev.jjs.HasSourceInfo;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.js.ast.JsVisitable;
import com.google.gwt.dev.util.TextOutput;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A variation on the standard source generation visitor that records the
 * locations of SourceInfo objects in the output.
 */
public class JsReportGenerationVisitor extends JsSourceGenerationVisitorWithSizeBreakdown {
  private final Map<Range, SourceInfo> sourceInfoMap = new HashMap<Range, SourceInfo>();
  private final TextOutput out;

  public JsReportGenerationVisitor(TextOutput out, JavaToJavaScriptMap map) {
    super(out, map);
    this.out = out;
  }

  public Map<Range, SourceInfo> getSourceInfoMap() {
    return Collections.unmodifiableMap(sourceInfoMap);
  }

  @Override
  protected <T extends JsVisitable<T>> T doAccept(T node) {
    boolean addEntry = node instanceof HasSourceInfo;
    int start = addEntry ? out.getPosition() : 0;
    T toReturn = super.doAccept(node);
    if (addEntry) {
      SourceInfo info = ((HasSourceInfo) node).getSourceInfo();
      sourceInfoMap.put(new Range(start, out.getPosition()), info);
    }
    return toReturn;
  }

  @Override
  protected <T extends JsVisitable<T>> void doAcceptList(List<T> collection) {
    for (T t : collection) {
      doAccept(t);
    }
  }

  @Override
  protected <T extends JsVisitable<T>> void doAcceptWithInsertRemove(
      List<T> collection) {
    for (T t : collection) {
      doAccept(t);
    }
  }
}
