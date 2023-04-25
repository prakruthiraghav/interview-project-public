package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.CreateUserPayload;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
public class UserController {

    // TODO: wire in the user repository (~ 1 line)
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/user")
    public ResponseEntity<Integer> createUser(@RequestBody CreateUserPayload payload) {
        // TODO: Create an user entity with information given in the payload, store it in the database
        //       and return the id of the user in 200 OK response

        // If user with same email exists, return error
        User userProbe = new User();
        userProbe.setEmail(payload.getEmail());
        Example<User> userExample = Example.of(userProbe, ExampleMatcher.matchingAny());
        if (userRepository.exists(userExample)) {
            ResponseEntity<Integer> response = new ResponseEntity<Integer>(HttpStatus.BAD_REQUEST);
            return response;
        }

        User user = new User();
        user.setName(payload.getName());
        user.setEmail(payload.getEmail());
        userRepository.save(user);

        ResponseEntity<Integer> response = new ResponseEntity<Integer>(user.getId(), HttpStatus.OK);
        return response;
    }

    @DeleteMapping("/user")
    public ResponseEntity<String> deleteUser(@RequestParam int userId) {
        // TODO: Return 200 OK if a user with the given ID exists, and the deletion is successful
        //       Return 400 Bad Request if a user with the ID does not exist
        //       The response body could be anything you consider appropriate

        // Return error if user does not exist
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return new ResponseEntity<String>("User with id: " + userId + " does not exist", HttpStatus.BAD_REQUEST);
        }

        // Return error if user has credit cards associated
        User user = userOptional.get();
        if (!user.getCreditCardList().isEmpty()) {
            return new ResponseEntity<String>("User with associated credit cards cannot be deleted",
                            HttpStatus.BAD_REQUEST);
        }

        userRepository.deleteById(userId);
        return new ResponseEntity<String>("User with id: " + userId + " deleted successfully", HttpStatus.OK);
    }
}
