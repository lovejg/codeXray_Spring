package com.codeXray.backend.community.repository;

import com.codeXray.backend.community.entity.CommunityPost;
import com.codeXray.backend.community.entity.PostType;
import com.codeXray.backend.community.entity.PostVote;
import com.codeXray.backend.config.QueryDslConfig;
import com.codeXray.backend.user.entity.AuthProvider;
import com.codeXray.backend.user.entity.User;
import com.codeXray.backend.user.entity.UserRole;
import com.codeXray.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

// 커뮤니티 목록이 DB 에서 정렬·페이지를 나누는지. 추천순은 예전에 Java 메모리 정렬이라 페이지를 나눌 수 없었다.
// 로컬 DB 에 다른 글이 있어도 섞이지 않게 이 테스트가 만든 작성자(authorId)로만 거른다
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDslConfig.class)
class CommunityPostPageTest {

    @Autowired UserRepository userRepository;
    @Autowired CommunityPostRepository postRepository;
    @Autowired EntityManager em;

    private User author;
    private CommunityPost minusOne;
    private CommunityPost plusTwo;
    private CommunityPost zero;

    @BeforeEach
    void setUp() {
        author = user("page-author");
        User voter1 = user("page-voter1");
        User voter2 = user("page-voter2");

        // 작성 순서: minusOne → plusTwo → zero (최신순이면 zero, plusTwo, minusOne)
        minusOne = post("점수 -1");
        plusTwo = post("점수 +2");
        zero = post("점수 0");

        vote(voter1, minusOne, -1);
        vote(voter1, plusTwo, 1);
        vote(voter2, plusTwo, 1);
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("추천순은 점수(+1/-1 합계) 높은 순, 투표가 없는 글은 0점으로 친다")
    void sortsByScoreInDatabase() {
        Page<CommunityPost> page = postRepository.findVisiblePosts(
                null, false, null, null, null, author.getId(), true, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(CommunityPost::getId)
                .containsExactly(plusTwo.getId(), zero.getId(), minusOne.getId());
    }

    @Test
    @DisplayName("페이지 크기로 잘리고 전체 개수는 따로 센다")
    void paginates() {
        Page<CommunityPost> first = postRepository.findVisiblePosts(
                null, false, null, null, null, author.getId(), false, PageRequest.of(0, 2));
        Page<CommunityPost> second = postRepository.findVisiblePosts(
                null, false, null, null, null, author.getId(), false, PageRequest.of(1, 2));

        assertThat(first.getContent()).hasSize(2);
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(second.getContent()).extracting(CommunityPost::getId).containsExactly(minusOne.getId());
    }

    private User user(String name) {
        return userRepository.save(User.builder()
                .email(name + "@test.example").password("x").nickname(name)
                .role(UserRole.USER).provider(AuthProvider.LOCAL).build());
    }

    private CommunityPost post(String title) {
        CommunityPost p = postRepository.save(CommunityPost.builder()
                .user(author).type(PostType.QUESTION).title(title).content("본문").isPrivate(false).build());
        em.flush(); // created_at 이 글마다 달라지게 하나씩 넣는다
        sleepAMillisecond();
        return p;
    }

    private void vote(User voter, CommunityPost post, int value) {
        em.persist(PostVote.builder().userId(voter.getId()).postId(post.getId()).value(value).build());
    }

    private static void sleepAMillisecond() {
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
