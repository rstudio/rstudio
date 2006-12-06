/*
 * Copyright 2006 Google Inc.
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
package java.lang;

/**
 * A fast way to create strings using multiple appends.
 */
public final class StringBuffer implements CharSequence {

  public StringBuffer() {
    assign();
  }

  /**
   * This implementation does not track capacity; using this constructor is
   * functionally equivalent to using the zero-argument constructor.
   */
  public StringBuffer(int length) {
    assign();
  }

  public StringBuffer(String s) {
    assign(s);
  }

  public StringBuffer append(boolean x) {
    return append(String.valueOf(x));
  }

  public StringBuffer append(char x) {
    return append(String.valueOf(x));
  }

  public StringBuffer append(char[] chs) {
    return append(chs, 0, chs.length);
  }

  public StringBuffer append(char[] chs, int offset, int len) {
    return append(String.valueOf(chs, offset, len));
  }

  public StringBuffer append(double x) {
    return append(String.valueOf(x));
  }

  public StringBuffer append(float x) {
    return append(String.valueOf(x));
  }

  public StringBuffer append(int x) {
    return append(String.valueOf(x));
  }

  public StringBuffer append(long x) {
    return append(String.valueOf(x));
  }

  public StringBuffer append(Object x) {
    return append(String.valueOf(x));
  }

  public native StringBuffer append(String toAppend) /*-{
    var last = this.js.length-1;
    var lastLength=this.js[last].length;
    if(this.length > lastLength*lastLength) {
      this.js[last]=this.js[last]+toAppend; 
    } else {
      this.js.push(toAppend);
    }
    this.length+=toAppend.length;
    return this;
  }-*/;

  public StringBuffer append(StringBuffer x) {
    return append(x == null ? "null" : x.toString());
  }

  public char charAt(int index) {
    return toString().charAt(index);
  }

  public StringBuffer delete(int start, int end) {
    return replace(start, end, "");
  }

  public StringBuffer deleteCharAt(int start) {
    return delete(start, start + 1);
  }

  public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
    String me = toString();
    while (srcBegin < srcEnd) {
      dst[dstBegin++] = me.charAt(srcBegin++);
    }
  }

  public int indexOf(String x) {
    return toString().indexOf(x);
  }

  public int indexOf(String x, int start) {
    return toString().indexOf(x, start);
  }

  public StringBuffer insert(int index, boolean x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuffer insert(int index, char x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuffer insert(int index, char[] x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuffer insert(int index, char[] x, int offset, int len) {
    return insert(index, String.valueOf(x, offset, len));
  }

  public StringBuffer insert(int index, double x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuffer insert(int index, float x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuffer insert(int index, int x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuffer insert(int index, long x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuffer insert(int index, Object x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuffer insert(int index, String x) {
    return replace(index, index, x);
  }

  public int lastIndexOf(String s) {
    return toString().lastIndexOf(s);
  }

  public int lastIndexOf(String s, int start) {
    return toString().lastIndexOf(s, start);
  }

  public native int length() /*-{
    return this.length;
  }-*/;

  public native StringBuffer replace(int start, int end, String toInsert) /*-{
    start = Math.max(Math.min(this.length,start),0);
    end = Math.max(Math.min(this.length,end),0);
    this.length = this.length - end + start + toInsert.length;
    var i=0;
    // Searching for the start
    var len = this.js[i].length;
    while(i < this.js.length && len < start) {
      start -= len;
      end -= len;
      len = this.js[++i].length;
    } 
    // adding a divide point so start falls between two chunks
    if(i < this.js.length && start > 0) {
      var extra = this.js[i].substring(start);
      this.js[i]=this.js[i].substring(0,start);
      this.js.splice(++i,0,extra);
      end-=start;
      start=0;
    }
  
    // eating deleted pieces
    var startOfDelete = i;
    var len = this.js[i].length;
    while(i < this.js.length && len < end) {
      end -= len;
      len = this.js[++i].length;
    }
    this.js.splice(startOfDelete,i-startOfDelete);

    // making sure that end falls between two chunks
    if(end > 0) {
      this.js[startOfDelete]=this.js[startOfDelete].substring(end);
    }

    this.js.splice(startOfDelete,0,toInsert);
    this.@java.lang.StringBuffer::maybeNormalize()();
    return this;
  }-*/;

  public void setCharAt(int index, char x) {
    replace(index, index + 1, String.valueOf(x));
  }

  public void setLength(int newLength) {
    int oldLength = length();
    if (newLength < oldLength) {
      delete(newLength, oldLength);
    } else {
      while (oldLength < newLength) {
        append((char) 0);
        ++oldLength;
      }
    }
  }

  public CharSequence subSequence(int start, int end) {
    return this.substring(start, end);
  }

  public String substring(int begin) {
    return toString().substring(begin);
  };

  public String substring(int begin, int end) {
    return toString().substring(begin, end);
  }

  public native String toString() /*-{
    this.@java.lang.StringBuffer::normalize()();
    return this.js[0];
  }-*/;

  native void maybeNormalize() /*-{
    if(this.js.length > 1 && (this.js.length * this.js.length * this.js.length) > this.length) {
      this.@java.lang.StringBuffer::normalize()();
    }
  }-*/;

  native void normalize() /*-{
    if(this.js.length > 1) {      
      this.js = [this.js.join("")];
      this.length = this.js[0].length;
    }
  }-*/;

  private void assign() {
    assign("");
  }

  private native void assign(String s) /*-{
    this.js = [s];
    this.length = s.length;
  }-*/;

}
