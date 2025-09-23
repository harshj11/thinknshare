package com.app.thinknshare.blog.mapper;

import com.app.thinknshare.blog.dto.BlogResponse;
import com.app.thinknshare.blog.entity.Blog;

public class BlogMapper {

    /**
     *
     * @param blog
     * @return
     */
    public static BlogResponse toResponse(Blog blog) {
        BlogResponse response = new BlogResponse();

        response.setId(blog.getId());
        response.setTitle(blog.getTitle());
        response.setContent(blog.getContent());
        response.setCreatedAt(blog.getCreatedAt());

        return response;
    }

}
