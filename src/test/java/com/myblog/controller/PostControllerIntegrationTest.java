package com.myblog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.config.DatabaseConfig;
import com.myblog.config.RootConfig;
import com.myblog.config.WebConfig;
import com.myblog.dto.CreatePostRequest;
import com.myblog.dto.UpdatePostRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class, WebConfig.class, DatabaseConfig.class})
@WebAppConfiguration
@Transactional
class PostControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        
        jdbcTemplate.execute("DELETE FROM post_images");
        jdbcTemplate.execute("DELETE FROM post_tags");
        jdbcTemplate.execute("DELETE FROM comments");
        jdbcTemplate.execute("DELETE FROM tags");
        jdbcTemplate.execute("DELETE FROM posts");
    }

    @Test
    void testGetPosts() throws Exception {
        mockMvc.perform(get("/api/posts")
                .param("search", "")
                .param("pageNumber", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.posts").isArray())
            .andExpect(jsonPath("$.hasPrev").exists())
            .andExpect(jsonPath("$.hasNext").exists())
            .andExpect(jsonPath("$.lastPage").exists());
    }

    @Test
    void testCreatePost() throws Exception {
        CreatePostRequest request = CreatePostRequest.builder()
            .title("New Post")
            .text("New content")
            .tags(Arrays.asList("tag1", "tag2"))
            .build();

        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.title").value("New Post"))
            .andExpect(jsonPath("$.text").value("New content"))
            .andExpect(jsonPath("$.likesCount").value(0))
            .andExpect(jsonPath("$.commentsCount").value(0));
    }

    @Test
    void testGetPostById() throws Exception {
        // Create a post first
        CreatePostRequest request = CreatePostRequest.builder()
            .title("Test Post")
            .text("Test content")
            .tags(Arrays.asList("tag1"))
            .build();

        String response = mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        Long postId = objectMapper.readTree(response).get("id").asLong();

        // Get the post
        mockMvc.perform(get("/api/posts/" + postId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(postId))
            .andExpect(jsonPath("$.title").value("Test Post"));
    }

    @Test
    void testUpdatePost() throws Exception {
        // Create a post first
        CreatePostRequest createRequest = CreatePostRequest.builder()
            .title("Original Title")
            .text("Original content")
            .tags(Arrays.asList("tag1"))
            .build();

        String response = mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        Long postId = objectMapper.readTree(response).get("id").asLong();

        // Update the post
        UpdatePostRequest updateRequest = UpdatePostRequest.builder()
            .id(postId)
            .title("Updated Title")
            .text("Updated content")
            .tags(Arrays.asList("tag2"))
            .build();

        mockMvc.perform(put("/api/posts/" + postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Updated Title"))
            .andExpect(jsonPath("$.text").value("Updated content"));
    }

    @Test
    void testDeletePost() throws Exception {
        // Create a post first
        CreatePostRequest request = CreatePostRequest.builder()
            .title("Post to Delete")
            .text("Content")
            .tags(Arrays.asList())
            .build();

        String response = mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        Long postId = objectMapper.readTree(response).get("id").asLong();

        // Delete the post
        mockMvc.perform(delete("/api/posts/" + postId))
            .andExpect(status().isOk());

        // Verify it's deleted
        mockMvc.perform(get("/api/posts/" + postId))
            .andExpect(status().isNotFound());
    }

    @Test
    void testIncrementLikes() throws Exception {
        // Create a post first
        CreatePostRequest request = CreatePostRequest.builder()
            .title("Post with Likes")
            .text("Content")
            .tags(Arrays.asList())
            .build();

        String response = mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        Long postId = objectMapper.readTree(response).get("id").asLong();

        // Increment likes
        mockMvc.perform(post("/api/posts/" + postId + "/likes"))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        // Increment again
        mockMvc.perform(post("/api/posts/" + postId + "/likes"))
            .andExpect(status().isOk())
            .andExpect(content().string("2"));
    }
}

