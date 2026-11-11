package com.example.demo.model.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.domain.Comment;
import com.example.demo.model.domain.Member;
import com.example.demo.model.domain.Post;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostOrderByIdAsc(Post post);
    boolean existsByAuthor(Member author); // 이 회원이 쓴 댓글이 있는지
}
