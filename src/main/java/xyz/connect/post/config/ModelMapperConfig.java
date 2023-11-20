package xyz.connect.post.config;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.modelmapper.AbstractConverter;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.connect.post.util.S3Util;
import xyz.connect.post.web.entity.Comment;
import xyz.connect.post.web.entity.Post;
import xyz.connect.post.web.model.request.CreatePost;
import xyz.connect.post.web.model.response.CommentDto;
import xyz.connect.post.web.model.response.PostDto;

@Configuration
@RequiredArgsConstructor
public class ModelMapperConfig {

    private final ModelMapper modelMapper = new ModelMapper();
    private final S3Util s3Util;

    @Bean
    public ModelMapper modelMapper() {
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT);
//        modelMapper.addConverter(postEntityToPost());
//        modelMapper.addConverter(createPostToPostEntity());
//        modelMapper.addConverter(commentEntityToComment());

        return modelMapper;
    }

    private Converter<Post, PostDto> postEntityToPost() {
        return new AbstractConverter<>() {
            @Override
            protected PostDto convert(Post source) {
                PostDto postDto = new PostDto();
                postDto.setPostId(source.getPostId());
                postDto.setAccountId(source.getAccountId());
                postDto.setContent(source.getContent());
                postDto.setImages(imageStringToList(source.getImages()));
                postDto.setCreatedAt(source.getCreatedAt());
                postDto.setViews(source.getViews());
                return postDto;
            }
        };
    }

    private Converter<CreatePost, Post> createPostToPostEntity() {
        return new AbstractConverter<>() {
            @Override
            protected Post convert(CreatePost source) {
                Post post = new Post();
                post.setPostId(null);
                post.setAccountId(null);
                post.setContent(source.content());
                if (source.images() != null && !source.images().isEmpty()) {
                    post.setImages(String.join(";", source.images()));
                }
                return post;
            }
        };
    }

    private Converter<Comment, CommentDto> commentEntityToComment() {
        return new AbstractConverter<>() {
            @Override
            protected CommentDto convert(Comment source) {
                CommentDto commentDto = new CommentDto();
                commentDto.setCommentId(source.getCommentId());
                commentDto.setPostId(source.getPost().getPostId());
                commentDto.setAccountId(source.getAccountId());
                commentDto.setContent(source.getContent());
                commentDto.setCreatedAt(source.getCreatedAt());
                return commentDto;
            }
        };
    }

    private List<String> imageStringToList(String images) {
        List<String> imageList = new ArrayList<>();
        for (String image : images.split(";")) {
            imageList.add(s3Util.getImageUrl(image));
        }

        return imageList;
    }
}
