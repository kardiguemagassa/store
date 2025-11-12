package com.store.store.controller;

import com.store.store.dto.user.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author Kardigué
 * @version 3.0
 * @since 2025-10-01
 */
@RestController
@RequestMapping("api/v1/dummy")
@Validated
public class DummyController {


    @Operation(summary = "Create a new user")
    @PostMapping("/create-user")
    public String createUser(@RequestBody UserDto userDto) {
        System.out.println(userDto);
        return "User created successfully";
    }

    @Operation(summary = "Create a new user using a RequestEntity")
    @PostMapping("/request-entity")
    public String createUserWithEntity(RequestEntity<UserDto> requestEntity) {
        HttpHeaders header = requestEntity.getHeaders();
        UserDto userDto = requestEntity.getBody();
        return "User created successfully";
    }

    @Operation(summary = "Read HTTP headers")
    @GetMapping("/headers")
    public String readHeaders(@RequestHeader HttpHeaders headers) {
        List<String> location= headers.get("User-Location");
        return "Recevied headers with value : " + headers.toString();
    }

    @Operation(summary = "Search for a user by name")
    @GetMapping("/search")
    public String searchUser(@Size(min = 5, max = 30) @RequestParam(required = false, defaultValue = "Guest",
            name = "name") String userName) {
        return "Searching for user : " + userName;
    }

    @Operation(summary = "Search for users based on multiple parameters")
    @GetMapping("/multiple-search")
    public String multipleSearch(@RequestParam Map<String,String> params) {
        return "Searching for user : " + params.get("firstName") + " " + params.get("lastName");
    }

    @Operation(summary = "Retrieve user and optionally post details")
    @GetMapping({"/user/{userId}/posts/{postId}", "/user/{userId}"})
    public String getUser(@PathVariable(name = "userId") String id,
                          @PathVariable(required = false) String postId) {
        return "Searching for user : " + id + " and post : " + postId;
    }

    @GetMapping({"/user/map/{userId}/posts/{postId}", "/user/map/{userId}"})
    public String getUserUsingMap(@PathVariable Map<String,String> pathVariables) {
        return "Searching for user : " + pathVariables.get("userId") + " and post : " + pathVariables.get("postId");
    }

}
