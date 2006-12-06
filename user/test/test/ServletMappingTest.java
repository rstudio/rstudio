package test;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

public class ServletMappingTest extends GWTTestCase {

  private static final int RPC_WAIT = 5000;

  public String getModuleName() {
    return "test.ServletMappingTest";
  }

  /**
   * Should call the implementation that returns 1.
   */
  public void testServletMapping1() {
    String url = "test";
    makeAsyncCall(GWT.getModuleBaseURL() + "test", new AsyncCallback() {
      public void onFailure(Throwable caught) {
        fail(caught.toString());
      }

      public void onSuccess(Object result) {
        finishTest();
        assertEquals(new Integer(1), result);
      }
    });
  }

  /**
   * Should call the implementation that returns 2.
   */
  public void testServletMapping2() {
    makeAsyncCall(GWT.getModuleBaseURL() + "test/longer", new AsyncCallback() {
      public void onFailure(Throwable caught) {
        fail(caught.toString());
      }

      public void onSuccess(Object result) {
        finishTest();
        assertEquals(new Integer(2), result);
      }
    });
  }

  /**
   * Should call the implementation that returns 3.
   */
  public void testServletMapping3() {
    makeAsyncCall(GWT.getModuleBaseURL() + "test/long", new AsyncCallback() {
      public void onFailure(Throwable caught) {
        fail(caught.toString());
      }

      public void onSuccess(Object result) {
        finishTest();
        assertEquals(new Integer(3), result);
      }
    });
  }

  /**
   * Should fail to find an entry point.
   */
  public void testBadRequestWithExtraPath() {
    makeAsyncCall(GWT.getModuleBaseURL() + "test/bogus/extra/path",
        new AsyncCallback() {
          public void onFailure(Throwable caught) {
            finishTest();
          }

          public void onSuccess(Object result) {
            finishTest();
            assertEquals(new Integer(1), result);
          }
        });
  }

  /**
   * Should fail to find an entry point.
   */
  public void testBadRequestWithQueryString() {
    makeAsyncCall(GWT.getModuleBaseURL() + "test/bogus?a=b&c=d",
        new AsyncCallback() {
          public void onFailure(Throwable caught) {
            finishTest();
          }

          public void onSuccess(Object result) {
            finishTest();
            assertEquals(new Integer(1), result);
          }
        });
  }

  /**
   * Should call the implementation that returns 3 with a query string.
   */
  public void testServletMapping3WithQueryString() {
    makeAsyncCall(GWT.getModuleBaseURL() + "test/long?a=b&c=d",
        new AsyncCallback() {
          public void onFailure(Throwable caught) {
            fail(caught.toString());
          }

          public void onSuccess(Object result) {
            finishTest();
            assertEquals(new Integer(3), result);
          }
        });
  }

  /**
   * Should call the implementation that returns 3 with a query string.
   */
  public void testTotallyDifferentServletMapping3() {
    makeAsyncCall(GWT.getModuleBaseURL()
      + "totally/different/but/valid?a=b&c=d", new AsyncCallback() {
      public void onFailure(Throwable caught) {
        fail(caught.toString());
      }

      public void onSuccess(Object result) {
        finishTest();
        assertEquals(new Integer(3), result);
      }
    });
  }

  private void makeAsyncCall(String url, AsyncCallback callback) {
    ServletMappingTestServiceAsync async = (ServletMappingTestServiceAsync) GWT
      .create(ServletMappingTestService.class);
    ServiceDefTarget target = (ServiceDefTarget) async;
    target.setServiceEntryPoint(url);
    delayTestFinish(RPC_WAIT);
    async.which(callback);
  }

}
