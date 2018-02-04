package com.github.atsu85.openapisnapshots.filter;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import io.swagger.models.HttpMethod;

public class SelectedOperations {
	private final Multimap<String, HttpMethod> operatonsByPaths = HashMultimap.create();

	Map<String, Collection<HttpMethod>> getOperatonsByPaths() {
		return operatonsByPaths.asMap();
	}

	Set<String> getPaths() {
		return getOperatonsByPaths().keySet();
	}

	public Collection<HttpMethod> getHttpMethods(String path) {
		return getOperatonsByPaths().get(path);
	}

	public SelectedOperations addOperations(Collection<OperationMethodAndPath> operations) {
		for (OperationMethodAndPath operation : operations) {
			addOperation(operation);
		}
		return this;
	}

	public SelectedOperations addOperation(OperationMethodAndPath operationMethodAndPath) {
		return addOperation(operationMethodAndPath.getMethod(), operationMethodAndPath.getPath());
	}

	public SelectedOperations addOperation(HttpMethod httpMethod, String path) {
		operatonsByPaths.put(path, httpMethod);
		return this;
	}

}
