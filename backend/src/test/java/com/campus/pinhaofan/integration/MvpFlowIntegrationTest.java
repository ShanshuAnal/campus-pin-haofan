package com.campus.pinhaofan.integration;

import com.campus.pinhaofan.common.AccessTokenBlacklist;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MvpFlowIntegrationTest {

    private static final String API_CONTEXT = "/api";
    private static final String PASSWORD = "P@ssw0rd123";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private AccessTokenBlacklist accessTokenBlacklist;

    @BeforeEach
    void resetDatabaseAndRedis() throws Exception {
        assumeTrue(mysqlAvailable(), "MySQL 8 is not available for integration tests");
        assumeTrue(redisAvailable(), "Redis is not available for integration tests");

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        populator.addScript(new FileSystemResource(schemaSqlPath()));
        populator.execute(dataSource);

        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushDb();
        }
    }

    @Test
    void mvpMainFlowFromRegistrationToLogoutBlacklist() throws Exception {
        long suffix = System.currentTimeMillis();
        Long userAId = register("it_a_" + suffix, "用户A");
        Long userBId = register("it_b_" + suffix, "用户B");
        register("it_c_" + suffix, "用户C");

        JsonNode loginA = login("it_a_" + suffix);
        String accessA = loginA.path("accessToken").asText();
        String refreshA = loginA.path("refreshToken").asText();

        Long orderId = createGroupOrder(accessA);
        JsonNode hall = ok(getApi("/api/group-orders", accessA));
        assertThat(hall.path("records")).hasSize(1);
        assertThat(hall.path("records").get(0).path("id").asLong()).isEqualTo(orderId);

        Long participantAId = joinOrder(accessA, orderId, "发起人套餐", "10.00");

        JsonNode loginB = login("it_b_" + suffix);
        String accessB = loginB.path("accessToken").asText();
        Long participantBId = joinOrder(accessB, orderId, "成员B套餐", "10.00");

        JsonNode loginC = login("it_c_" + suffix);
        String accessC = loginC.path("accessToken").asText();
        Long participantCId = joinOrder(accessC, orderId, "成员C套餐", "10.00");

        JsonNode detailBeforeLock = ok(getApi("/api/group-orders/" + orderId, accessA));
        assertThat(detailBeforeLock.path("participants")).hasSize(3);
        assertAmount(detailBeforeLock.path("orderAmount").path("originalTotalAmount"), "30.00");
        assertAmount(detailBeforeLock.path("orderAmount").path("payableTotalAmount"), "30.00");

        expectCode(postApi("/api/group-orders/" + orderId + "/participants", accessB, joinBody("重复加入", "10.00")),
                409,
                "不能重复加入同一拼单");
        expectCode(postApi("/api/group-orders/" + orderId + "/lock", accessB, "{\"remark\":\"非发起人锁单\"}"),
                403,
                "只有发起人可以锁单");

        JsonNode lock = ok(postApi("/api/group-orders/" + orderId + "/lock", accessA, "{\"remark\":\"人数已齐\"}"));
        assertThat(lock.path("order").path("status").asText()).isEqualTo("LOCKED");
        assertAmount(lock.path("order").path("originalTotalAmount"), "30.00");
        assertAmount(lock.path("order").path("actualDiscountAmount"), "10.00");
        assertAmount(lock.path("order").path("payableTotalAmount"), "20.00");
        assertThat(lock.path("allocations")).hasSize(3);

        expectCode(postApi("/api/group-orders/" + orderId + "/participants", accessB, joinBody("锁单后加入", "10.00")),
                400,
                "拼单已锁定，不能继续加入");
        expectCode(postApi("/api/group-orders/" + orderId + "/participants/" + participantCId + "/payments/mark",
                        accessB,
                        "{\"remark\":\"替别人付款\"}"),
                403,
                "只能标记自己的付款");
        expectCode(postApi("/api/group-orders/" + orderId + "/participants/" + participantCId + "/payments/confirm",
                        accessB,
                        "{\"remark\":\"非发起人确认\"}"),
                403,
                "只有发起人可以确认付款");

        JsonNode paidB = ok(postApi("/api/group-orders/" + orderId + "/participants/" + participantBId + "/payments/mark",
                accessB,
                "{\"remark\":\"B 已付款\"}"));
        assertThat(paidB.path("paymentStatus").asText()).isEqualTo("PAID");
        JsonNode paidC = ok(postApi("/api/group-orders/" + orderId + "/participants/" + participantCId + "/payments/mark",
                accessC,
                "{\"remark\":\"C 已付款\"}"));
        assertThat(paidC.path("paymentStatus").asText()).isEqualTo("PAID");

        JsonNode confirmedB = ok(postApi("/api/group-orders/" + orderId + "/participants/" + participantBId + "/payments/confirm",
                accessA,
                "{\"remark\":\"确认 B\"}"));
        assertThat(confirmedB.path("paymentStatus").asText()).isEqualTo("CONFIRMED");
        JsonNode confirmedC = ok(postApi("/api/group-orders/" + orderId + "/participants/" + participantCId + "/payments/confirm",
                accessA,
                "{\"remark\":\"确认 C\"}"));
        assertThat(confirmedC.path("paymentStatus").asText()).isEqualTo("CONFIRMED");

        JsonNode pickupAssignee = ok(putApi("/api/group-orders/" + orderId + "/pickup-assignee",
                accessA,
                """
                        {
                          "pickupUserId": %d,
                          "pickupLocation": "一教大厅门口",
                          "estimatedArrivalTime": "2099-05-27 19:10:00",
                          "remark": "用户B取餐"
                        }
                        """.formatted(userBId)));
        assertThat(pickupAssignee.path("pickupRecord").path("pickupStatus").asText()).isEqualTo("WAITING_ORDER");

        expectCode(patchApi("/api/group-orders/" + orderId + "/pickup-status",
                        accessB,
                        "{\"pickupStatus\":\"ARRIVED\",\"remark\":\"跳跃状态\"}"),
                400,
                "非法取餐状态流转");

        assertPickupStatus(orderId, accessB, "WAITING_DELIVERY", "DELIVERING");
        assertPickupStatus(orderId, accessB, "ARRIVED", "ARRIVED");
        assertPickupStatus(orderId, accessB, "PICKED_UP", "PICKED_UP");
        assertPickupStatus(orderId, accessB, "DISTRIBUTED", "FINISHED");

        JsonNode myOrders = ok(getApi("/api/my/group-orders?scope=HISTORY&pageNum=1&pageSize=10", accessB));
        assertThat(myOrders.path("records")).hasSize(1);
        assertThat(myOrders.path("records").get(0).path("order").path("id").asLong()).isEqualTo(orderId);
        assertThat(myOrders.path("records").get(0).path("order").path("status").asText()).isEqualTo("FINISHED");

        JsonNode dashboard = ok(getApi("/api/dashboard/summary?scope=MINE", accessA));
        assertThat(dashboard.path("todayOrderCount").asLong()).isEqualTo(1L);
        assertThat(dashboard.path("successOrderCount").asLong()).isEqualTo(1L);
        assertAmount(dashboard.path("totalSavedAmount"), "10.00");

        JsonNode logout = ok(postApi("/api/auth/logout",
                accessA,
                "{\"refreshToken\":\"" + refreshA + "\"}"));
        assertThat(logout.path("logout").asBoolean()).isTrue();
        assertThat(stringRedisTemplate.hasKey(accessTokenBlacklist.buildKey(accessA))).isTrue();

        expectCode(getApi("/api/auth/me", accessA), 401, "登录已失效");

        JsonNode finalDetail = ok(getApi("/api/group-orders/" + orderId, accessB));
        assertThat(finalDetail.path("order").path("status").asText()).isEqualTo("FINISHED");
        assertThat(finalDetail.path("pickupStatus").asText()).isEqualTo("DISTRIBUTED");
        assertThat(participantAId).isNotNull();
    }

    @Test
    void protectedEndpointRejectsMissingToken() throws Exception {
        expectCode(getApi("/api/group-orders", null), 401, "未登录");
    }

    private Long register(String username, String nickname) throws Exception {
        JsonNode data = ok(postApi(
                "/api/auth/register",
                null,
                """
                        {
                          "username": "%s",
                          "password": "%s",
                          "nickname": "%s"
                        }
                        """.formatted(username, PASSWORD, nickname)
        ));
        assertThat(data.path("username").asText()).isEqualTo(username);
        assertThat(data.path("status").asText()).isEqualTo("ACTIVE");
        return data.path("userId").asLong();
    }

    private JsonNode login(String username) throws Exception {
        JsonNode data = ok(postApi(
                "/api/auth/login",
                null,
                """
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username, PASSWORD)
        ));
        assertThat(data.path("accessToken").asText()).isNotBlank();
        assertThat(data.path("refreshToken").asText()).isNotBlank();
        return data;
    }

    private Long createGroupOrder(String accessToken) throws Exception {
        String deadlineTime = LocalDateTime.now().plusDays(1).format(DATE_TIME_FORMATTER);
        JsonNode data = ok(postApi(
                "/api/group-orders",
                accessToken,
                """
                        {
                          "title": "集成测试奶茶拼单",
                          "orderType": "MILK_TEA",
                          "merchantName": "一号门奶茶",
                          "pickupLocation": "一教大厅门口",
                          "deadlineTime": "%s",
                          "maxParticipants": 6,
                          "discountThresholdAmount": 30.00,
                          "discountAmount": 10.00,
                          "remark": "集成测试"
                        }
                        """.formatted(deadlineTime)
        ));
        assertThat(data.path("status").asText()).isEqualTo("CREATED");
        assertThat(data.path("participantCount").asInt()).isZero();
        return data.path("id").asLong();
    }

    private Long joinOrder(String accessToken, Long orderId, String itemName, String unitPrice) throws Exception {
        JsonNode data = ok(postApi("/api/group-orders/" + orderId + "/participants",
                accessToken,
                joinBody(itemName, unitPrice)));
        assertThat(data.path("participant").path("paymentStatus").asText()).isEqualTo("UNPAID");
        return data.path("participant").path("id").asLong();
    }

    private String joinBody(String itemName, String unitPrice) {
        return """
                {
                  "remark": "少冰",
                  "mealItems": [
                    {
                      "itemName": "%s",
                      "quantity": 1,
                      "unitPrice": %s,
                      "remark": "少冰"
                    }
                  ]
                }
                """.formatted(itemName, unitPrice);
    }

    private void assertPickupStatus(Long orderId, String accessToken, String pickupStatus, String orderStatus) throws Exception {
        JsonNode data = ok(patchApi("/api/group-orders/" + orderId + "/pickup-status",
                accessToken,
                "{\"pickupStatus\":\"" + pickupStatus + "\",\"remark\":\"推进到 " + pickupStatus + "\"}"));
        assertThat(data.path("pickupRecord").path("pickupStatus").asText()).isEqualTo(pickupStatus);
        assertThat(data.path("orderStatus").asText()).isEqualTo(orderStatus);
    }

    private ResultActions getApi(String path, String accessToken) throws Exception {
        var builder = get(path).contextPath(API_CONTEXT);
        if (accessToken != null) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        return mockMvc.perform(builder);
    }

    private ResultActions postApi(String path, String accessToken, String body) throws Exception {
        var builder = post(path)
                .contextPath(API_CONTEXT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (accessToken != null) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        return mockMvc.perform(builder);
    }

    private ResultActions putApi(String path, String accessToken, String body) throws Exception {
        var builder = put(path)
                .contextPath(API_CONTEXT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (accessToken != null) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        return mockMvc.perform(builder);
    }

    private ResultActions patchApi(String path, String accessToken, String body) throws Exception {
        var builder = patch(path)
                .contextPath(API_CONTEXT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (accessToken != null) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        return mockMvc.perform(builder);
    }

    private JsonNode ok(ResultActions actions) throws Exception {
        JsonNode root = response(actions);
        assertThat(root.path("code").asInt()).isEqualTo(200);
        assertThat(root.path("message").asText()).isEqualTo("success");
        return root.path("data");
    }

    private void expectCode(ResultActions actions, int code, String message) throws Exception {
        JsonNode root = response(actions);
        assertThat(root.path("code").asInt()).isEqualTo(code);
        assertThat(root.path("message").asText()).isEqualTo(message);
        assertThat(root.path("data").isNull()).isTrue();
    }

    private JsonNode response(ResultActions actions) throws Exception {
        MvcResult result = actions.andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private void assertAmount(JsonNode node, String expected) {
        assertThat(node.decimalValue()).isEqualByComparingTo(new BigDecimal(expected));
    }

    private boolean mysqlAvailable() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (SQLException exception) {
            return false;
        }
    }

    private boolean redisAvailable() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            return "PONG".equalsIgnoreCase(pong);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private Path schemaSqlPath() {
        Path backendWorkingDirectoryPath = Path.of("..", "sql", "schema.sql").normalize();
        if (Files.exists(backendWorkingDirectoryPath)) {
            return backendWorkingDirectoryPath;
        }
        return Path.of("sql", "schema.sql").normalize();
    }
}
