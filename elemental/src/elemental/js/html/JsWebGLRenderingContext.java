/*
 * Copyright 2012 Google Inc.
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
package elemental.js.html;
import elemental.js.util.JsIndexable;
import elemental.html.ArrayBuffer;
import elemental.html.Int32Array;
import elemental.html.WebGLActiveInfo;
import elemental.html.ImageElement;
import elemental.html.WebGLRenderbuffer;
import elemental.html.Float32Array;
import elemental.html.WebGLBuffer;
import elemental.html.CanvasElement;
import elemental.html.WebGLShader;
import elemental.html.ImageData;
import elemental.html.WebGLTexture;
import elemental.html.WebGLRenderingContext;
import elemental.html.WebGLContextAttributes;
import elemental.util.Indexable;
import elemental.html.CanvasRenderingContext;
import elemental.html.WebGLUniformLocation;
import elemental.html.WebGLShaderPrecisionFormat;
import elemental.html.WebGLProgram;
import elemental.html.ArrayBufferView;
import elemental.html.VideoElement;
import elemental.html.WebGLFramebuffer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.js.stylesheets.*;
import elemental.js.events.*;
import elemental.js.util.*;
import elemental.js.dom.*;
import elemental.js.html.*;
import elemental.js.css.*;
import elemental.js.stylesheets.*;

import java.util.Date;

public class JsWebGLRenderingContext extends JsCanvasRenderingContext  implements WebGLRenderingContext {
  protected JsWebGLRenderingContext() {}

  public final native int getDrawingBufferHeight() /*-{
    return this.drawingBufferHeight;
  }-*/;

  public final native int getDrawingBufferWidth() /*-{
    return this.drawingBufferWidth;
  }-*/;

  public final native void activeTexture(int texture) /*-{
    this.activeTexture(texture);
  }-*/;

  public final native void attachShader(WebGLProgram program, WebGLShader shader) /*-{
    this.attachShader(program, shader);
  }-*/;

  public final native void bindAttribLocation(WebGLProgram program, int index, String name) /*-{
    this.bindAttribLocation(program, index, name);
  }-*/;

  public final native void bindBuffer(int target, WebGLBuffer buffer) /*-{
    this.bindBuffer(target, buffer);
  }-*/;

  public final native void bindFramebuffer(int target, WebGLFramebuffer framebuffer) /*-{
    this.bindFramebuffer(target, framebuffer);
  }-*/;

  public final native void bindRenderbuffer(int target, WebGLRenderbuffer renderbuffer) /*-{
    this.bindRenderbuffer(target, renderbuffer);
  }-*/;

  public final native void bindTexture(int target, WebGLTexture texture) /*-{
    this.bindTexture(target, texture);
  }-*/;

  public final native void blendColor(float red, float green, float blue, float alpha) /*-{
    this.blendColor(red, green, blue, alpha);
  }-*/;

  public final native void blendEquation(int mode) /*-{
    this.blendEquation(mode);
  }-*/;

  public final native void blendEquationSeparate(int modeRGB, int modeAlpha) /*-{
    this.blendEquationSeparate(modeRGB, modeAlpha);
  }-*/;

  public final native void blendFunc(int sfactor, int dfactor) /*-{
    this.blendFunc(sfactor, dfactor);
  }-*/;

  public final native void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) /*-{
    this.blendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
  }-*/;

  public final native void bufferData(int target, ArrayBuffer data, int usage) /*-{
    this.bufferData(target, data, usage);
  }-*/;

  public final native void bufferData(int target, ArrayBufferView data, int usage) /*-{
    this.bufferData(target, data, usage);
  }-*/;

  public final native void bufferData(int target, double size, int usage) /*-{
    this.bufferData(target, size, usage);
  }-*/;

  public final native void bufferSubData(int target, double offset, ArrayBuffer data) /*-{
    this.bufferSubData(target, offset, data);
  }-*/;

  public final native void bufferSubData(int target, double offset, ArrayBufferView data) /*-{
    this.bufferSubData(target, offset, data);
  }-*/;

  public final native int checkFramebufferStatus(int target) /*-{
    return this.checkFramebufferStatus(target);
  }-*/;

  public final native void clear(int mask) /*-{
    this.clear(mask);
  }-*/;

  public final native void clearColor(float red, float green, float blue, float alpha) /*-{
    this.clearColor(red, green, blue, alpha);
  }-*/;

  public final native void clearDepth(float depth) /*-{
    this.clearDepth(depth);
  }-*/;

  public final native void clearStencil(int s) /*-{
    this.clearStencil(s);
  }-*/;

  public final native void colorMask(boolean red, boolean green, boolean blue, boolean alpha) /*-{
    this.colorMask(red, green, blue, alpha);
  }-*/;

  public final native void compileShader(WebGLShader shader) /*-{
    this.compileShader(shader);
  }-*/;

  public final native void compressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, ArrayBufferView data) /*-{
    this.compressedTexImage2D(target, level, internalformat, width, height, border, data);
  }-*/;

  public final native void compressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, ArrayBufferView data) /*-{
    this.compressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, data);
  }-*/;

  public final native void copyTexImage2D(int target, int level, int internalformat, int x, int y, int width, int height, int border) /*-{
    this.copyTexImage2D(target, level, internalformat, x, y, width, height, border);
  }-*/;

  public final native void copyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height) /*-{
    this.copyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
  }-*/;

  public final native JsWebGLBuffer createBuffer() /*-{
    return this.createBuffer();
  }-*/;

  public final native JsWebGLFramebuffer createFramebuffer() /*-{
    return this.createFramebuffer();
  }-*/;

  public final native JsWebGLProgram createProgram() /*-{
    return this.createProgram();
  }-*/;

  public final native JsWebGLRenderbuffer createRenderbuffer() /*-{
    return this.createRenderbuffer();
  }-*/;

  public final native JsWebGLShader createShader(int type) /*-{
    return this.createShader(type);
  }-*/;

  public final native JsWebGLTexture createTexture() /*-{
    return this.createTexture();
  }-*/;

  public final native void cullFace(int mode) /*-{
    this.cullFace(mode);
  }-*/;

  public final native void deleteBuffer(WebGLBuffer buffer) /*-{
    this.deleteBuffer(buffer);
  }-*/;

  public final native void deleteFramebuffer(WebGLFramebuffer framebuffer) /*-{
    this.deleteFramebuffer(framebuffer);
  }-*/;

  public final native void deleteProgram(WebGLProgram program) /*-{
    this.deleteProgram(program);
  }-*/;

  public final native void deleteRenderbuffer(WebGLRenderbuffer renderbuffer) /*-{
    this.deleteRenderbuffer(renderbuffer);
  }-*/;

  public final native void deleteShader(WebGLShader shader) /*-{
    this.deleteShader(shader);
  }-*/;

  public final native void deleteTexture(WebGLTexture texture) /*-{
    this.deleteTexture(texture);
  }-*/;

  public final native void depthFunc(int func) /*-{
    this.depthFunc(func);
  }-*/;

  public final native void depthMask(boolean flag) /*-{
    this.depthMask(flag);
  }-*/;

  public final native void depthRange(float zNear, float zFar) /*-{
    this.depthRange(zNear, zFar);
  }-*/;

  public final native void detachShader(WebGLProgram program, WebGLShader shader) /*-{
    this.detachShader(program, shader);
  }-*/;

  public final native void disable(int cap) /*-{
    this.disable(cap);
  }-*/;

  public final native void disableVertexAttribArray(int index) /*-{
    this.disableVertexAttribArray(index);
  }-*/;

  public final native void drawArrays(int mode, int first, int count) /*-{
    this.drawArrays(mode, first, count);
  }-*/;

  public final native void drawElements(int mode, int count, int type, double offset) /*-{
    this.drawElements(mode, count, type, offset);
  }-*/;

  public final native void enable(int cap) /*-{
    this.enable(cap);
  }-*/;

  public final native void enableVertexAttribArray(int index) /*-{
    this.enableVertexAttribArray(index);
  }-*/;

  public final native void finish() /*-{
    this.finish();
  }-*/;

  public final native void flush() /*-{
    this.flush();
  }-*/;

  public final native void framebufferRenderbuffer(int target, int attachment, int renderbuffertarget, WebGLRenderbuffer renderbuffer) /*-{
    this.framebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
  }-*/;

  public final native void framebufferTexture2D(int target, int attachment, int textarget, WebGLTexture texture, int level) /*-{
    this.framebufferTexture2D(target, attachment, textarget, texture, level);
  }-*/;

  public final native void frontFace(int mode) /*-{
    this.frontFace(mode);
  }-*/;

  public final native void generateMipmap(int target) /*-{
    this.generateMipmap(target);
  }-*/;

  public final native JsWebGLActiveInfo getActiveAttrib(WebGLProgram program, int index) /*-{
    return this.getActiveAttrib(program, index);
  }-*/;

  public final native JsWebGLActiveInfo getActiveUniform(WebGLProgram program, int index) /*-{
    return this.getActiveUniform(program, index);
  }-*/;

  public final native JsIndexable getAttachedShaders(WebGLProgram program) /*-{
    return this.getAttachedShaders(program);
  }-*/;

  public final native int getAttribLocation(WebGLProgram program, String name) /*-{
    return this.getAttribLocation(program, name);
  }-*/;

  public final native Object getBufferParameter(int target, int pname) /*-{
    return this.getBufferParameter(target, pname);
  }-*/;

  public final native JsWebGLContextAttributes getContextAttributes() /*-{
    return this.getContextAttributes();
  }-*/;

  public final native int getError() /*-{
    return this.getError();
  }-*/;

  public final native Object getExtension(String name) /*-{
    return this.getExtension(name);
  }-*/;

  public final native Object getFramebufferAttachmentParameter(int target, int attachment, int pname) /*-{
    return this.getFramebufferAttachmentParameter(target, attachment, pname);
  }-*/;

  public final native Object getParameter(int pname) /*-{
    return this.getParameter(pname);
  }-*/;

  public final native String getProgramInfoLog(WebGLProgram program) /*-{
    return this.getProgramInfoLog(program);
  }-*/;

  public final native Object getProgramParameter(WebGLProgram program, int pname) /*-{
    return this.getProgramParameter(program, pname);
  }-*/;

  public final native Object getRenderbufferParameter(int target, int pname) /*-{
    return this.getRenderbufferParameter(target, pname);
  }-*/;

  public final native String getShaderInfoLog(WebGLShader shader) /*-{
    return this.getShaderInfoLog(shader);
  }-*/;

  public final native Object getShaderParameter(WebGLShader shader, int pname) /*-{
    return this.getShaderParameter(shader, pname);
  }-*/;

  public final native JsWebGLShaderPrecisionFormat getShaderPrecisionFormat(int shadertype, int precisiontype) /*-{
    return this.getShaderPrecisionFormat(shadertype, precisiontype);
  }-*/;

  public final native String getShaderSource(WebGLShader shader) /*-{
    return this.getShaderSource(shader);
  }-*/;

  public final native Object getTexParameter(int target, int pname) /*-{
    return this.getTexParameter(target, pname);
  }-*/;

  public final native Object getUniform(WebGLProgram program, WebGLUniformLocation location) /*-{
    return this.getUniform(program, location);
  }-*/;

  public final native JsWebGLUniformLocation getUniformLocation(WebGLProgram program, String name) /*-{
    return this.getUniformLocation(program, name);
  }-*/;

  public final native Object getVertexAttrib(int index, int pname) /*-{
    return this.getVertexAttrib(index, pname);
  }-*/;

  public final native double getVertexAttribOffset(int index, int pname) /*-{
    return this.getVertexAttribOffset(index, pname);
  }-*/;

  public final native void hint(int target, int mode) /*-{
    this.hint(target, mode);
  }-*/;

  public final native boolean isBuffer(WebGLBuffer buffer) /*-{
    return this.isBuffer(buffer);
  }-*/;

  public final native boolean isContextLost() /*-{
    return this.isContextLost();
  }-*/;

  public final native boolean isEnabled(int cap) /*-{
    return this.isEnabled(cap);
  }-*/;

  public final native boolean isFramebuffer(WebGLFramebuffer framebuffer) /*-{
    return this.isFramebuffer(framebuffer);
  }-*/;

  public final native boolean isProgram(WebGLProgram program) /*-{
    return this.isProgram(program);
  }-*/;

  public final native boolean isRenderbuffer(WebGLRenderbuffer renderbuffer) /*-{
    return this.isRenderbuffer(renderbuffer);
  }-*/;

  public final native boolean isShader(WebGLShader shader) /*-{
    return this.isShader(shader);
  }-*/;

  public final native boolean isTexture(WebGLTexture texture) /*-{
    return this.isTexture(texture);
  }-*/;

  public final native void lineWidth(float width) /*-{
    this.lineWidth(width);
  }-*/;

  public final native void linkProgram(WebGLProgram program) /*-{
    this.linkProgram(program);
  }-*/;

  public final native void pixelStorei(int pname, int param) /*-{
    this.pixelStorei(pname, param);
  }-*/;

  public final native void polygonOffset(float factor, float units) /*-{
    this.polygonOffset(factor, units);
  }-*/;

  public final native void readPixels(int x, int y, int width, int height, int format, int type, ArrayBufferView pixels) /*-{
    this.readPixels(x, y, width, height, format, type, pixels);
  }-*/;

  public final native void releaseShaderCompiler() /*-{
    this.releaseShaderCompiler();
  }-*/;

  public final native void renderbufferStorage(int target, int internalformat, int width, int height) /*-{
    this.renderbufferStorage(target, internalformat, width, height);
  }-*/;

  public final native void sampleCoverage(float value, boolean invert) /*-{
    this.sampleCoverage(value, invert);
  }-*/;

  public final native void scissor(int x, int y, int width, int height) /*-{
    this.scissor(x, y, width, height);
  }-*/;

  public final native void shaderSource(WebGLShader shader, String string) /*-{
    this.shaderSource(shader, string);
  }-*/;

  public final native void stencilFunc(int func, int ref, int mask) /*-{
    this.stencilFunc(func, ref, mask);
  }-*/;

  public final native void stencilFuncSeparate(int face, int func, int ref, int mask) /*-{
    this.stencilFuncSeparate(face, func, ref, mask);
  }-*/;

  public final native void stencilMask(int mask) /*-{
    this.stencilMask(mask);
  }-*/;

  public final native void stencilMaskSeparate(int face, int mask) /*-{
    this.stencilMaskSeparate(face, mask);
  }-*/;

  public final native void stencilOp(int fail, int zfail, int zpass) /*-{
    this.stencilOp(fail, zfail, zpass);
  }-*/;

  public final native void stencilOpSeparate(int face, int fail, int zfail, int zpass) /*-{
    this.stencilOpSeparate(face, fail, zfail, zpass);
  }-*/;

  public final native void texImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, ArrayBufferView pixels) /*-{
    this.texImage2D(target, level, internalformat, width, height, border, format, type, pixels);
  }-*/;

  public final native void texImage2D(int target, int level, int internalformat, int format, int type, ImageData pixels) /*-{
    this.texImage2D(target, level, internalformat, format, type, pixels);
  }-*/;

  public final native void texImage2D(int target, int level, int internalformat, int format, int type, ImageElement image) /*-{
    this.texImage2D(target, level, internalformat, format, type, image);
  }-*/;

  public final native void texImage2D(int target, int level, int internalformat, int format, int type, CanvasElement canvas) /*-{
    this.texImage2D(target, level, internalformat, format, type, canvas);
  }-*/;

  public final native void texImage2D(int target, int level, int internalformat, int format, int type, VideoElement video) /*-{
    this.texImage2D(target, level, internalformat, format, type, video);
  }-*/;

  public final native void texParameterf(int target, int pname, float param) /*-{
    this.texParameterf(target, pname, param);
  }-*/;

  public final native void texParameteri(int target, int pname, int param) /*-{
    this.texParameteri(target, pname, param);
  }-*/;

  public final native void texSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, ArrayBufferView pixels) /*-{
    this.texSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
  }-*/;

  public final native void texSubImage2D(int target, int level, int xoffset, int yoffset, int format, int type, ImageData pixels) /*-{
    this.texSubImage2D(target, level, xoffset, yoffset, format, type, pixels);
  }-*/;

  public final native void texSubImage2D(int target, int level, int xoffset, int yoffset, int format, int type, ImageElement image) /*-{
    this.texSubImage2D(target, level, xoffset, yoffset, format, type, image);
  }-*/;

  public final native void texSubImage2D(int target, int level, int xoffset, int yoffset, int format, int type, CanvasElement canvas) /*-{
    this.texSubImage2D(target, level, xoffset, yoffset, format, type, canvas);
  }-*/;

  public final native void texSubImage2D(int target, int level, int xoffset, int yoffset, int format, int type, VideoElement video) /*-{
    this.texSubImage2D(target, level, xoffset, yoffset, format, type, video);
  }-*/;

  public final native void uniform1f(WebGLUniformLocation location, float x) /*-{
    this.uniform1f(location, x);
  }-*/;

  public final native void uniform1fv(WebGLUniformLocation location, Float32Array v) /*-{
    this.uniform1fv(location, v);
  }-*/;

  public final native void uniform1i(WebGLUniformLocation location, int x) /*-{
    this.uniform1i(location, x);
  }-*/;

  public final native void uniform1iv(WebGLUniformLocation location, Int32Array v) /*-{
    this.uniform1iv(location, v);
  }-*/;

  public final native void uniform2f(WebGLUniformLocation location, float x, float y) /*-{
    this.uniform2f(location, x, y);
  }-*/;

  public final native void uniform2fv(WebGLUniformLocation location, Float32Array v) /*-{
    this.uniform2fv(location, v);
  }-*/;

  public final native void uniform2i(WebGLUniformLocation location, int x, int y) /*-{
    this.uniform2i(location, x, y);
  }-*/;

  public final native void uniform2iv(WebGLUniformLocation location, Int32Array v) /*-{
    this.uniform2iv(location, v);
  }-*/;

  public final native void uniform3f(WebGLUniformLocation location, float x, float y, float z) /*-{
    this.uniform3f(location, x, y, z);
  }-*/;

  public final native void uniform3fv(WebGLUniformLocation location, Float32Array v) /*-{
    this.uniform3fv(location, v);
  }-*/;

  public final native void uniform3i(WebGLUniformLocation location, int x, int y, int z) /*-{
    this.uniform3i(location, x, y, z);
  }-*/;

  public final native void uniform3iv(WebGLUniformLocation location, Int32Array v) /*-{
    this.uniform3iv(location, v);
  }-*/;

  public final native void uniform4f(WebGLUniformLocation location, float x, float y, float z, float w) /*-{
    this.uniform4f(location, x, y, z, w);
  }-*/;

  public final native void uniform4fv(WebGLUniformLocation location, Float32Array v) /*-{
    this.uniform4fv(location, v);
  }-*/;

  public final native void uniform4i(WebGLUniformLocation location, int x, int y, int z, int w) /*-{
    this.uniform4i(location, x, y, z, w);
  }-*/;

  public final native void uniform4iv(WebGLUniformLocation location, Int32Array v) /*-{
    this.uniform4iv(location, v);
  }-*/;

  public final native void uniformMatrix2fv(WebGLUniformLocation location, boolean transpose, Float32Array array) /*-{
    this.uniformMatrix2fv(location, transpose, array);
  }-*/;

  public final native void uniformMatrix3fv(WebGLUniformLocation location, boolean transpose, Float32Array array) /*-{
    this.uniformMatrix3fv(location, transpose, array);
  }-*/;

  public final native void uniformMatrix4fv(WebGLUniformLocation location, boolean transpose, Float32Array array) /*-{
    this.uniformMatrix4fv(location, transpose, array);
  }-*/;

  public final native void useProgram(WebGLProgram program) /*-{
    this.useProgram(program);
  }-*/;

  public final native void validateProgram(WebGLProgram program) /*-{
    this.validateProgram(program);
  }-*/;

  public final native void vertexAttrib1f(int indx, float x) /*-{
    this.vertexAttrib1f(indx, x);
  }-*/;

  public final native void vertexAttrib1fv(int indx, Float32Array values) /*-{
    this.vertexAttrib1fv(indx, values);
  }-*/;

  public final native void vertexAttrib2f(int indx, float x, float y) /*-{
    this.vertexAttrib2f(indx, x, y);
  }-*/;

  public final native void vertexAttrib2fv(int indx, Float32Array values) /*-{
    this.vertexAttrib2fv(indx, values);
  }-*/;

  public final native void vertexAttrib3f(int indx, float x, float y, float z) /*-{
    this.vertexAttrib3f(indx, x, y, z);
  }-*/;

  public final native void vertexAttrib3fv(int indx, Float32Array values) /*-{
    this.vertexAttrib3fv(indx, values);
  }-*/;

  public final native void vertexAttrib4f(int indx, float x, float y, float z, float w) /*-{
    this.vertexAttrib4f(indx, x, y, z, w);
  }-*/;

  public final native void vertexAttrib4fv(int indx, Float32Array values) /*-{
    this.vertexAttrib4fv(indx, values);
  }-*/;

  public final native void vertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, double offset) /*-{
    this.vertexAttribPointer(indx, size, type, normalized, stride, offset);
  }-*/;

  public final native void viewport(int x, int y, int width, int height) /*-{
    this.viewport(x, y, width, height);
  }-*/;
}
