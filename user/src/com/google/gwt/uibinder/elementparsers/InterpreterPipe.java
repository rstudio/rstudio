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
package com.google.gwt.uibinder.elementparsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.uibinder.rebind.XMLElement;
import com.google.gwt.uibinder.rebind.XMLElement.Interpreter;
import com.google.gwt.uibinder.rebind.XMLElement.PostProcessingInterpreter;

import java.util.ArrayList;
import java.util.List;

/**
 * Pairs {@link XMLElement.Interpreter} instances.
 *
 * @param <T> The type returned by all members of the pipe
 */
class InterpreterPipe<T> implements PostProcessingInterpreter<T> {
  public static <T>InterpreterPipe<T> newPipe(Interpreter<T>... pipes) {
    InterpreterPipe<T> rtn = new InterpreterPipe<T>();
    for (int i = 0; i < pipes.length; ++i) {
      rtn.add(pipes[i]);
    }
    return rtn;
  }

  private final List<Interpreter<T>> pipe =
      new ArrayList<Interpreter<T>>();

  public void add(Interpreter<T> i) {
    pipe.add(i);
  }

  /**
   * Interpreters are fired in the order they were handed to the constructor. If
   * an interpreter gives a non-null result, downstream interpreters don't
   * fire.
   *
   * @return The T or null returned by the last pipelined interpreter to run
   * @throws UnableToCompleteException on error
   */
  public T interpretElement(XMLElement elem) throws UnableToCompleteException {
    T rtn = null;
    for (XMLElement.Interpreter<T> i : pipe) {
      rtn = i.interpretElement(elem);
      if (null != rtn) {
        break;
      }
    }
    return rtn;
  }

  /**
   * Called by various {@link XMLElement} consumeInner*() methods after all
   * elements have been handed to {@link #interpretElement}. Passes the
   * text to be post processed to each pipe member that is instanceof
   * {@link PostProcessingInterpreter}.
   */
  public String postProcess(String consumedText) throws UnableToCompleteException {
    for (XMLElement.Interpreter<T> i : pipe) {
      if (i instanceof PostProcessingInterpreter<?>) {
        consumedText = ((PostProcessingInterpreter<T>) i).postProcess(consumedText);
      }
    }
    return consumedText;
  }
}
