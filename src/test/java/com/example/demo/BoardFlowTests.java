package com.example.demo;

import java.time.LocalDateTime;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import com.example.demo.model.domain.*;
import com.example.demo.model.repository.*;
import com.example.demo.model.service.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@org.springframework.test.context.TestExecutionListeners(listeners = {
    org.springframework.test.context.support.DependencyInjectionTestExecutionListener.class,
    org.springframework.test.context.transaction.TransactionalTestExecutionListener.class
})
class BoardFlowTests {
    @Autowired WebApplicationContext context;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager entityManager;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    @Autowired PostRepository posts;
    @Autowired MemberRepository members;
    @Autowired CommentRepository comments;
    @Autowired PostService postService;
    @Autowired CommentService commentService;
    @Autowired MemberService memberService;
    MockMvc mvc;
    Member writer, other, admin;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .defaultRequest(get("/").accept(MediaType.TEXT_HTML)).addFilters(security).build();
        writer = member("board-writer", "USER");
        other = member("board-other", "USER");
        admin = member("board-admin", "ADMIN");
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    Member member(String username, String role) {
        Member m = new Member();
        m.setUsername(username); m.setName(username); m.setRole(role); m.setPassword("unused");
        return members.saveAndFlush(m);
    }
    Post article(String title, String content) {
        Post p = new Post(); p.setTitle(title); p.setContent(content); p.setAuthor(writer);
        p.setCreatedAt(LocalDateTime.now()); return posts.saveAndFlush(p);
    }
    MockHttpSession session(Member m) {
        var ctx = SecurityContextHolder.createEmptyContext();
        var user = User.withUsername(m.getUsername()).password("unused").roles(m.getRole()).build();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, ctx);
        return session;
    }
    String csrf(MvcResult page) throws Exception {
        var matcher = java.util.regex.Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"")
                .matcher(page.getResponse().getContentAsString());
        assertTrue(matcher.find()); return matcher.group(1);
    }

    @Test void paginationSearchAndOwnPostsRender() throws Exception {
        for (int i=1; i<=23; i++) article("페이징 전용 " + i, "내용");
        article("다른 제목", "페이징 전용 본문 검색");
        var first = mvc.perform(get("/board").param("keyword", " 페이징 전용 "))
                .andExpect(status().isOk()).andReturn();
        var page = (org.springframework.data.domain.Page<?>) first.getModelAndView().getModel().get("page");
        assertEquals(24, page.getTotalElements()); assertEquals(10, page.getNumberOfElements());
        String html = first.getResponse().getContentAsString();
        assertTrue(html.contains("1 / 3 페이지")); assertTrue(html.contains("keyword="));
        var second = postService.findPage("페이징 전용", 1);
        assertEquals(10, second.getNumberOfElements()); assertEquals(4, postService.findPage("페이징 전용", 2).getNumberOfElements());
        assertNotEquals(page.getContent().getFirst(), second.getContent().getFirst());
        mvc.perform(get("/board").param("page", "-1")).andExpect(status().isOk());
        mvc.perform(get("/board").param("page", "999")).andExpect(status().isOk());
        mvc.perform(get("/board").param("keyword", "' OR 1=1 --"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("검색 결과가 없습니다.")));
        mvc.perform(get("/board/my").session(session(writer))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("내가 쓴 글")));
    }

    @Test void commentLifecycleEnforcesOwnershipCsrfAndPostRelationship() throws Exception {
        Post p = article("댓글 테스트", "내용");
        var anonymous = mvc.perform(get("/board/" + p.getId())).andExpect(status().isOk()).andReturn();
        assertTrue(anonymous.getResponse().getContentAsString().contains("하면 댓글을 쓸 수 있습니다."));
        var session = session(other);
        String token = csrf(mvc.perform(get("/board/" + p.getId()).session(session)).andReturn());
        String url = "/board/" + p.getId() + "/comments";
        mvc.perform(post(url).session(session).param("content", "댓글")).andExpect(status().isForbidden());
        mvc.perform(post(url).session(session).param("_csrf", token).param("content", "  첫 댓글  "))
                .andExpect(redirectedUrl("/board/" + p.getId() + "#comments"));
        Comment c = comments.findByPostOrderByIdAsc(p).getFirst();
        assertEquals(other.getId(), c.getAuthor().getId()); assertEquals("첫 댓글", c.getContent());
        for (String invalid : new String[]{"   ", "가".repeat(501)}) {
            mvc.perform(post(url).session(session).param("_csrf", token).param("content", invalid))
                    .andExpect(redirectedUrl("/board/" + p.getId() + "?commentError#comments"));
        }
        assertEquals(1, comments.findByPostOrderByIdAsc(p).size());
        var writerSession = session(writer);
        String writerToken = csrf(mvc.perform(get("/board/" + p.getId()).session(writerSession)).andReturn());
        mvc.perform(post(url + "/" + c.getId() + "/delete").session(writerSession).param("_csrf", writerToken))
                .andExpect(status().isForbidden());
        Post another = article("다른 글", "내용");
        mvc.perform(post("/board/" + another.getId() + "/comments/" + c.getId() + "/delete")
                .session(session).param("_csrf", token)).andExpect(status().isNotFound());
        mvc.perform(post(url + "/" + c.getId() + "/delete").session(session).param("_csrf", token))
                .andExpect(redirectedUrl("/board/" + p.getId() + "?commentDeleted#comments"));
        assertFalse(comments.existsById(c.getId()));
    }

    @Test void postDeletionCascadesAndMemberDeletionPreservesContributions() {
        Post p = article("삭제 테스트", "내용");
        Comment c = commentService.write(p.getId(), "다른 회원 댓글", other.getUsername());
        var auth = User.withUsername(admin.getUsername()).password("unused").roles("ADMIN").build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(auth, null, auth.getAuthorities()));
        assertThrows(IllegalArgumentException.class, () -> memberService.deleteMember(writer.getId()));
        assertThrows(IllegalArgumentException.class, () -> memberService.deleteMember(other.getId()));
        // 다음 HTTP 요청처럼 영속성 컨텍스트를 새로 조회한다.
        entityManager.flush(); entityManager.clear();
        postService.delete(p.getId()); posts.flush();
        assertFalse(posts.existsById(p.getId())); assertFalse(comments.existsById(c.getId()));
        memberService.deleteMember(other.getId()); members.flush();
        assertFalse(members.existsById(other.getId()));
    }
}
