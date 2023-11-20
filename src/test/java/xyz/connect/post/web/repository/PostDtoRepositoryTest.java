package xyz.connect.post.web.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import xyz.connect.post.web.entity.Post;

@DataJpaTest
class PostDtoRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Test
    void createNewPost() {
        //given
        Post post = generatePostEntity();

        //when
        Post resultEntity = postRepository.save(post);

        //then
        assertThat(post.getPostId()).isEqualTo(resultEntity.getPostId());
        assertThat(post.getAccountId()).isEqualTo(resultEntity.getAccountId());
    }

    private Post generatePostEntity() {
        Post post = new Post();
        post.setPostId(1L);
        post.setAccountId(1L);
        post.setContent("content");
        post.setImages("image");
        return post;
    }
}