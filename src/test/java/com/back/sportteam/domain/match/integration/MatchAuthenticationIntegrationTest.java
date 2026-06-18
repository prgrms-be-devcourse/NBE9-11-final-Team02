package com.back.sportteam.domain.match.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import java.util.Map;
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
                MatchParticipantStatus.PAYMENT_PENDING
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
        MvcResult result = mockMvc.perform(post("/api/v1/matches")
                        .header("Authorization", bearer(host.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(matchCreateBody(reservationId))))
                .andExpect(status().isCreated())
                .andReturn();

        return read(result, "$.data.matchId");
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
        return Map.of(
                "reservationId", reservationId,
                "title", "인증 연동 테스트 매칭",
                "sportType", "FUTSAL",
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
