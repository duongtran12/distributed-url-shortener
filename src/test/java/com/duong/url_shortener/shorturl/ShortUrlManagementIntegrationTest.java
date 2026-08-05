package com.duong.url_shortener.shorturl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.duong.url_shortener.security.JwtTokenService;
import com.duong.url_shortener.user.User;
import com.duong.url_shortener.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "debug=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ShortUrlManagementIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ShortUrlRepository shortUrlRepository;

	@Autowired
	private JwtTokenService jwtTokenService;

	@Autowired
	private ShortUrlProperties shortUrlProperties;

	private User owner;
	private User otherUser;
	private String ownerToken;

	@BeforeEach
	void setUp() {
		shortUrlRepository.deleteAll();
		userRepository.deleteAll();
		owner = userRepository.saveAndFlush(
				User.create("owner@example.com", "encoded-password", "Owner"));
		otherUser = userRepository.saveAndFlush(
				User.create("other@example.com", "encoded-password", "Other"));
		ownerToken = jwtTokenService.createAccessToken(owner);
	}

	@Test
	void shouldListOnlyOwnedUrlsWithPagination() throws Exception {
		shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "owned01", "https://example.com/one", false, null));
		shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "owned02", "https://example.com/two", false, null));
		shortUrlRepository.saveAndFlush(
				ShortUrl.create(otherUser, "foreign", "https://example.com/foreign", false, null));

		mockMvc.perform(get("/api/v1/urls")
				.param("page", "0")
				.param("size", "1")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(1))
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.totalPages").value(2));
	}

	@Test
	void shouldSearchAndFilterOnlyOwnedUrls() throws Exception {
		shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "SpringDocs", "https://docs.spring.io/reference", false, null));
		ShortUrl disabled = ShortUrl.create(
				owner, "archive1", "https://example.com/spring-archive", false, null);
		disabled.disable();
		shortUrlRepository.saveAndFlush(disabled);
		shortUrlRepository.saveAndFlush(
				ShortUrl.create(otherUser, "spring-private", "https://example.com/spring", false, null));

		mockMvc.perform(get("/api/v1/urls")
				.param("query", "SPRING")
				.param("status", "ACTIVE")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].shortCode").value("SpringDocs"))
				.andExpect(jsonPath("$.totalElements").value(1));

		mockMvc.perform(get("/api/v1/urls")
				.param("query", "spring")
				.param("status", "DISABLED")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].shortCode").value("archive1"));
	}

	@Test
	void shouldTreatSearchWildcardsAsLiteralCharactersAndLimitQueryLength() throws Exception {
		shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "percent1", "https://example.com/rate%25", false, null));
		shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "ordinary", "https://example.com/plain", false, null));

		mockMvc.perform(get("/api/v1/urls")
				.param("query", "%")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].shortCode").value("percent1"));

		mockMvc.perform(get("/api/v1/urls")
				.param("query", "a".repeat(201))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturnOwnedUrlDetails() throws Exception {
		ShortUrl shortUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "details", "https://example.com/details", true, null));

		mockMvc.perform(get("/api/v1/urls/{id}", shortUrl.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(shortUrl.getId()))
				.andExpect(jsonPath("$.shortCode").value("details"))
				.andExpect(jsonPath("$.originalUrl").value("https://example.com/details"));
	}

	@Test
	void shouldGenerateQrCodeForOwnedUrlAndHideForeignUrl() throws Exception {
		ShortUrl ownedUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "qrtest01", "https://example.com/qr", false, null));
		ShortUrl foreignUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(otherUser, "qrforeign", "https://example.com/private", false, null));

		byte[] png = mockMvc.perform(get("/api/v1/urls/{id}/qr", ownedUrl.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_PNG))
				.andExpect(header().string(
						HttpHeaders.CONTENT_DISPOSITION,
						"inline; filename=\"qrtest01-qr.png\""))
				.andReturn()
				.getResponse()
				.getContentAsByteArray();

		var image = ImageIO.read(new ByteArrayInputStream(png));
		var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
		String decodedUrl = new MultiFormatReader().decode(bitmap).getText();
		String baseUrl = shortUrlProperties.baseUrl().replaceAll("/$", "");
		assertEquals(baseUrl + "/qrtest01", decodedUrl);

		mockMvc.perform(get("/api/v1/urls/{id}/qr", foreignUrl.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SHORT_URL_NOT_FOUND"));
	}

	@Test
	void shouldHideUrlOwnedByAnotherUser() throws Exception {
		ShortUrl shortUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(otherUser, "private", "https://example.com/private", false, null));

		mockMvc.perform(get("/api/v1/urls/{id}", shortUrl.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SHORT_URL_NOT_FOUND"));
	}

	@Test
	void shouldRejectInvalidPaginationAndRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/urls")
				.param("size", "101")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/v1/urls"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldDisableAndReEnableOwnedUrl() throws Exception {
		ShortUrl shortUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "toggle1", "https://example.com/toggle", false, null));

		updateStatus(shortUrl.getId(), "DISABLED")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DISABLED"));

		mockMvc.perform(get("/{shortCode}", "toggle1"))
				.andExpect(status().isGone());

		updateStatus(shortUrl.getId(), "ACTIVE")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACTIVE"));
	}

	@Test
	void shouldNotUpdateAnotherUsersUrlOrAcceptBlockedStatus() throws Exception {
		ShortUrl foreignUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(otherUser, "foreign2", "https://example.com/foreign", false, null));

		updateStatus(foreignUrl.getId(), "DISABLED")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SHORT_URL_NOT_FOUND"));

		ShortUrl ownedUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "owned03", "https://example.com/owned", false, null));

		updateStatus(ownedUrl.getId(), "BLOCKED")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

		ownedUrl.block();
		shortUrlRepository.saveAndFlush(ownedUrl);

		updateStatus(ownedUrl.getId(), "ACTIVE")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("SHORT_URL_BLOCKED"));
	}

	@Test
	void shouldUpdateOwnedUrlDestinationAndExpiration() throws Exception {
		ShortUrl shortUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "editable", "https://example.com/old", false, null));

		mockMvc.perform(put("/api/v1/urls/{id}", shortUrl.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "originalUrl": "https://example.com/new",
						  "expiresAt": "2099-12-31T23:59:59Z"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.shortCode").value("editable"))
				.andExpect(jsonPath("$.originalUrl").value("https://example.com/new"))
				.andExpect(jsonPath("$.expiresAt").value("2099-12-31T23:59:59Z"));

		mockMvc.perform(get("/{shortCode}", "editable"))
				.andExpect(status().isFound())
				.andExpect(header().string(HttpHeaders.LOCATION, "https://example.com/new"));
	}

	@Test
	void shouldRejectInvalidUpdateAndHideForeignUrl() throws Exception {
		ShortUrl foreignUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(otherUser, "foreign3", "https://example.com/foreign", false, null));

		mockMvc.perform(put("/api/v1/urls/{id}", foreignUrl.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"originalUrl": "https://example.com/new"}
						"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SHORT_URL_NOT_FOUND"));

		ShortUrl ownedUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "owned04", "https://example.com/owned", false, null));

		mockMvc.perform(put("/api/v1/urls/{id}", ownedUrl.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "originalUrl": "javascript:alert(1)",
						  "expiresAt": "2020-01-01T00:00:00Z"
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_URL_SCHEME"));
	}

	@Test
	void shouldDeleteOwnedUrlAndStopRedirecting() throws Exception {
		ShortUrl shortUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "remove1", "https://example.com/remove", false, null));

		mockMvc.perform(delete("/api/v1/urls/{id}", shortUrl.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/{shortCode}", "remove1"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SHORT_URL_NOT_FOUND"));
	}

	@Test
	void shouldNotDeleteAnotherUsersUrl() throws Exception {
		ShortUrl foreignUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(otherUser, "foreign4", "https://example.com/foreign", false, null));

		mockMvc.perform(delete("/api/v1/urls/{id}", foreignUrl.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SHORT_URL_NOT_FOUND"));

		mockMvc.perform(get("/{shortCode}", "foreign4"))
				.andExpect(status().isFound());
	}

	private org.springframework.test.web.servlet.ResultActions updateStatus(
			Long id,
			String newStatus) throws Exception {
		return mockMvc.perform(patch("/api/v1/urls/{id}/status", id)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"status": "%s"}
						""".formatted(newStatus)));
	}
}
