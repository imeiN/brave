package brave.jersey.server;

import brave.CurrentSpanCustomizer;
import brave.servlet.TracingFilter;
import brave.test.http.ITServletContainer;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.AssumptionViolatedException;
import org.junit.Test;

public class ITTaggingApplicationEventListener extends ITServletContainer {

  @Override @Test public void reportsClientAddress() {
    throw new AssumptionViolatedException("TODO!");
  }

  @Override @Test public void async() {
    throw new AssumptionViolatedException("TODO! hangs");
  }

  @Override @Test public void reportsSpanOnException_async() {
    throw new AssumptionViolatedException("TODO! hangs");
  }

  @Override @Test public void addsErrorTagOnException_async() {
    throw new AssumptionViolatedException("TODO! hangs");
  }

  @Override public void init(ServletContextHandler handler) {
    ResourceConfig config = new ResourceConfig();
    config.register(new TestResource(httpTracing));
    config.register(TaggingApplicationEventListener.create(
        CurrentSpanCustomizer.create(httpTracing.tracing()))
    );
    handler.addServlet(new ServletHolder(new ServletContainer(config)), "/*");

    addFilter(handler, TracingFilter.create(httpTracing));
  }

  void addFilter(ServletContextHandler handler, Filter filter) {
    handler.addFilter(new FilterHolder(filter), "/*", EnumSet.allOf(DispatcherType.class));
  }
}
