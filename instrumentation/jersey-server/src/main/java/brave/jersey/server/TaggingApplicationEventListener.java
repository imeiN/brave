package brave.jersey.server;

import brave.SpanCustomizer;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.internal.routing.RoutingContext;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.glassfish.jersey.uri.UriTemplate;

import static org.glassfish.jersey.server.monitoring.RequestEvent.Type.REQUEST_MATCHED;

/**
 * Adds application-tier tags such as the {@link #CONTROLLER_CLASS} and {@link #CONTROLLER_METHOD}.
 * This also sets the request property "http.route" so that it can be used in naming the http span.
 *
 * <p>Use this instead of {@link TracingApplicationEventListener} when you are tracing at a lower level,
 * like via {@code brave.servlet.TracingFilter}.
 *
 * <p>Tagging policy adopted from spring cloud sleuth 1.3.x
 */
@Provider
public class TaggingApplicationEventListener
    implements ApplicationEventListener, RequestEventListener {
  /** The simple class name that processed the request. Ex "BookController" */
  public static final String CONTROLLER_CLASS = "jaxrs.resource.class";

  /** The method name that processed the request. Ex "listOfBooks" */
  public static final String CONTROLLER_METHOD = "jaxrs.resource.method";

  public static ApplicationEventListener create(SpanCustomizer spanCustomizer) {
    return new TaggingApplicationEventListener(spanCustomizer);
  }

  final SpanCustomizer spanCustomizer;

  @Inject TaggingApplicationEventListener(SpanCustomizer spanCustomizer) { // internal
    this.spanCustomizer = spanCustomizer;
  }

  @Override public void onEvent(ApplicationEvent event) {
    // only onRequest is used
  }

  @Override public RequestEventListener onRequest(RequestEvent requestEvent) {
    if (requestEvent.getType() == RequestEvent.Type.START) return this;
    return null;
  }

  @Override public void onEvent(RequestEvent event) {
    // Note: until REQUEST_MATCHED, we don't know metadata such as if the request is async or not
    if (event.getType() != REQUEST_MATCHED) return;
    event.getContainerRequest().setProperty("http.route", route(event.getContainerRequest()));
    Invocable i =
        event.getContainerRequest().getUriInfo().getMatchedResourceMethod().getInvocable();
    spanCustomizer.tag(CONTROLLER_CLASS, i.getHandler().getHandlerClass().getSimpleName());
    spanCustomizer.tag(CONTROLLER_METHOD, i.getHandlingMethod().getName());
  }

  /**
   * This returns the matched template as defined by a base URL and path expressions.
   *
   * <p>Matched templates are pairs of (resource path, method path) added with
   * {@link RoutingContext#pushTemplates(UriTemplate, UriTemplate)}.
   * This code skips redundant slashes from either source caused by Path("/") or Path("").
   */
  static String route(ContainerRequest request) {
    ExtendedUriInfo uriInfo = request.getUriInfo();
    List<UriTemplate> templates = uriInfo.getMatchedTemplates();
    int templateCount = templates.size();
    if (templateCount == 0) return "";
    assert templateCount % 2 == 0 : "expected matched templates to be resource/method pairs";
    StringBuilder builder = null; // don't allocate unless you need it!
    String basePath = uriInfo.getBaseUri().getPath();
    String result = null;
    if (!"/".equals(basePath)) { // skip empty base paths
      result = basePath;
    }
    for (int i = templateCount - 1; i >= 0; i--) {
      String template = templates.get(i).getTemplate();
      if ("/".equals(template)) continue; // skip allocation
      if (builder != null) {
        builder.append(template);
      } else if (result != null) {
        builder = new StringBuilder(result).append(template);
        result = null;
      } else {
        result = template;
      }
    }
    return result != null ? result : builder != null ? builder.toString() : "";
  }
}
