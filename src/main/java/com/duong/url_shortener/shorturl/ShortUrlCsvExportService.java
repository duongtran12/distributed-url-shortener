package com.duong.url_shortener.shorturl;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

@Service
public class ShortUrlCsvExportService {

	private static final int MAX_EXPORT_ROWS = 10_000;
	private static final byte[] UTF_8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

	private final ShortUrlService shortUrlService;

	public ShortUrlCsvExportService(ShortUrlService shortUrlService) {
		this.shortUrlService = shortUrlService;
	}

	public CsvExport export(
			Long userId,
			String query,
			String tag,
			ShortUrlStatus status,
			Boolean pinned,
			ShortUrlSort sort) {
		ShortUrlPageResponse page = shortUrlService.findAllOwnedBy(
				userId, 0, MAX_EXPORT_ROWS, query, tag, status, pinned, sort);
		StringBuilder csv = new StringBuilder(
				"short_code,short_url,title,tag,destination,status,pinned,custom_alias,clicks,expires_at,created_at\r\n");
		page.content().forEach(link -> csv
				.append(cell(link.shortCode())).append(',')
				.append(cell(link.shortUrl())).append(',')
				.append(cell(link.title())).append(',')
				.append(cell(link.tag())).append(',')
				.append(cell(link.originalUrl())).append(',')
				.append(link.status()).append(',')
				.append(link.pinned()).append(',')
				.append(link.customAlias()).append(',')
				.append(link.clickCount()).append(',')
				.append(cell(link.expiresAt())).append(',')
				.append(cell(link.createdAt())).append("\r\n"));

		byte[] content = csv.toString().getBytes(StandardCharsets.UTF_8);
		byte[] excelCompatibleContent = new byte[UTF_8_BOM.length + content.length];
		System.arraycopy(UTF_8_BOM, 0, excelCompatibleContent, 0, UTF_8_BOM.length);
		System.arraycopy(content, 0, excelCompatibleContent, UTF_8_BOM.length, content.length);
		return new CsvExport("shortwave-links-%s.csv".formatted(LocalDate.now()), excelCompatibleContent);
	}

	private String cell(Object value) {
		if (value == null) {
			return "";
		}
		String text = value.toString();
		if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
			text = "'" + text;
		}
		return "\"" + text.replace("\"", "\"\"") + "\"";
	}

	public record CsvExport(String filename, byte[] content) {
	}
}
