package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.AbstractMessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.demo.model.domain.Member;
import com.example.demo.model.domain.Post;
import com.example.demo.model.repository.MemberRepository;
import com.example.demo.model.repository.PostRepository;
import com.example.demo.model.service.CommentService;
import com.example.demo.model.service.EnrollmentService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:week12-realtime;MODE=MySQL;DB_CLOSE_DELAY=-1")
class RealtimeEnrollmentTests {
    @Autowired MemberRepository members;
    @Autowired PostRepository posts;
    @Autowired EnrollmentService enrollments;
    @Autowired CommentService comments;
    @Autowired PlatformTransactionManager transactions;
    @Autowired @Qualifier("brokerChannel") AbstractMessageChannel brokerChannel;

    private final JsonMapper json = new JsonMapper();
    private final List<Message<?>> messages = new ArrayList<>();
    private final ChannelInterceptor capture = new ChannelInterceptor() {
        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            messages.add(message);
            return message;
        }
    };

    @BeforeEach
    void captureMessages() {
        brokerChannel.addInterceptor(capture);
    }

    @AfterEach
    void stopCapture() {
        brokerChannel.removeInterceptor(capture);
    }

    @Test
    void sendsSeatChangesAndWriterNotificationsOnlyAfterCommit() {
        Member writer = createMember("admin", "작성자");
        Member student = createMember("student1", "홍길동");
        Member other = createMember("student3", "김학생");
        Long a = createRecruitment(writer, "A", 5);
        Long b = createRecruitment(writer, "B", 5);
        Long c = createRecruitment(writer, "C", 1);
        enrollments.confirm(student.getUsername(), List.of(a));
        assertEquals(List.of("/topic/seats", "/user/admin/queue/notifications"), destinations(messages));
        assertEquals(4, payloadAt(0).path("remaining").asInt());
        assertEquals(4, posts.findById(a).orElseThrow().getRemaining());

        enrollments.confirm(other.getUsername(), List.of(c));
        messages.clear();
        assertThrows(IllegalStateException.class, () -> enrollments.confirm(student.getUsername(), List.of(b, c)));
        assertEquals(5, posts.findById(b).orElseThrow().getRemaining());
        assertTrue(messages.isEmpty());

        enrollments.confirm(student.getUsername(), List.of(b));
        assertEquals(List.of("/topic/seats", "/user/admin/queue/notifications"), destinations(messages));
        messages.clear();

        enrollments.confirm(writer.getUsername(), List.of(a));
        assertEquals(List.of("/topic/seats"), destinations(messages));
    }

    @Test
    void notifiesWriterAboutCancellationAndCommentsAfterCommit() {
        Member writer = createMember("exercise-admin", "작성자");
        Member student = createMember("exercise-student1", "홍길동");
        Member applicant = createMember("exercise-student3", "김학생");
        String writerDestination = "/user/exercise-admin/queue/notifications";
        Long c = createRecruitment(writer, "C", 1);
        enrollments.confirm(applicant.getUsername(), List.of(c));
        Long enrollmentId = enrollments.findMine(applicant.getUsername()).getFirst().getId();
        assertTrue(posts.findById(c).orElseThrow().isFull());
        messages.clear();

        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            enrollments.cancel(enrollmentId, applicant.getUsername());
            comments.write(c, "롤백할 댓글", student.getUsername());
            assertTrue(messages.isEmpty());
            status.setRollbackOnly();
        });
        assertTrue(messages.isEmpty());
        assertTrue(posts.findById(c).orElseThrow().isFull());

        enrollments.cancel(enrollmentId, applicant.getUsername());
        assertEquals(List.of("/topic/seats", writerDestination), destinations(messages));
        assertEquals(c.longValue(), payloadAt(0).path("postId").asLong());
        assertEquals(1, payloadAt(0).path("remaining").asInt());
        assertEquals(1, posts.findById(c).orElseThrow().getRemaining());
        assertEquals("김학생님이 'C' 신청을 취소했습니다.", payloadAt(1).path("text").asString());
        messages.clear();

        comments.write(c, "신청 관련 질문입니다.", student.getUsername());
        assertEquals(List.of(writerDestination), destinations(messages));
        assertEquals(c.longValue(), payloadAt(0).path("postId").asLong());
        assertEquals("홍길동님이 'C' 에 댓글을 남겼습니다.", payloadAt(0).path("text").asString());
        messages.clear();

        comments.write(c, "작성자 본인의 댓글입니다.", writer.getUsername());
        assertTrue(messages.isEmpty());
    }

    private JsonNode payloadAt(int index) {
        return json.readTree((byte[]) messages.get(index).getPayload());
    }

    private List<String> destinations(List<Message<?>> messages) {
        return messages.stream().map(message -> SimpMessageHeaderAccessor.getDestination(message.getHeaders())).toList();
    }

    private Member createMember(String username, String name) {
        Member member = new Member();
        member.setUsername(username);
        member.setName(name);
        member.setPassword("unused");
        member.setRole("USER");
        return members.save(member);
    }

    private Long createRecruitment(Member writer, String title, int capacity) {
        Post post = new Post();
        post.setTitle(title);
        post.setContent("12주차 실시간 신청 테스트");
        post.setCapacity(capacity);
        post.setAuthor(writer);
        post.setCreatedAt(LocalDateTime.now());
        return posts.save(post).getId();
    }
}
