package com.github.atsu85.openapisnapshots.test;

import java.io.File;
import java.io.IOException;

import lombok.AllArgsConstructor;

import org.apache.http.Header;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.atsu85.openapisnapshots.filter.FilteredOpenApiDocCreator;
import com.github.atsu85.openapisnapshots.filter.SelectedOperations;

import io.github.robwin.swagger.test.SwaggerAssert;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;

@AllArgsConstructor
public class RemoteEndpointApiSnapshotTestHelper {
	private final String latestApiDocUrl;
	private final Header authHeader;

	/**
	 * @return file that should be used to store content downloaded from {@link #latestApiDocUrl}
	 */
	private final File latestApiDocFile;

	private final File expectedEndpointsSnapshotFile;

	private final SelectedOperations usedEndpoints;

	private final FilteredOpenApiDocCreator filteredOpenApiDocCreator = new FilteredOpenApiDocCreator();

	public void validateLatestApiDocCompatibilityWithSnapshot() {
		DownloadUtil.downloadToFile(latestApiDocUrl, authHeader, latestApiDocFile);

		String latestApiDocPath = latestApiDocFile.getAbsolutePath();
		Swagger swagger = new SwaggerParser().read(latestApiDocPath);
		MatcherAssert.assertThat("Failed to parse Swagger api-doc model. Check\n"
						+ "\t1) " + latestApiDocPath + "\n"
						+ "\t2) " + latestApiDocUrl,
				swagger,
				Matchers.notNullValue()
		);

		File expectedEndpointsSnapshotFile = getSnapshotFileOrCreateWhenMissing(latestApiDocFile, swagger);

		checkSnapshotContainsAllUsedEndpoints(expectedEndpointsSnapshotFile);

		new SwaggerAssert(swagger)
				.satisfiesContract(expectedEndpointsSnapshotFile.getPath());
	}

	private void checkSnapshotContainsAllUsedEndpoints(File endpointsSnapshotFile) {
		// TODO
		try {
			ObjectNode jsonNode = filteredOpenApiDocCreator.readFromFile(endpointsSnapshotFile);
			ObjectNode pathsNode = (ObjectNode) jsonNode.get("paths");
			for (JsonNode pathNode : pathsNode) {
				ObjectNode pathONode = (ObjectNode) pathNode;
				System.out.println("pathNode" + pathONode);
			}

			//			pathsNode.forEach((JsonNode pathNode) -> System.out.println(pathsNode));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private File getSnapshotFileOrCreateWhenMissing(File latestApiDocFile, Swagger swagger) {
		if (!expectedEndpointsSnapshotFile.isFile()) {
			updateUsedEndpointsSnapshotsFileFromApiDoc(latestApiDocFile, swagger);
			MatcherAssert.assertThat(expectedEndpointsSnapshotFile.isFile(), Matchers.equalTo(true));
		}
		return expectedEndpointsSnapshotFile;
	}

	private void updateUsedEndpointsSnapshotsFileFromApiDoc(File fullApiDocFile, Swagger swagger) {
		filteredOpenApiDocCreator.writeUsedEndpointsSnapshotsFile(fullApiDocFile, expectedEndpointsSnapshotFile, usedEndpoints, swagger);
	}

}
