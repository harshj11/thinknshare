package com.app.thinknshare.blog.controller;

import com.app.thinknshare.blog.dto.BlogRequest;
import com.app.thinknshare.blog.dto.BlogResponse;
import com.app.thinknshare.blog.entity.Blog;
import com.app.thinknshare.blog.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/blog")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    @PostMapping
    public ResponseEntity<BlogResponse> createBlog(@RequestBody BlogRequest blogRequest) {
        return new ResponseEntity<>(blogService.createBlog(blogRequest), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BlogResponse> getBlog(@PathVariable("id") Long blogId) {
        return new ResponseEntity<>(blogService.getBlog(blogId), HttpStatus.OK);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BlogResponse> updateBlog(@PathVariable("id") Long blogId, @RequestBody BlogRequest blogRequest) {
        return new ResponseEntity<>(blogService.updateBlog(blogRequest, blogId), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BlogResponse> deleteBlog(@PathVariable("id") Long blogId) {
        blogService.deleteBlog(blogId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
