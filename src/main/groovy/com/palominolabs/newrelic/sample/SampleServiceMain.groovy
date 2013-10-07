package com.palominolabs.newrelic.sample

import com.codahale.metrics.JmxReporter
import com.codahale.metrics.MetricRegistry
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Scopes
import com.google.inject.servlet.ServletModule
import com.ning.http.client.AsyncHttpClient
import com.ning.http.client.Response
import com.palominolabs.config.ConfigModuleBuilder
import com.palominolabs.http.server.HttpServerConnectorConfig
import com.palominolabs.http.server.HttpServerWrapperConfig
import com.palominolabs.http.server.HttpServerWrapperFactory
import com.palominolabs.http.server.HttpServerWrapperModule
import com.palominolabs.http.url.UrlBuilder
import com.palominolabs.jersey.cors.CorsResourceFilterFactory
import com.palominolabs.jersey.dispatchwrapper.ResourceMethodWrappedDispatchModule
import com.palominolabs.jersey.newrelic.JerseyNewRelicModule
import com.palominolabs.jersey.newrelic.NewRelicResourceFilterFactory
import com.palominolabs.metrics.jersey.HttpStatusCodeCounterResourceFilterFactory
import com.palominolabs.metrics.jersey.JerseyResourceMetrics
import com.palominolabs.metrics.jersey.ResourceMethodMetricsModule
import com.palominolabs.servlet.newrelic.NewRelicUnmappedThrowableFilter
import com.sun.jersey.api.core.ResourceConfig
import com.sun.jersey.guice.JerseyServletModule
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.logging.LogManager
import javax.servlet.http.HttpServlet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler

class SampleServiceMain {
  private static final Logger logger = LoggerFactory.getLogger(SampleServiceMain.class);

  private static final String HOST = "localhost"
  private static final int PORT = 8080

  public static void main(String[] args) {

    LogManager.getLogManager().reset()
    SLF4JBridgeHandler.install()

    final MetricRegistry registry = new MetricRegistry()

    // start up an http server
    HttpServerWrapperConfig config = new HttpServerWrapperConfig()
        .withHttpServerConnectorConfig(HttpServerConnectorConfig.forHttp(HOST, PORT))

    getInjector(registry).getInstance(HttpServerWrapperFactory.class)
        .getHttpServerWrapper(config)
        .start()

    // start a jmx reporter so you can inspect metrics with jconsole or visual vm
    JmxReporter reporter = JmxReporter.forRegistry(registry).build()
    reporter.start()

    // execute some requests against our service

    ScheduledExecutorService svc = Executors.newScheduledThreadPool(1)

    AsyncHttpClient client = new AsyncHttpClient()

    svc.scheduleAtFixedRate({
      UrlBuilder builder = UrlBuilder.forHost("http", HOST, PORT).pathSegment("resource")

      Response resp = client.prepareGet(builder.toUrlString()).addHeader("Origin", HOST).execute().get()
      logger.info("Got allow-origin: ${resp.getHeader("Access-Control-Allow-Origin")}")

      client.prepareGet(builder.pathSegment("subresource").toUrlString()).execute().get()
    }, 0, 1, TimeUnit.SECONDS)
  }

  private static Injector getInjector(registry) {
    return Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        // we're using an http server
        install(new HttpServerWrapperModule())

        // bind the resource so GuiceContainer can find it
        bind(SampleResource)

        // register metric filter as well as new relic transaction name filter
        final Map<String, String> initParams = new HashMap<>()
        initParams.put(ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES,
            [HttpStatusCodeCounterResourceFilterFactory, NewRelicResourceFilterFactory, CorsResourceFilterFactory]
              .collect({Class c -> c.canonicalName})
              .join(','))

        install(new ServletModule() {
          @Override
          protected void configureServlets() {
            // jersey-guice integration
            bind(GuiceContainer.class)

            // configure jersey with init params
            serve("/*").with(GuiceContainer.class as Class<? extends HttpServlet>, initParams)

            // inform new relic of unmapped throwables
            bind(NewRelicUnmappedThrowableFilter.class)
            filter("/*").through(NewRelicUnmappedThrowableFilter.class)
          }
        })

        // standard jersey-guice module
        install(new JerseyServletModule())

        // set up metrics for all resource methods
        install(new ResourceMethodMetricsModule())
        // prereqs
        install(new ResourceMethodWrappedDispatchModule())
        install(new ConfigModuleBuilder().build())

        // set new relic transaction names based on jersey resource methods
        install(new JerseyNewRelicModule())

        // bind registry so that jersey-metrics-filter can find it
        bind(MetricRegistry.class).annotatedWith(JerseyResourceMetrics.class).toInstance(registry)
      }
    })
  }
}
