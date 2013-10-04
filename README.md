This extremely simple HTTP service demonstrates the use of several libraries:
- [jersey-metrics-filter](https://github.com/palominolabs/jersey-metrics-filter), automatic [metrics](http://metrics.codahale.com/) for all [Jersey](https://jersey.java.net/) resource methods
- [jersey-new-relic](https://github.com/palominolabs/jersey-new-relic), Jersey integration into [New Relic](http://newrelic.com/) monitoring
- [jetty-http-server-wrapper](https://github.com/palominolabs/jetty-http-server-wrapper), a simple wrapper around the [Jetty](http://www.eclipse.org/jetty/) http server
- [jersey-cors-filter](https://github.com/palominolabs/jersey-cors-filter) to apply CORS headers to Jersey HTTP responses
- [url-builder](https://github.com/palominolabs/url-builder) to safely build URLs

To use it, all you need to do is copy `newrelic/newrelic.yml.template` to `newrelic/newrelic.yml` and edit the `license_key` field to have your New Relic license key. If you do not have a New Relic account, you can run it without doing this step and though you'll have a stack trace show up in the logs, everything except actually reporting data to New Relic's systems will still work fine.

Once that is done, run the service with `gradle run`. It will make some HTTP requests to itself once a second so that some data will get sent to New Relic. Once the service has been running for a few minutes, check your New Relic dashboard. (It takes a few minutes for New Relic's webapp to update.) It intentionally has a few responses with an error status code (409) occasionally.

You can also open up `jconsole` or `jvisualvm` or the standalone VisualVM app or any other JMX browser to look at what metrics are being reported (look in the `metrics` namespace).