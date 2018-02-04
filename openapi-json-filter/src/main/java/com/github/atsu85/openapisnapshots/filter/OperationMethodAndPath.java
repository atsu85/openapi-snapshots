package com.github.atsu85.openapisnapshots.filter;

import lombok.AllArgsConstructor;
import lombok.Getter;

import io.swagger.models.HttpMethod;

@Getter
@AllArgsConstructor
public class OperationMethodAndPath {
	private HttpMethod method;
	private String path;
}
