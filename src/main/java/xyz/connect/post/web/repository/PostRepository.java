package xyz.connect.post.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import xyz.connect.post.web.entity.Post;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
}
