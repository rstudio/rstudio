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
import com.google.gwt.dev.asm.Label;
import com.google.gwt.dev.asm.MethodVisitor;

/**
 * A <code>MethodAdapter</code> for type mapping.
 * 
 * @author Eugene Kuleshov
 */
public class RemappingMethodAdapter extends LocalVariablesSorter {

    protected final Remapper remapper;

    public RemappingMethodAdapter(
        int access,
        String desc,
        MethodVisitor mv,
        Remapper renamer)
    {
        super(access, desc, mv);
        this.remapper = renamer;
    }

    public void visitFieldInsn(
        int opcode,
        String owner,
        String name,
        String desc)
    {
        super.visitFieldInsn(opcode,
                remapper.mapType(owner),
                remapper.mapFieldName(owner, name, desc),
                remapper.mapDesc(desc));
    }

    public void visitMethodInsn(
        int opcode,
        String owner,
        String name,
        String desc)
    {
        super.visitMethodInsn(opcode,
                remapper.mapType(owner),
                remapper.mapMethodName(owner, name, desc),
                remapper.mapMethodDesc(desc));
    }

    public void visitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, remapper.mapType(type));
    }

    public void visitLdcInsn(Object cst) {
        super.visitLdcInsn(remapper.mapValue(cst));
    }

    public void visitMultiANewArrayInsn(String desc, int dims) {
        super.visitMultiANewArrayInsn(remapper.mapDesc(desc), dims);
    }

    public void visitTryCatchBlock(
        Label start,
        Label end,
        Label handler,
        String type)
    {
        super.visitTryCatchBlock(start, end, handler, // 
                type == null ? null : remapper.mapType(type));
    }
    
    public void visitLocalVariable(
        String name,
        String desc,
        String signature,
        Label start,
        Label end,
        int index)
    {
        super.visitLocalVariable(name,
                remapper.mapDesc(desc),
                remapper.mapSignature(signature, true),
                start,
                end,
                index);
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        AnnotationVisitor av = mv.visitAnnotation(desc, visible);
        return av == null ? av : new RemappingAnnotationAdapter(av, remapper);
    }
    
    public AnnotationVisitor visitAnnotationDefault() {
        AnnotationVisitor av = mv.visitAnnotationDefault();
        return av == null ? av : new RemappingAnnotationAdapter(av, remapper);
    }
    
    public AnnotationVisitor visitParameterAnnotation(
        int parameter,
        String desc,
        boolean visible)
    {
        AnnotationVisitor av = mv.visitParameterAnnotation(parameter,
                desc,
                visible);
        return av == null ? av : new RemappingAnnotationAdapter(av, remapper);
    }
    
    public void visitFrame(
        int type,
        int nLocal,
        Object[] local,
        int nStack,
        Object[] stack)
    {
        super.visitFrame(type, nLocal, remapEntries(nLocal, local), nStack, remapEntries(nStack, stack));
    }

    private Object[] remapEntries(int n, Object[] entries) {
        for (int i = 0; i < n; i++) {
            if (entries[i] instanceof String) {
                Object[] newEntries = new Object[n];
                if (i > 0) {
                    System.arraycopy(entries, 0, newEntries, 0, i);
                }
                do {
                    Object t = entries[i];
                    newEntries[i++] = t instanceof String
                            ? remapper.mapType((String) t)
                            : t;
                } while (i < n);
                return newEntries;
            }
        }
        return entries;
    }
    
}
