package com.store.store.controller;

import com.store.store.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * The DummyController class provides RESTful API endpoints for handling user-related operations.
 * It is mainly used for creating users, handling headers, searching users through
 * different query parameters, and retrieving user or user post information.
 *
 * @author Kardigué
 * @version 3.0
 * @since 2025-10-01
 */
@RestController
@RequestMapping("api/v1/dummy")
@Validated
public class DummyController {

    /**
     * Handles the creation of a new user using the details provided in the request body.
     *
     * @param userDto the user details provided in the request body, encapsulated in a UserDto object
     * @return a success message indicating the user has been created successfully
     */
    @Operation(summary = "Create a new user")
    @PostMapping("/create-user")
    public String createUser(@RequestBody UserDto userDto) {
        System.out.println(userDto);
        return "User created successfully";
    }

    /**
     * Handles the creation of a new user as described in the request body and headers.
     * This method extracts the user details and headers from the provided {@code RequestEntity}
     * and processes the user creation request.
     *
     * @param requestEntity represents the HTTP request entity, which contains the user details
     *                       (in the form of {@code UserDto}) as well as the headers.
     * @return a success message indicating the user has been created successfully.
     */
    @Operation(summary = "Create a new user using a RequestEntity")
    @PostMapping("/request-entity")
    public String createUserWithEntity(RequestEntity<UserDto> requestEntity) {
        HttpHeaders header = requestEntity.getHeaders();
        UserDto userDto = requestEntity.getBody();
        return "User created successfully";
    }

    /**
     * Reads and processes HTTP headers from the incoming request.
     *
     * @param headers the HTTP headers provided in the request, encapsulated in an {@code HttpHeaders} object
     * @return a response string containing a message about the received headers
     */
    @Operation(summary = "Read HTTP headers")
    @GetMapping("/headers")
    public String readHeaders(@RequestHeader HttpHeaders headers) {
        List<String> location= headers.get("User-Location");
        return "Recevied headers with value : " + headers.toString();
    }

    /**
     * Handles the search for a user by their name. If no name is provided, a default name
     * "Guest" is used.
     *
     * @param userName the name of the user to search for, with a minimum length of 5
     *                 characters and a maximum length of 30 characters. This parameter
     *                 is optional and uses "Guest" as the default value if not provided.
     * @return a message indicating that the search for the user with the specified name is in progress.
     */
    @Operation(summary = "Search for a user by name")
    @GetMapping("/search")
    public String searchUser(@Size(min = 5, max = 30) @RequestParam(required = false, defaultValue = "Guest",
            name = "name") String userName) {
        return "Searching for user : " + userName;
    }

    /**
     * Handles multi-parameter search for users based on query parameters provided in the request.
     *
     * @param params a map containing the query parameters where keys represent parameter names
     *               (e.g., "firstName", "lastName") and values represent the corresponding search values.
     * @return a string message detailing the search results based on the provided parameters.
     */
    @Operation(summary = "Search for users based on multiple parameters")
    @GetMapping("/multiple-search")
    public String multipleSearch(@RequestParam Map<String,String> params) {
        return "Searching for user : " + params.get("firstName") + " " + params.get("lastName");
    }

    /**
     * Handles the retrieval of a user and optionally a specific post associated with the user.
     * Supports two paths: one that fetches only the user details and another that fetches both
     * the user and the specific post details.
     *
     * @param id the unique identifier of the user to be retrieved
     * @param postId the optional unique identifier of the post associated with the user;
     *               if not provided, only the user details will be retrieved
     * @return a message indicating the search for the user and optional post is in progress
     */
    @Operation(summary = "Retrieve user and optionally post details")
    @GetMapping({"/user/{userId}/posts/{postId}", "/user/{userId}"})
    public String getUser(@PathVariable(name = "userId") String id,
                          @PathVariable(required = false) String postId) {
        return "Searching for user : " + id + " and post : " + postId;
    }

    /**
     * Retrieves information about a user and optionally a specific post associated with the user.
     * Supports two request paths: one fetching only the user details and another fetching both
     * the user and the specific post details, using path variables provided as a map.
     *
     * @param pathVariables a map containing the path variables where keys represent the variable names
     *                      (e.g., "userId", "postId") and values represent the corresponding values.
     * @return a string message indicating the search for the user and optionally the post is in progress.
     */
    @GetMapping({"/user/map/{userId}/posts/{postId}", "/user/map/{userId}"})
    public String getUserUsingMap(@PathVariable Map<String,String> pathVariables) {
        return "Searching for user : " + pathVariables.get("userId") + " and post : "
                + pathVariables.get("postId");
    }

}
