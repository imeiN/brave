package brave.spring.webmvc;

import brave.SpanCustomizer;
import brave.servlet.TracingFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Adds application-tier tags such as the {@link #CONTROLLER_CLASS} and {@link #CONTROLLER_METHOD}.
 * This also sets the request attribute "http.route" so that it can be used in naming the http span.
 *
 * <p>Use this instead of {@link TracingHandlerInterceptor} when you are tracing at a lower level,
 * like via {@link TracingFilter}.
 *
 * <p>Tagging policy adopted from spring cloud sleuth 1.3.x
 */
public final class TaggingHandlerInterceptor implements HandlerInterceptor {
  public static HandlerInterceptor create(SpanCustomizer spanCustomizer) {
    return new TaggingHandlerInterceptor(spanCustomizer);
  }

  /** The simple class name that processed the request. Ex "BookController" */
  public static final String CONTROLLER_CLASS = "mvc.controller.class";

  /** The method name that processed the request. Ex "listOfBooks" */
  public static final String CONTROLLER_METHOD = "mvc.controller.method";

  // redefined from HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE as doesn't exist until Spring 3
  static final String BEST_MATCHING_PATTERN_ATTRIBUTE =
      "org.springframework.web.servlet.HandlerMapping.bestMatchingPattern";

  final SpanCustomizer spanCustomizer;

  @Autowired TaggingHandlerInterceptor(SpanCustomizer spanCustomizer) { // internal
    this.spanCustomizer = spanCustomizer;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o) {
    if (o instanceof HandlerMethod) {
      HandlerMethod handlerMethod = ((HandlerMethod) o);
      spanCustomizer.tag(CONTROLLER_CLASS, handlerMethod.getBeanType().getSimpleName());
      spanCustomizer.tag(CONTROLLER_METHOD, handlerMethod.getMethod().getName());
    } else {
      spanCustomizer.tag(CONTROLLER_CLASS, o.getClass().getSimpleName());
    }

    // set the "http.route" attribute so that the servlet adapter can read it.
    Object httpRoute = request.getAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE);
    request.setAttribute("http.route", httpRoute != null ? httpRoute.toString() : "");
    return true;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
      ModelAndView modelAndView) {
  }

  @Override public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) {
  }
}
