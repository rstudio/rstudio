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
package com.google.gwt.dev.shell.rewrite;

import com.google.gwt.dev.asm.ClassAdapter;
import com.google.gwt.dev.asm.ClassVisitor;
import com.google.gwt.dev.asm.Opcodes;

/**
 * Performs any rewriting necessary to ensure that class files are 1.5
 * compatible.
 */
class ForceClassVersion15 extends ClassAdapter {

  public ForceClassVersion15(ClassVisitor v) {
    super(v);
  }

  @Override
  public void visit(final int version, final int access, final String name,
      final String signature, final String superName, final String[] interfaces) {
    assert (version >= Opcodes.V1_5 && version <= Opcodes.V1_6);
    super.visit(Opcodes.V1_5, access, name, signature, superName, interfaces);
  }
}
