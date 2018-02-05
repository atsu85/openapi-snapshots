# OpenApi Snapshots
The aim of this project is to help testing remote api compatibility with the expectations of the consumer.
It works with API documentation (api-doc) that conforms to
[OpenAPI Specification](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md),
that is used by for example Swagger UI,
and can be generated automatically based on service endpoints (by the service provider).

## Idea
1. Remote service provider should generate api-doc in OpenAPI format based on the endpoints exposed to the service consumers.
1. Consumer of the remote service should specify operations that are used by the consumer.
1. Consumer of the remote service should create snapshot of the api-doc of the remote service that is considered OK:
   1. Obtain latest api-doc of the remote service.
   1. Remove operations and model definitions that are not used by the consumer.
   1. Save snapshot file for running compatibility tests in the future.
1. Check, that latest api-doc of the remote service is compatible with the snapshot saved previously:
   1. Obtain latest api-doc of the remote service.
   2. Check that all operations and model definitions in previously created snapshot are compatible with the latest version of the api-doc.
Test should fail, if incompatibility is detected.

### Sample test
To benefit from this library, you could create a test based on following example:
```Java
package com.example;

import java.io.File;

import org.apache.http.message.BasicHeader;
import org.junit.Test;

import com.github.atsu85.openapisnapshots.filter.SelectedOperations;
import com.github.atsu85.openapisnapshots.test.RemoteEndpointApiSnapshotTestHelper;

import io.swagger.models.HttpMethod;

public class DemoRemoteEndpointApiSnapshotUnitTest {

	private SelectedOperations getUsedOperations() {
		return new SelectedOperations()
				.addOperation(HttpMethod.POST, "/pets")
				.addOperation(HttpMethod.GET, "/pets/{petId}")
				.addOperation(HttpMethod.DELETE, "/pets/{petId}");
	}

	@Test // you can use smth else instead of JUnit to run tests
	public void validateLatestApiDocCompatibilityWithSnapshot() {
		RemoteEndpointApiSnapshotTestHelper snapshotTestHelper = new RemoteEndpointApiSnapshotTestHelper(
				"http://remote-service1.com:8080/api-doc.json", // url that should be used to obtain latest api-doc
				new BasicHeader("Authorization", "Basic encodedUserAndPassword"), // what ever authorization header is used
				new File("./target/remoteEndpointApiDocs/remote-service1.json"), // could be temporary file, that is deleted right after test finishes
				new File("./src/test/resources/expectedEndpointsSnapshots/remote-service1.json"), // store this somewhere, for example in the project git repository
				getUsedOperations()
		);
		snapshotTestHelper.validateLatestApiDocCompatibilityWithSnapshot();
	}

}
```
As seen from the code, you should:
1. Define operations of the remote service, that are used by the consuming project.
1. Create instance of RemoteEndpointApiSnapshotTestHelper with
   1. url of the remote service api-doc
   1. authentication header for accessing api-doc
   1. file, where the remote api-doc should be saved (could be useful for diffing with snapshot when checking differences)
   1. file, that should contain the expected snapshot of the api-doc (will be created when it doesn't exist, store it in project git for next test executions)
   1. used operations of the remote service
1. Run compatibility check
1. If test failed, then
   1. inspect the failure message
   1. update your implementation (or notify remote service provider about breaking change - perhaps it was accidental)
   1. update snapshot: you could either
      1. update it manually...
      1. ...or update it automatically:
         1. delete the snapshot file
         1. rerun test to recreate the snapshot file
         1. diff the snapshot file with updated file (for example from git history)


## OpenApi Snapshots libraries

This project contains following libraries:
1. [openapi-json-filter](openapi-json-filter) - allows filtering openapi api-doc json file retaining only selected operations (and models used by those operations)
2. [openapi-snapshots-test](openapi-snapshots-test) - contains helper class for openapi api-doc snapshot tests from the consumer perspective:
verifies that all operations and model definitions of operations in snapshot api-doc file
(created using openapi-json-filter during first run, if it was missing)
are compatible with the latest api-doc of the same service.
[Assertj-swagger](https://github.com/RobWin/assertj-swagger) is used internally to check compatibility between the latest and snapshot of api-doc.

## Installation

To install the library using Maven, add the [JitPack](https://jitpack.io/) repository and openapi-snapshots-test dependency:

```xml
...
<repositories>
	<repository>
			<id>jitpack.io</id>
			<url>https://jitpack.io</url>
	</repository>
</repositories>
...
<dependencies>
	<dependency>
		<groupId>com.github.atsu85.openapi-snapshots</groupId>
		<artifactId>openapi-snapshots-test</artifactId><!-- or "openapi-json-filter" if you just want to filter some operations  -->
		<version>REPLACE ME!!!</version><!-- see notes bellow to get either snapshot or specific commit or tag or other version -->
		<scope>test</scope>
	</dependency>
</dependencies>
...
```

> Note, artifacts for this project are built automatically by [JitPack](https://jitpack.io/docs/#how-to) based on the github repository.

> Note, if You are only interested in filtering some operations from existing api-doc json file, You can use `openapi-json-filter` instead of `openapi-snapshots-test` as the artifact id.

> Note, version can be replaced with
* either any [released version of this project](../../releases)
* or any [git tag of this project](../../tags)
* or any [git commit hash](../../commits/master)
* or with `master-SNAPSHOT` - to indicate the latest commit of master branch (NB! Dependency managers, such as Maven cache SNAPSHOTs by default, see [JitPack documentation](https://jitpack.io/docs/#snapshots))
