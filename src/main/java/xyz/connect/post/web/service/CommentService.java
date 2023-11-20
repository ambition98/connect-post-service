package xyz.connect.post.web.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import xyz.connect.post.custom_exception.PostApiException;
import xyz.connect.post.enumeration.ErrorCode;
import xyz.connect.post.web.entity.Comment;
import xyz.connect.post.web.entity.Post;
import xyz.connect.post.web.model.request.CreateComment;
import xyz.connect.post.web.model.request.UpdateComment;
import xyz.connect.post.web.model.response.CommentDto;
import xyz.connect.post.web.repository.CommentRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostService postService;
    private final ModelMapper modelMapper;

    public List<CommentDto> getComments(Long postId, Pageable pageable) {
        Post post = postService.findPost(postId);
        List<Comment> commentList = commentRepository.findByPost(post, pageable)
                .getContent();
        List<CommentDto> commentDtoList = new ArrayList<>();

        for (Comment entity : commentList) {
            commentDtoList.add(modelMapper.map(entity, CommentDto.class));
        }

        log.info(postId + "번 Post의 CommentDto " + commentDtoList.size() + "개 조회 완료");
        return commentDtoList;
    }

    public CommentDto createComment(CreateComment createComment, long accountId) {
        Post post = postService.findPost(createComment.postId());

        Comment comment = modelMapper.map(createComment, Comment.class);
        comment.setPost(post);
        comment.setAccountId(accountId);
        comment.setContent(createComment.content());
        Comment resultEntity = commentRepository.save(comment);

        CommentDto commentDto = modelMapper.map(resultEntity, CommentDto.class);
        log.info("CommentDto 등록 완료: " + commentDto);
        return commentDto;
    }

    public CommentDto updateComment(Long commentId, UpdateComment updateComment, long accountId) {
        Comment comment = findComment(commentId);
        if (comment.getAccountId() != accountId) {
            throw new PostApiException(ErrorCode.UNAUTHORIZED);
        }

        comment.setContent(updateComment.content());
        Comment resultEntity = commentRepository.save(comment);
        CommentDto commentDto = modelMapper.map(resultEntity, CommentDto.class);
        log.info("CommentDto 수정 완료: " + commentDto);
        return commentDto;
    }

    public void deleteComment(Long commentId, long accountId) {
        Comment comment = findComment(commentId);
        if (comment.getAccountId() != accountId) {
            throw new PostApiException(ErrorCode.UNAUTHORIZED);
        }

        commentRepository.delete(comment);
        log.info(comment.getPost() + "번 PostDto 의 " + comment.getCommentId()
                + "번 CommentDto 삭제 완료");
    }

    public Comment findComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new PostApiException(ErrorCode.NOT_FOUND));
    }
}
