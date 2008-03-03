/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2007 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.google.gwt.dev.asm.commons;

import com.google.gwt.dev.asm.AnnotationVisitor;

/**
 * An <code>AnnotationVisitor</code> adapter for type remapping.
 * 
 * @author Eugene Kuleshov
 */
public class RemappingAnnotationAdapter implements AnnotationVisitor {
    
    private final AnnotationVisitor av;
    
    private final Remapper renamer;

    public RemappingAnnotationAdapter(AnnotationVisitor av, Remapper renamer) {
        this.av = av;
        this.renamer = renamer;
    }

    public void visit(String name, Object value) {
        av.visit(name, renamer.mapValue(value));
    }

    public void visitEnum(String name, String desc, String value) {
        av.visitEnum(name, renamer.mapType(desc), value);
    }

    public AnnotationVisitor visitAnnotation(String name, String desc) {
        AnnotationVisitor v = av.visitAnnotation(name, renamer.mapType(desc));
        return v == null ? null : (v == av
                ? this
                : new RemappingAnnotationAdapter(v, renamer));
    }

    public AnnotationVisitor visitArray(String name) {
        AnnotationVisitor v = av.visitArray(name);
        return v == null ? null : (v == av
                ? this
                : new RemappingAnnotationAdapter(v, renamer));
    }

    public void visitEnd() {
        av.visitEnd();
    }
}
