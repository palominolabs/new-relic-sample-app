package com.palominolabs.newrelic.sample

import com.codahale.metrics.JmxReporter
import com.codahale.metrics.MetricRegistry
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.servlet.ServletModule
import com.palominolabs.config.ConfigModuleBuilder
import com.palominolabs.http.server.HttpServerConnectorConfig
import com.palominolabs.http.server.HttpServerWrapperConfig
import com.palominolabs.http.server.HttpServerWrapperFactory
import com.palominolabs.http.server.HttpServerWrapperModule
import com.palominolabs.jersey.dispatchwrapper.ResourceMethodWrappedDispatchModule
import com.palominolabs.metrics.jersey.HttpStatusCodeCounterResourceFilterFactory
import com.palominolabs.metrics.jersey.JerseyResourceMetrics
import com.palominolabs.metrics.jersey.ResourceMethodMetricsModule
import com.sun.jersey.api.core.ResourceConfig
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer
import java.util.logging.LogManager
import javax.servlet.http.HttpServlet
import org.slf4j.bridge.SLF4JBridgeHandler

class NewRelicMain {
  public static void main(String[] args) {

    LogManager.getLogManager().reset()
    SLF4JBridgeHandler.install()

    Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        install(new HttpServerWrapperModule())

        bind(FooResource)

        final Map<String, String> initParams = new HashMap<>();
        initParams.put(ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES,
            HttpStatusCodeCounterResourceFilterFactory.class.getCanonicalName());

        install(new ServletModule() {
          @Override
          protected void configureServlets() {
            bind(GuiceContainer.class)

            serve("/*").with(GuiceContainer.class as Class<? extends HttpServlet>, initParams);
          }
        })

        install(new ResourceMethodMetricsModule())
        install(new ResourceMethodWrappedDispatchModule())
        install(new ConfigModuleBuilder().build())

        MetricRegistry registry = new MetricRegistry()
        bind(MetricRegistry.class).annotatedWith(JerseyResourceMetrics.class).toInstance(registry)
      }
    })

    HttpServerWrapperConfig config = new HttpServerWrapperConfig()
        .withHttpServerConnectorConfig(HttpServerConnectorConfig.forHttp("localhost", 8080))

    injector.getInstance(HttpServerWrapperFactory.class)
        .getHttpServerWrapper(config)
        .start()

    final JmxReporter reporter = JmxReporter.forRegistry(injector.getInstance(Key.get(MetricRegistry,
        JerseyResourceMetrics))).build();
    reporter.start();
  }
}
