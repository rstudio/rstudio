/*
 MIT
*/
(function(a){"object"===typeof exports&&"object"===typeof module?module.exports=a(require("../../xterm")):"function"==typeof define?define(["../../xterm"],a):a(window.Terminal)})(function(a){var e={proposeGeometry:function(f){if(!f.element.parentElement)return null;var b=window.getComputedStyle(f.element.parentElement),a=parseInt(b.getPropertyValue("height")),b=Math.max(0,parseInt(b.getPropertyValue("width"))-17),d=window.getComputedStyle(f.element),c=parseInt(d.getPropertyValue("padding-top"))+parseInt(d.getPropertyValue("padding-bottom")),
d=parseInt(d.getPropertyValue("padding-right"))+parseInt(d.getPropertyValue("padding-left")),a=a-c,b=b-d,c=f.rowContainer.firstElementChild,d=c.innerHTML,e;c.style.display="inline";c.innerHTML="W";f=c.getBoundingClientRect().width;c.style.display="";e=c.getBoundingClientRect().height;c.innerHTML=d;a=parseInt(a/e);return{cols:parseInt(b/f),rows:a}},fit:function(a){var b=e.proposeGeometry(a);b&&a.resize(b.cols,b.rows)}};a.prototype.proposeGeometry=function(){return e.proposeGeometry(this)};a.prototype.fit=
function(){return e.fit(this)};return e});
