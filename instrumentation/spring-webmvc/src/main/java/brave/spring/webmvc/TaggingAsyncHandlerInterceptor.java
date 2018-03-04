package brave.spring.webmvc;

import brave.SpanCustomizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Same as {@link TaggingHandlerInterceptor} except it can be used as both an {@link
 * AsyncHandlerInterceptor} or a normal {@link HandlerInterceptor}.
 */
public final class TaggingAsyncHandlerInterceptor extends HandlerInterceptorAdapter {
  public static AsyncHandlerInterceptor create(SpanCustomizer spanCustomizer) {
    return new TaggingAsyncHandlerInterceptor(spanCustomizer);
  }

  final HandlerInterceptor delegate;

  @Autowired TaggingAsyncHandlerInterceptor(SpanCustomizer spanCustomizer) { // internal
    delegate = TaggingHandlerInterceptor.create(spanCustomizer);
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o)
      throws Exception {
    return delegate.preHandle(request, response, o);
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object o, Exception ex) throws Exception {
    delegate.afterCompletion(request, response, o, ex);
  }
}
