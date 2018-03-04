package brave.servlet;

import brave.test.http.ITServlet3Container;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class ITTracingFilter extends ITServlet3Container {

  @Override protected Filter newTracingFilter() {
    return TracingFilter.create(httpTracing);
  }

  @Override protected void addFilter(ServletContextHandler handler, Filter filter) {
    handler.addFilter(new FilterHolder(filter), "/*", EnumSet.allOf(DispatcherType.class));
  }
}
