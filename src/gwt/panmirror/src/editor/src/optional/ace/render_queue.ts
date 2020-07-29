/*
 * render_queue.ts
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { CodeBlockNodeView } from './ace';

/**
 * Represents a queue of Ace editor instances that are rendered asynchronously.
 */
export class AceRenderQueue {
  private readonly renderQueue: CodeBlockNodeView[] = [];

  private renderCompleted: boolean = false;
  private renderTimer: number = 0;
  private container?: HTMLElement;
  private needsSort: boolean = true;
  private bypass: number = 5;
  private observer?: IntersectionObserver;
  private visible: boolean = true;

  /**
   * Sets (or replaces) the scroll container hosting the render queue. The
   * scroll container is used to prioritize the queue (i.e. objects in the
   * viewport or close to it are to be given more priority).
   * 
   * @param container The HTML element of the scroll container
   */
  public setContainer(container: HTMLElement) {

    // Handler for scroll events in the container
    const handleScroll = (evt: Event) => {
      // Whenever a scroll event occurs, we need to re-sort the queue so that
      // objects in or closest to the viewport are given priority.
      this.needsSort = true;

      // If we don't think we're visible but we're scrolling and have height,
      // then we are in fact visible. (This catches a case where the
      // intsersection observer created below doesn't fire for visiblity
      // changes.)
      if (!this.visible && this.container && this.container.offsetHeight > 0) {
        this.visible = true;
        this.processRenderQueue();
      }
    };

    // Skip if we're already looking at this container
    if (this.container === container) {
      return;
    }

    // Clean up handlers on any previous container
    if (this.container) {
      this.container.removeEventListener("scroll", handleScroll);
    }
    if (this.observer) {
      this.observer.disconnect();
    }

    this.container = container;

    // Create intersection observer to ensure that we don't needlessly churn
    // through renders while offscreen.
    this.observer = new IntersectionObserver((entries: IntersectionObserverEntry[]) => {
      for (const entry of entries) {
        if (entry.isIntersecting && !this.visible) {
          // We just became visible; process the render queue now.
          this.visible = true;
          this.processRenderQueue();
        }
        if (!entry.isIntersecting && this.visible) {
          // We just lost visibility; end processing of the render queue. (Note
          // that we only do this when connected to the DOM as another reason
          // the element can become invisible is ProseMirror swapping it out
          // internally.)
          if (this.container?.parentElement) {
            this.visible = false;
            this.cancelTimer();
          }
        }
      }
    }, {
      root: null
    });
    this.observer.observe(container);

    // Hook up event handlers
    this.container.addEventListener("scroll", handleScroll);
  }

  /**
   * Indicates whether the render queue has a scroll container defined
   */
  public hasContainer(): boolean {
    if (this.container) {
      return true;
    }
    return false;
  }

  /**
   * Indicates whether the rendering is finished.
   */
  public isRenderCompleted(): boolean {
    return this.renderCompleted;
  }

  /**
   * Adds a node view to the render queue
   */
  public add(view: CodeBlockNodeView) {
    // We allow the first few code blocks to render synchronously instead of
    // being dumped into the queue for later processing. This slightly increases
    // startup time but prevents the flicker that would otherwise occur as
    // editors render one by one.
    if (this.bypass > 0) {
      this.bypass--;
      view.initEditor();
      return;
    }

    this.renderQueue.push(view);

    // Defer/debounce rendering of editors until event loop finishes
    if (this.renderTimer !== 0) {
      window.clearTimeout(this.renderTimer);
    }
    this.renderTimer = window.setTimeout(() => {
      this.processRenderQueue();
    }, 0);
  }

  /**
   * Processes the queue of editor instances that need to be rendered.
   */
  private processRenderQueue() {
    // No work to do if queue is empty
    if (this.renderQueue.length === 0) {
      return;
    }

    // Don't render while hidden; it wastes resources plus can result in
    // incorrect sizing calculations
    if (!this.visible) {
      return;
    }

    // Sort the queue if required
    if (this.needsSort) {
      // Compute offset for sorting (based on scroll position)
      let offset = 0;
      if (this.container) {
        offset = this.container.scrollTop;
      }

      // Sort the render queue based on distance from the top of the viewport
      this.renderQueue.sort((a, b) => {
        return Math.abs(a.dom.offsetTop - offset) - Math.abs(b.dom.offsetTop - offset);
      });

      // Clear flag so we don't sort the queue on every render
      this.needsSort = false;
    }

    // Pop the next view (editor instance) to be rendered off the stack
    const view = this.renderQueue.shift();

    // Render this view
    if (view) {
      view.initEditor();
    }

    if (this.renderQueue.length > 0) {
      // There are still remaining editors to be rendered, so process again on
      // the next event loop.
      this.renderTimer = window.setTimeout(() => {
        this.processRenderQueue();
      }, 0);
    } else {
      // No remaining editors; we're done.
      this.renderCompleted = true;
      this.destroy();
    }
  }

  /**
   * Cancels the timer that is responsible for triggering the processing of the
   * next unit in the render queue.
   */
  private cancelTimer() {
    if (this.renderTimer !== 0) {
      window.clearTimeout(this.renderTimer);
      this.renderTimer = 0;
    }
  }

  /**
   * Cleans up the render queue instance
   */
  private destroy() {
    // Cancel any pending renders
    this.cancelTimer();

    // Clean up resize observer
    if (this.observer) {
      this.observer.disconnect();
    }
  }
}

