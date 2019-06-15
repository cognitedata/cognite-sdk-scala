package com.cognite.sdk.scala.v06

import com.cognite.sdk.scala.common.{ApiKeyAuth, Auth, InvalidAuthentication, SdkTest}

class ClientTest extends SdkTest {
  "Client" should "fetch the project using login/status if necessary" in {
    noException should be thrownBy new GenericClient()(auth, backend)
    new GenericClient()(auth, backend).project should not be empty
  }
  it should "throw an exception if the authentication is invalid and project is not specified" in {
    implicit val auth: Auth = ApiKeyAuth("invalid-key")
    an[InvalidAuthentication] should be thrownBy new GenericClient()(auth, backend)
  }
  it should "not throw an exception if the authentication is invalid and project is specified" in {
    implicit val auth: Auth = ApiKeyAuth("invalid-key", project = Some("random-project"))
    noException should be thrownBy new GenericClient()(auth, backend)
  }
}
