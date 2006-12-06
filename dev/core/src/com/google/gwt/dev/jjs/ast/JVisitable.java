// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

public interface JVisitable {
    void traverse(JVisitor visitor);
}
