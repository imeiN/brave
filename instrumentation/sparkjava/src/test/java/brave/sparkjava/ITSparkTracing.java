package brave.sparkjava;

import brave.test.http.ITHttpServer;
import brave.propagation.ExtraFieldPropagation;
import okhttp3.Response;
import org.junit.After;
import org.junit.AssumptionViolatedException;
import spark.Spark;

public class ITSparkTracing extends ITHttpServer {

  @Override protected Response get(String path) throws Exception {
    if (path.toLowerCase().indexOf("async") == -1) return super.get(path);
    throw new AssumptionViolatedException("ignored until https://github.com/perwendel/spark/issues/208");
  }

  @Override protected void init() throws Exception {
    stop();

    SparkTracing spark = SparkTracing.create(httpTracing);

    Spark.before(spark.before());
    Spark.exception(Exception.class, spark.exception(
        (exception, request, response) -> response.body("exception"))
    );
    Spark.afterAfter(spark.afterAfter());

    Spark.options("/", (req, res) -> "");
    Spark.get("/foo", (req, res) -> "bar");
    Spark.get("/extra", (req, res) -> ExtraFieldPropagation.get(EXTRA_KEY));
    Spark.get("/badrequest", (req, res) -> {
      res.status(400);
      return res;
    });
    Spark.get("/child", (req, res) -> {
      httpTracing.tracing().tracer().nextSpan().name("child").start().finish();
      return "happy";
    });
    Spark.get("/exception", (req, res) -> {
      throw new Exception();
    });

    Spark.awaitInitialization();
  }

  @Override
  protected String url(String path) {//default port 4567
    return "http://localhost:4567" + path;
  }

  /**
   * Spark stop asynchronously but share one class Instance,
   * so AddressAlreadyUsed Exception may happen.
   * See:https://github.com/perwendel/spark/issues/705 .
   * Just sleep 1 second to avoid this happens,
   * after Spark.awaitStopped add,I will fix it.
   */
  @After
  public void stop() throws InterruptedException {
    Spark.stop();
    Thread.sleep(1000);
  }
}
