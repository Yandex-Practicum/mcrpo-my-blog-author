package com.myblog.dao;

import com.myblog.config.DatabaseConfig;
import com.myblog.dao.impl.PostDaoImpl;
import com.myblog.dao.impl.TagDaoImpl;
import com.myblog.model.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {DatabaseConfig.class, PostDaoImpl.class, TagDaoImpl.class})
@Transactional
class PostDaoIntegrationTest {

    @Autowired
    private PostDao postDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM post_images");
        jdbcTemplate.execute("DELETE FROM post_tags");
        jdbcTemplate.execute("DELETE FROM comments");
        jdbcTemplate.execute("DELETE FROM tags");
        jdbcTemplate.execute("DELETE FROM posts");
    }

    @Test
    void testCreateAndFindPost() {
        // Given
        Post post = Post.builder()
            .title("Test Post")
            .text("Test content")
            .tags(Arrays.asList("tag1", "tag2"))
            .build();

        // When
        Post createdPost = postDao.create(post);

        // Then
        assertNotNull(createdPost.getId());
        assertEquals("Test Post", createdPost.getTitle());
        assertEquals(0, createdPost.getLikesCount());
        
        Optional<Post> foundPost = postDao.findById(createdPost.getId());
        assertTrue(foundPost.isPresent());
        assertEquals(createdPost.getId(), foundPost.get().getId());
        assertEquals(2, foundPost.get().getTags().size());
    }

    @Test
    void testFindAllPosts() {
        // Given
        Post post1 = Post.builder()
            .title("First Post")
            .text("First content")
            .tags(Arrays.asList("java"))
            .build();
        postDao.create(post1);

        Post post2 = Post.builder()
            .title("Second Post")
            .text("Second content")
            .tags(Arrays.asList("spring"))
            .build();
        postDao.create(post2);

        // When
        List<Post> posts = postDao.findAll("", 1, 10);

        // Then
        assertEquals(2, posts.size());
    }

    @Test
    void testSearchPostsByTitle() {
        // Given
        Post post1 = Post.builder()
            .title("Java Tutorial")
            .text("Content")
            .tags(Arrays.asList())
            .build();
        postDao.create(post1);

        Post post2 = Post.builder()
            .title("Spring Framework")
            .text("Content")
            .tags(Arrays.asList())
            .build();
        postDao.create(post2);

        // When
        List<Post> posts = postDao.findAll("Java", 1, 10);

        // Then
        assertEquals(1, posts.size());
        assertEquals("Java Tutorial", posts.get(0).getTitle());
    }

    @Test
    void testSearchPostsByTag() {
        // Given
        Post post1 = Post.builder()
            .title("Post 1")
            .text("Content")
            .tags(Arrays.asList("java", "spring"))
            .build();
        postDao.create(post1);

        Post post2 = Post.builder()
            .title("Post 2")
            .text("Content")
            .tags(Arrays.asList("python"))
            .build();
        postDao.create(post2);

        // When
        List<Post> posts = postDao.findAll("#java", 1, 10);

        // Then
        assertEquals(1, posts.size());
        assertEquals("Post 1", posts.get(0).getTitle());
    }

    @Test
    void testUpdatePost() {
        // Given
        Post post = Post.builder()
            .title("Original Title")
            .text("Original content")
            .tags(Arrays.asList("tag1"))
            .build();
        Post createdPost = postDao.create(post);

        // When
        createdPost.setTitle("Updated Title");
        createdPost.setText("Updated content");
        createdPost.setTags(Arrays.asList("tag2", "tag3"));
        Post updatedPost = postDao.update(createdPost);

        // Then
        assertEquals("Updated Title", updatedPost.getTitle());
        assertEquals("Updated content", updatedPost.getText());
        assertEquals(2, updatedPost.getTags().size());
    }

    @Test
    void testDeletePost() {
        // Given
        Post post = Post.builder()
            .title("Test Post")
            .text("Test content")
            .tags(Arrays.asList())
            .build();
        Post createdPost = postDao.create(post);

        // When
        postDao.delete(createdPost.getId());

        // Then
        Optional<Post> foundPost = postDao.findById(createdPost.getId());
        assertFalse(foundPost.isPresent());
    }

    @Test
    void testIncrementLikes() {
        // Given
        Post post = Post.builder()
            .title("Test Post")
            .text("Test content")
            .tags(Arrays.asList())
            .build();
        Post createdPost = postDao.create(post);

        // When
        postDao.incrementLikes(createdPost.getId());
        postDao.incrementLikes(createdPost.getId());

        // Then
        Optional<Post> updatedPost = postDao.findById(createdPost.getId());
        assertTrue(updatedPost.isPresent());
        assertEquals(2, updatedPost.get().getLikesCount());
    }

    @Test
    void testGetTotalCount() {
        // Given
        postDao.create(Post.builder().title("Post 1").text("Content").tags(Arrays.asList()).build());
        postDao.create(Post.builder().title("Post 2").text("Content").tags(Arrays.asList()).build());
        postDao.create(Post.builder().title("Post 3").text("Content").tags(Arrays.asList()).build());

        // When
        int count = postDao.getTotalCount("");

        // Then
        assertEquals(3, count);
    }
}

