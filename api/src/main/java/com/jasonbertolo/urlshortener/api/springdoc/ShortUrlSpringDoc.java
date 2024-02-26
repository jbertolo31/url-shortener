package com.jasonbertolo.urlshortener.api.springdoc;

import com.jasonbertolo.urlshortener.api.handler.ShortUrlHandler;
import com.jasonbertolo.urlshortener.api.model.dto.ShortUrlCreateDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@RouterOperations({
        @RouterOperation(method = RequestMethod.POST, path = "/api/v1/shorturl",
                beanClass = ShortUrlHandler.class,
                beanMethod = "createUserShortUrl",
                operation = @Operation(operationId = "createUserShortUrl",
                        summary = "Create a new Short URL",
                        description = "Create a new Short URL from a longer URL.",
                        tags = "short-url",
                        security = @SecurityRequirement(name = "Bearer Token Authentication"),
                        requestBody = @RequestBody(required = true, description = "JSON object with valid OWASP URL",
                                content = @Content(schema = @Schema(implementation = ShortUrlCreateDto.class), examples = {
                                        @ExampleObject(name = "Example 1", description = "A simple URL with params",
                                                value = "{\"url\": \"https://www.example.com?param1=test&param2=abc123\"}"),
                                        @ExampleObject(name = "Example 2", description = "An invalid URL, scheme not supported",
                                                value = "{\"url\": \"gs://bucket/object\"}"),
                                        @ExampleObject(name = "Example 3", description = "An invalid URL, bad format",
                                                value = "{\"url\": \"this is not a url\"}"),
                                })),
                        responses = {
                        @ApiResponse(responseCode = "201", description = "Created, location header set with shortened URL",
                                content = @Content(schema =
                                @Schema(implementation = com.jasonbertolo.urlshortener.api.model.dto.ApiResponse.class),
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        examples = {
                                                @ExampleObject("""
                                                {
                                                    "status": "ok",
                                                    "data": {
                                                        "key": "Zwn5MX",
                                                        "id": "65c7f311b964ce0ace2cbd21",
                                                        "url": "https://app.plex.tv/",
                                                        "created_at": "2024-02-10T22:05:05.220Z",
                                                        "last_updated_at": "2024-02-10T22:05:05.220Z",
                                                        "expires_at": "2029-02-09T22:05:05.220Z",
                                                        "description": "Plex"
                                                    }
                                                }
                                                """)
                                        })),
                        @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
                        @ApiResponse(responseCode = "401", description = "Unautheticated", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Not allowed to perform action", content = @Content),
                        @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content)}
                )),
        @RouterOperation(method = RequestMethod.GET, path = "/api/v1/shorturl/{id}",
                beanClass = ShortUrlHandler.class,
                beanMethod = "getUserShortUrl",
                operation = @Operation(operationId = "getUserShortUrl",
                        summary = "Get a user's ShortUrl",
                        description = "Get a ShortUrl belonging to the requesting user.",
                        tags = "short-url",
                        security = @SecurityRequirement(name = "Bearer Token Authentication"),
                        parameters = {@Parameter(in = ParameterIn.PATH, name = "id", description = "The id of the ShortUrl",
                                example = "65c08e82e86c37711c7dc0ba")},
                        responses = {
                        @ApiResponse(responseCode = "200", description = "ShortUrl response object", content = {
                                @Content(schema =
                                @Schema(implementation = com.jasonbertolo.urlshortener.api.model.dto.ApiResponse.class),
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        examples = {
                                        @ExampleObject("""
                                                {
                                                    "status": "ok",
                                                    "data": {
                                                        "key": "Zwn5MX",
                                                        "id": "65c7f311b964ce0ace2cbd21",
                                                        "url": "https://app.plex.tv/",
                                                        "created_at": "2024-02-10T22:05:05.220Z",
                                                        "last_updated_at": "2024-02-10T22:05:05.220Z",
                                                        "expires_at": "2029-02-09T22:05:05.220Z",
                                                        "description": "Plex"
                                                    }
                                                }
                                                """)
                                        }),
                        }),
                        @ApiResponse(responseCode = "401", description = "Unautheticated", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Not allowed to perform action", content = @Content),
                        @ApiResponse(responseCode = "404", description = "ShortUrl not found", content = @Content),
                        @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content)}
                )),
        @RouterOperation(method = RequestMethod.GET, path = "/api/v1/shorturl",
                beanClass = ShortUrlHandler.class,
                beanMethod = "getUserShortUrls",
                operation = @Operation(operationId = "getUserShortUrls",
                        summary = "List user's ShortUrls",
                        description = "List ShortUrls belonging to the requesting user.",
                        tags = "short-url",
                        security = @SecurityRequirement(name = "Bearer Token Authentication"),
                        parameters = {
                                @Parameter(in = ParameterIn.QUERY, name = "page", description = "The page number for pagination",
                                        example = "0"),
                                @Parameter(in = ParameterIn.QUERY, name = "size", description = "The number of objects per page",
                                        example = "20"),
                                @Parameter(in = ParameterIn.QUERY, name = "sortField", description = "The field to sort on in order",
                                        example = "lastUpdatedAt"),
                                @Parameter(in = ParameterIn.QUERY, name = "sortDirection", description = "Either 'asc' or 'desc'",
                                        example = "desc")},
                        responses = {
                                @ApiResponse(responseCode = "200", description = "ShortUrl response object", content = {
                                        @Content(schema =
                                        @Schema(implementation = com.jasonbertolo.urlshortener.api.model.dto.ApiResponse.class),
                                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                examples = {
                                                        @ExampleObject("""
                                                            {
                                                                "status": "ok",
                                                                "data": {
                                                                    "content": [
                                                                        {
                                                                            "key": "SzWjCB",
                                                                            "id": "65c5ca08a1b4ae7a6fd725c6",
                                                                            "url": "https://getbootstrap.com/docs/5.3/components/toasts/",
                                                                            "created_at": "2024-02-09T06:45:28.563Z",
                                                                            "last_updated_at": "2024-02-09T06:45:28.563Z",
                                                                            "expires_at": "2029-02-08T06:45:28.562Z"
                                                                        },
                                                                        {
                                                                            "key": "HyPNsj",
                                                                            "id": "65c5ca0ca1b4ae7a6fd725c7",
                                                                            "url": "https://getbootstrap.com/docs/5.3/components/toasts/",
                                                                            "created_at": "2024-02-09T06:45:32.591Z",
                                                                            "last_updated_at": "2024-02-09T06:45:32.591Z",
                                                                            "expires_at": "2029-02-08T06:45:32.590Z"
                                                                        },
                                                                        {
                                                                            "key": "sy9iK2",
                                                                            "id": "65c7f30cb964ce0ace2cbd19",
                                                                            "url": "https://app.plex.tv/",
                                                                            "created_at": "2024-02-10T22:05:00.208Z",
                                                                            "last_updated_at": "2024-02-10T22:05:00.208Z",
                                                                            "expires_at": "2029-02-09T22:05:00.197Z",
                                                                            "description": "Plex"
                                                                        }
                                                                    ],
                                                                    "pageable": {
                                                                        "page_number": 0,
                                                                        "page_size": 3,
                                                                        "sort": {
                                                                            "empty": true,
                                                                            "sorted": false,
                                                                            "unsorted": true
                                                                        },
                                                                        "offset": 0,
                                                                        "paged": true,
                                                                        "unpaged": false
                                                                    },
                                                                    "total_pages": 5,
                                                                    "total_elements": 13,
                                                                    "last": false,
                                                                    "size": 3,
                                                                    "number": 0,
                                                                    "sort": {
                                                                        "empty": true,
                                                                        "sorted": false,
                                                                        "unsorted": true
                                                                    },
                                                                    "number_of_elements": 3,
                                                                    "first": true,
                                                                    "empty": false
                                                                }
                                                            }
                                                                """)
                                                }),
                                }),
                                @ApiResponse(responseCode = "401", description = "Unautheticated", content = @Content),
                                @ApiResponse(responseCode = "403", description = "Not allowed to perform action", content = @Content),
                                @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content)}
                )),
        @RouterOperation(method = RequestMethod.DELETE, path = "/api/v1/shorturl/{id}",
                beanClass = ShortUrlHandler.class,
                beanMethod = "deleteUserShortUrl",
                operation = @Operation(operationId = "deleteUserShortUrl",
                        summary = "Delete a user's ShortUrl",
                        description = "Delete a user's ShortUrl object and remove it from cache.",
                        tags = "short-url",
                        security = @SecurityRequirement(name = "Bearer Token Authentication"),
                        parameters = {@Parameter(in = ParameterIn.PATH, name = "id", description = "The id of the ShortUrl",
                                example = "65c08e82e86c37711c7dc0ba")},
                        responses = {
                                @ApiResponse(responseCode = "204", description = "ShortUrl empty body", content = @Content),
                                @ApiResponse(responseCode = "401", description = "Unautheticated", content = @Content),
                                @ApiResponse(responseCode = "403", description = "Required authorities not met", content = @Content),
                                @ApiResponse(responseCode = "404", description = "ShortUrl not found", content = @Content),
                                @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content)}
                )),
        @RouterOperation(method = RequestMethod.GET, path = "/api/v1/cache/{key}",
                beanClass = ShortUrlHandler.class,
                beanMethod = "getAndCacheShortUrl",
                operation = @Operation(operationId = "getAndCacheShortUrl",
                        summary = "[INTERNAL] Get and cache any ShortUrl",
                        description = "[INTERNAL] Get any ShortUrl and cache. Requires Client Credentials.",
                        tags = "cache",
                        security = @SecurityRequirement(name = "Client Credentials"),
                        parameters = {@Parameter(in = ParameterIn.PATH, name = "key", description = "The key of the ShortUrl",
                                example = "Zwn5MX")},
                        responses = {
                                @ApiResponse(responseCode = "200", description = "ShortUrl response object", content = {
                                        @Content(schema =
                                        @Schema(implementation = com.jasonbertolo.urlshortener.api.model.dto.ApiResponse.class),
                                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                examples = {
                                                        @ExampleObject("""
                                                {
                                                    "status": "ok",
                                                    "data": {
                                                        "key": "Zwn5MX",
                                                        "id": "65c7f311b964ce0ace2cbd21",
                                                        "url": "https://app.plex.tv/",
                                                        "created_at": "2024-02-10T22:05:05.220Z",
                                                        "last_updated_at": "2024-02-10T22:05:05.220Z",
                                                        "expires_at": "2029-02-09T22:05:05.220Z",
                                                        "description": "Plex"
                                                    }
                                                }
                                                """)
                                                }),
                                }),
                                @ApiResponse(responseCode = "401", description = "Unautheticated", content = @Content),
                                @ApiResponse(responseCode = "403", description = "Not allowed to perform action", content = @Content),
                                @ApiResponse(responseCode = "404", description = "ShortUrl not found", content = @Content),
                                @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content)}
                )),
})
public @interface ShortUrlSpringDoc {}
