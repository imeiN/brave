package brave.spring.webmvc;

import brave.CurrentSpanCustomizer;
import brave.http.HttpTracing;
import brave.servlet.TracingFilter;
import brave.test.http.ITServletContainer;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/** This tests when you use servlet for tracing but MVC for tagging */
public class ITTaggingAsyncHandlerInterceptor extends ITServletContainer {

  @Configuration
  @EnableWebMvc
  static class TracingConfig extends WebMvcConfigurerAdapter {
    @Bean AsyncHandlerInterceptor taggingInterceptor(HttpTracing httpTracing) {
      return TaggingAsyncHandlerInterceptor.create(
          CurrentSpanCustomizer.create(httpTracing.tracing())
      );
    }

    @Autowired
    private AsyncHandlerInterceptor taggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
      registry.addInterceptor(taggingInterceptor);
    }
  }

  @Override public void init(ServletContextHandler handler) {
    AnnotationConfigWebApplicationContext appContext =
        new AnnotationConfigWebApplicationContext() {
          // overriding this allows us to register dependencies of TracingHandlerInterceptor
          // without passing static state to a configuration class.
          @Override protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) {
            beanFactory.registerSingleton("httpTracing", httpTracing);
            super.loadBeanDefinitions(beanFactory);
          }
        };

    appContext.register(TestController.class); // the test resource
    appContext.register(TracingConfig.class); // generic tracing setup
    DispatcherServlet servlet = new DispatcherServlet(appContext);
    servlet.setDispatchOptionsRequest(true);
    handler.addServlet(new ServletHolder(servlet), "/*");

    // add the trace filter
    handler.getServletContext()
        .addFilter("tracingFilter", TracingFilter.create(httpTracing))
        .addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
  }
}
