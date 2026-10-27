package MidiControl.unit.Routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import MidiControl.MidiDeviceManager.MidiDeviceDTO;
import MidiControl.MidiDeviceManager.MidiSendEngine;
import MidiControl.Mocks.FakeSession;
import MidiControl.Mocks.MockMidiIOManager;
import MidiControl.Mocks.MockMidiOut;
import MidiControl.Mocks.MockMidiServer;
import MidiControl.Mocks.MockSubscriptionManager;
import MidiControl.Server.ServerRouter;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.UserInterface.UiModelService;
import MidiControl.UserInterface.DTO.UiModelDTO;

import java.util.List;
import java.util.EnumMap;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import MidiControl.Access.Authenticate;
import MidiControl.Access.Authenticate.Role;
import MidiControl.Access.RoleCredentialStore;
import MidiControl.Routing.WebSocketEndpoint;

public class ServerRouterTest {

    private static final String TEST_ADMIN_PASSWORD = "test-admin-password";
    private static final String TEST_OPERATOR_PASSWORD = "test-operator-password";
    private static final String TEST_FINGERPRINT_SALT = "test-fingerprint-salt";
    private static final String TEST_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/123.0.0.0 Safari/537.36";

    private static Authenticate.ClientFingerprint testFingerprint() {
        return new Authenticate.ClientFingerprint(TEST_FINGERPRINT_SALT, "192.168.1.20", "Chrome", "Windows");
    }

    private static Authenticate authenticateWithPasswords(String operatorPassword, String adminPassword) {
        EnumMap<Role, RoleCredentialStore.Credential> credentials = new EnumMap<>(Role.class);
        credentials.put(Role.GUEST, new RoleCredentialStore.Credential(true, "", "", 0));
        credentials.put(Role.OPERATOR, credentialFor(operatorPassword));
        credentials.put(Role.ADMIN, credentialFor(adminPassword));
        return new Authenticate(credentials);
    }

    private static RoleCredentialStore.Credential credentialFor(String password) {
        if (password == null || password.isBlank()) {
            return new RoleCredentialStore.Credential(false, "", "", 0);
        }
        return RoleCredentialStore.createCredential(password.toCharArray());
    }

    private static void setClientMetadata(FakeSession session) {
        session.getUserProperties().put(WebSocketEndpoint.CLIENT_IP_PROPERTY, "192.168.1.20");
        session.getUserProperties().put(WebSocketEndpoint.USER_AGENT_PROPERTY, TEST_USER_AGENT);
    }

    private static String passwordProof(
            String password,
            String role,
            String challengeId,
            String passwordSalt,
            int iterations) throws Exception {
        String message = challengeId + "\n"
                + role + "\n"
                + TEST_FINGERPRINT_SALT;
        byte[] verifier = RoleCredentialStore.derivePasswordHash(
                password.toCharArray(),
                RoleCredentialStore.decodeHash(passwordSalt, 16),
                iterations);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(verifier, "HmacSHA256"));
        String proof = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        java.util.Arrays.fill(verifier, (byte) 0);
        return proof;
    }

    private static String passwordProof(String password, JsonObject challenge) throws Exception {
        return passwordProof(
                password,
                challenge.get("role").getAsString(),
                challenge.get("challengeId").getAsString(),
                challenge.get("passwordSalt").getAsString(),
                challenge.get("iterations").getAsInt());
    }

    private static String authenticateAs(Authenticate authenticate, String role, String password, String sessionId) {
        Authenticate.PasswordChallengeResult challenge =
                authenticate.beginPasswordAuthentication(role, TEST_FINGERPRINT_SALT, sessionId);
        if (!challenge.successful()) {
            throw new AssertionError("Could not start " + role + " authentication: " + challenge.message());
        }
        try {
            String proof = passwordProof(
                    password,
                    role,
                    challenge.challengeId(),
                    challenge.passwordSalt(),
                    challenge.iterations());
            Authenticate.AuthenticationResult result = authenticate.completePasswordAuthentication(
                    challenge.challengeId(), sessionId, proof, testFingerprint());
            if (!result.successful()) {
                throw new AssertionError("Could not authenticate " + role + ": " + result.message());
            }
            return result.token();
        } catch (Exception exception) {
            throw new AssertionError("Could not build " + role + " authentication proof", exception);
        }
    }

    private CanonicalRegistry makeMinimalRegistry() {
        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");
        SysexParser parser = new SysexParser(mappings);
        return new CanonicalRegistry(mappings, parser);
    }

    // Minimal UiModelService stub for tests
    private static class MockUiModelService implements UiModelService {
        @Override
        public UiModelDTO buildUiModel(String contextId, String uiType) {
            UiModelDTO dto = new UiModelDTO();
            dto.contextId = contextId;
            dto.controls = List.of();
            return dto;
        }
    }

    private static class Env {
        CanonicalRegistry registry;
        MockMidiServer server;
        MockMidiIOManager io;
        ServerRouter router;
        FakeSession session;
        Authenticate authenticate;
        String authToken;
    }

    private Env makeEnv() {
        Env env = new Env();
        env.registry = makeMinimalRegistry();
        env.server = new MockMidiServer(env.registry);
        env.io = new MockMidiIOManager(env.server);
        env.io.setMidiOutForTest(new MockMidiOut());
        env.server.setMockIo(env.io);

        env.authenticate = authenticateWithPasswords(TEST_OPERATOR_PASSWORD, TEST_ADMIN_PASSWORD);
        env.router = new ServerRouter(
            new MockUiModelService(),
            env.server.getSubscriptionManager(),
            env.registry,
            env.io,
            env.authenticate
        );

        env.session = new FakeSession("1");
        setClientMetadata(env.session);
        env.authToken = authenticateAs(
                env.authenticate, "admin", TEST_ADMIN_PASSWORD, "admin-session-" + env.session.getId());
        return env;
    }
    
    private static JsonObject parseJson(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static String typeOf(String json) {
        return parseJson(json).get("type").getAsString();
    }

    private static JsonObject payloadOf(String json) {
        return parseJson(json).getAsJsonObject("payload");
    }

    private static void sendAsAdmin(Env env, String message) {
        JsonObject request = parseJson(message);
        JsonObject payload = request.getAsJsonObject("payload");
        payload.addProperty("authToken", env.authToken);
        payload.addProperty("fingerprintSalt", TEST_FINGERPRINT_SALT);
        env.router.handleMessage(env.session, request.toString());
    }

    private static String issueAdminToken(Authenticate authenticate) {
        return authenticateAs(authenticate, "admin", TEST_ADMIN_PASSWORD, "admin-token-session");
    }

    private static void sendAsAdmin(ServerRouter router, FakeSession session, String token, String message) {
        sendWithToken(router, session, token, message);
    }

    private static void sendWithToken(ServerRouter router, FakeSession session, String token, String message) {
        setClientMetadata(session);
        JsonObject request = parseJson(message);
        JsonObject payload = request.getAsJsonObject("payload");
        payload.addProperty("authToken", token);
        payload.addProperty("fingerprintSalt", TEST_FINGERPRINT_SALT);
        router.handleMessage(session, request.toString());
    }



    @Test
    void testSetControlValue_UnregisteredCanonicalId_ReturnsError() {
        Env env = makeEnv();

        sendAsAdmin(env, """
            {"requestId": 1,"type":"set-control-value","payload":{"canonicalId":"__definitely_missing__","value":77}}
        """);

        String msg = env.session.lastSent;
        assertNotNull(msg, "Router did not send any response for set-control-value");

        assertEquals("error", typeOf(msg), msg);

        JsonObject payload = payloadOf(msg);
        assertEquals("UNKNOWN_CANONICAL_ID", payload.get("code").getAsString(), msg);
        assertTrue(payload.get("message").getAsString().contains("__definitely_missing__"), msg);

        // Your error schema does not include "status"
        assertTrue(!payload.has("status"), msg);
    }

    @Test
    void testSetControlValue_RegisteredCanonicalId_ReturnsAck() {
        Env env = makeEnv();

        String existingId = env.registry.getAllInstances().stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("Registry has no instances"))
                .getCanonicalId();

        sendAsAdmin(env, """
            {"requestId": 1,"type":"set-control-value","payload":{"canonicalId":"%s","value":77}}
        """.formatted(existingId));

        String msg = env.session.lastSent;
        assertNotNull(msg, "Router did not send any response for set-control-value");

        assertEquals("ack", typeOf(msg), msg);

        JsonObject payload = payloadOf(msg);
        assertEquals("ok", payload.get("status").getAsString(), msg);
    }

    @Test
    void controlChangeRequiresAuthenticationAndOperatorCannotChangeOtherControls() {
        Env env = makeEnv();
        String controlId = env.registry.getAllInstances().stream()
                .filter(instance -> !instance.getCanonicalId().matches(
                        "^kInputToMix\\.kMix\\d+Level\\.\\d+$"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Registry has no instances"))
                .getCanonicalId();
        int originalValue = env.registry.resolveCanonicalId(controlId).getValue();
        String request = """
            {"requestId":"req-control","type":"set-control-value","payload":{"canonicalId":"%s","value":77}}
        """.formatted(controlId);

        env.router.handleMessage(env.session, request);
        assertEquals("UNAUTHENTICATED", payloadOf(env.session.lastSent).get("code").getAsString());
        assertEquals(originalValue, env.registry.resolveCanonicalId(controlId).getValue());

        String operatorToken = authenticateAs(
                env.authenticate, "operator", TEST_OPERATOR_PASSWORD, "operator-token-session");
        sendWithToken(env.router, env.session, operatorToken, request);

        assertEquals("FORBIDDEN", payloadOf(env.session.lastSent).get("code").getAsString());
        assertEquals(originalValue, env.registry.resolveCanonicalId(controlId).getValue());
    }

    @Test
    void authenticateRequestReturnsTokenThatCanBeResumedOnAnotherSession() {
        Env env = makeEnv();
        FakeSession newSession = new FakeSession("new-session");
        setClientMetadata(newSession);

        env.router.handleMessage(newSession, """
            {"requestId":"auth-1","type":"authenticate","payload":{"role":"operator","fingerprintSalt":"%s"}}
        """.formatted(TEST_FINGERPRINT_SALT));

        JsonObject response = parseJson(newSession.lastSent);
        assertEquals("authentication-result", response.get("type").getAsString());
        JsonObject challenge = response.getAsJsonObject("payload");
        assertEquals("challenge", challenge.get("status").getAsString());
        assertFalse(newSession.lastSent.contains(TEST_OPERATOR_PASSWORD));

        String proof;
        try {
            proof = passwordProof(TEST_OPERATOR_PASSWORD, challenge);
        } catch (Exception exception) {
            throw new AssertionError("Could not build test password proof", exception);
        }
        env.router.handleMessage(newSession, """
            {"requestId":"auth-2","type":"authenticate","payload":{"challengeId":"%s","proof":"%s","fingerprintSalt":"%s"}}
        """.formatted(
                challenge.get("challengeId").getAsString(),
                proof,
                TEST_FINGERPRINT_SALT));

        JsonObject payload = parseJson(newSession.lastSent).getAsJsonObject("payload");
        assertEquals("ok", payload.get("status").getAsString());
        assertEquals("operator", payload.get("role").getAsString());
        assertFalse(payload.get("token").getAsString().isBlank());

        String token = payload.get("token").getAsString();
        FakeSession refreshedSession = new FakeSession("refreshed-session");
        setClientMetadata(refreshedSession);
        env.router.handleMessage(refreshedSession, """
            {"requestId":"auth-3","type":"authenticate","payload":{"token":"%s","fingerprintSalt":"%s"}}
        """.formatted(token, TEST_FINGERPRINT_SALT));
        JsonObject resumedPayload = parseJson(refreshedSession.lastSent).getAsJsonObject("payload");
        assertEquals("ok", resumedPayload.get("status").getAsString());
        assertEquals("operator", resumedPayload.get("role").getAsString());
        assertEquals(token, resumedPayload.get("token").getAsString());

        sendWithToken(env.router, refreshedSession, token, """
            {"requestId":"devices-1","type":"list-midi-devices","payload":{}}
        """);
        assertEquals("midi-device-list", parseJson(refreshedSession.lastSent).get("type").getAsString());
    }

    @Test
    void unconfiguredPrivilegedRolesAreNotOfferedAChallenge() {
        Env env = makeEnv();
        Authenticate disabledAuth = authenticateWithPasswords(null, "");
        ServerRouter router = new ServerRouter(
                new MockUiModelService(),
                env.server.getSubscriptionManager(),
                env.registry,
                env.io,
                disabledAuth);
        FakeSession session = new FakeSession("disabled-role-session");
        setClientMetadata(session);

        router.handleMessage(session, """
            {"requestId":"options-1","type":"get-authentication-options"}
        """);
        JsonObject options = parseJson(session.lastSent).getAsJsonObject("payload");
        assertTrue(options.get("guest").getAsBoolean());
        assertFalse(options.get("operator").getAsBoolean());
        assertFalse(options.get("admin").getAsBoolean());

        router.handleMessage(session, """
            {"requestId":"auth-disabled","type":"authenticate","payload":{"role":"operator","fingerprintSalt":"%s"}}
        """.formatted(TEST_FINGERPRINT_SALT));
        JsonObject response = parseJson(session.lastSent);
        assertEquals("error", response.getAsJsonObject("payload").get("status").getAsString());
        assertFalse(response.getAsJsonObject("payload").has("challengeId"));
        assertTrue(response.getAsJsonObject("payload").get("message").getAsString()
                .contains("no password hash is configured"));
    }


    @Test
    void testListMidiDevices() {
        Env env = makeEnv();

        MidiDeviceDTO d1 = new MidiDeviceDTO();
        d1.id = "0";
        d1.name = "DeviceA";
        env.io.devices.add(d1);

        MidiDeviceDTO d2 = new MidiDeviceDTO();
        d2.id = "1";
        d2.name = "DeviceB";
        env.io.devices.add(d2);

        sendAsAdmin(env, """
            {"requestId": 1,"type":"list-midi-devices","payload":{}}
        """);

        String msg = env.session.lastSent;
        assertNotNull(msg, "Router did not send midi-device-list response");
        assertTrue(msg.contains("DeviceA"), msg);
        assertTrue(msg.contains("DeviceB"), msg);
        assertTrue(msg.contains("\"type\":\"midi-device-list\""), msg);
    }

    @Test
    void testSetMidiDeviceSuccess() {
        Env env = makeEnv();
        env.io.setResult = true;

        sendAsAdmin(env, """
            {"requestId": 1,"type":"set-midi-device","payload":{"deviceId":3}}
        """);

        assertEquals(3, env.io.lastSetIndex);
        String msg = env.session.lastSent;
        assertNotNull(msg, "Router did not send response for set-midi-device");
        assertTrue(msg.contains("\"status\":\"ok\""), msg);
    }

    @Test
    void testUnknownType() {
        Env env = makeEnv();

        sendAsAdmin(env, """
            {"requestId": 1,"type":"does-not-exist","payload":{}}
        """);

        String msg = env.session.lastSent;
        assertNotNull(msg, "Router did not send error response for unknown type");
        assertTrue(msg.contains("\"type\":\"error\""), msg);
        assertTrue(msg.contains("UNKNOWN_TYPE"), msg);
    }

    @Test
    void testUnsubscribeContext() {
        CanonicalRegistry registry = makeMinimalRegistry();
        MockMidiServer server = new MockMidiServer(registry);
        MockMidiIOManager io = new MockMidiIOManager(server);
        server.setMockIo(io);

        MockSubscriptionManager subs = new MockSubscriptionManager();

        Authenticate authenticate = authenticateWithPasswords(TEST_OPERATOR_PASSWORD, TEST_ADMIN_PASSWORD);
        ServerRouter router = new ServerRouter(
            new MockUiModelService(),
            subs,
            registry,
            io,
            authenticate
        );

        FakeSession session = new FakeSession("1");
        String token = issueAdminToken(authenticate);

        sendAsAdmin(router, session, token, """
            {"requestId": 1,"type":"unsubscribe-context","payload":{"contextId":"eq1"}}
        """);

        assertEquals("eq1", subs.lastUnsubscribed);
        String msg = session.lastSent;
        assertNotNull(msg, "Router did not send response for unsubscribe-context");
        assertTrue(msg.contains("\"status\":\"ok\""), msg);
    }

    @Test
    void testSubscribeContext() {
        CanonicalRegistry registry = makeMinimalRegistry();
        MockMidiServer server = new MockMidiServer(registry);
        MockMidiIOManager io = new MockMidiIOManager(server);
        server.setMockIo(io);

        MockSubscriptionManager subs = new MockSubscriptionManager();

        Authenticate authenticate = authenticateWithPasswords(TEST_OPERATOR_PASSWORD, TEST_ADMIN_PASSWORD);
        ServerRouter router = new ServerRouter(
            new MockUiModelService(),
            subs,
            registry,
            io,
            authenticate
        );

        FakeSession session = new FakeSession("1");
        String token = issueAdminToken(authenticate);

        sendAsAdmin(router, session, token, """
            {"requestId": 1,"type":"subscribe-context","payload":{"contextId":"eq1"}}
        """);

        assertEquals("eq1", subs.lastSubscribed);
        String msg = session.lastSent;
        assertNotNull(msg, "Router did not send response for subscribe-context");
        assertTrue(msg.contains("\"status\":\"ok\""), msg);
    }

    @Test
    void testGetUiModel() {
        CanonicalRegistry registry = makeMinimalRegistry();
        MockMidiServer server = new MockMidiServer(registry);
        MockMidiIOManager io = new MockMidiIOManager(server);
        server.setMockIo(io);

        Authenticate authenticate = authenticateWithPasswords(TEST_OPERATOR_PASSWORD, TEST_ADMIN_PASSWORD);
        ServerRouter router = new ServerRouter(
            new MockUiModelService(),
            new MockSubscriptionManager(),
            registry,
            io,
            authenticate
        );

        FakeSession session = new FakeSession("1");
        String token = issueAdminToken(authenticate);

        sendAsAdmin(router, session, token, """
            {"type":"get-ui-model","payload":{"contextId":"ch1"}}
        """);

        String msg = session.lastSent;
        assertNotNull(msg, "Router did not send ui-model response");
        assertTrue(msg.contains("\"type\":\"ui-model\""), msg);
        assertTrue(msg.contains("\"contextId\":\"ch1\""), msg);
    }

    @Test
    void testUpdateSettingChangesMappings() {
        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");
        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));
        assertNull(registry.getGroup("kMixToMatrix"));

        MockMidiServer server = new MockMidiServer(registry);
        MockMidiIOManager io = new MockMidiIOManager(server);
        server.setMockIo(io);
        io.sendEngine = new MidiSendEngine(io.getMidiOut(), 100,100);

        Authenticate authenticate = authenticateWithPasswords(TEST_OPERATOR_PASSWORD, TEST_ADMIN_PASSWORD);
        ServerRouter router = new ServerRouter(
            new MockUiModelService(),
            new MockSubscriptionManager(),
            registry,
            io,
            authenticate
        );

        FakeSession session = new FakeSession("1");
        String token = issueAdminToken(authenticate);

        sendAsAdmin(router, session, token, """
            {"type": "apply-midi-settings", "requestId": "req-1", "payload": {"inputDeviceId":0,"outputDeviceId":0,"consoleType":"YAMAHA_M7CL","safeprofile":"true","mainprofile":"false","highprofile":"false"}}
        """);

        //m7cl will have controls that 01v96 has not and vice versa
        assertNull(registry.getGroup("kAUXToMatrix"));
        assertNotNull(registry.getGroup("kMixToMatrix"));
    }
}
