
package elemental.events;

import elemental.events.*;

/**
  * <code>AnimationEvent</code> objects provide information about events that occur related to <a rel="internal" href="https://developer.mozilla.org/en/CSS/CSS_animations" title="en/CSS/CSS_animations">CSS animations</a>.
  */
public interface WebKitAnimationEvent extends Event {


  /**
    * The name of the animation on which the animation event occurred.
    */
  String getAnimationName();


  /**
    * The amount of time, in seconds, the animation had been running at the time the event occurred.
    */
  double getElapsedTime();
}
