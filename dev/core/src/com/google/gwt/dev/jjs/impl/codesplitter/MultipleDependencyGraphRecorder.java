/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev.jjs.impl.codesplitter;

import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.impl.ControlFlowAnalyzer;

import java.util.List;

/**
 * A dependency recorder that can record multiple dependency graphs. It has
 * methods for starting and finishing new dependency graphs.
 */
public interface MultipleDependencyGraphRecorder extends ControlFlowAnalyzer.DependencyRecorder {
  /**
   * A {@link MultipleDependencyGraphRecorder} that does nothing.
   */
  MultipleDependencyGraphRecorder NULL_RECORDER =
      new MultipleDependencyGraphRecorder() {
        @Override
        public void close() {
        }

        @Override
        public void endDependencyGraph() {
        }

        @Override
        public void methodIsLiveBecause(JMethod liveMethod, List<JMethod> dependencyChain) {
        }

        @Override
        public void open() {
        }

        @Override
        public void startDependencyGraph(String name, String extendz) {
        }
      };

  /**
   * Stop recording dependencies.
   */
  void close();

  /**
   * Stop recording the current dependency graph.
   */
  void endDependencyGraph();

  void open();

  /**
   * Start a new dependency graph. It can be an extension of a previously
   * recorded dependency graph, in which case the dependencies in the previous
   * graph will not be repeated.
   */
  void startDependencyGraph(String name, String extendz);
}
