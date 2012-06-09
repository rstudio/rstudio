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
package elemental.html;
import elemental.util.Indexable;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * The bulk of the operations available at present with <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/canvas">&lt;canvas&gt;</a></code>
 are available through this interface, returned by a call to <code>getContext()</code> on the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/canvas">&lt;canvas&gt;</a></code>
 element, with "2d" as its argument.
  */
public interface CanvasRenderingContext2D extends CanvasRenderingContext {


  /**
    * Color or style to use inside shapes. Default <code>#000</code> (black).
    */
  Object getFillStyle();

  void setFillStyle(Object arg);


  /**
    * Default value <code>10px sans-serif</code>.
    */
  String getFont();

  void setFont(String arg);


  /**
    * Alpha value that is applied to shapes and images before they are composited onto the canvas. Default <code>1.0</code> (opaque).
    */
  float getGlobalAlpha();

  void setGlobalAlpha(float arg);


  /**
    * With <code>globalAplpha</code> applied this sets how shapes and images are drawn onto the existing bitmap. Possible values: <ul> <li><code>source-atop</code></li> <li><code>source-in</code></li> <li><code>source-out</code></li> <li><code>source-over</code> (default)</li> <li><code>destination-atop</code></li> <li><code>destination-in</code></li> <li><code>destination-out</code></li> <li><code>destination-over</code></li> <li><code>lighter</code></li> <li><code>xor</code></li> </ul>
    */
  String getGlobalCompositeOperation();

  void setGlobalCompositeOperation(String arg);


  /**
    * Type of endings on the end of lines. Possible values: <code>butt</code> (default), <code>round</code>, <code>square</code>
    */
  String getLineCap();

  void setLineCap(String arg);


  /**
    * Defines the type of corners where two lines meet. Possible values: <code>round</code>, <code>bevel</code>, <code>miter</code> (default)
    */
  String getLineJoin();

  void setLineJoin(String arg);


  /**
    * Width of lines. Default <code>1.0</code>
    */
  float getLineWidth();

  void setLineWidth(float arg);


  /**
    * Default <code>10</code>.
    */
  float getMiterLimit();

  void setMiterLimit(float arg);


  /**
    * Specifies the blurring effect. Default <code>0</code>
    */
  float getShadowBlur();

  void setShadowBlur(float arg);


  /**
    * Color of the shadow. Default fully-transparent black.
    */
  String getShadowColor();

  void setShadowColor(String arg);


  /**
    * Horizontal distance the shadow will be offset. Default 0.
    */
  float getShadowOffsetX();

  void setShadowOffsetX(float arg);


  /**
    * Vertical distance the shadow will be offset. Default 0.
    */
  float getShadowOffsetY();

  void setShadowOffsetY(float arg);


  /**
    * Color or style to use for the lines around shapes. Default <code>#000</code> (black).
    */
  Object getStrokeStyle();

  void setStrokeStyle(Object arg);


  /**
    * Possible values: <code>start</code> (default), <code>end</code>, <code>left</code>, <code>right</code> or <code>center</code>.
    */
  String getTextAlign();

  void setTextAlign(String arg);

  String getTextBaseline();

  void setTextBaseline(String arg);

  float getWebkitBackingStorePixelRatio();


  /**
    * Image smoothing mode; if disabled, images will not be smoothed if scaled. 
<span title="(Firefox 3.6 / Thunderbird 3.1 / Fennec 1.0)
">Requires Gecko 1.9.2</span>
    */
  boolean isWebkitImageSmoothingEnabled();

  void setWebkitImageSmoothingEnabled(boolean arg);


  /**
    * An array which specifies the lengths of alternating dashes and gaps.
    */
  Indexable getWebkitLineDash();

  void setWebkitLineDash(Indexable arg);


  /**
    * Specifies where to start a dasharray on a line.
    */
  float getWebkitLineDashOffset();

  void setWebkitLineDashOffset(float arg);


  /**
    * <p>Adds an arc to the path which it center is at <em>(x, y)</em> position with radius<em> r</em> starting at <em>startAngle</em> and ending at <em>endAngle</em> going in the given direction by <em>anticlockwise</em> (defaulting to clockwise).</p>

<div id="section_11"><span id="Parameters"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>x</code></dt> <dd>The x axis of the coordinate for the arc's center</dd> <dt><code>y</code></dt> <dd>The y axis of the coordinate for the arc's center.</dd> <dt><code>radius</code></dt> <dd>The arc's radius</dd> <dt><code>startAngle</code></dt> <dd>The starting point, measured from the x axis , from which it will be drawed expressed as radians.</dd> <dt><code>endAngle</code></dt> <dd>The end arc's angle to which it will be drawed expressed as radians.</dd> <dt><code>anticlockwise</code> 
<span title="(Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
">Optional from Gecko 2.0</span>
</dt> <dd>When <code>true</code> draws the arc anticlockwise, otherwise in a clockwise direction.</dd>
</dl>
</div>
    */
  void arc(float x, float y, float radius, float startAngle, float endAngle, boolean anticlockwise);


  /**
    * <p>Adds an arc with the given control points and radius, connected to the previous point by a straight line.</p>

<div id="section_13"><span id="Parameters_2"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>x1</code></dt> <dd></dd> <dt><code>y1</code></dt> <dd></dd> <dt><code>x2</code></dt> <dd></dd> <dt><code>y2</code></dt> <dd></dd> <dt><code>radius</code></dt> <dd>The arc's radius.</dd>
</dl>
</div>
    */
  void arcTo(float x1, float y1, float x2, float y2, float radius);

  void beginPath();

  void bezierCurveTo(float cp1x, float cp1y, float cp2x, float cp2y, float x, float y);


  /**
    * <p>Clears the rectangle defined by it starting point at <em>(x, y)</em> and has a <em>w</em> width and a <em>h</em> height.</p>

<div id="section_19"><span id="Parameters_5"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>x</code></dt> <dd>The x axis of the coordinate for the rectangle starting point.</dd> <dt><code>y</code></dt> <dd>The y axis of the coordinate for the rectangle starting point.</dd> <dt><code>width</code></dt> <dd>The rectangle's width.</dd> <dt><code>height</code></dt> <dd>The rectangle's height.</dd>
</dl>
</div>
    */
  void clearRect(float x, float y, float width, float height);

  void clearShadow();

  void clip();

  void closePath();

  ImageData createImageData(ImageData imagedata);

  ImageData createImageData(float sw, float sh);

  CanvasGradient createLinearGradient(float x0, float y0, float x1, float y1);


  /**
    * <div id="section_31"><span id="Parameters_10"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>image</code></dt> <dd>A DOM <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/element">element</a></code>
 to use as the source image for the pattern. This can be any element, although typically you'll use an <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/Image" class="new">Image</a></code>
 or <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/canvas">&lt;canvas&gt;</a></code>
.</dd> <dt><code>repetition</code></dt> <dd>?</dd>
</dl>
</div><div id="section_32"><span id="Return_value_3"></span><h6 class="editable">Return value</h6>
<p>A new DOM canvas pattern object for use in pattern-based operations.</p>
</div><div id="section_33"><span id="Exceptions_thrown"></span><h6 class="editable">Exceptions thrown</h6>
<dl> <dt><code>NS_ERROR_DOM_INVALID_STATE_ERR</code> 
<span title="(Firefox 10.0 / Thunderbird 10.0)
">Requires Gecko 10.0</span>
</dt> <dd>The specified <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/canvas">&lt;canvas&gt;</a></code>
 element for the <code>image</code> parameter is zero-sized (that is, one or both of its dimensions are 0 pixels).</dd>
</dl>
</div>
    */
  CanvasPattern createPattern(CanvasElement canvas, String repetitionType);


  /**
    * <div id="section_31"><span id="Parameters_10"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>image</code></dt> <dd>A DOM <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/element">element</a></code>
 to use as the source image for the pattern. This can be any element, although typically you'll use an <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/Image" class="new">Image</a></code>
 or <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/canvas">&lt;canvas&gt;</a></code>
.</dd> <dt><code>repetition</code></dt> <dd>?</dd>
</dl>
</div><div id="section_32"><span id="Return_value_3"></span><h6 class="editable">Return value</h6>
<p>A new DOM canvas pattern object for use in pattern-based operations.</p>
</div><div id="section_33"><span id="Exceptions_thrown"></span><h6 class="editable">Exceptions thrown</h6>
<dl> <dt><code>NS_ERROR_DOM_INVALID_STATE_ERR</code> 
<span title="(Firefox 10.0 / Thunderbird 10.0)
">Requires Gecko 10.0</span>
</dt> <dd>The specified <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/canvas">&lt;canvas&gt;</a></code>
 element for the <code>image</code> parameter is zero-sized (that is, one or both of its dimensions are 0 pixels).</dd>
</dl>
</div>
    */
  CanvasPattern createPattern(ImageElement image, String repetitionType);

  CanvasGradient createRadialGradient(float x0, float y0, float r0, float x1, float y1, float r1);


  /**
    * <p>Draws the specified image. This method is available in multiple formats, providing a great deal of flexibility in its use.</p>

<div id="section_41"><span id="Parameters_13"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>image</code></dt> <dd>An element to draw into the context; the specification permits any image element (that is, <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/img">&lt;img&gt;</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/canvas">&lt;canvas&gt;</a></code>
, and <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/video">&lt;video&gt;</a></code>
). Some browsers, including Firefox, let you use any arbitrary element.</dd> <dt><code>dx</code></dt> <dd>The X coordinate in the destination canvas at which to place the top-left corner of the source <code>image</code>.</dd> <dt><code>dy</code></dt> <dd>The Y coordinate in the destination canvas at which to place the top-left corner of the source <code>image</code>.</dd> <dt><code>dw</code></dt> <dd>The width to draw the <code>image</code> in the destination canvas. This allows scaling of the drawn image. If not specified, the image is not scaled in width when drawn.</dd> <dt><code>dh</code></dt> <dd>The height to draw the <code>image</code> in the destination canvas. This allows scaling of the drawn image. If not specified, the image is not scaled in height when drawn.</dd> <dt><code>sx</code></dt> <dd>The X coordinate of the top left corner of the sub-rectangle of the source image to draw into the destination context.</dd> <dt><code>sy</code></dt> <dd>The Y coordinate of the top left corner of the sub-rectangle of the source image to draw into the destination context.</dd> <dt><code>sw</code></dt> <dd>The width of the sub-rectangle of the source image to draw into the destination context. If not specified, the entire rectangle from the coordinates specified by <code>sx</code> and <code>sy</code> to the bottom-right corner of the image is used. If you specify a negative value, the image is flipped horizontally when drawn.</dd> <dt><code>sh</code></dt> <dd>The height of the sub-rectangle of the source image to draw into the destination context. If you specify a negative value, the image is flipped vertically when drawn.</dd>
</dl>
<p>The diagram below illustrates the meanings of the various parameters.</p>
<p><img alt="drawImage.png" class="internal default" src="https://developer.mozilla.org/@api/deki/files/5494/=drawImage.png"></p>
</div><div id="section_42"><span id="Exceptions_thrown_2"></span><h6 class="editable">Exceptions thrown</h6>
<dl> <dt><code>INDEX_SIZE_ERR</code></dt> <dd>If the canvas or source rectangle width or height is zero.</dd> <dt><code>INVALID_STATE_ERR</code></dt> <dd>The image has no image data.</dd> <dt><code>TYPE_MISMATCH_ERR</code></dt> <dd>The specified source element isn't supported. This is not thrown by Firefox, since any element may be used as a source image.</dd>
</dl>
</div><div id="section_43"><span id="Compatibility_notes"></span><h6 class="editable">Compatibility notes</h6>
<ul> <li>Prior to Gecko 7.0 (Firefox 7.0 / Thunderbird 7.0 / SeaMonkey 2.4)
, Firefox threw an exception if any of the coordinate values was non-finite or zero. As per the specification, this no longer happens.</li> <li>Support for flipping the image by using negative values for <code>sw</code> and <code>sh</code> was added in Gecko 5.0 (Firefox 5.0 / Thunderbird 5.0 / SeaMonkey 2.2)
.</li> <li>Gecko 9.0 (Firefox 9.0 / Thunderbird 9.0 / SeaMonkey 2.6)
 now correctly supports CORS for drawing images across domains without <a title="en/CORS_Enabled_Image#What_is_a_.22tainted.22_canvas.3F" rel="internal" href="https://developer.mozilla.org/en/CORS_Enabled_Image#What_is_a_.22tainted.22_canvas.3F">tainting the canvas</a>.</li>
</ul>
</div>
    */
  void drawImage(ImageElement image, float x, float y);


  /**
    * <p>Draws the specified image. This method is available in multiple formats, providing a great deal of flexibility in its use.</p>

<div id="section_41"><span id="Parameters_13"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>image</code></dt> <dd>An element to draw into the context; the specification permits any image element (that is, <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/img">&lt;img&gt;</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/canvas">&lt;canvas&gt;</a></code>
, and <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/video">&lt;video&gt;</a></code>
). Some browsers, including Firefox, let you use any arbitrary element.</dd> <dt><code>dx</code></dt> <dd>The X coordinate in the destination canvas at which to place the top-left corner of the source <code>image</code>.</dd> <dt><code>dy</code></dt> <dd>The Y coordinate in the destination canvas at which to place the top-left corner of the source <code>image</code>.</dd> <dt><code>dw</code></dt> <dd>The width to draw the <code>image</code> in the destination canvas. This allows scaling of the drawn image. If not specified, the image is not scaled in width when drawn.</dd> <dt><code>dh</code></dt> <dd>The height to draw the <code>image</code> in the destination canvas. This allows scaling of the drawn image. If not specified, the image is not scaled in height when drawn.</dd> <dt><code>sx</code></dt> <dd>The X coordinate of the top left corner of the sub-rectangle of the source image to draw into the destination context.</dd> <dt><code>sy</code></dt> <dd>The Y coordinate of the top left corner of the sub-rectangle of the source image to draw into the destination context.</dd> <dt><code>sw</code></dt> <dd>The width of the sub-rectangle of the source image to draw into the destination context. If not specified, the entire rectangle from the coordinates specified by <code>sx</code> and <code>sy</code> to the bottom-right corner of the image is used. If you specify a negative value, the image is flipped horizontally when drawn.</dd> <dt><code>sh</code></dt> <dd>The height of the sub-rectangle of the source image to draw into the destination context. If you specify a negative value, the image is flipped vertically when drawn.</dd>
</dl>
<p>The diagram below illustrates the meanings of the various parameters.</p>
<p><img alt="drawImage.png" class="internal default" src="https://developer.mozilla.org/@api/deki/files/5494/=drawImage.png"></p>
</div><div id="section_42"><span id="Exceptions_thrown_2"></span><h6 class="editable">Exceptions thrown</h6>
<dl> <dt><code>INDEX_SIZE_ERR</code></dt> <dd>If the canvas or source rectangle width or height is zero.</dd> <dt><code>INVALID_STATE_ERR</code></dt> <dd>The image has no image data.</dd> <dt><code>TYPE_MISMATCH_ERR</code></dt> <dd>The specified source element isn't supported. This is not thrown by Firefox, since any element may be used as a source image.</dd>
</dl>
</div><div id="section_43"><span id="Compatibility_notes"></span><h6 class="editable">Compatibility notes</h6>
<ul> <li>Prior to Gecko 7.0 (Firefox 7.0 / Thunderbird 7.0 / SeaMonkey 2.4)
, Firefox threw an exception if any of the coordinate values was non-finite or zero. As per the specification, this no longer happens.</li> <li>Support for flipping the image by using negative values for <code>sw</code> and <code>sh</code> was added in Gecko 5.0 (Firefox 5.0 / Thunderbird 5.0 / SeaMonkey 2.2)
.</li> <li>Gecko 9.0 (Firefox 9.0 / Thunderbird 9.0 / SeaMonkey 2.6)
 now correctly supports CORS for drawing images across domains without <a title="en/CORS_Enabled_Image#What_is_a_.22tainted.22_canvas.3F" rel="internal" href="https://developer.mozilla.org/en/CORS_Enabled_Image#What_is_a_.22tainted.22_canvas.3F">tainting the canvas</a>.</li>
</ul>
</div>
    */
  void drawImage(ImageElement image, float x, float y, float width, float height);


  /**
    * <p>Draws the specified image. This method is available in multiple formats, providing a great deal of flexibility in its use.</p>

<div id="section_41"><span id="Parameters_13"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>image</code></dt> <dd>An element to draw into the context; the specification permits any image element (that is, <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/img">&lt;img&gt;</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/canvas">&lt;canvas&gt;</a></code>
, and <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/video">&lt;video&gt;</a></code>
). Some browsers, including Firefox, let you use any arbitrary element.</dd> <dt><code>dx</code></dt> <dd>The X coordinate in the destination canvas at which to place the top-left corner of the source <code>image</code>.</dd> <dt><code>dy</code></dt> <dd>The Y coordinate in the destination canvas at which to place the top-left corner of the source <code>image</code>.</dd> <dt><code>dw</code></dt> <dd>The width to draw the <code>image</code> in the destination canvas. This allows scaling of the drawn image. If not specified, the image is not scaled in width when drawn.</dd> <dt><code>dh</code></dt> <dd>The height to draw the <code>image</code> in the destination canvas. This allows scaling of the drawn image. If not specified, the image is not scaled in height when drawn.</dd> <dt><code>sx</code></dt> <dd>The X coordinate of the top left corner of the sub-rectangle of the source image to draw into the destination context.</dd> <dt><code>sy</code></dt> <dd>The Y coordinate of the top left corner of the sub-rectangle of the source image to draw into the destination context.</dd> <dt><code>sw</code></dt> <dd>The width of the sub-rectangle of the source image to draw into the destination context. If not specified, the entire rectangle from the coordinates specified by <code>sx</code> and <code>sy</code> to the bottom-right corner of the image is used. If you specify a negative value, the image is flipped horizontally when drawn.</dd> <dt><code>sh</code></dt> <dd>The height of the sub-rectangle of the source image to draw into the destination context. If you specify a negative value, the image is flipped vertically when drawn.</dd>
</dl>
<p>The diagram below illustrates the meanings of the various parameters.</p>
<p><img alt="drawImage.png" class="internal default" src="https://developer.mozilla.org/@api/deki/files/5494/=drawImage.png"></p>
</div><div id="section_42"><span id="Exceptions_thrown_2"></span><h6 class="editable">Exceptions thrown</h6>
<dl> <dt><code>INDEX_SIZE_ERR</code></dt> <dd>If the canvas or source rectangle width or height is zero.</dd> <dt><code>INVALID_STATE_ERR</code></dt> <dd>The image has no image data.</dd> <dt><code>TYPE_MISMATCH_ERR</code></dt> <dd>The specified source element isn't supported. This is not thrown by Firefox, since any element may be used as a source image.</dd>
</dl>
</div><div id="section_43"><span id="Compatibility_notes"></span><h6 class="editable">Compatibility notes</h6>
<ul> <li>Prior to Gecko 7.0 (Firefox 7.0 / Thunderbird 7.0 / SeaMonkey 2.4)
, Firefox threw an exception if any of the coordinate values was non-finite or zero. As per the specification, this no longer happens.</li> <li>Support for flipping the image by using negative values for <code>sw</code> and <code>sh</code> was added in Gecko 5.0 (Firefox 5.0 / Thunderbird 5.0 / SeaMonkey 2.2)
.</li> <li>Gecko 9.0 (Firefox 9.0 / Thunderbird 9.0 / SeaMonkey 2.6)
 now correctly supports CORS for drawing images across domains without <a title="en/CORS_Enabled_Image#What_is_a_.22tainted.22_canvas.3F" rel="internal" href="https://developer.mozilla.org/en/CORS_Enabled_Image#What_is_a_.22tainted.22_canvas.3F">tainting the canvas</a>.</li>
</ul>
</div>
    */
  void drawImage(ImageElement image, float sx, float sy, float sw, float sh, float dx, float dy, float dw, float dh);


  /**
    * <p>Draws the specified image. This method is available in multiple formats, providing a great deal of flexibility in its use.</p>

<div id="section_41"><span id="Parameters_13"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>image</code></dt> <dd>An element to draw into the context; the specification permits any image element (that is, <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/img">&lt;img&gt;</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/canvas">&lt;canvas&gt;</a></code>
, and <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/video">&lt;video&gt;</a></code>
). Some browsers, including Firefox, let you use any arbitrary element.</dd> <dt><code>dx</code></dt> <dd>The X coordinate in the destination canvas at which to place the top-left corner of the source <code>image</code>.</dd> <dt><code>dy</code></dt> <dd>The Y coordinate in the destination canvas at which to place the top-left corner of the source <code>image</code>.</dd> <dt><code>dw</code></dt> <dd>The width to draw the <code>image</code> in the destination canvas. This allows scaling of the drawn image. If not specified, the image is not scaled in width when drawn.</dd> <dt><code>dh</code></dt> <dd>The height to draw the <code>image</code> in the destination canvas. This allows scaling of the drawn image. If not specified, the image is not scaled in height when drawn.</dd> <dt><code>sx</code></dt> <dd>The X coordinate of the top left corner of the sub-rectangle of the source image to draw into the destination context.</dd> <dt><code>sy</code></dt> <dd>The Y coordinate of the top left corner of the sub-rectangle of the source image to draw into the destination context.</dd> <dt><code>sw</code></dt> <dd>The width of the sub-rectangle of the source image to draw into the destination context. If not specified, the entire rectangle from the coordinates specified by <code>sx</code> and <code>sy</code> to the bottom-right corner of the image is used. If you specify a negative value, the image is flipped horizontally when drawn.</dd> <dt><code>sh</code></dt> <dd>The height of the sub-rectangle of the source image to draw into the destination context. If you specify a negative value, the image is flipped vertically when drawn.</dd>
</dl>
<p>The diagram below illustrates the meanings of the various parameters.</p>
<p><img alt="drawImage.png" class="internal default" src="https://developer.mozilla.org/@api/deki/files/5494/=drawImage.png"></p>
</div><div id="section_42"><span id="Exceptions_thrown_2"></span><h6 class="editable">Exceptions thrown</h6>
<dl> <dt><code>INDEX_SIZE_ERR</code></dt> <dd>If the canvas or source rectangle width or height is zero.</dd> <dt><code>INVALID_STATE_ERR</code></dt> <dd>The image has no image data.</dd> <dt><code>TYPE_MISMATCH_ERR</code></dt> <dd>The specified source element isn't supported. This is not thrown by Firefox, since any element may be used as a source image.</dd>
</dl>
</div><div id="section_43"><span id="Compatibility_notes"></span><h6 class="editable">Compatibility notes</h6>
<ul> <li>Prior to Gecko 7.0 (Firefox 7.0 / Thunderbird 7.0 / SeaMonkey 2.4)
, Firefox threw an exception if any of the coordinate values was non-finite or zero. As per the specification, this no longer happens.</li> <li>Support for flipping the image by using negative values for <code>sw</code> and <code>sh</code> was added in Gecko 5.0 (Firefox 5.0 / Thunderbird 5.0 / SeaMonkey 2.2)
.</li> <li>Gecko 9.0 (Firefox 9.0 / Thunderbird 9.0 / SeaMonkey 2.6)
 now correctly supports CORS for drawing images across domains without <a title="en/CORS_Enabled_Image#What_is_a_.22tainted.22_canvas.3F" rel="internal" href="https://developer.mozilla.org/en/CORS_Enabled_Image#What_is_a_.22tainted.22_canvas.3F">tainting the canvas</a>.</li>
</ul>
</div>
    */
  void drawImage(CanvasElement canvas, float x, float y);


  /**
    * <p>Draws the specified image. This method is available in multiple formats, providing a great deal of flexibility in its use.</p>

<div id="section_41"><span id="Parameters_13"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>image</code></dt> <dd>An element to draw into the context; the specification permits any image element (that is, <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/img">&lt;img&gt;</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/canvas">&lt;canvas&gt;</a></code>
, and <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/video">&lt;video&gt;</a></code>
). Some browsers, including Firefox, let you use any arbitrary element.</dd> <dt><code>dx</code></dt> <dd>The X coordinate in the destination canvas at which to place the top-left corner of the source <code>image</code>.</dd> <dt><code>dy</code></dt> <dd>The Y coordinate in the destination canvas at which to place the top-left corner of the source <code>image</code>.</dd> <dt><code>dw</code></dt> <dd>The width to draw the <code>image</code> in the destination canvas. This allows scaling of the drawn image. If not specified, the image is not scaled in width when drawn.</dd> <dt><code>dh</code></dt> <dd>The height to draw the <code>image</code> in the destination canvas. This allows scaling of the drawn image. If not specified, the image is not scaled in height when drawn.</dd> <dt><code>sx</code></dt> <dd>The X coordinate of the top left corner of the sub-rectangle of the source image to draw into the destination context.</dd> <dt><code>sy</code></dt> <dd>The Y coordinate of the top left corner of the sub-rectangle of the source image to draw into the destination context.</dd> <dt><code>sw</code></dt> <dd>The width of the sub-rectangle of the source image to draw into the destination context. If not specified, the entire rectangle from the coordinates specified by <code>sx</code> and <code>sy</code> to the bottom-right corner of the image is used. If you specify a negative value, the image is flipped horizontally when drawn.</dd> <dt><code>sh</code></dt> <dd>The height of the sub-rectangle of the source image to draw into the destination context. If you specify a negative value, the image is flipped vertically when drawn.</dd>
</dl>
<p>The diagram below illustrates the meanings of the various parameters.</p>
<p><img alt="drawImage.png" class="internal default" src="https://developer.mozilla.org/@api/deki/files/5494/=drawImage.png"></p>
</div><div id="section_42"><span id="Exceptions_thrown_2"></span><h6 class="editable">Exceptions thrown</h6>
<dl> <dt><code>INDEX_SIZE_ERR</code></dt> <dd>If the canvas or source rectangle width or height is zero.</dd> <dt><code>INVALID_STATE_ERR</code></dt> <dd>The image has no image data.</dd> <dt><code>TYPE_MISMATCH_ERR</code></dt> <dd>The specified source element isn't supported. This is not thrown by Firefox, since any element may be used as a source image.</dd>
</dl>
</div><div id="section_43"><span id="Compatibility_notes"></span><h6 class="editable">Compatibility notes</h6>
<ul> <li>Prior to Gecko 7.0 (Firefox 7.0 / Thunderbird 7.0 / SeaMonkey 2.4)
, Firefox threw an exception if any of the coordinate values was non-finite or zero. As per the specification, this no longer happens.</li> <li>Support for flipping the image by using negative values for <code>sw</code> and <code>sh</code> was added in Gecko 5.0 (Firefox 5.0 / Thunderbird 5.0 / SeaMonkey 2.2)
.</li> <li>Gecko 9.0 (Firefox 9.0 / Thunderbird 9.0 / SeaMonkey 2.6)
 now correctly supports CORS for drawing images across domains without <a title="en/CORS_Enabled_Image#What_is_a_.22tainted.22_canvas.3F" rel="internal" href="https://developer.mozilla.org/en/CORS_Enabled_Image#What_is_a_.22tainted.22_canvas.3F">tainting the canvas</a>.</li>
</ul>
</div>
    */
  void drawImage(CanvasElement canvas, float x, float y, float width, float height);


  /**
    * <p>Draws the specified image. This method is available in multiple formats, providing a great deal of flexibility in its use.</p>

<div id="section_41"><span id="Parameters_13"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>image</code></dt> <dd>An element to draw into the context; the specification permits any image element (that is, <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/img">&lt;img&gt;</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/canvas">&lt;canvas&gt;</a></code>
, and <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/video">&lt;video&gt;</a></code>
). Some browsers, including Firefox, let you use any arbitrary element.</dd> <dt><code>dx</code></dt> <dd>The X coordinate in the destination canvas at which to place the top-left corner of the source <code>image</code>.</dd> <dt><code>dy</code></dt> <dd>The Y coordinate in the destination canvas at which to place the top-left corner of the source <code>image</code>.</dd> <dt><code>dw</code></dt> <dd>The width to draw the <code>image</code> in the destination canvas. This allows scaling of the drawn image. If not specified, the image is not scaled in width when drawn.</dd> <dt><code>dh</code></dt> <dd>The height to draw the <code>image</code> in the destination canvas. This allows scaling of the drawn image. If not specified, the image is not scaled in height when drawn.</dd> <dt><code>sx</code></dt> <dd>The X coordinate of the top left corner of the sub-rectangle of the source image to draw into the destination context.</dd> <dt><code>sy</code></dt> <dd>The Y coordinate of the top left corner of the sub-rectangle of the source image to draw into the destination context.</dd> <dt><code>sw</code></dt> <dd>The width of the sub-rectangle of the source image to draw into the destination context. If not specified, the entire rectangle from the coordinates specified by <code>sx</code> and <code>sy</code> to the bottom-right corner of the image is used. If you specify a negative value, the image is flipped horizontally when drawn.</dd> <dt><code>sh</code></dt> <dd>The height of the sub-rectangle of the source image to draw into the destination context. If you specify a negative value, the image is flipped vertically when drawn.</dd>
</dl>
<p>The diagram below illustrates the meanings of the various parameters.</p>
<p><img alt="drawImage.png" class="internal default" src="https://developer.mozilla.org/@api/deki/files/5494/=drawImage.png"></p>
</div><div id="section_42"><span id="Exceptions_thrown_2"></span><h6 class="editable">Exceptions thrown</h6>
<dl> <dt><code>INDEX_SIZE_ERR</code></dt> <dd>If the canvas or source rectangle width or height is zero.</dd> <dt><code>INVALID_STATE_ERR</code></dt> <dd>The image has no image data.</dd> <dt><code>TYPE_MISMATCH_ERR</code></dt> <dd>The specified source element isn't supported. This is not thrown by Firefox, since any element may be used as a source image.</dd>
</dl>
</div><div id="section_43"><span id="Compatibility_notes"></span><h6 class="editable">Compatibility notes</h6>
<ul> <li>Prior to Gecko 7.0 (Firefox 7.0 / Thunderbird 7.0 / SeaMonkey 2.4)
, Firefox threw an exception if any of the coordinate values was non-finite or zero. As per the specification, this no longer happens.</li> <li>Support for flipping the image by using negative values for <code>sw</code> and <code>sh</code> was added in Gecko 5.0 (Firefox 5.0 / Thunderbird 5.0 / SeaMonkey 2.2)
.</li> <li>Gecko 9.0 (Firefox 9.0 / Thunderbird 9.0 / SeaMonkey 2.6)
 now correctly supports CORS for drawing images across domains without <a title="en/CORS_Enabled_Image#What_is_a_.22tainted.22_canvas.3F" rel="internal" href="https://developer.mozilla.org/en/CORS_Enabled_Image#What_is_a_.22tainted.22_canvas.3F">tainting the canvas</a>.</li>
</ul>
</div>
    */
  void drawImage(CanvasElement canvas, float sx, float sy, float sw, float sh, float dx, float dy, float dw, float dh);


  /**
    * <p>Draws the specified image. This method is available in multiple formats, providing a great deal of flexibility in its use.</p>

<div id="section_41"><span id="Parameters_13"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>image</code></dt> <dd>An element to draw into the context; the specification permits any image element (that is, <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/img">&lt;img&gt;</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/canvas">&lt;canvas&gt;</a></code>
, and <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/video">&lt;video&gt;</a></code>
). Some browsers, including Firefox, let you use any arbitrary element.</dd> <dt><code>dx</code></dt> <dd>The X coordinate in the destination canvas at which to place the top-left corner of the source <code>image</code>.</dd> <dt><code>dy</code></dt> <dd>The Y coordinate in the destination canvas at which to place the top-left corner of the source <code>image</code>.</dd> <dt><code>dw</code></dt> <dd>The width to draw the <code>image</code> in the destination canvas. This allows scaling of the drawn image. If not specified, the image is not scaled in width when drawn.</dd> <dt><code>dh</code></dt> <dd>The height to draw the <code>image</code> in the destination canvas. This allows scaling of the drawn image. If not specified, the image is not scaled in height when drawn.</dd> <dt><code>sx</code></dt> <dd>The X coordinate of the top left corner of the sub-rectangle of the source image to draw into the destination context.</dd> <dt><code>sy</code></dt> <dd>The Y coordinate of the top left corner of the sub-rectangle of the source image to draw into the destination context.</dd> <dt><code>sw</code></dt> <dd>The width of the sub-rectangle of the source image to draw into the destination context. If not specified, the entire rectangle from the coordinates specified by <code>sx</code> and <code>sy</code> to the bottom-right corner of the image is used. If you specify a negative value, the image is flipped horizontally when drawn.</dd> <dt><code>sh</code></dt> <dd>The height of the sub-rectangle of the source image to draw into the destination context. If you specify a negative value, the image is flipped vertically when drawn.</dd>
</dl>
<p>The diagram below illustrates the meanings of the various parameters.</p>
<p><img alt="drawImage.png" class="internal default" src="https://developer.mozilla.org/@api/deki/files/5494/=drawImage.png"></p>
</div><div id="section_42"><span id="Exceptions_thrown_2"></span><h6 class="editable">Exceptions thrown</h6>
<dl> <dt><code>INDEX_SIZE_ERR</code></dt> <dd>If the canvas or source rectangle width or height is zero.</dd> <dt><code>INVALID_STATE_ERR</code></dt> <dd>The image has no image data.</dd> <dt><code>TYPE_MISMATCH_ERR</code></dt> <dd>The specified source element isn't supported. This is not thrown by Firefox, since any element may be used as a source image.</dd>
</dl>
</div><div id="section_43"><span id="Compatibility_notes"></span><h6 class="editable">Compatibility notes</h6>
<ul> <li>Prior to Gecko 7.0 (Firefox 7.0 / Thunderbird 7.0 / SeaMonkey 2.4)
, Firefox threw an exception if any of the coordinate values was non-finite or zero. As per the specification, this no longer happens.</li> <li>Support for flipping the image by using negative values for <code>sw</code> and <code>sh</code> was added in Gecko 5.0 (Firefox 5.0 / Thunderbird 5.0 / SeaMonkey 2.2)
.</li> <li>Gecko 9.0 (Firefox 9.0 / Thunderbird 9.0 / SeaMonkey 2.6)
 now correctly supports CORS for drawing images across domains without <a title="en/CORS_Enabled_Image#What_is_a_.22tainted.22_canvas.3F" rel="internal" href="https://developer.mozilla.org/en/CORS_Enabled_Image#What_is_a_.22tainted.22_canvas.3F">tainting the canvas</a>.</li>
</ul>
</div>
    */
  void drawImage(VideoElement video, float x, float y);


  /**
    * <p>Draws the specified image. This method is available in multiple formats, providing a great deal of flexibility in its use.</p>

<div id="section_41"><span id="Parameters_13"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>image</code></dt> <dd>An element to draw into the context; the specification permits any image element (that is, <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/img">&lt;img&gt;</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/canvas">&lt;canvas&gt;</a></code>
, and <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/video">&lt;video&gt;</a></code>
). Some browsers, including Firefox, let you use any arbitrary element.</dd> <dt><code>dx</code></dt> <dd>The X coordinate in the destination canvas at which to place the top-left corner of the source <code>image</code>.</dd> <dt><code>dy</code></dt> <dd>The Y coordinate in the destination canvas at which to place the top-left corner of the source <code>image</code>.</dd> <dt><code>dw</code></dt> <dd>The width to draw the <code>image</code> in the destination canvas. This allows scaling of the drawn image. If not specified, the image is not scaled in width when drawn.</dd> <dt><code>dh</code></dt> <dd>The height to draw the <code>image</code> in the destination canvas. This allows scaling of the drawn image. If not specified, the image is not scaled in height when drawn.</dd> <dt><code>sx</code></dt> <dd>The X coordinate of the top left corner of the sub-rectangle of the source image to draw into the destination context.</dd> <dt><code>sy</code></dt> <dd>The Y coordinate of the top left corner of the sub-rectangle of the source image to draw into the destination context.</dd> <dt><code>sw</code></dt> <dd>The width of the sub-rectangle of the source image to draw into the destination context. If not specified, the entire rectangle from the coordinates specified by <code>sx</code> and <code>sy</code> to the bottom-right corner of the image is used. If you specify a negative value, the image is flipped horizontally when drawn.</dd> <dt><code>sh</code></dt> <dd>The height of the sub-rectangle of the source image to draw into the destination context. If you specify a negative value, the image is flipped vertically when drawn.</dd>
</dl>
<p>The diagram below illustrates the meanings of the various parameters.</p>
<p><img alt="drawImage.png" class="internal default" src="https://developer.mozilla.org/@api/deki/files/5494/=drawImage.png"></p>
</div><div id="section_42"><span id="Exceptions_thrown_2"></span><h6 class="editable">Exceptions thrown</h6>
<dl> <dt><code>INDEX_SIZE_ERR</code></dt> <dd>If the canvas or source rectangle width or height is zero.</dd> <dt><code>INVALID_STATE_ERR</code></dt> <dd>The image has no image data.</dd> <dt><code>TYPE_MISMATCH_ERR</code></dt> <dd>The specified source element isn't supported. This is not thrown by Firefox, since any element may be used as a source image.</dd>
</dl>
</div><div id="section_43"><span id="Compatibility_notes"></span><h6 class="editable">Compatibility notes</h6>
<ul> <li>Prior to Gecko 7.0 (Firefox 7.0 / Thunderbird 7.0 / SeaMonkey 2.4)
, Firefox threw an exception if any of the coordinate values was non-finite or zero. As per the specification, this no longer happens.</li> <li>Support for flipping the image by using negative values for <code>sw</code> and <code>sh</code> was added in Gecko 5.0 (Firefox 5.0 / Thunderbird 5.0 / SeaMonkey 2.2)
.</li> <li>Gecko 9.0 (Firefox 9.0 / Thunderbird 9.0 / SeaMonkey 2.6)
 now correctly supports CORS for drawing images across domains without <a title="en/CORS_Enabled_Image#What_is_a_.22tainted.22_canvas.3F" rel="internal" href="https://developer.mozilla.org/en/CORS_Enabled_Image#What_is_a_.22tainted.22_canvas.3F">tainting the canvas</a>.</li>
</ul>
</div>
    */
  void drawImage(VideoElement video, float x, float y, float width, float height);


  /**
    * <p>Draws the specified image. This method is available in multiple formats, providing a great deal of flexibility in its use.</p>

<div id="section_41"><span id="Parameters_13"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>image</code></dt> <dd>An element to draw into the context; the specification permits any image element (that is, <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/img">&lt;img&gt;</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/canvas">&lt;canvas&gt;</a></code>
, and <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/video">&lt;video&gt;</a></code>
). Some browsers, including Firefox, let you use any arbitrary element.</dd> <dt><code>dx</code></dt> <dd>The X coordinate in the destination canvas at which to place the top-left corner of the source <code>image</code>.</dd> <dt><code>dy</code></dt> <dd>The Y coordinate in the destination canvas at which to place the top-left corner of the source <code>image</code>.</dd> <dt><code>dw</code></dt> <dd>The width to draw the <code>image</code> in the destination canvas. This allows scaling of the drawn image. If not specified, the image is not scaled in width when drawn.</dd> <dt><code>dh</code></dt> <dd>The height to draw the <code>image</code> in the destination canvas. This allows scaling of the drawn image. If not specified, the image is not scaled in height when drawn.</dd> <dt><code>sx</code></dt> <dd>The X coordinate of the top left corner of the sub-rectangle of the source image to draw into the destination context.</dd> <dt><code>sy</code></dt> <dd>The Y coordinate of the top left corner of the sub-rectangle of the source image to draw into the destination context.</dd> <dt><code>sw</code></dt> <dd>The width of the sub-rectangle of the source image to draw into the destination context. If not specified, the entire rectangle from the coordinates specified by <code>sx</code> and <code>sy</code> to the bottom-right corner of the image is used. If you specify a negative value, the image is flipped horizontally when drawn.</dd> <dt><code>sh</code></dt> <dd>The height of the sub-rectangle of the source image to draw into the destination context. If you specify a negative value, the image is flipped vertically when drawn.</dd>
</dl>
<p>The diagram below illustrates the meanings of the various parameters.</p>
<p><img alt="drawImage.png" class="internal default" src="https://developer.mozilla.org/@api/deki/files/5494/=drawImage.png"></p>
</div><div id="section_42"><span id="Exceptions_thrown_2"></span><h6 class="editable">Exceptions thrown</h6>
<dl> <dt><code>INDEX_SIZE_ERR</code></dt> <dd>If the canvas or source rectangle width or height is zero.</dd> <dt><code>INVALID_STATE_ERR</code></dt> <dd>The image has no image data.</dd> <dt><code>TYPE_MISMATCH_ERR</code></dt> <dd>The specified source element isn't supported. This is not thrown by Firefox, since any element may be used as a source image.</dd>
</dl>
</div><div id="section_43"><span id="Compatibility_notes"></span><h6 class="editable">Compatibility notes</h6>
<ul> <li>Prior to Gecko 7.0 (Firefox 7.0 / Thunderbird 7.0 / SeaMonkey 2.4)
, Firefox threw an exception if any of the coordinate values was non-finite or zero. As per the specification, this no longer happens.</li> <li>Support for flipping the image by using negative values for <code>sw</code> and <code>sh</code> was added in Gecko 5.0 (Firefox 5.0 / Thunderbird 5.0 / SeaMonkey 2.2)
.</li> <li>Gecko 9.0 (Firefox 9.0 / Thunderbird 9.0 / SeaMonkey 2.6)
 now correctly supports CORS for drawing images across domains without <a title="en/CORS_Enabled_Image#What_is_a_.22tainted.22_canvas.3F" rel="internal" href="https://developer.mozilla.org/en/CORS_Enabled_Image#What_is_a_.22tainted.22_canvas.3F">tainting the canvas</a>.</li>
</ul>
</div>
    */
  void drawImage(VideoElement video, float sx, float sy, float sw, float sh, float dx, float dy, float dw, float dh);

  void drawImageFromRect(ImageElement image);

  void drawImageFromRect(ImageElement image, float sx);

  void drawImageFromRect(ImageElement image, float sx, float sy);

  void drawImageFromRect(ImageElement image, float sx, float sy, float sw);

  void drawImageFromRect(ImageElement image, float sx, float sy, float sw, float sh);

  void drawImageFromRect(ImageElement image, float sx, float sy, float sw, float sh, float dx);

  void drawImageFromRect(ImageElement image, float sx, float sy, float sw, float sh, float dx, float dy);

  void drawImageFromRect(ImageElement image, float sx, float sy, float sw, float sh, float dx, float dy, float dw);

  void drawImageFromRect(ImageElement image, float sx, float sy, float sw, float sh, float dx, float dy, float dw, float dh);

  void drawImageFromRect(ImageElement image, float sx, float sy, float sw, float sh, float dx, float dy, float dw, float dh, String compositeOperation);


  /**
    * Fills the subpaths with the current fill style.
    */
  void fill();


  /**
    * <p>Draws a filled rectangle at <em>(x, y) </em>position whose size is determined by <em>width</em> and <em>height</em>.</p>

<div id="section_49"><span id="Parameters_16"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>x</code></dt> <dd>The x axis of the coordinate for the rectangle starting point.</dd> <dt><code>y</code></dt> <dd>The y axis of the coordinate for the rectangle starting point.</dd> <dt><code>width</code></dt> <dd>The rectangle's width.</dd> <dt><code>height</code></dt> <dd>The rectangle's height.</dd>
</dl>
</div>
    */
  void fillRect(float x, float y, float width, float height);

  void fillText(String text, float x, float y);

  void fillText(String text, float x, float y, float maxWidth);


  /**
    * <p>Returns an <code><a class="external" rel="external" href="http://dev.w3.org/html5/2dcontext/Overview.html#imagedata" title="http://dev.w3.org/html5/2dcontext/Overview.html#imagedata" target="_blank">ImageData</a></code> object representing the underlying pixel data for the area of the canvas denoted by the rectangle which starts at <em>(sx, sy)</em> and has a <em>sw</em> width and <em>sh</em> height.</p>

<div id="section_53"><span id="Parameters_18"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>sx</code></dt> <dd>The x axis of the coordinate for the rectangle startpoint from which the ImageData will be extracted.</dd> <dt><code>sy</code></dt> <dd>The x axis of the coordinate for the rectangle endpoint from which the ImageData will be extracted.</dd> <dt><code>sw</code></dt> <dd>The width of the rectangle from which the ImageData will be extracted.</dd> <dt><code>sh</code></dt> <dd>The height of the rectangle from which the ImageData will be extracted.</dd>
</dl>
</div><div id="section_54"><span id="Return_value_6"></span><h6 class="editable">Return value</h6>
<p>Returns an <code><a class="external" rel="external" href="http://www.w3.org/TR/2011/WD-2dcontext-20110405/#imagedata" title="http://www.w3.org/TR/2011/WD-2dcontext-20110405/#imagedata" target="_blank">ImageData</a></code> object containing the image data for the given rectangle of the canvas.</p>
</div>
    */
  ImageData getImageData(float sx, float sy, float sw, float sh);


  /**
    * <p>Reports whether or not the specified point is contained in the current path.</p>

<div id="section_56"><span id="Parameters_19"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>x</code></dt> <dd>The X coordinate of the point to check.</dd> <dt><code>y</code></dt> <dd>The Y coordinate of the point to check.</dd>
</dl>
</div><div id="section_57"><span id="Return_value_7"></span><h6 class="editable">Return value</h6>
<p><code>true</code> if the specified point is contained in the current path; otherwise <code>false</code>.</p>
</div><div id="section_58"><span id="Compatibility_notes_2"></span><h6 class="editable">Compatibility notes</h6>
<ul> <li>Prior to Gecko 7.0 (Firefox 7.0 / Thunderbird 7.0 / SeaMonkey 2.4)
, this method incorrectly failed to multiply the specified point's coordinates by the current transformation matrix before comparing it to the path. Now this method works correctly even if the context is rotated, scaled, or otherwise transformed.</li>
</ul>
</div>
    */
  boolean isPointInPath(float x, float y);


  /**
    * <p>Connects the last point in the subpath to the <code>x, y</code> coordinates with a straight line.</p>

<div id="section_60"><span id="Parameters_20"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>x</code></dt> <dd>The x axis of the coordinate for the end of the line.</dd> <dt><code>y</code></dt> <dd>The y axis of the coordinate for the end of the line.</dd>
</dl>
</div>
    */
  void lineTo(float x, float y);

  TextMetrics measureText(String text);


  /**
    * <p>Moves the starting point of a new subpath to the <strong>(x, y)</strong> coordinates.</p>

<div id="section_65"><span id="Parameters_22"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>x</code></dt> <dd>The x axis of the point.</dd> <dt><code>y</code></dt> <dd>The y axis of the point.</dd>
</dl>
</div>
    */
  void moveTo(float x, float y);


  /**
    * <h6 class="editable">Compatibility notes</h6>
<ul> <li>Starting in Gecko 10.0 (Firefox 10.0 / Thunderbird 10.0)
, non-finite values to any of these parameters causes the call to putImageData() to be silently ignored, rather than throwing an exception.</li>
</ul>
    */
  void putImageData(ImageData imagedata, float dx, float dy);


  /**
    * <h6 class="editable">Compatibility notes</h6>
<ul> <li>Starting in Gecko 10.0 (Firefox 10.0 / Thunderbird 10.0)
, non-finite values to any of these parameters causes the call to putImageData() to be silently ignored, rather than throwing an exception.</li>
</ul>
    */
  void putImageData(ImageData imagedata, float dx, float dy, float dirtyX, float dirtyY, float dirtyWidth, float dirtyHeight);

  void quadraticCurveTo(float cpx, float cpy, float x, float y);

  void rect(float x, float y, float width, float height);


  /**
    * Restores the drawing style state to the last element on the 'state stack' saved by save()
    */
  void restore();

  void rotate(float angle);


  /**
    * Saves the current drawing style state using a stack so you can revert any change you make to it using restore().
    */
  void save();

  void scale(float sx, float sy);

  void setAlpha(float alpha);

  void setCompositeOperation(String compositeOperation);

  void setFillColor(String color);

  void setFillColor(String color, float alpha);

  void setFillColor(float grayLevel);

  void setFillColor(float grayLevel, float alpha);

  void setFillColor(float r, float g, float b, float a);

  void setFillColor(float c, float m, float y, float k, float a);

  void setShadow(float width, float height, float blur);

  void setShadow(float width, float height, float blur, String color);

  void setShadow(float width, float height, float blur, String color, float alpha);

  void setShadow(float width, float height, float blur, float grayLevel);

  void setShadow(float width, float height, float blur, float grayLevel, float alpha);

  void setShadow(float width, float height, float blur, float r, float g, float b, float a);

  void setShadow(float width, float height, float blur, float c, float m, float y, float k, float a);

  void setStrokeColor(String color);

  void setStrokeColor(String color, float alpha);

  void setStrokeColor(float grayLevel);

  void setStrokeColor(float grayLevel, float alpha);

  void setStrokeColor(float r, float g, float b, float a);

  void setStrokeColor(float c, float m, float y, float k, float a);

  void setTransform(float m11, float m12, float m21, float m22, float dx, float dy);


  /**
    * Strokes the subpaths with the current stroke style.
    */
  void stroke();


  /**
    * <p>Paints a rectangle which it starting point is at <em>(x, y)</em> and has a<em> w</em> width and a <em>h</em> height onto the canvas, using the current stroke style.</p>

<div id="section_88"><span id="Parameters_33"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>x</code></dt> <dd>The x axis for the starting point of the rectangle.</dd> <dt><code>y</code></dt> <dd>The y axis for the starting point of the rectangle.</dd> <dt><code>w</code></dt> <dd>The rectangle's width.</dd> <dt><code>h</code></dt> <dd>The rectangle's height.</dd>
</dl>
</div>
    */
  void strokeRect(float x, float y, float width, float height);


  /**
    * <p>Paints a rectangle which it starting point is at <em>(x, y)</em> and has a<em> w</em> width and a <em>h</em> height onto the canvas, using the current stroke style.</p>

<div id="section_88"><span id="Parameters_33"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>x</code></dt> <dd>The x axis for the starting point of the rectangle.</dd> <dt><code>y</code></dt> <dd>The y axis for the starting point of the rectangle.</dd> <dt><code>w</code></dt> <dd>The rectangle's width.</dd> <dt><code>h</code></dt> <dd>The rectangle's height.</dd>
</dl>
</div>
    */
  void strokeRect(float x, float y, float width, float height, float lineWidth);

  void strokeText(String text, float x, float y);

  void strokeText(String text, float x, float y, float maxWidth);

  void transform(float m11, float m12, float m21, float m22, float dx, float dy);


  /**
    * <p>Moves the origin point of the context to (x, y).</p>

<div id="section_94"><span id="Parameters_36"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>x</code></dt> <dd>The x axis for the point to be considered as the origin.</dd> <dt><code>y</code></dt> <dd>The x axis for the point to be considered as the origin.</dd>
</dl>
</div>
    */
  void translate(float tx, float ty);

  ImageData webkitGetImageDataHD(float sx, float sy, float sw, float sh);

  void webkitPutImageDataHD(ImageData imagedata, float dx, float dy);

  void webkitPutImageDataHD(ImageData imagedata, float dx, float dy, float dirtyX, float dirtyY, float dirtyWidth, float dirtyHeight);
}
