package com.github.atsu85.openapisnapshots.test;

import java.io.File;

import lombok.AllArgsConstructor;

import org.apache.http.Header;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;

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

		new SwaggerAssert(swagger)
				.satisfiesContract(expectedEndpointsSnapshotFile.getPath());
	}

	private File getSnapshotFileOrCreateWhenMissing(File latestApiDocFile, Swagger swagger) {
		if (!expectedEndpointsSnapshotFile.isFile()) {
			updateUsedEndpointsSnapshotsFileFromApiDoc(latestApiDocFile, swagger);
			MatcherAssert.assertThat(expectedEndpointsSnapshotFile.isFile(), Matchers.equalTo(true));
		}
		return expectedEndpointsSnapshotFile;
	}

	private void updateUsedEndpointsSnapshotsFileFromApiDoc(File fullApiDocFile, Swagger swagger) {
		new FilteredOpenApiDocCreator().writeUsedEndpointsSnapshotsFile(fullApiDocFile, expectedEndpointsSnapshotFile, usedEndpoints, swagger);
	}

}