package com.github.atsu85.openapisnapshots.test;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class DownloadUtil {

	public static void downloadToFile(String url, Header authHeader, File outFile) {
		CloseableHttpClient httpClient = HttpClients.createDefault();

		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader(authHeader);
		try {
			HttpEntity respEntity = httpClient.execute(httpGet).getEntity();
			if (respEntity != null) {
				FileUtils.copyInputStreamToFile(respEntity.getContent(), outFile);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			httpGet.releaseConnection();
		}
	}

}
