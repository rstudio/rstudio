/*
 * xterm-compat.js
 *
 * Compatibility layer for xterm.js 5.5.0 to handle internal API access
 * and provide fallbacks for functionality that RStudio depends on.
 */

(function() {
   'use strict';

   // Wait for Terminal to be available and extend its prototype with compatibility methods
   function installCompatLayer() {
      if (typeof window.Terminal === 'undefined') {
         // If Terminal not available yet, try again in a moment
         setTimeout(installCompatLayer, 10);
         return;
      }
      
      // Add compatibility layer for cursor position access
      Object.defineProperty(window.Terminal.prototype, 'rstudioCursorX', {
         get: function() {
            // Try documented API first (xterm.js 5.x)
            if (this.buffer && this.buffer.active) {
               return this.buffer.active.cursorX;
            }
            // Fallback to internal API if available
            if (this._core && this._core.buffer) {
               return this._core.buffer.x;
            }
            // Last resort - try direct buffer access (xterm.js 4.x style)
            if (this.buffer) {
               return this.buffer.x || 0;
            }
            return 0;
         }
      });

      Object.defineProperty(window.Terminal.prototype, 'rstudioCursorY', {
         get: function() {
            // Try documented API first (xterm.js 5.x)
            if (this.buffer && this.buffer.active) {
               return this.buffer.active.cursorY;
            }
            // Fallback to internal API if available
            if (this._core && this._core.buffer) {
               return this._core.buffer.y;
            }
            // Last resort - try direct buffer access (xterm.js 4.x style)
            if (this.buffer) {
               return this.buffer.y || 0;
            }
            return 0;
         }
      });

      // Add compatibility for alt buffer detection
      window.Terminal.prototype.rstudioAltBufferActive = function() {
         // Try documented API first
         if (this.buffer && this.buffer.active && this.buffer.alternate) {
            return this.buffer.active === this.buffer.alternate;
         }
         // Fallback to internal API
         if (this._core && this._core.buffers) {
            return this._core.buffers.active === this._core.buffers.alt;
         }
         // Cannot determine, assume primary buffer
         return false;
      };

      // Add compatibility for current line access
      window.Terminal.prototype.rstudioCurrentLine = function() {
         try {
            // Try documented API first (xterm.js 5.x)
            if (this.buffer && this.buffer.active) {
               const y = this.buffer.active.cursorY;
               const line = this.buffer.active.getLine(y);
               if (line) {
                  return line.translateToString ? line.translateToString() : line.toString();
               }
            }
            
            // Fallback to internal API
            if (this._core && this._core.buffer && this._core.buffer.lines) {
               const y = this._core.buffer.y || 0;
               const ybase = this._core.buffer.ybase || 0;
               const lineBuf = this._core.buffer.lines.get(y + ybase);
               if (lineBuf && lineBuf.translateToString) {
                  return lineBuf.translateToString();
               }
            }
         } catch (e) {
            console.warn('Error accessing current line:', e);
         }
         return null;
      };

      // Add compatibility for options API
      window.Terminal.prototype.rstudioSetOption = function(key, value) {
         // xterm.js 5.x uses direct property access
         if (this.options && typeof this.options === 'object') {
            // Handle theme specially as it's an object
            if (key === 'theme') {
               // For theme, we need to handle the selection -> selectionBackground rename
               if (value && value.selection && !value.selectionBackground) {
                  value.selectionBackground = value.selection;
                  delete value.selection;
               }
               this.options.theme = value;
            } else {
               this.options[key] = value;
            }
         } else if (typeof this.setOption === 'function') {
            // Fallback to old API if available
            this.setOption(key, value);
         }
      };

      window.Terminal.prototype.rstudioGetOption = function(key) {
         // xterm.js 5.x uses direct property access
         if (this.options && typeof this.options === 'object') {
            return this.options[key];
         } else if (typeof this.getOption === 'function') {
            // Fallback to old API if available
            return this.getOption(key);
         }
         return undefined;
      };

      // Add compatibility for fit addon dimensions
      window.Terminal.prototype.rstudioProposeDimensions = function() {
         // If we have the fit addon attached, use it
         if (this.rstudioFitAddon_ && this.rstudioFitAddon_.proposeDimensions) {
            const dims = this.rstudioFitAddon_.proposeDimensions();
            // Handle different return formats between versions
            if (dims) {
               return {
                  cols: dims.cols || dims.columns,
                  rows: dims.rows
               };
            }
         }
         
         // Fallback calculation if fit addon not available
         if (this._core && this._core._renderService && this._core._renderService.dimensions) {
            const dimensions = this._core._renderService.dimensions;
            if (dimensions.actualCellWidth && dimensions.actualCellHeight) {
               const parentElement = this.element || this._core.element;
               if (parentElement && parentElement.parentElement) {
                  const cols = Math.floor(parentElement.parentElement.clientWidth / dimensions.actualCellWidth);
                  const rows = Math.floor(parentElement.parentElement.clientHeight / dimensions.actualCellHeight);
                  return { cols: cols, rows: rows };
               }
            }
         }
         
         // Default fallback
         return { cols: 80, rows: 24 };
      };
   }
   
   // Start trying to install the compatibility layer
   installCompatLayer();
})();