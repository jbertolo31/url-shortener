package com.jasonbertolo.urlshortener.api.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.mongodb.client.result.DeleteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.repository.init.Jackson2ResourceReader;
import org.springframework.data.repository.init.ResourceReader;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * A {@link ReactiveMongoRepository} populator that will read json from test resources, map it into objects and then
 * saves to the database. Reads JSON data, not BSON data. JSON files are mapped with {@link ObjectMapper}. Operates on
 * a single class at a time.
 */
public class ReactiveMongoRepositoryPopulator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveMongoRepositoryPopulator.class);

    private static final ResourceReader READER = new Jackson2ResourceReader(
            new Jackson2ObjectMapperBuilder()
                    .annotationIntrospector(new JacksonIgnoreIntrospector())
                    .build()
    );

    private final ReactiveMongoOperations reactiveMongoOperations;
    private final Class<?> typeParameterClass;
    private final ClassPathResource classPathResource;

    /**
     * Create a new ReactiveMongoRepositoryPopulator.
     *
     * @param reactiveMongoOperations ReactiveMongoOperations bean.
     * @param typeParameterClass The class you would like to populate data for.
     * @param jsonFilePath The test resource path. Eg. /data/samples.json.
     * @param clazz Classloader class used to for {@link ClassPathResource#ClassPathResource(String, Class)}
     */
    public ReactiveMongoRepositoryPopulator(ReactiveMongoOperations reactiveMongoOperations, Class<?> typeParameterClass,
                                            String jsonFilePath, @Nullable Class<?> clazz) {
        this.reactiveMongoOperations = reactiveMongoOperations;
        this.typeParameterClass = typeParameterClass;
        classPathResource = new ClassPathResource(jsonFilePath, clazz);
    }

    /**
     * Populate the database with objects mapped from resources.
     *
     * @return An empty Mono. Errors are resumed, check logging output.
     */
    public Mono<Void> populate() {
        return readFromResource(classPathResource)
                .switchIfEmpty(Mono.error(new IllegalStateException("Failed to read from resource")))
                .flatMapMany(object -> Flux.fromIterable(((Collection<?>) object)).flatMap(this::save))
                .onErrorResume(e -> {
                    LOGGER.error("Error populating resource, skipping.", e);
                    return Mono.empty();
                })
                .then();
    }

    /**
     * Delete all documents with a specific type.
     *
     * @return The result.
     */
    public Mono<DeleteResult> flushDb() {
        return reactiveMongoOperations.remove(new Query(), typeParameterClass);
    }

    /**
     * Get the {@link ReactiveMongoOperations} associated with this populator.
     */
    public ReactiveMongoOperations getReactiveMongoOperations() {
        return reactiveMongoOperations;
    }

    /**
     * Get the class associated with this populator.
     */
    public Class<?> getTypeClass() {
        return typeParameterClass;
    }

    /**
     * Get the resource path for the data associated with this populator.
     */
    public String getResourcePath() {
        return classPathResource.getPath();
    }

    private Mono<Object> readFromResource(Resource resource) {
        LOGGER.debug("Reading resource: {}", resource);
        try {
            return Mono.fromCallable(() -> READER.readFrom(resource, null));
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Failed to read from object.", e));
        }
    }

    private Mono<Object> save(Object object) {
        if (object == null) {
            return Mono.error(new RuntimeException("Cannot save object, it was null"));
        }
        LOGGER.debug("Saving {}", object);
        return reactiveMongoOperations.save(object);
    }

    /**
     * A {@link JacksonAnnotationIntrospector} that will ignore {@link JsonProperty.Access} annotations and
     * {@link JsonIgnore}.
     */
    private static final class JacksonIgnoreIntrospector extends JacksonAnnotationIntrospector {
        @Override
        public JsonProperty.Access findPropertyAccess(Annotated annotated) {
            JsonProperty.Access access = super.findPropertyAccess(annotated);
            if (access == JsonProperty.Access.READ_ONLY
                    || access == JsonProperty.Access.WRITE_ONLY
                    || super._isIgnorable(annotated)) {
                return JsonProperty.Access.AUTO;
            }
            return access;
        }
    }
}
