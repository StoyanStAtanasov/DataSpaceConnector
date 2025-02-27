/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.core.system;

import org.eclipse.dataspaceconnector.core.BaseExtension;
import org.eclipse.dataspaceconnector.core.CoreExtension;
import org.eclipse.dataspaceconnector.core.util.TopologicalSort;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ConfigurationExtension;
import org.eclipse.dataspaceconnector.spi.system.Feature;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.Requires;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base service extension context.
 * <p>Prior to using, {@link #initialize()} must be called.</p>
 */
public class DefaultServiceExtensionContext implements ServiceExtensionContext {
    private final Monitor monitor;
    private final TypeManager typeManager;

    private final Map<Class<?>, Object> services = new HashMap<>();
    private final ServiceLocator serviceLocator;
    private List<ConfigurationExtension> configurationExtensions;
    private String connectorId;

    public DefaultServiceExtensionContext(TypeManager typeManager, Monitor monitor) {
        this(typeManager, monitor, new ServiceLocatorImpl());
    }

    public DefaultServiceExtensionContext(TypeManager typeManager, Monitor monitor, ServiceLocator serviceLocator) {
        this.typeManager = typeManager;
        this.monitor = monitor;
        this.serviceLocator = serviceLocator;
        // register as services
        registerService(TypeManager.class, typeManager);
        registerService(Monitor.class, monitor);
    }

    @Override
    public String getConnectorId() {
        return connectorId;
    }

    @Override
    public Monitor getMonitor() {
        return monitor;
    }

    @Override
    public TypeManager getTypeManager() {
        return typeManager;
    }

    /**
     * Attempts to resolve the setting by delegating to configuration extensions, VM properties, and then env variables, in that order; otherwise
     * the default value is returned.
     */
    @Override
    public String getSetting(String key, String defaultValue) {
        String value;
        for (ConfigurationExtension extension : configurationExtensions) {
            value = extension.getSetting(key);
            if (value != null) {
                return value;
            }
        }
        value = System.getProperty(key);
        if (value != null) {
            return value;
        }
        value = System.getenv(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public <T> boolean hasService(Class<T> type) {
        return services.containsKey(type);
    }

    @Override
    public <T> T getService(Class<T> type) {
        T service = (T) services.get(type);
        if (service == null) {
            throw new EdcException("Service not found: " + type.getName());
        }
        return service;
    }

    @Override
    public <T> T getService(Class<T> type, boolean isOptional) {
        if (!isOptional) {
            return getService(type);
        }
        return (T) services.get(type);
    }

    @Override
    public <T> void registerService(Class<T> type, T service) {
        if (hasService(type)) {
            monitor.warning("A service of the type " + type.getCanonicalName() + " was already registered and has now been replaced");
        }
        services.put(type, service);
    }

    @Override
    public List<InjectionContainer<ServiceExtension>> loadServiceExtensions() {
        List<ServiceExtension> serviceExtensions = loadExtensions(ServiceExtension.class, true);

        //the first sort is only to verify that there are no "upward" dependencies from PRIMORDIAL -> DEFAULT
        var sips = sortExtensions(serviceExtensions);

        return Collections.unmodifiableList(sips);
    }

    @Override
    public <T> List<T> loadExtensions(Class<T> type, boolean required) {
        return serviceLocator.loadImplementors(type, required);
    }

    @Override
    public <T> T loadSingletonExtension(Class<T> type, boolean required) {
        return serviceLocator.loadSingletonImplementor(type, required);
    }

    @Override
    public void initialize() {
        configurationExtensions = loadExtensions(ConfigurationExtension.class, false);
        configurationExtensions.forEach(ext -> {
            ext.initialize(monitor);
            monitor.info("Initialized " + ext.name());
        });
        connectorId = getSetting("edc.connector.name", "edc-" + UUID.randomUUID());
    }

    private List<InjectionContainer<ServiceExtension>> sortExtensions(List<ServiceExtension> loadedExtensions) {
        Map<String, List<ServiceExtension>> dependencyMap = new HashMap<>();
        addDefaultExtensions(loadedExtensions);

        // add all provided features to the dependency map
        loadedExtensions.forEach(ext -> getProvidedFeatures(ext).forEach(feature -> dependencyMap.computeIfAbsent(feature, k -> new ArrayList<>()).add(ext)));
        var sort = new TopologicalSort<ServiceExtension>();

        // check if all injected fields are satisfied, collect missing ones and throw exception otherwise
        var unsatisfiedInjectionPoints = new ArrayList<InjectionPoint<ServiceExtension>>();
        var injectionPoints = loadedExtensions.stream().flatMap(ext -> getInjectedFields(ext).stream().peek(injectionPoint -> {
            List<ServiceExtension> dependencies = dependencyMap.get(injectionPoint.getFeatureName());
            if (dependencies == null) {
                unsatisfiedInjectionPoints.add(injectionPoint);
            } else {
                dependencies.forEach(dependency -> sort.addDependency(ext, dependency));
            }
        })).collect(Collectors.toList());

        if (!unsatisfiedInjectionPoints.isEmpty()) {
            var string = "The following injected fields were not provided:\n";
            string += unsatisfiedInjectionPoints.stream().map(InjectionPoint::toString).collect(Collectors.joining("\n"));
            throw new EdcInjectionException(string);
        }

        //check that all the @Required features are there
        var unsatisfiedRequirements = new ArrayList<String>();
        loadedExtensions.forEach(ext -> {
            var features = getRequiredFeatures(ext.getClass());
            features.forEach(feature -> {
                var dependencies = dependencyMap.get(feature);
                if (dependencies == null) {
                    unsatisfiedRequirements.add(feature);
                } else {
                    dependencies.forEach(dependency -> sort.addDependency(ext, dependency));
                }
            });
        });

        if (!unsatisfiedRequirements.isEmpty()) {
            var string = String.format("The following @Require'd features were not provided: [%s]", String.join(", ", unsatisfiedRequirements));
            throw new EdcException(string);
        }

        sort.sort(loadedExtensions);

        // todo: should the list of InjectionContainers be generated directly by the flatmap?
        // convert the sorted list of extensions into an equally sorted list of InjectionContainers
        return loadedExtensions.stream().map(se -> new InjectionContainer<>(se, injectionPoints.stream().filter(ip -> ip.getInstance() == se).collect(Collectors.toSet()))).collect(Collectors.toList());
    }

    private Set<String> getRequiredFeatures(Class<?> clazz) {
        var requiresAnnotation = clazz.getAnnotation(Requires.class);
        if (requiresAnnotation != null) {
            var features = requiresAnnotation.value();
            return Stream.of(features).map(this::getFeatureValue).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    /**
     * Handles core-, transfer- and contract-extensions and inserts them at the beginning of the list so that
     * explicit @Requires annotations are not necessary
     */
    private void addDefaultExtensions(List<ServiceExtension> loadedExtensions) {
        var baseDependencies = loadedExtensions.stream().filter(e -> e.getClass().getAnnotation(BaseExtension.class) != null).collect(Collectors.toList());
        if (baseDependencies.isEmpty()) {
            throw new EdcException("No base dependencies were found on the classpath. Please add the \"core:base\" module to your classpath!");
        }
        var coreDependencies = loadedExtensions.stream().filter(e -> e.getClass().getAnnotation(CoreExtension.class) != null).collect(Collectors.toList());

        //make sure the core- and transfer-dependencies are ALWAYS ordered first
        loadedExtensions.removeAll(baseDependencies);
        loadedExtensions.removeAll(coreDependencies);
        coreDependencies.forEach(se -> loadedExtensions.add(0, se));
        baseDependencies.forEach(se -> loadedExtensions.add(0, se));
    }

    /**
     * Obtains all features a specific extension provides as strings
     */
    private Set<InjectionPoint<ServiceExtension>> getInjectedFields(ServiceExtension ext) {
        // initialize with legacy list

        var injectFields = Arrays.stream(ext.getClass().getDeclaredFields()).filter(f -> f.getAnnotation(Inject.class) != null).map(f -> new FieldInjectionPoint<>(ext, f, getFeatureValue(f.getType())));

        return injectFields.collect(Collectors.toSet());
    }

    /**
     * expands parent features (e.g. "some:feature" into child features, if they are proviced, e.g. "some:feature:subfeature"
     *
     * @param requiredFeature The potential parent feature. Could also be the child feature.
     * @param allFeatures     All available features provided by the entirety of all extensions.
     * @return If the given required feature is a parent feature, all sub-features that are in the same namespace are returned. Else, a singleton Set of the required feature itself is returned.
     */
    private Set<String> expand(String requiredFeature, Set<String> allFeatures) {
        return allFeatures.stream().filter(feature -> feature.startsWith(requiredFeature)).collect(Collectors.toSet());
    }


    /**
     * Obtains all features a specific extension requires as strings
     */
    private Set<String> getProvidedFeatures(ServiceExtension ext) {
        var allProvides = new HashSet<String>();

        var providesAnnotation = ext.getClass().getAnnotation(Provides.class);
        if (providesAnnotation != null) {
            var featureStrings = Arrays.stream(providesAnnotation.value()).map(this::getFeatureValue).collect(Collectors.toSet());
            allProvides.addAll(featureStrings);
        }
        return allProvides;
    }

    private String getFeatureValue(Class<?> featureClass) {
        var annotation = featureClass.getAnnotation(Feature.class);
        if (annotation == null) {
            return featureClass.getName();
        }
        return annotation.value();
    }


}
