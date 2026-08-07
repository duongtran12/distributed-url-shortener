package com.duong.url_shortener.shorturl;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/urls")
@Validated
public class ShortUrlController {

	private final ShortUrlService shortUrlService;
	private final ShortUrlQrCodeService shortUrlQrCodeService;
	private final ShortUrlCsvExportService shortUrlCsvExportService;

	public ShortUrlController(
			ShortUrlService shortUrlService,
			ShortUrlQrCodeService shortUrlQrCodeService,
			ShortUrlCsvExportService shortUrlCsvExportService) {
		this.shortUrlService = shortUrlService;
		this.shortUrlQrCodeService = shortUrlQrCodeService;
		this.shortUrlCsvExportService = shortUrlCsvExportService;
	}

	@GetMapping(value = "/export", produces = "text/csv")
	public ResponseEntity<byte[]> export(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) @Size(max = 200) String query,
			@RequestParam(required = false) @Size(max = 32) String tag,
			@RequestParam(required = false) ShortUrlStatus status,
			@RequestParam(required = false) Boolean pinned,
			@RequestParam(defaultValue = "NEWEST") ShortUrlSort sort) {
		ShortUrlCsvExportService.CsvExport export = shortUrlCsvExportService.export(
				jwt.getClaim("uid"), query, tag, status, pinned, sort);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
				.header(
						HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(export.filename()).build().toString())
				.body(export.content());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ShortUrlResponse create(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateShortUrlRequest request) {
		return shortUrlService.create(jwt.getClaim("uid"), request);
	}

	@GetMapping
	public ShortUrlPageResponse findAll(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(required = false) @Size(max = 200) String query,
			@RequestParam(required = false) @Size(max = 32) String tag,
			@RequestParam(required = false) ShortUrlStatus status,
			@RequestParam(required = false) Boolean pinned,
			@RequestParam(defaultValue = "NEWEST") ShortUrlSort sort) {
		return shortUrlService.findAllOwnedBy(jwt.getClaim("uid"), page, size, query, tag, status, pinned, sort);
	}

	@PostMapping("/bulk")
	public BulkShortUrlResponse bulkUpdate(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody BulkShortUrlRequest request) {
		return shortUrlService.bulkUpdate(jwt.getClaim("uid"), request);
	}

	@GetMapping("/{id}")
	public ShortUrlResponse findById(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable @Min(1) Long id) {
		return shortUrlService.findOwnedById(jwt.getClaim("uid"), id);
	}

	@PostMapping("/{id}/duplicate")
	@ResponseStatus(HttpStatus.CREATED)
	public ShortUrlResponse duplicate(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable @Min(1) Long id) {
		return shortUrlService.duplicate(jwt.getClaim("uid"), id);
	}

	@GetMapping(value = "/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<byte[]> getQrCode(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable @Min(1) Long id) {
		ShortUrlQrCodeService.QrCodeImage image =
				shortUrlQrCodeService.generate(jwt.getClaim("uid"), id);
		return ResponseEntity.ok()
				.contentType(MediaType.IMAGE_PNG)
				.header(
						HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.inline().filename(image.filename()).build().toString())
				.body(image.content());
	}

	@PatchMapping("/{id}/status")
	public ShortUrlResponse updateStatus(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable @Min(1) Long id,
			@Valid @RequestBody UpdateShortUrlStatusRequest request) {
		return shortUrlService.updateStatus(jwt.getClaim("uid"), id, request);
	}

	@PatchMapping("/{id}/pin")
	public ShortUrlResponse updatePin(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable @Min(1) Long id,
			@Valid @RequestBody UpdateShortUrlPinRequest request) {
		return shortUrlService.updatePin(jwt.getClaim("uid"), id, request);
	}

	@PutMapping("/{id}")
	public ShortUrlResponse update(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable @Min(1) Long id,
			@Valid @RequestBody UpdateShortUrlRequest request) {
		return shortUrlService.update(jwt.getClaim("uid"), id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable @Min(1) Long id) {
		shortUrlService.delete(jwt.getClaim("uid"), id);
	}
}
