package brave.jersey.server;

import brave.test.http.ITServletContainer;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.AssumptionViolatedException;
import org.junit.Test;

public class ITTracingApplicationEventListener extends ITServletContainer {

  @Override @Test public void reportsClientAddress() {
    throw new AssumptionViolatedException("TODO!");
  }

  /** Tests that the span propagates between under asynchronous callbacks managed by jersey. */
  @Test public void managedAsync() throws Exception {
    get("/managedAsync");

    takeSpan();
  }

  @Override public void init(ServletContextHandler handler) {
    ResourceConfig config = new ResourceConfig();
    config.register(new TestResource(httpTracing));
    config.register(TracingApplicationEventListener.create(httpTracing));
    handler.addServlet(new ServletHolder(new ServletContainer(config)), "/*");
  }
}
