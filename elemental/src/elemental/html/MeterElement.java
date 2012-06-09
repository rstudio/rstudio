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
import elemental.dom.Element;
import elemental.dom.NodeList;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <p>The HTML <em>meter</em> element (<code>&lt;meter&gt;</code>) represents either a scalar value within a known range or a fractional value.</p>
<div class="note"><strong>Usage note: </strong>Unless the <strong>value</strong> attribute is between 0 and 1 (inclusive), the <strong>min</strong> attribute and <strong>max</strong> attribute should define the range so that the <strong>value</strong> attribute's value is within it.</div>
  */
public interface MeterElement extends Element {


  /**
    * The lower numeric bound of the high end of the measured range. This must be less than the maximum value (<strong>max</strong> attribute), and it also must be greater than the low value and minimum value (<strong>low</strong> attribute and <strong>min</strong> attribute, respectively), if any are specified. If unspecified, or if greater than the maximum value, the <strong>high</strong> value is equal to the maximum value.
    */
  double getHigh();

  void setHigh(double arg);

  NodeList getLabels();


  /**
    * The upper numeric bound of the low end of the measured range. This must be greater than the minimum value (<strong>min</strong> attribute), and it also must be less than the high value and maximum value (<strong>high</strong> attribute and <strong>max</strong> attribute, respectively), if any are specified. If unspecified, or if less than the minimum value, the <strong>low</strong> value is equal to the minimum value.
    */
  double getLow();

  void setLow(double arg);


  /**
    * The upper numeric bound of the measured range. This must be greater than the minimum value (<strong>min</strong> attribute), if specified. If unspecified, the maximum value is 1.
    */
  double getMax();

  void setMax(double arg);


  /**
    * The lower numeric bound of the measured range. This must be less than the maximum value (<strong>max</strong> attribute), if specified. If unspecified, the minimum value is 0.
    */
  double getMin();

  void setMin(double arg);


  /**
    * This attribute indicates the optimal numeric value. It must be within the range (as defined by the <strong>min</strong> attribute and <strong>max</strong> attribute). When used with the <strong>low</strong> attribute and <strong>high</strong> attribute, it gives an indication where along the range is considered preferable. For example, if it is between the <strong>min</strong> attribute and the <strong>low</strong> attribute, then the lower range is considered preferred.
    */
  double getOptimum();

  void setOptimum(double arg);


  /**
    * The current numeric value. This must be between the minimum and maximum values (<strong>min</strong> attribute and <strong>max</strong> attribute) if they are specified. If unspecified or malformed, the value is 0. If specified, but not within the range given by the <strong>min</strong> attribute and <strong>max</strong> attribute, the value is equal to the nearest end of the range.
    */
  double getValue();

  void setValue(double arg);
}
