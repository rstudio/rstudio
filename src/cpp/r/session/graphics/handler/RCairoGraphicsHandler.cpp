/*
 * RCairoGraphicsHandler.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


// include pango headers first so we can allow them to have their
// own definition of TRUE and FALSE (R will subsequently need to redefine
// them as Rboolean enumerated values)
#include <pango/pango.h>
#include <pango/pangocairo.h>
#undef TRUE
#undef FALSE

#include <math.h>

#include "RGraphicsHandler.hpp"

using namespace core ;

namespace r {
namespace session {
namespace graphics {
namespace handler {
namespace cairo {

namespace {
   
// NOTE: Cairo implementation forked from devX11.c and cairoX11.c

struct CairoDeviceData
{
   CairoDeviceData()
      : fontscale(0.0), lwdscale(0.0), surface(NULL), context(NULL)
   {
   }

   double fontscale;
   double lwdscale;
   cairo_surface_t* surface;
   cairo_t* context;
};

CairoDeviceData* devDescToCDD(pDevDesc devDesc)
{
   DeviceContext* pDC = (DeviceContext*)devDesc->deviceSpecific;
   return (CairoDeviceData*) pDC->pDeviceSpecific;
}


// these from devX11.c
double RedGamma = 1.0;
double GreenGamma = 1.0;
double BlueGamma = 1.0;   
 
// this from Defn.h (used by Pango helper functions)
#define streql(s, t)	(!strcmp((s), (t)))

void setCairoColor(unsigned int col, cairo_t *cc)
{
   unsigned int alpha = R_ALPHA(col);
   double red, blue, green;
   
   red = R_RED(col)/255.0;
   green = R_GREEN(col)/255.0;
   blue = R_BLUE(col)/255.0;
   red = pow(red, RedGamma);
   green = pow(green, GreenGamma);
   blue = pow(blue, BlueGamma);
   
   /* These optimizations should not be necessary, but alpha = 1
    seems to cause image fallback in some backends */
   if (alpha == 255)
      cairo_set_source_rgb(cc, red, green, blue);
   else
      cairo_set_source_rgba(cc, red, green, blue, alpha/255.0);
}

void setCairoLineType(const pGEcontext gc, CairoDeviceData* pCDD)
{
   cairo_t *cc = pCDD->context;
   double lwd = gc->lwd;
   cairo_line_cap_t lcap = CAIRO_LINE_CAP_SQUARE;
   cairo_line_join_t ljoin = CAIRO_LINE_JOIN_ROUND;
   switch(gc->lend)
   {
      case GE_ROUND_CAP: lcap = CAIRO_LINE_CAP_ROUND; break;
      case GE_BUTT_CAP: lcap = CAIRO_LINE_CAP_BUTT; break;
      case GE_SQUARE_CAP: lcap = CAIRO_LINE_CAP_SQUARE; break;
   }
   switch(gc->ljoin)
   {
      case GE_ROUND_JOIN: ljoin = CAIRO_LINE_JOIN_ROUND; break;
      case GE_MITRE_JOIN: ljoin = CAIRO_LINE_JOIN_MITER; break;
      case GE_BEVEL_JOIN: ljoin = CAIRO_LINE_JOIN_BEVEL; break;
   }
   cairo_set_line_width(cc, (lwd > 0.01 ? lwd : 0.01) * pCDD->lwdscale);
   cairo_set_line_cap(cc, lcap);
   cairo_set_line_join(cc, ljoin);
   cairo_set_miter_limit(cc, gc->lmitre);
   
   if (gc->lty == 0 || gc->lty == -1)
      cairo_set_dash(cc, 0, 0, 0);
   else 
   {
      double ls[16], lwd = (gc->lwd > 1) ? gc->lwd : 1;
      int l, dt = gc->lty;
      for (l = 0; dt != 0; dt >>= 4, l++)
         ls[l] = (dt & 0xF) * lwd * pCDD->lwdscale;
      cairo_set_dash(cc, ls, l, 0);
   }
}
   
PangoFontDescription *PG_getFont(const pGEcontext gc, double fs)
{
   PangoFontDescription *fontdesc;
   gint face = gc->fontface;
   double size = gc->cex * gc->ps * fs;
   
   if (face < 1 || face > 5) face = 1;
   
   fontdesc = pango_font_description_new();
   if (face == 5)
   {
      pango_font_description_set_family(fontdesc, "symbol");
   }
   else 
   {
      const char *fm = gc->fontfamily;
      if(streql(fm, "mono")) fm = "courier";
      else if(streql(fm, "serif")) fm = "times";
      else if(streql(fm, "sans")) fm = "helvetica";
      pango_font_description_set_family(fontdesc, fm[0] ? fm : "helvetica");
      if(face == 2 || face == 4)
         pango_font_description_set_weight(fontdesc, PANGO_WEIGHT_BOLD);
      if(face == 3 || face == 4)
         pango_font_description_set_style(fontdesc, PANGO_STYLE_OBLIQUE);
   }
   gint scaledSize = PANGO_SCALE * (gint)size;
   pango_font_description_set_size(fontdesc, scaledSize);
   
   return fontdesc;
}
 
PangoLayout *PG_layout(PangoFontDescription *desc, cairo_t *cc, const char *str)
{
   // create the layout
   PangoLayout *layout = pango_cairo_create_layout(cc);
   
   pango_layout_set_font_description(layout, desc);
   pango_layout_set_text(layout, str, -1);
   return layout;
}


void PG_text_extents(cairo_t *cc, 
                     PangoLayout *layout,
                     gint *lbearing, 
                     gint *rbearing,
                     gint *width, 
                     gint *ascent, 
                     gint *descent, 
                     int ink)
{
   PangoRectangle rect, lrect;
   
   pango_layout_line_get_pixel_extents(pango_layout_get_line(layout, 0),
                                       &rect, 
                                       &lrect);
   
   if(width) 
      *width = lrect.width;
   
   if(ink) 
   {
      if(ascent) *ascent = PANGO_ASCENT(rect);
      if(descent) *descent = PANGO_DESCENT(rect);
      if(lbearing) *lbearing = PANGO_LBEARING(rect);
      if(rbearing) *rbearing = PANGO_RBEARING(rect);
   } 
   else 
   {
      if(ascent) *ascent = PANGO_ASCENT(lrect);
      if(descent) *descent = PANGO_DESCENT(lrect);
      if(lbearing) *lbearing = PANGO_LBEARING(lrect);
      if(rbearing) *rbearing = PANGO_RBEARING(lrect);
   }
}

bool completeInitialization(CairoDeviceData* pCDD)
{
   // check whether we succeeded
   cairo_status_t res = cairo_surface_status(pCDD->surface);
   if (res != CAIRO_STATUS_SUCCESS)
   {
      Rf_warning("cairo error '%s'", cairo_status_to_string(res));
      return false;
   }

   // initialize context
   pCDD->context = cairo_create(pCDD->surface);
   res = cairo_status(pCDD->context);
   if (res != CAIRO_STATUS_SUCCESS)
   {
      Rf_warning("cairo error '%s'", cairo_status_to_string(res));
      return false;
   }

   // initial drawing settings
   cairo_set_operator(pCDD->context, CAIRO_OPERATOR_OVER);
   cairo_reset_clip(pCDD->context);
   cairo_set_antialias(pCDD->context, CAIRO_ANTIALIAS_SUBPIXEL);

   // scales
   pCDD->fontscale = 1.0;

   // set lwdscale as per the behavior of devX11.c (not sure why the line
   // width is scaled but we do so here to follow suit)
   pCDD->lwdscale = 72.0 / 96.0;

   // success
   return true;
}

} // anonymous namespace


bool initializeWithFile(const FilePath& filePath,
                        int width,
                        int height,
                        bool displayListOn,
                        DeviceContext* pDC)
{
   // initialize file info
   pDC->targetPath = filePath;
   pDC->width = width;
   pDC->height = height;

   // initialize cairo context
   CairoDeviceData* pCDD = (CairoDeviceData*)pDC->pDeviceSpecific;
   pCDD->surface = NULL;
   pCDD->context = NULL;

   // create surface
   pCDD->surface = cairo_image_surface_create(CAIRO_FORMAT_ARGB32,
                                              width,
                                              height);

   // complete initialization
   return completeInitialization(pCDD);

}

DeviceContext* allocate(pDevDesc dev)
{
   DeviceContext* pDC = new DeviceContext(dev);
   CairoDeviceData* pCDD  = new CairoDeviceData();
   pDC->pDeviceSpecific = pCDD;
   return pDC;
}

void destroy(DeviceContext* pDC)
{
   // first cleanup cairo constructs
   CairoDeviceData* pCDD = (CairoDeviceData*)pDC->pDeviceSpecific;
   if (pCDD->surface != NULL)
   {
      ::cairo_surface_destroy(pCDD->surface);
      pCDD->surface = NULL;
   }
   if (pCDD->context != NULL)
   {
      ::cairo_destroy(pCDD->context);
      pCDD->context = NULL;
   }

   // now free memory
   delete pCDD;
   delete pDC;
}

void setSize(pDevDesc pDev)
{
   dev_desc::setSize(pDev);
}

void setDeviceAttributes(pDevDesc pDev)
{
   int resolution = 72;
   int pointSize = 12;

   pDev->cra[0] = 0.9 * pointSize * resolution/72.0;
   pDev->cra[1] = 1.2 * pointSize * resolution/72.0;
   pDev->startps = pointSize;
   pDev->ipr[0] = pDev->ipr[1] = 1.0/resolution;
   pDev->xCharOffset = 0.4900;
   pDev->yCharOffset = 0.3333;
   pDev->yLineBias = 0.1;

   pDev->canClip = TRUE;
   pDev->canHAdj = 2;
   pDev->canChangeGamma = FALSE;
   pDev->startcol = R_RGB(0, 0, 0);
   pDev->startfill = R_RGB(255, 255, 255);
   pDev->startlty = LTY_SOLID;
   pDev->startfont = 1;
   pDev->startgamma = 1;
   pDev->displayListOn = TRUE;

   // no support for events yet
   pDev->canGenMouseDown = FALSE;
   pDev->canGenMouseMove = FALSE;
   pDev->canGenMouseUp = FALSE;
   pDev->canGenKeybd = FALSE;
   pDev->gettingEvent = FALSE;
}

void onBeforeAddDevice(DeviceContext* pDC)
{
}

void onAfterAddDevice(DeviceContext* pDC)
{
}


bool resyncDisplayListBeforeWriteToPNG()
{
   return true;
}

Error writeToPNG(const FilePath& targetPath,
                 DeviceContext* pDC,
                 bool /* keepContextAlive */)
{
   CairoDeviceData* pCDD = (CairoDeviceData*)pDC->pDeviceSpecific;

   std::string pngFile = targetPath.absolutePath();

   cairo_status_t res = cairo_surface_write_to_png (pCDD->surface,
                                                    pngFile.c_str());
   if (res != CAIRO_STATUS_SUCCESS)
   {
      std::string err = std::string("Cairo error saving PNG: ") +
                        cairo_status_to_string(res);
      return systemError(boost::system::errc::io_error, err, ERROR_LOCATION);
   }
   else
   {
      return Success();
   }
}


void circle(double x,
            double y,
            double r,
            const pGEcontext gc,
            pDevDesc dev)
{
   CairoDeviceData* pCDD = devDescToCDD(dev);
   cairo_t* cc = pCDD->context;
   
   // tweak radius for legibility -- note that all of the other devices do
   // this in some measure: devQuartz doubles the radius, devWindows doubles
   // it with minimum of 2, etc. we add 1.0 because this yielded results
   // that were optically simillar to devQuartz when running demo(graphics)
   r += 1.0;
   
   cairo_new_path(cc);
   cairo_arc(cc, x, y, r, 0.0, 2 * M_PI); 
   
   if (R_ALPHA(gc->fill) > 0) 
   {
      setCairoColor(gc->fill, cc);
      cairo_fill_preserve(cc);
   }
   
   if (R_ALPHA(gc->col) > 0 && gc->lty != -1) 
   {
      setCairoColor(gc->col, cc);
      setCairoLineType(gc, pCDD);
      cairo_stroke(cc);
   }
}

void line(double x1,
          double y1,
          double x2,
          double y2,
          const pGEcontext gc,
          pDevDesc dev)
{
   CairoDeviceData* pCDD = devDescToCDD(dev);
   cairo_t* cc = pCDD->context;
   
   if (R_ALPHA(gc->col) > 0) 
   {
      setCairoColor(gc->col, cc);
      setCairoLineType(gc, pCDD);
      cairo_new_path(cc);
      cairo_move_to(cc, x1, y1);
      cairo_line_to(cc, x2, y2);
      cairo_stroke(cc);
   }
   
}

void polygon(int n,
             double *x,
             double *y,
             const pGEcontext gc,
             pDevDesc dev)
{
   CairoDeviceData* pCDD = devDescToCDD(dev);
   cairo_t* cc = pCDD->context;
   
   cairo_new_path(cc);
   cairo_move_to(cc, x[0], y[0]);
   for(int i = 0; i < n; i++) cairo_line_to(cc, x[i], y[i]);
   cairo_close_path(cc);
   
   if (R_ALPHA(gc->fill) > 0) 
   {
      setCairoColor(gc->fill, cc);
      cairo_fill_preserve(cc);
   }
   
   if (R_ALPHA(gc->col) > 0 && gc->lty != -1) 
   {
      setCairoColor(gc->col, cc);
      setCairoLineType(gc, pCDD);
      cairo_stroke(cc);
   }
}

void polyline(int n,
              double *x,
              double *y,
              const pGEcontext gc,
              pDevDesc dev)
{
   CairoDeviceData* pCDD = devDescToCDD(dev);
   cairo_t* cc = pCDD->context;
   
   if (R_ALPHA(gc->col) > 0) 
   {
      setCairoColor(gc->col, cc);
      setCairoLineType(gc, pCDD);
      cairo_new_path(cc);
      cairo_move_to(cc, x[0], y[0]);
      for(int i = 0; i < n; i++) cairo_line_to(cc, x[i], y[i]);
      cairo_stroke(cc);
   }
}

void rect(double x0,
          double y0,
          double x1,
          double y1,
          const pGEcontext gc,
          pDevDesc dev)
{
   CairoDeviceData* pCDD = devDescToCDD(dev);
   cairo_t* cc = pCDD->context;
   
   cairo_new_path(cc);
   cairo_rectangle(cc, x0, y0, x1 - x0, y1 - y0);
   
   if (R_ALPHA(gc->fill) > 0) 
   {
      setCairoColor(gc->fill, cc);
      cairo_fill_preserve(cc);
   }
   
   if (R_ALPHA(gc->col) > 0 && gc->lty != -1) 
   {
      setCairoColor(gc->col, cc);
      setCairoLineType(gc, pCDD);
      cairo_stroke(cc);
   }
}
   

void path(double *x,
          double *y,
          int npoly,
          int *nper,
          Rboolean winding,
          const pGEcontext gc,
          pDevDesc dev)
{
   int i, j, n;
   CairoDeviceData* pCDD = devDescToCDD(dev);
   cairo_t* cc = pCDD->context;

    cairo_new_path(cc);
    n = 0;
    for (i=0; i < npoly; i++) {
        cairo_move_to(cc, x[n], y[n]);
        n++;
        for(j=1; j < nper[i]; j++) {
            cairo_line_to(cc, x[n], y[n]);
            n++;
        }
        cairo_close_path(cc);
    }

    if (R_ALPHA(gc->fill) > 0) {
      if (winding)
        cairo_set_fill_rule(cc, CAIRO_FILL_RULE_WINDING);
      else
        cairo_set_fill_rule(cc, CAIRO_FILL_RULE_EVEN_ODD);

      setCairoColor(gc->fill, cc);
      cairo_fill_preserve(cc);
    }
    if (R_ALPHA(gc->col) > 0 && gc->lty != -1) {
      setCairoColor(gc->col, cc);
      setCairoLineType(gc, pCDD);
      cairo_stroke(cc);
    }
}

void raster(unsigned int *raster,
            int w,
            int h,
            double x,
            double y,
            double width,
            double height,
            double rot,
            Rboolean interpolate,
            const pGEcontext gc,
            pDevDesc dev)
{
    const void *vmax = vmaxget();
    int i;
    cairo_surface_t *image;
    unsigned char *imageData;
    CairoDeviceData* pCDD = devDescToCDD(dev);
    cairo_t* cc = pCDD->context;

    imageData = (unsigned char *) R_alloc(4*w*h, sizeof(unsigned char));
    /* The R ABGR needs to be converted to a Cairo ARGB
     * AND values need to by premultiplied by alpha
     */
    for (i=0; i<w*h; i++) {
        int alpha = R_ALPHA(raster[i]);
        imageData[i*4 + 3] = alpha;
        if (alpha < 255) {
            imageData[i*4 + 2] = R_RED(raster[i]) * alpha / 255;
            imageData[i*4 + 1] = R_GREEN(raster[i]) * alpha / 255;
            imageData[i*4 + 0] = R_BLUE(raster[i]) * alpha / 255;
        } else {
            imageData[i*4 + 2] = R_RED(raster[i]);
            imageData[i*4 + 1] = R_GREEN(raster[i]);
            imageData[i*4 + 0] = R_BLUE(raster[i]);
        }
    }
    image = cairo_image_surface_create_for_data(imageData,
                                                CAIRO_FORMAT_ARGB32,
                                                w, h,
                                                4*w);

    cairo_save(cc);

    cairo_translate(cc, x, y);
    cairo_rotate(cc, -rot*M_PI/180);
    cairo_scale(cc, width/w, height/h);
    /* Flip vertical first */
    cairo_translate(cc, 0, h/2.0);
    cairo_scale(cc, 1, -1);
    cairo_translate(cc, 0, -h/2.0);

    cairo_set_source_surface(cc, image, 0, 0);

    if (interpolate) {
        cairo_pattern_set_filter(cairo_get_source(cc),
                                 CAIRO_FILTER_BILINEAR);
    } else {
        cairo_pattern_set_filter(cairo_get_source(cc),
                                 CAIRO_FILTER_NEAREST);
    }

    cairo_paint(cc);

    cairo_restore(cc);
    cairo_surface_destroy(image);

    vmaxset(vmax);
}

SEXP cap(pDevDesc dd)
{
   return R_NilValue;
}


void metricInfo(int c,
                const pGEcontext gc,
                double* ascent,
                double* descent,
                double* width,
                pDevDesc dev)
{
   CairoDeviceData* pCDD = devDescToCDD(dev);
   cairo_t* cc = pCDD->context;

   char str[16];
   int Unicode = mbcslocale;
   PangoFontDescription *desc = PG_getFont(gc, pCDD->fontscale);
   PangoLayout *layout;
   gint iascent, idescent, iwidth;
   
   if(c == 0) c = 77;
   if(c < 0) {c = -c; Unicode = 1;}
   
   if(Unicode) {
      Rf_ucstoutf8(str, (unsigned int) c);
   } else {
      /* Here we assume that c < 256 */
      str[0] = c; str[1] = 0;
   }
   layout = PG_layout(desc, cc, str);
   PG_text_extents(cc, layout, NULL, NULL, &iwidth,
                   &iascent, &idescent, 1);
   g_object_unref(layout);
   pango_font_description_free(desc);
   *ascent = iascent;
   *descent = idescent;
   *width = iwidth;
#if 0
   printf("c = %d, '%s', face %d %f %f %f\n",
          c, str, gc->fontface, *width, *ascent, *descent);
#endif
}

double strWidth(const char *str, const pGEcontext gc, pDevDesc dev)
{
   CairoDeviceData* pCDD = devDescToCDD(dev);
   cairo_t* cc = pCDD->context;

   gint width;
   PangoFontDescription *desc = PG_getFont(gc, pCDD->fontscale);
   PangoLayout *layout = PG_layout(desc, cc, str);
   
   PG_text_extents(cc, layout, NULL, NULL, &width, NULL, NULL, 0);
   g_object_unref(layout);
   pango_font_description_free(desc);
   return (double) width;
}
   
void text(double x,
          double y,
          const char *str,
          double rot,
          double hadj,
          const pGEcontext gc,
          pDevDesc dev)
{
   CairoDeviceData* pCDD = devDescToCDD(dev);
   cairo_t* cc = pCDD->context;

   gint ascent, lbearing, width;
   PangoLayout *layout;
   
   if (R_ALPHA(gc->col) > 0) 
   {
      PangoFontDescription *desc = PG_getFont(gc, pCDD->fontscale);
      cairo_save(cc);
      layout = PG_layout(desc, cc, str);
      PG_text_extents(cc, layout, &lbearing, NULL, &width,
                      &ascent, NULL, 0);
      cairo_move_to(cc, x, y);
      if (rot != 0.0) cairo_rotate(cc, -rot/180.*M_PI);
      /* pango has a coord system at top left */
      cairo_rel_move_to(cc, -lbearing - width*hadj, -ascent);
      setCairoColor(gc->col, cc);
      pango_cairo_show_layout(cc, layout);
      cairo_restore(cc);
      g_object_unref(layout);
      pango_font_description_free(desc);
   }
   
}
   
void clip(double x0, double x1, double y0, double y1, pDevDesc dev)
{
  CairoDeviceData* pCDD = devDescToCDD(dev);
   cairo_t* cc = pCDD->context;
   
   if (x1 < x0) { double h = x1; x1 = x0; x0 = h; };
   if (y1 < y0) { double h = y1; y1 = y0; y0 = h; };
   
   cairo_reset_clip(cc);
   cairo_new_path(cc);
   /* Add 1 per X11_Clip */
   cairo_rectangle(cc, x0, y0, x1 - x0 + 1, y1 - y0 + 1);
   cairo_clip(cc);
}

void newPage(const pGEcontext gc, pDevDesc dev)
{
   CairoDeviceData* pCDD = devDescToCDD(dev);
   cairo_t* cc = pCDD->context;

   cairo_reset_clip(cc);
  
   /* First clear it */
   cairo_set_operator (cc, CAIRO_OPERATOR_CLEAR);
   cairo_paint (cc);
   cairo_set_operator (cc, CAIRO_OPERATOR_OVER);
    
   setCairoColor(gc->fill, cc);
   cairo_new_path(cc);
   cairo_paint(cc);
}

void mode(int mode, pDevDesc dev)
{

}

void onBeforeExecute(DeviceContext* pDC)
{

}

} // namespace cairo


void installCairoHandler()
{
   handler::allocate = cairo::allocate;
   handler::destroy = cairo::destroy;
   handler::initializeWithFile = cairo::initializeWithFile;
   handler::setSize = cairo::setSize;
   handler::setDeviceAttributes = cairo::setDeviceAttributes;
   handler::onBeforeAddDevice = cairo::onBeforeAddDevice;
   handler::onAfterAddDevice = cairo::onAfterAddDevice;
   handler::resyncDisplayListBeforeWriteToPNG = cairo::resyncDisplayListBeforeWriteToPNG;
   handler::writeToPNG = cairo::writeToPNG;
   handler::circle = cairo::circle;
   handler::line = cairo::line;
   handler::polygon = cairo::polygon;
   handler::polyline = cairo::polyline;
   handler::rect = cairo::rect;
   handler::path = cairo::path;
   handler::raster = cairo::raster;
   handler::cap = cairo::cap;
   handler::metricInfo = cairo::metricInfo;
   handler::strWidth = cairo::strWidth;
   handler::text = cairo::text;
   handler::clip = cairo::clip;
   handler::newPage = cairo::newPage;
   handler::mode = cairo::mode;
   handler::onBeforeExecute = cairo::onBeforeExecute;
}

} // namespace handler
} // namespace graphics
} // namespace session
} // namespace r



