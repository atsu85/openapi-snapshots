package com.github.atsu85.openapisnapshots.filter;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import lombok.AllArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;

import com.github.atsu85.openapisnapshots.filter.util.CustomMatchers;
import com.google.common.collect.Sets;

import io.swagger.models.HttpMethod;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.AbstractSerializableParameter;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;

@AllArgsConstructor
public class SwaggerApiSelectedOperationsModelsResolver {
	private static final String MODEL_DEFINITION_PREFIX = "#/definitions/";
	private final Swagger swagger;

	public Set<String> getModelNamesFromPaths(SelectedOperations selectedOperations) {
		checkSelectedOperationsPathsExistInGivenSwaggerApiDoc(selectedOperations);

		Set<ModelImpl> modelsUsedByOperations = selectedOperations.getOperatonsByPaths().entrySet().stream()
				.map(operationsByPath -> getRequestAndResponseModels(operationsByPath.getKey(), operationsByPath.getValue()))
				.flatMap(modelDefinitions -> modelDefinitions.stream())
				.collect(toSet());

		Set<ModelImpl> models = getModelsWithTransitiveModels(modelsUsedByOperations);
		return models.stream()
				.map(ModelImpl::getName)
				.collect(toSet());
	}

	private void checkSelectedOperationsPathsExistInGivenSwaggerApiDoc(SelectedOperations selectedOperations) {
		Set<String> selectedPaths = selectedOperations.getPaths();
		Set<String> existingPaths = swagger.getPaths().keySet();
		assertThat("Given Swagger api-doc doesn't include path of all selected operations", existingPaths, CustomMatchers.hasAtLeastItemsInAnyOrder(selectedPaths));
	}

	private Set<ModelImpl> getRequestAndResponseModels(String endpointPath, Collection<HttpMethod> httpOperations) {
		Set<ModelImpl> definitions = new HashSet<>();
		Path path = swagger.getPath(endpointPath);
		for (HttpMethod httpOperation : httpOperations) {
			Operation operation = getOperation(path, httpOperation);
			assertThat(operation, Matchers.notNullValue());
			Set<ModelImpl> requestDefinitions = getModelDefinitionsFromParameters(operation);
			if (HttpMethod.POST.equals(httpOperation)) {
				assertThat(requestDefinitions, Matchers.hasSize(Matchers.equalTo(1)));
			}
			definitions.addAll(requestDefinitions);
			definitions.add(getResponseBodyModel(operation));
		}
		definitions.remove(null);
		return definitions;
	}

	private Set<ModelImpl> getModelsWithTransitiveModels(Set<ModelImpl> models) {
		Set<ModelImpl> results = new HashSet<>(models);
		boolean searchMore;
		do {
			Set<ModelImpl> referencedModels = getReferencedModels(results);
			searchMore = results.addAll(referencedModels);
		} while (searchMore);
		return results;
	}

	private Set<ModelImpl> getReferencedModels(Set<? extends Model> models) {
		return models.stream()
				.map(model -> getProperties(model))
				.flatMap(propsByName -> propsByName.values().stream())
				.map(property -> getReferencedModels(property))
				.flatMap(Set::stream)
				.filter(Objects::nonNull)
				.collect(toSet());
	}

	private Set<ModelImpl> getReferencedModels(Property property) {
		if (property instanceof RefProperty) {
			RefProperty refProperty = (RefProperty) property;
			return Sets.newHashSet(getModel(refProperty));
		}
		if (property instanceof MapProperty) {
			MapProperty mapProperty = (MapProperty) property;
			return getReferencedModels(mapProperty.getAdditionalProperties());
		}
		if (property instanceof ArrayProperty) {
			ArrayProperty arrayProperty = (ArrayProperty) property;
			Property itemProp = arrayProperty.getItems();
			return getReferencedModels(itemProp);
		}
		if (property instanceof ObjectProperty) {
			ObjectProperty objectProperty = (ObjectProperty) property;
			Set<ModelImpl> models = objectProperty.getProperties().values().stream()
					.map(this::getReferencedModels)
					.reduce(new HashSet<>(), (referencedModelsAccumulator, referencedModels) -> {
						referencedModelsAccumulator.addAll(referencedModels);
						return referencedModelsAccumulator;
					});
			if (!models.isEmpty()) {
				throw new RuntimeException("TODO found ObjectProperty that references some models!");
			}
			return models;
		}
		return Collections.emptySet();
	}

	private Set<ModelImpl> getModelDefinitionsFromParameters(Operation postOperation) {
		return postOperation.getParameters().stream()
				.map(this::getModel)
				.filter(Objects::nonNull)
				.collect(toSet());
	}

	private ModelImpl getModel(Parameter param) {
		Model model = null;
		if (param instanceof BodyParameter) {
			BodyParameter requestParam = (BodyParameter) param;
			model = requestParam.getSchema();
		} else if (param instanceof HeaderParameter) {
			AbstractSerializableParameter headerParam = (HeaderParameter) param;
			Property property = headerParam.getItems();
			Set<ModelImpl> models = getReferencedModels(property);
			if (models.size() > 0) {
				assertThat(models, Matchers.hasSize(1));
				model = models.iterator().next();
			}
		} else if (param instanceof PathParameter || param instanceof QueryParameter) {
			return null;
		} else {
			throw new RuntimeException("TODO implement resolving models from paraf of type " + param.getClass());
		}
		if (model instanceof RefModel) {
			model = getModel((RefModel) model);
		}
		return (ModelImpl) model;
	}

	private ModelImpl getResponseBodyModel(Operation operation) {
		Response response = operation.getResponses().get("200");
		Property schema = response.getSchema();
		Set<ModelImpl> models = getReferencedModels(schema);
		if (models.isEmpty()) {
			return null;
		}
		if (models.size() == 1) {
			return models.iterator().next();
		}
		throw new RuntimeException("FIXME: didn't expect to find more than one referenced model from response of operation " + operation);
	}

	private ModelImpl getModel(RefModel model) {
		return getModelByDefinitionRef(model.get$ref());
	}

	private ModelImpl getModel(RefProperty refProperty) {
		String definitionRef = refProperty.get$ref();
		return getModelByDefinitionRef(definitionRef);
	}

	private ModelImpl getModelByDefinitionRef(String definitionRef) {
		assertThat(definitionRef, Matchers.startsWith(MODEL_DEFINITION_PREFIX));
		String definitionName = StringUtils.substringAfter(definitionRef, MODEL_DEFINITION_PREFIX);
		Model model = swagger.getDefinitions().get(definitionName);
		assertThat(model, Matchers.instanceOf(ModelImpl.class));
		ModelImpl modelImpl = (ModelImpl) model;
		if (modelImpl.getName() == null) {
			modelImpl.setName(definitionName);
		} else {
			assertThat(modelImpl.getName(), Matchers.equalTo(definitionName));
		}
		assertThat("didn't find model by ref '" + definitionRef + "'", model, Matchers.notNullValue());
		return modelImpl;
	}

	private Map<String, Property> getProperties(Model model) {
		if (model instanceof RefModel) {
			Model realModel = getModel((RefModel) model);
			return getProperties(realModel);
		}
		Map<String, Property> properties = model.getProperties();
		if (properties == null) {
			return Collections.emptyMap();
		}
		return properties;
	}

	private Operation getOperation(Path path, HttpMethod httpOperation) {
		Operation operation;
		if (HttpMethod.POST.equals(httpOperation)) {
			operation = path.getPost();
		} else if (HttpMethod.GET.equals(httpOperation)) {
			operation = path.getGet();
		} else if (HttpMethod.PUT.equals(httpOperation)) {
			operation = path.getPut();
		} else if (HttpMethod.DELETE.equals(httpOperation)) {
			operation = path.getDelete();
		} else {
			throw new RuntimeException("TODO implement adding definitions from http `" + httpOperation + "` operation");
		}
		return operation;
	}
}
