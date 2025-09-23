package com.app.thinknshare.blog.dto;

import com.app.thinknshare.blog.entity.Blog;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlogResponse {
    private Long id;
    private String title;
    private String content;
    private String authorName;
    private LocalDateTime createdAt;
}