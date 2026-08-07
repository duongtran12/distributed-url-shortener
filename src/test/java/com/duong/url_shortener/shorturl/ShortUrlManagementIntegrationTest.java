package com.duong.url_shortener.shorturl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

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
import org.springframework.jdbc.core.JdbcTemplate;
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
	private ShortUrlAuditRepository auditRepository;

	@Autowired
	private JwtTokenService jwtTokenService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ShortUrlProperties shortUrlProperties;

	private User owner;
	private User otherUser;
	private String ownerToken;

	@BeforeEach
	void setUp() {
		auditRepository.deleteAll();
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
				ShortUrl.create(
						owner,
						"ownedtitle",
						"https://docs.example.com/reference",
						"Spring Handbook",
						false,
						null));
		ShortUrl disabled = ShortUrl.create(
				owner, "archive1", "https://example.com/spring-archive", false, null);
		disabled.disable();
		shortUrlRepository.saveAndFlush(disabled);
		shortUrlRepository.saveAndFlush(
				ShortUrl.create(
						otherUser,
						"private1",
						"https://example.com/private",
						"Spring Handbook Private",
						false,
						null));

		mockMvc.perform(get("/api/v1/urls")
				.param("query", "HANDBOOK")
				.param("status", "ACTIVE")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].shortCode").value("ownedtitle"))
				.andExpect(jsonPath("$.content[0].title").value("Spring Handbook"))
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
	void shouldFilterOwnedUrlsByNormalizedTag() throws Exception {
		shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "tagged01", "https://example.com/campaign", "Campaign", "marketing", false, null));
		shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "tagged02", "https://example.com/internal", "Internal", "internal", false, null));
		shortUrlRepository.saveAndFlush(
				ShortUrl.create(otherUser, "tagged03", "https://example.com/private", "Private", "marketing", false, null));

		mockMvc.perform(get("/api/v1/urls")
				.param("tag", "MARKETING")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].shortCode").value("tagged01"))
				.andExpect(jsonPath("$.content[0].tag").value("marketing"));
	}

	@Test
	void shouldListDistinctSortedTagsForTheOwner() throws Exception {
		shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "taglist01", "https://example.com/one", "One", "social", false, null));
		shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "taglist02", "https://example.com/two", "Two", "marketing", false, null));
		shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "taglist03", "https://example.com/three", "Three", "social", false, null));
		shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "taglist04", "https://example.com/four", false, null));
		shortUrlRepository.saveAndFlush(
				ShortUrl.create(otherUser, "taglist05", "https://example.com/private", "Private", "private", false, null));

		mockMvc.perform(get("/api/v1/urls/tags")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0]").value("marketing"))
				.andExpect(jsonPath("$[1]").value("social"));
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
	void shouldSortOwnedUrlsByClicksWithStablePagination() throws Exception {
		ShortUrl low = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "sortlow", "https://example.com/low", false, null));
		ShortUrl high = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "sorthigh", "https://example.com/high", false, null));
		ShortUrl middle = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "sortmid", "https://example.com/middle", false, null));
		ShortUrl foreign = shortUrlRepository.saveAndFlush(
				ShortUrl.create(otherUser, "sortforeign", "https://example.com/foreign", false, null));

		setClickCount(low.getId(), 2);
		setClickCount(high.getId(), 20);
		setClickCount(middle.getId(), 7);
		setClickCount(foreign.getId(), 100);

		mockMvc.perform(get("/api/v1/urls")
				.param("sort", "MOST_CLICKED")
				.param("size", "2")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].shortCode").value("sorthigh"))
				.andExpect(jsonPath("$.content[0].clickCount").value(20))
				.andExpect(jsonPath("$.content[1].shortCode").value("sortmid"))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.totalPages").value(2));

		mockMvc.perform(get("/api/v1/urls")
				.param("sort", "MOST_CLICKED")
				.param("size", "2")
				.param("page", "1")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].shortCode").value("sortlow"));

		mockMvc.perform(get("/api/v1/urls")
				.param("sort", "POPULAR")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isBadRequest());
	}

	@Test
	void shouldPinOwnedUrlAboveTheRequestedSortOrder() throws Exception {
		ShortUrl pinned = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "pinfirst", "https://example.com/pinned", false, null));
		ShortUrl popular = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "popular", "https://example.com/popular", false, null));
		ShortUrl foreign = shortUrlRepository.saveAndFlush(
				ShortUrl.create(otherUser, "foreignpin", "https://example.com/foreign", false, null));
		setClickCount(pinned.getId(), 1);
		setClickCount(popular.getId(), 100);

		updatePin(pinned.getId(), true)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pinned").value(true));

		mockMvc.perform(get("/api/v1/urls")
				.param("pinned", "true")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].shortCode").value("pinfirst"))
				.andExpect(jsonPath("$.totalElements").value(1));

		mockMvc.perform(get("/api/v1/urls")
				.param("pinned", "false")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].shortCode").value("popular"));

		mockMvc.perform(get("/api/v1/urls")
				.param("sort", "MOST_CLICKED")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].shortCode").value("pinfirst"))
				.andExpect(jsonPath("$.content[1].shortCode").value("popular"));

		updatePin(foreign.getId(), true)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SHORT_URL_NOT_FOUND"));

		mockMvc.perform(patch("/api/v1/urls/{id}/pin", pinned.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
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
	void shouldExportFilteredOwnedUrlsAsSafeCsv() throws Exception {
		ShortUrl exported = ShortUrl.create(
				owner, "csvowned", "https://example.com/a,b", "=SUM(1,2)", "reports", false, null);
		exported.disable();
		exported.setPinned(true);
		shortUrlRepository.saveAndFlush(exported);
		shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "csvactive", "https://example.com/active", false, null));
		ShortUrl foreign = ShortUrl.create(
				otherUser, "csvforeign", "https://example.com/foreign", "Foreign", "reports", false, null);
		foreign.disable();
		shortUrlRepository.saveAndFlush(foreign);

		byte[] content = mockMvc.perform(get("/api/v1/urls/export")
				.param("tag", "reports")
				.param("status", "DISABLED")
				.param("pinned", "true")
				.param("sort", "OLDEST")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/csv;charset=UTF-8"))
				.andExpect(header().string(
						HttpHeaders.CONTENT_DISPOSITION,
						org.hamcrest.Matchers.containsString("shortwave-links-")))
				.andReturn().getResponse().getContentAsByteArray();

		assertEquals((byte) 0xEF, content[0]);
		String csv = new String(content, 3, content.length - 3, StandardCharsets.UTF_8);
		assertTrue(csv.contains("\"csvowned\""));
		assertTrue(csv.contains("\"https://example.com/a,b\""));
		assertTrue(csv.contains("\"'=SUM(1,2)\""));
		assertFalse(csv.contains("csvactive"));
		assertFalse(csv.contains("csvforeign"));
		assertEquals(2, csv.lines().count());
	}

	@Test
	void shouldDuplicateOwnedUrlWithANewGeneratedCode() throws Exception {
		ShortUrl source = shortUrlRepository.saveAndFlush(
				ShortUrl.create(
						owner,
						"sourcealias",
						"https://example.com/campaign",
						"Campaign",
						"marketing",
						true,
						java.time.Instant.parse("2099-12-31T23:59:59Z")));
		source.disable();
		shortUrlRepository.saveAndFlush(source);

		mockMvc.perform(post("/api/v1/urls/{id}/duplicate", source.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(source.getId().intValue())))
				.andExpect(jsonPath("$.shortCode").value(org.hamcrest.Matchers.not("sourcealias")))
				.andExpect(jsonPath("$.originalUrl").value("https://example.com/campaign"))
				.andExpect(jsonPath("$.title").value("Campaign"))
				.andExpect(jsonPath("$.tag").value("marketing"))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.customAlias").value(false))
				.andExpect(jsonPath("$.expiresAt").value("2099-12-31T23:59:59Z"));

		assertEquals(2, shortUrlRepository.count());
	}

	@Test
	void shouldRejectDuplicatingBlockedOrForeignUrls() throws Exception {
		ShortUrl blocked = ShortUrl.create(owner, "blockedcopy", "https://example.com/blocked", false, null);
		blocked.block();
		shortUrlRepository.saveAndFlush(blocked);
		ShortUrl foreign = shortUrlRepository.saveAndFlush(
				ShortUrl.create(otherUser, "foreigncopy", "https://example.com/foreign", false, null));

		mockMvc.perform(post("/api/v1/urls/{id}/duplicate", blocked.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("SHORT_URL_BLOCKED"));

		mockMvc.perform(post("/api/v1/urls/{id}/duplicate", foreign.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SHORT_URL_NOT_FOUND"));
		assertEquals(2, shortUrlRepository.count());
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
	void shouldUpdateOwnedUrlDetailsAndClearTitle() throws Exception {
		ShortUrl shortUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "editable", "https://example.com/old", false, null));

		mockMvc.perform(put("/api/v1/urls/{id}", shortUrl.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "originalUrl": "https://example.com/new",
						  "title": "  Updated campaign  ",
						  "tag": "MARKETING",
						  "expiresAt": "2099-12-31T23:59:59Z"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.shortCode").value("editable"))
				.andExpect(jsonPath("$.originalUrl").value("https://example.com/new"))
				.andExpect(jsonPath("$.title").value("Updated campaign"))
				.andExpect(jsonPath("$.tag").value("marketing"))
				.andExpect(jsonPath("$.expiresAt").value("2099-12-31T23:59:59Z"));

		mockMvc.perform(put("/api/v1/urls/{id}", shortUrl.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "originalUrl": "https://example.com/new",
						  "title": "",
						  "tag": "",
						  "expiresAt": "2099-12-31T23:59:59Z"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").doesNotExist())
				.andExpect(jsonPath("$.tag").doesNotExist());

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

	@Test
	void shouldReturnOnlyOwnedAuditEventsAndPreserveDeletedShortCode() throws Exception {
		ShortUrl ownedUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "auditowned", "https://example.com/owned", false, null));
		ShortUrl foreignUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(otherUser, "auditforeign", "https://example.com/foreign", false, null));

		updatePin(ownedUrl.getId(), true).andExpect(status().isOk());
		mockMvc.perform(delete("/api/v1/urls/{id}", ownedUrl.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isNoContent());

		String otherToken = jwtTokenService.createAccessToken(otherUser);
		mockMvc.perform(patch("/api/v1/urls/{id}/pin", foreignUrl.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"pinned": true}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/audit/short-urls")
				.param("page", "0")
				.param("size", "10")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.content[0].action").value("DELETED"))
				.andExpect(jsonPath("$.content[0].shortCode").value("auditowned"))
				.andExpect(jsonPath("$.content[0].shortUrlId").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.content[1].action").value("PIN_CHANGED"));

		mockMvc.perform(get("/api/v1/audit/short-urls")
				.param("action", "DELETED")
				.param("page", "0")
				.param("size", "10")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].action").value("DELETED"))
				.andExpect(jsonPath("$.content[0].shortCode").value("auditowned"));
	}

	@Test
	void shouldUpdateAndDeleteOwnedUrlsInBulk() throws Exception {
		ShortUrl first = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "bulkone", "https://example.com/one", false, null));
		ShortUrl second = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "bulktwo", "https://example.com/two", false, null));

		bulkRequest(first.getId(), second.getId(), "DISABLE")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.action").value("DISABLE"))
				.andExpect(jsonPath("$.affected").value(2));
		assertEquals(ShortUrlStatus.DISABLED, shortUrlRepository.findById(first.getId()).orElseThrow().getStatus());
		assertEquals(ShortUrlStatus.DISABLED, shortUrlRepository.findById(second.getId()).orElseThrow().getStatus());

		bulkRequest(first.getId(), second.getId(), "DELETE")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.affected").value(2));
		assertEquals(0, shortUrlRepository.count());
	}

	@Test
	void shouldRejectBulkRequestAtomicallyWhenAUrlIsNotOwned() throws Exception {
		ShortUrl owned = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "bulkowned", "https://example.com/owned", false, null));
		ShortUrl foreign = shortUrlRepository.saveAndFlush(
				ShortUrl.create(otherUser, "bulkforeign", "https://example.com/foreign", false, null));

		bulkRequest(owned.getId(), foreign.getId(), "DISABLE")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SHORT_URL_NOT_FOUND"));

		assertEquals(ShortUrlStatus.ACTIVE, shortUrlRepository.findById(owned.getId()).orElseThrow().getStatus());
		assertEquals(ShortUrlStatus.ACTIVE, shortUrlRepository.findById(foreign.getId()).orElseThrow().getStatus());
	}

	@Test
	void shouldSetAndClearTagsInBulk() throws Exception {
		ShortUrl first = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "tagbulkone", "https://example.com/one", false, null));
		ShortUrl second = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "tagbulktwo", "https://example.com/two", false, null));

		bulkTagRequest(first.getId(), second.getId(), "SET_TAG", "MARKETING")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.action").value("SET_TAG"))
				.andExpect(jsonPath("$.affected").value(2));
		assertEquals("marketing", shortUrlRepository.findById(first.getId()).orElseThrow().getTag());
		assertEquals("marketing", shortUrlRepository.findById(second.getId()).orElseThrow().getTag());

		bulkRequest(first.getId(), second.getId(), "CLEAR_TAG")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.action").value("CLEAR_TAG"));
		assertEquals(null, shortUrlRepository.findById(first.getId()).orElseThrow().getTag());
		assertEquals(null, shortUrlRepository.findById(second.getId()).orElseThrow().getTag());

		bulkRequest(first.getId(), second.getId(), "SET_TAG")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BULK_TAG_REQUIRED"));
	}

	private org.springframework.test.web.servlet.ResultActions bulkRequest(
			Long firstId,
			Long secondId,
			String action) throws Exception {
		return mockMvc.perform(post("/api/v1/urls/bulk")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"ids": [%d, %d], "action": "%s"}
						""".formatted(firstId, secondId, action)));
	}

	private org.springframework.test.web.servlet.ResultActions bulkTagRequest(
			Long firstId,
			Long secondId,
			String action,
			String tag) throws Exception {
		return mockMvc.perform(post("/api/v1/urls/bulk")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"ids": [%d, %d], "action": "%s", "tag": "%s"}
						""".formatted(firstId, secondId, action, tag)));
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

	private org.springframework.test.web.servlet.ResultActions updatePin(
			Long id,
			boolean pinned) throws Exception {
		return mockMvc.perform(patch("/api/v1/urls/{id}/pin", id)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"pinned": %s}
						""".formatted(pinned)));
	}

	private void setClickCount(Long id, long clicks) {
		jdbcTemplate.update("UPDATE short_urls SET click_count = ? WHERE id = ?", clicks, id);
	}
}
