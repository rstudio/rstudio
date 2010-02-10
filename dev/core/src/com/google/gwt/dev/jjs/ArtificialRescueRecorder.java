/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.dev.jjs;

import com.google.gwt.core.client.impl.ArtificialRescue;
import com.google.gwt.core.client.impl.ArtificialRescue.Rescue;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.HasEnclosingType;
import com.google.gwt.dev.jjs.ast.JAnnotation;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.impl.JsniRefLookup;
import com.google.gwt.dev.util.JsniRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Process ArtificialRescue annotations.
 */
public class ArtificialRescueRecorder {
  private class Recorder extends JVisitor {
    private JDeclaredType currentClass;

    @Override
    public void endVisit(JAnnotation x, Context ctx) {
      if (x.getType() == artificialRescueType) {
        ArtificialRescue annotation = JAnnotation.createAnnotation(
            ArtificialRescue.class, x);
        for (Rescue rescue : annotation.value()) {
          process(rescue);
        }
      }
    }

    @Override
    public void endVisit(JDeclaredType x, Context ctx) {
      assert currentClass == x;
    }

    @Override
    public boolean visit(JDeclaredType x, Context ctx) {
      currentClass = x;

      /*
       * We only care about annotations declared on the type itself, so we can
       * skip the traversal of fields and methods.
       */
      accept(x.getAnnotations());
      return false;
    }

    private void process(Rescue rescue) {
      assert rescue != null : "rescue";

      String typeName = rescue.className();
      JReferenceType classType = (JReferenceType) program.getTypeFromJsniRef(typeName);
      String[] fields = rescue.fields();
      boolean instantiable = rescue.instantiable();
      String[] methods = rescue.methods();

      assert classType != null : "classType " + typeName;
      assert fields != null : "fields";
      assert methods != null : "methods";

      if (instantiable) {
        currentClass.addArtificialRescue(classType);

        // Make sure that a class literal for the type has been allocated
        program.getLiteralClass(classType);
      }

      if (classType instanceof JDeclaredType) {
        List<String> toRescue = new ArrayList<String>();
        Collections.addAll(toRescue, fields);
        Collections.addAll(toRescue, methods);

        for (String name : toRescue) {
          JsniRef ref = JsniRef.parse("@" + classType.getName() + "::" + name);
          final String[] errors = {null};
          HasEnclosingType node = JsniRefLookup.findJsniRefTarget(ref, program,
              new JsniRefLookup.ErrorReporter() {
                public void reportError(String error) {
                  errors[0] = error;
                }
              });
          if (errors[0] != null) {
            // Should have been caught by ArtificialRescueChecker
            throw new InternalCompilerException(
                "Unable to artificially rescue " + name + ": " + errors[0]);
          }

          currentClass.addArtificialRescue((JNode) node);
          if (node instanceof JField) {
            JField field = (JField) node;
            if (!field.isFinal()) {
              field.setVolatile();
            }
          }
        }
      }
    }
  }

  public static void exec(JProgram program) {
    new ArtificialRescueRecorder(program).execImpl();
  }

  private final JDeclaredType artificialRescueType;
  private final JProgram program;

  private ArtificialRescueRecorder(JProgram program) {
    this.program = program;
    artificialRescueType = program.getFromTypeMap(ArtificialRescue.class.getName());
  }

  private void execImpl() {
    new Recorder().accept(program);
  }
}
