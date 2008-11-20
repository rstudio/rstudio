package kellegous.client;

import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

public class Subject {
  public static EventListener create(int numberOfObserver) {
    final Button subject = new Button("a button");
    for (int i = 0; i < numberOfObserver; ++i) {
      subject.addClickListener(new ClickListener() {
        private int count = 0;

        public void onClick(Widget sender) {
          count++;
        }
      });
    }
    return subject;
  }
}
