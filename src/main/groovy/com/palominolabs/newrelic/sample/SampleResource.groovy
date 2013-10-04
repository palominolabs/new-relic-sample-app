package com.palominolabs.newrelic.sample

import com.palominolabs.jersey.cors.Cors
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.core.Response

@Path("resource")
class SampleResource {

  @GET
  @Cors
  String get() {
    return "some data"
  }

  @GET
  @Path("subresource")
  Response getSubresource() {
    // occasionally return a different status code
    if (Math.random() > 0.9) {
      return Response.status(Response.Status.CONFLICT).entity("conflict").build()
    }

    return Response.ok().entity("ok").build()
  }
}
