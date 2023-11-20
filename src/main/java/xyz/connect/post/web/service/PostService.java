package xyz.connect.post.web.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import xyz.connect.post.custom_exception.PostApiException;
import xyz.connect.post.enumeration.ErrorCode;
import xyz.connect.post.event.DeletedPostEvent;
import xyz.connect.post.event.UpdatedPostEvent;
import xyz.connect.post.web.entity.Post;
import xyz.connect.post.web.entity.redis.PostViewsEntity;
import xyz.connect.post.web.model.request.CreatePost;
import xyz.connect.post.web.model.request.UpdatePost;
import xyz.connect.post.web.model.response.PostDto;
import xyz.connect.post.web.repository.PostRepository;
import xyz.connect.post.web.repository.redis.PostViewsRedisRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final ModelMapper modelMapper;
    private final PostViewsRedisRepository postViewRedisRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PostDto createPost(CreatePost createPost, long accountId) {
        Post post = modelMapper.map(createPost, Post.class);
        post.setAccountId(accountId);
        post.setContent(createPost.content());

        Post resultEntity = postRepository.save(post);
        PostDto postDto = modelMapper.map(resultEntity, PostDto.class);
        log.info("PostDto 등록 완료: " + post);

        return postDto;
    }

    public PostDto getPost(Long postId) {
        Post post = findPost(postId);
        PostDto postDto = modelMapper.map(post, PostDto.class);

        // getCachedViews() 와 increaseCachedViews() 실행 간격 사이에 스케쥴러가 실행되어
        // 캐싱된 조회수가 사라질 가능성이 존재한다. 이 경우 조회수 증가는 무시된다.
        // 하지만 매우 적은 확률이고, PostDto 조회수는 오차가 발생하더라도 큰 문제가 없다.
        // 따라서 검증과정을 거치지 않는 것이 효율적이라 판단
        long cachedViews = getCachedViews(post);
        postDto.setViews(cachedViews);
        increaseCachedViews(post);
        log.info("PostDto 조회 완료: " + postDto);
        return postDto;
    }

    // 댓글이 포함되지 않은 PostDto 를 반환
    public List<PostDto> getPosts(Pageable pageable) {
        List<Post> postList = postRepository.findAll(pageable).getContent();
        List<PostDto> postDtos = new ArrayList<>();
        for (var postEntity : postList) {
            PostDto postDto = modelMapper.map(postEntity, PostDto.class);
            long cachedViews = getCachedViews(postEntity);
            postDto.setViews(cachedViews);
            postDtos.add(postDto);
        }

        log.info("PostDto " + postDtos.size() + "개 조회 완료");
        return postDtos;
    }

    public PostDto updatePost(Long postId, UpdatePost updatePost, long accountId) {
        Post post = findPost(postId);
        if (post.getAccountId() != accountId) {
            throw new PostApiException(ErrorCode.UNAUTHORIZED);
        }

        post.setContent(updatePost.content());
        if (updatePost.images() != null && !updatePost.images().isEmpty()) {
            if (post.getImages() != null) { // 이미지가 존재했었다면 삭제 이벤트 실행
                List<String> originalImages = Arrays.stream(post.getImages().split(";"))
                        .filter(Objects::nonNull)
                        .toList();
                eventPublisher.publishEvent(
                        new UpdatedPostEvent(originalImages, updatePost.images()));
            }
            post.setImages(String.join(";", updatePost.images()));
        }

        Post resultEntity = postRepository.save(post);
        PostDto postDto = modelMapper.map(resultEntity, PostDto.class);
        log.info("PostDto 수정 완료: " + postDto);
        return postDto;
    }

    public void deletePost(Long postId, long accountId) {
        Post post = findPost(postId);
        if (post.getAccountId() != accountId) {
            throw new PostApiException(ErrorCode.UNAUTHORIZED);
        }

        List<String> images = Arrays.stream(post.getImages().split(";"))
                .filter(Objects::nonNull)
                .toList();
        eventPublisher.publishEvent(new DeletedPostEvent(images));

        postRepository.delete(post);
        log.info(post.getPostId() + "번 PostDto 삭제 완료");
    }

    public Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostApiException(ErrorCode.NOT_FOUND));
    }

    // 1. 캐싱된 조회수를 가져온다.
    //   1) 캐싱된 조회수가 존재하면 리턴한다.
    //   2) 캐싱된 조회수가 없으면 캐싱 후 전달받은 Post 의 조회수를 리턴한다.
    private long getCachedViews(Post post) {
        PostViewsEntity postViewsEntity = getPostViewsEntityOrNew(post.getPostId());

        long cachedViews = postViewsEntity.getViews();
        if (cachedViews < 1) {
            cachedViews = post.getViews();
        }

        postViewsEntity.setViews(cachedViews);
        postViewRedisRepository.save(postViewsEntity);

        return cachedViews;
    }

    // 캐싱된 조회수를 1 증가시킨다
    private void increaseCachedViews(Post post) {
        PostViewsEntity postViewsEntity = getPostViewsEntityOrNew(post.getPostId());
        postViewsEntity.setViews(postViewsEntity.getViews() + 1);
        postViewRedisRepository.save(postViewsEntity);
    }

    // new PostViewsEntity 는 views == 0
    private PostViewsEntity getPostViewsEntityOrNew(long postId) {
        return postViewRedisRepository.findById(postId).orElse(new PostViewsEntity(postId));
    }
}
