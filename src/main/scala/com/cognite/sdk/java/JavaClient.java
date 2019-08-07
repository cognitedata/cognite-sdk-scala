package com.cognite.sdk.java;

import com.cognite.sdk.scala.common.ApiKeyAuth;
import com.cognite.sdk.scala.common.Auth;
import com.cognite.sdk.scala.v1.AssetCreate;
import com.cognite.sdk.scala.v1.Client;
import com.softwaremill.sttp.HttpURLConnectionBackend$;
import com.softwaremill.sttp.SttpBackend;
import com.softwaremill.sttp.SttpBackendOptions;
import scala.Option;
import scala.collection.JavaConverters;
import scala.concurrent.duration.FiniteDuration;
import scala.runtime.AbstractFunction1;
import scala.runtime.AbstractFunction2;
import scala.runtime.BoxedUnit;

import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class JavaClient {
  private final String applicationName;
  private final Auth auth;
  private final Client client;

  public JavaClient(String applicationName) {
    String apiKey = System.getenv("COGNITE_API_KEY");
    if (apiKey == null) {
      throw new RuntimeException("COGNITE_API_KEY not set");
    }
    this.applicationName = applicationName;
    this.auth = new ApiKeyAuth(apiKey, Option.apply(null));
    SttpBackend<?, Object> backend = HttpURLConnectionBackend$.MODULE$.apply(
        new SttpBackendOptions(new FiniteDuration(30, TimeUnit.SECONDS), Option.apply(null)),
        new AbstractFunction1<HttpURLConnection, BoxedUnit>() {
          public BoxedUnit apply(HttpURLConnection connection) {
            return BoxedUnit.UNIT;
          }
        },
        new AbstractFunction1<String, URL>() {
          public URL apply(String url) {
            try {
              return new URL(url);
            } catch (MalformedURLException e) {
              throw new RuntimeException(e);
            }
          }
        },
        new AbstractFunction2<URL, Option<Proxy>, URLConnection>() {
          public URLConnection apply(URL url, Option<Proxy> proxy) {
            try {
              if (proxy.isDefined()) {
                return url.openConnection(proxy.get());
              } else {
                return url.openConnection();
              }
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        }
    );
    this.client = new Client(this.applicationName, this.auth, backend);

    this.client.assets().create(JavaConverters.asScalaBuffer(
        Collections.singletonList(new AssetCreate("from-java",
            Option.apply(null),
            Option.apply(null),
            Option.apply(null),
            Option.apply(null),
            Option.apply(null),
            Option.apply(null)))));
  }
}
