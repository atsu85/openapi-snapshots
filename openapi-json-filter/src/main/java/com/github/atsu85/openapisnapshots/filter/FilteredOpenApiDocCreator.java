package com.github.atsu85.openapisnapshots.filter;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.atsu85.openapisnapshots.filter.util.CustomMatchers;
import com.google.common.collect.Sets;

import io.swagger.models.HttpMethod;
import io.swagger.models.Swagger;

public class FilteredOpenApiDocCreator {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	/**
	 * @param fullApiDoc                   - original/full api-doc json file, that must contain selected operations
	 * @param selectedOperationsApiDocFile - output file, that will contain selected operations
	 * @param selectedOperations           -
	 * @param swagger
	 */
	public void writeUsedEndpointsSnapshotsFile(File fullApiDoc, File selectedOperationsApiDocFile, SelectedOperations selectedOperations, Swagger swagger) {
		Set<String> usedModelNames = getUsedModelNames(selectedOperations, swagger);

		try {
			ObjectNode jsonNode = readFromFile(fullApiDoc);
			removeUnusedJsonNodes(jsonNode, selectedOperations, usedModelNames);
			writeToFile(jsonNode, selectedOperationsApiDocFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Set<String> getUsedModelNames(SelectedOperations selectedOperations, Swagger swagger) {
		SwaggerApiSelectedOperationsModelsResolver apiDocHelper = new SwaggerApiSelectedOperationsModelsResolver(swagger);
		return apiDocHelper.getModelNamesFromPaths(selectedOperations);
	}

	private void removeUnusedJsonNodes(ObjectNode jsonNode, SelectedOperations selectedOperations, Set<String> usedModelNames) {
		// remove unused paths (including all operations of that path)
		ObjectNode pathsNode = removeUnusedPaths(jsonNode, selectedOperations.getPaths());

		// remove unused operations of the same path where some http methods correspond to operations that are used
		removeUnusedOperationsFromPathsHavingUsedOperations(pathsNode, selectedOperations);

		// remove unused model definitions
		removeUnusedModelDefinitions(jsonNode, usedModelNames);
	}

	private ObjectNode readFromFile(File fullApiDoc) throws IOException {
		return (ObjectNode) OBJECT_MAPPER.readTree(fullApiDoc);
	}

	private void writeToFile(ObjectNode jsonNode, File selectedOperationsApiDocFileFile) throws IOException {
		OBJECT_MAPPER
				.writerWithDefaultPrettyPrinter()
				.writeValue(selectedOperationsApiDocFileFile, jsonNode);
	}

	private ObjectNode removeUnusedPaths(ObjectNode jsonNode, Set<String> usedEndpointPaths) {
		ObjectNode pathsNode = (ObjectNode) jsonNode.get("paths");
		removeFieldsExcept(pathsNode, usedEndpointPaths);
		return pathsNode;
	}

	private void removeUnusedModelDefinitions(ObjectNode jsonNode, Set<String> usedModelNames) {
		ObjectNode definitionsNode = (ObjectNode) jsonNode.get("definitions");
		removeFieldsExcept(definitionsNode, usedModelNames);
	}

	private void removeUnusedOperationsFromPathsHavingUsedOperations(ObjectNode pathsNode, SelectedOperations usedOperations) {
		Iterator<Map.Entry<String, JsonNode>> pathsIt = pathsNode.fields();
		while (pathsIt.hasNext()) {
			Map.Entry<String, JsonNode> httpPathsByPathName = pathsIt.next();
			String pathName = httpPathsByPathName.getKey();
			ObjectNode pathNode = (ObjectNode) httpPathsByPathName.getValue();

			Set<String> usedHttpMethods = usedOperations.getHttpMethods(pathName).stream()
					.map(HttpMethod::name)
					.map(String::toLowerCase)
					.collect(Collectors.toSet());

			removeFieldsExcept(pathNode, usedHttpMethods);
		}
	}

	private void removeFieldsExcept(ObjectNode pathsNode, Set<String> fieldNamesToPreserve) {
		Set<String> unusedFieldss = getOtherFields(pathsNode, fieldNamesToPreserve);
		pathsNode.remove(unusedFieldss);
	}

	private Set<String> getOtherFields(ObjectNode definitionsNode, Collection<String> fieldsToPreserve) {
		Set<String> allFieldNames = Sets.newHashSet(definitionsNode.fieldNames());
		assertThat(allFieldNames, CustomMatchers.hasAtLeastItemsInAnyOrder(fieldsToPreserve));
		Set<String> fieldsToRemove = Sets.newHashSet(allFieldNames);
		fieldsToRemove.removeAll(fieldsToPreserve);
		return fieldsToRemove;
	}

}
