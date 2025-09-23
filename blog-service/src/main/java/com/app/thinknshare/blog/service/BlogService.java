package com.app.thinknshare.blog.service;

import com.app.thinknshare.blog.dto.BlogRequest;
import com.app.thinknshare.blog.dto.BlogResponse;
import com.app.thinknshare.blog.entity.Blog;
import com.app.thinknshare.blog.exception.ResourceNotFoundException;
import com.app.thinknshare.blog.mapper.BlogMapper;
import com.app.thinknshare.blog.repository.BlogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BlogService {
    private final BlogRepository blogRepository;

    /**
     *
     *
     * @param blogRequest
     * @return
     */
    public BlogResponse createBlog(BlogRequest blogRequest) {
        Blog newBlog = new Blog();

        newBlog.setTitle(blogRequest.getTitle());
        newBlog.setContent(blogRequest.getContent());
        newBlog.setAuthorId(101L);
        newBlog.setCreatedAt(LocalDateTime.now());

        blogRepository.save(newBlog);

        return BlogMapper.toResponse(newBlog);
    }

    /**
     *
     *
     * @param blogId
     * @return
     */
    public BlogResponse getBlog(Long blogId) {
        Blog existingBlog = blogRepository.findById(blogId).orElseThrow(() ->
            new ResourceNotFoundException(
                    String.format("The blog with id %d doesn't exists!", blogId)
            )
        );

        return BlogMapper.toResponse(existingBlog);
    }

    /**
     *
     * @param blogRequest
     * @return
     */
    public BlogResponse updateBlog(BlogRequest blogRequest, Long blogId) {
        Blog existingBlog = blogRepository.findById(blogId).orElseThrow(() -> new ResourceNotFoundException(
                String.format("The blog with id %d doesn't exists!", blogId)
        ));

        existingBlog.setTitle(blogRequest.getTitle());
        existingBlog.setContent(blogRequest.getContent());

        blogRepository.save(existingBlog);

        return BlogMapper.toResponse(existingBlog);
    }

    /**
     *
     * @param blogId
     * @return
     */
    public void deleteBlog(Long blogId) {
        Blog existingBlog = blogRepository.findById(blogId).orElseThrow(() -> new ResourceNotFoundException(
                String.format("The blog with id %d doesn't exists!", blogId)
        ));

        blogRepository.deleteById(blogId);
    }
}
