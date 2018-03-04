package brave.spring.webmvc;

import brave.CurrentSpanCustomizer;
import brave.http.HttpTracing;
import brave.servlet.TracingFilter;
import brave.test.http.ITServletContainer;
import javax.servlet.Filter;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/** This tests when you use servlet for tracing but MVC for tagging */
public abstract class BaseITTaggingHandlerInterceptor extends ITServletContainer {

  @Configuration
  @EnableWebMvc
  static class TracingConfig extends WebMvcConfigurerAdapter {
    @Bean HandlerInterceptor taggingInterceptor(HttpTracing httpTracing) {
      return TaggingHandlerInterceptor.create(
          CurrentSpanCustomizer.create(httpTracing.tracing())
      );
    }

    @Autowired
    private HandlerInterceptor taggingInterceptor;

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
    addFilter(handler, TracingFilter.create(httpTracing));
  }

  // abstract because filter registration types were not introduced until servlet 3.0
  protected abstract void addFilter(ServletContextHandler handler, Filter filter);
}
