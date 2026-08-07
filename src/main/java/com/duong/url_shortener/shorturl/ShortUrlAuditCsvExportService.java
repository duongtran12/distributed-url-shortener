package com.duong.url_shortener.shorturl;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

@Service
public class ShortUrlAuditCsvExportService {

	private static final int MAX_EXPORT_ROWS = 10_000;
	private static final byte[] UTF_8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

	private final ShortUrlAuditService auditService;

	public ShortUrlAuditCsvExportService(ShortUrlAuditService auditService) {
		this.auditService = auditService;
	}

	public CsvExport export(Long userId, ShortUrlAuditAction action) {
		ShortUrlAuditPageResponse page = auditService.findAll(userId, action, 0, MAX_EXPORT_ROWS);
		StringBuilder csv = new StringBuilder("timestamp,action,short_code,details\r\n");
		page.content().forEach(event -> csv
				.append(cell(event.createdAt())).append(',')
				.append(event.action()).append(',')
				.append(cell(event.shortCode())).append(',')
				.append(cell(event.details())).append("\r\n"));

		byte[] content = csv.toString().getBytes(StandardCharsets.UTF_8);
		byte[] excelCompatibleContent = new byte[UTF_8_BOM.length + content.length];
		System.arraycopy(UTF_8_BOM, 0, excelCompatibleContent, 0, UTF_8_BOM.length);
		System.arraycopy(content, 0, excelCompatibleContent, UTF_8_BOM.length, content.length);
		return new CsvExport("shortwave-audit-%s.csv".formatted(LocalDate.now()), excelCompatibleContent);
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
