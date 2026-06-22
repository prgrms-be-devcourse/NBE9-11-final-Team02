package com.back.sportteam.domain.match.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.back.sportteam.domain.facility.entity.FacilitySlot;
import com.back.sportteam.domain.facility.repository.FacilitySlotRepository;
import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchParticipantRole;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.user.entity.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class MatchAuthenticationIntegrationTest {

    private static final LocalDateTime RECRUIT_DEADLINE = LocalDateTime.of(2099, Month.JUNE, 10, 10, 0);
    private static final LocalDateTime CANCEL_DEADLINE = LocalDateTime.of(2099, Month.JUNE, 12, 10, 0);

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private FacilitySlotRepository facilitySlotRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchParticipantRepository matchParticipantRepository;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
    }

    @Test
    void 로그인_토큰으로_매칭방을_생성하면_인증된_회원이_방장으로_저장된다() throws Exception {
        AuthSession host = signupAndLogin("host-auth-flow@example.com", "방장");
        FacilitySlot slot = facilitySlotRepository.save(createSlot());

        MvcResult result = mockMvc.perform(post("/api/v1/matches")
                        .header("Authorization", bearer(host.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(matchCreateBody(slot.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hostId").value(host.userId()))
                .andExpect(jsonPath("$.data.reservationId").value(slot.getId()))
                .andReturn();

        String matchId = read(result, "$.data.matchId");
        Match match = matchRepository.findById(matchId).orElseThrow();

        assertThat(match.getHostId()).isEqualTo(host.userId());
        assertThat(matchParticipantRepository.findByMatchIdAndUserIdAndStatus(
                matchId,
                host.userId(),
                MatchParticipantStatus.ACTIVE
        )).isPresent()
                .get()
                .satisfies(participant -> assertThat(participant.getRole()).isEqualTo(MatchParticipantRole.HOST));
    }

    @Test
    void 다른_회원_토큰으로_방장_전용_API를_호출하면_403_응답을_반환한다() throws Exception {
        AuthSession host = signupAndLogin("owner-auth-flow@example.com", "방장");
        AuthSession other = signupAndLogin("other-auth-flow@example.com", "참가자");
        FacilitySlot slot = facilitySlotRepository.save(createSlot());
        String matchId = createMatch(host, slot.getId());

        mockMvc.perform(delete("/api/v1/matches/{matchId}", matchId)
                        .header("Authorization", bearer(other.accessToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MATCH_004"));
    }

    @Test
    void 다른_회원_토큰으로_매칭방_확정_API를_호출하면_403_응답을_반환한다() throws Exception {
        AuthSession host = signupAndLogin("confirm-owner-auth-flow@example.com", "확정방장");
        AuthSession other = signupAndLogin("confirm-other-auth-flow@example.com", "확정참가자");
        FacilitySlot slot = facilitySlotRepository.save(createSlot());
        String matchId = createMatch(host, slot.getId());

        mockMvc.perform(patch("/api/v1/matches/{matchId}/confirm", matchId)
                        .header("Authorization", bearer(other.accessToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MATCH_004"));
    }

    @Test
    void 방장_토큰으로_참가_이탈_API를_호출하면_400_응답을_반환한다() throws Exception {
        AuthSession host = signupAndLogin("leave-owner-auth-flow@example.com", "이탈방장");
        FacilitySlot slot = facilitySlotRepository.save(createSlot());
        String matchId = createMatch(host, slot.getId());
        activateHostParticipant(matchId, host.userId());

        mockMvc.perform(delete("/api/v1/matches/{matchId}/participants/me", matchId)
                        .header("Authorization", bearer(host.accessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MATCH_010"));
    }

    @Test
    void 매칭방_목록을_조건과_페이징으로_조회한다() throws Exception {
        AuthSession host = signupAndLogin("search-auth-flow@example.com", "검색방장");
        FacilitySlot futsalSlot = facilitySlotRepository.save(createSlot());
        FacilitySlot tennisSlot = facilitySlotRepository.save(createSlot());
        createMatch(host, matchCreateBody(futsalSlot.getId(), "검색 제외 풋살 매칭", "FUTSAL"));
        String tennisTitle = "검색 대상 테니스 매칭";
        createMatch(host, matchCreateBody(tennisSlot.getId(), tennisTitle, "TENNIS"));

        MvcResult result = mockMvc.perform(get("/api/v1/matches")
                        .header("Authorization", bearer(host.accessToken()))
                        .param("sportType", "TENNIS")
                        .param("status", "RECRUITING")
                        .param("sort", "DEADLINE_ASC")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/content");
        List<JsonNode> matches = StreamSupport.stream(content.spliterator(), false).toList();
        assertThat(matches)
                .isNotEmpty()
                .allSatisfy(match -> assertThat(match.get("sportType").asText()).isEqualTo("TENNIS"))
                .anySatisfy(match -> assertThat(match.get("title").asText()).isEqualTo(tennisTitle));
    }

    private AuthSession signupAndLogin(String email, String nickname) throws Exception {
        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", "password123",
                                "nickname", nickname,
                                "role", UserRole.USER.name()
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        String userId = read(signupResult, "$.data.userId");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", "password123"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = read(loginResult, "$.data.accessToken");
        return new AuthSession(userId, accessToken);
    }

    private String createMatch(AuthSession host, String reservationId) throws Exception {
        return createMatch(host, matchCreateBody(reservationId));
    }

    private String createMatch(AuthSession host, Map<String, Object> body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/matches")
                        .header("Authorization", bearer(host.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andReturn();

        return read(result, "$.data.matchId");
    }

    private void activateHostParticipant(String matchId, String hostId) {
        var hostParticipant = matchParticipantRepository.findByMatchIdAndUserIdAndStatus(
                        matchId,
                        hostId,
                        MatchParticipantStatus.ACTIVE
                )
                .orElseThrow();
        hostParticipant.activate();
        matchParticipantRepository.saveAndFlush(hostParticipant);
    }

    private FacilitySlot createSlot() {
        return FacilitySlot.create(
                "facility-id",
                LocalDate.of(2099, Month.JUNE, 13),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                100_000
        );
    }

    private Map<String, Object> matchCreateBody(String reservationId) {
        return matchCreateBody(reservationId, "인증 연동 테스트 매칭", "FUTSAL");
    }

    private Map<String, Object> matchCreateBody(String reservationId, String title, String sportType) {
        return Map.of(
                "reservationId", reservationId,
                "title", title,
                "sportType", sportType,
                "capacity", 10,
                "feePerPerson", 10_000,
                "minSkillLevel", "ANY",
                "maxSkillLevel", "ANY",
                "requiredGender", "ANY",
                "recruitDeadline", RECRUIT_DEADLINE.toString(),
                "cancelDeadline", CANCEL_DEADLINE.toString()
        );
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String read(MvcResult result, String path) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.at(path.replace("$", "").replace(".", "/")).asText();
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private record AuthSession(String userId, String accessToken) {
    }
}
