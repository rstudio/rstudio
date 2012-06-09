
package elemental.css;

import elemental.events.*;

/**
  * 
  */
public interface WebKitCSSMatrix {

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

  WebKitCSSMatrix inverse();

  WebKitCSSMatrix multiply(WebKitCSSMatrix secondMatrix);

  WebKitCSSMatrix rotate(double rotX, double rotY, double rotZ);

  WebKitCSSMatrix rotateAxisAngle(double x, double y, double z, double angle);

  WebKitCSSMatrix scale(double scaleX, double scaleY, double scaleZ);

  void setMatrixValue(String string);

  WebKitCSSMatrix skewX(double angle);

  WebKitCSSMatrix skewY(double angle);

  String toString();

  WebKitCSSMatrix translate(double x, double y, double z);
}
