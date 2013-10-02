package com.palominolabs.newrelic.sample

import javax.ws.rs.GET
import javax.ws.rs.Path

@Path("foo")
class FooResource {

  @GET
  String get() {
    return "foo"
  }

  @GET
  @Path("bar")
  String getBar() {
    return "bar"
  }
}
