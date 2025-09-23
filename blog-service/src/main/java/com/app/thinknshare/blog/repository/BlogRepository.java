package com.app.thinknshare.blog.repository;

import com.app.thinknshare.blog.entity.Blog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogRepository extends JpaRepository<Blog, Long> {
}