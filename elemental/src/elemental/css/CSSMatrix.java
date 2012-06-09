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
package elemental.css;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * 
  */
public interface CSSMatrix {

  double getA();

  void setA(double arg);

  double getB();

  void setB(double arg);

  double getC();

  void setC(double arg);

  double getD();

  void setD(double arg);

  double getE();

  void setE(double arg);

  double getF();

  void setF(double arg);

  double getM11();

  void setM11(double arg);

  double getM12();

  void setM12(double arg);

  double getM13();

  void setM13(double arg);

  double getM14();

  void setM14(double arg);

  double getM21();

  void setM21(double arg);

  double getM22();

  void setM22(double arg);

  double getM23();

  void setM23(double arg);

  double getM24();

  void setM24(double arg);

  double getM31();

  void setM31(double arg);

  double getM32();

  void setM32(double arg);

  double getM33();

  void setM33(double arg);

  double getM34();

  void setM34(double arg);

  double getM41();

  void setM41(double arg);

  double getM42();

  void setM42(double arg);

  double getM43();

  void setM43(double arg);

  double getM44();

  void setM44(double arg);

  CSSMatrix inverse();

  CSSMatrix multiply(CSSMatrix secondMatrix);

  CSSMatrix rotate(double rotX, double rotY, double rotZ);

  CSSMatrix rotateAxisAngle(double x, double y, double z, double angle);

  CSSMatrix scale(double scaleX, double scaleY, double scaleZ);

  void setMatrixValue(String string);

  CSSMatrix skewX(double angle);

  CSSMatrix skewY(double angle);

  CSSMatrix translate(double x, double y, double z);
}
