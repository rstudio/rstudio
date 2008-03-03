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

import com.google.gwt.dev.asm.signature.SignatureVisitor;

/**
 * A <code>SignatureVisitor</code> adapter for type mapping.
 * 
 * @author Eugene Kuleshov
 */
public class RemappingSignatureAdapter implements SignatureVisitor {
    private final SignatureVisitor v;
    private final Remapper remapper;
    private String className;
    
    public RemappingSignatureAdapter(SignatureVisitor v, Remapper remapper) {
        this.v = v;
        this.remapper = remapper;
    }

    public void visitClassType(String name) {
        className = name;
        v.visitClassType(remapper.mapType(name));
    }

    public void visitInnerClassType(String name) {
        className = className + '$' + name;
        String remappedName = remapper.mapType(className);
        v.visitInnerClassType(remappedName.substring(remappedName.lastIndexOf('$') + 1));
    }

    public void visitFormalTypeParameter(String name) {
        v.visitFormalTypeParameter(name);
    }

    public void visitTypeVariable(String name) {
        v.visitTypeVariable(name);
    }

    public SignatureVisitor visitArrayType() {
        v.visitArrayType();
        return this;
    }

    public void visitBaseType(char descriptor) {
        v.visitBaseType(descriptor);
    }

    public SignatureVisitor visitClassBound() {
        v.visitClassBound();
        return this;
    }

    public SignatureVisitor visitExceptionType() {
        v.visitExceptionType();
        return this;
    }

    public SignatureVisitor visitInterface() {
        v.visitInterface();
        return this;
    }

    public SignatureVisitor visitInterfaceBound() {
        v.visitInterfaceBound();
        return this;
    }

    public SignatureVisitor visitParameterType() {
        v.visitParameterType();
        return this;
    }

    public SignatureVisitor visitReturnType() {
        v.visitReturnType();
        return this;
    }

    public SignatureVisitor visitSuperclass() {
        v.visitSuperclass();
        return this;
    }

    public void visitTypeArgument() {
        v.visitTypeArgument();
    }

    public SignatureVisitor visitTypeArgument(char wildcard) {
        v.visitTypeArgument(wildcard);
        return this;
    }

    public void visitEnd() {
        v.visitEnd();
    }

}
