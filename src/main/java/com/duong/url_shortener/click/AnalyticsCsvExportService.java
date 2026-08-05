package com.duong.url_shortener.click;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

@Service
public class AnalyticsCsvExportService {

	private static final byte[] UTF_8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

	private final AnalyticsOverviewService analyticsOverviewService;

	public AnalyticsCsvExportService(AnalyticsOverviewService analyticsOverviewService) {
		this.analyticsOverviewService = analyticsOverviewService;
	}

	public AnalyticsCsvExport export(Long userId, LocalDate from, LocalDate to) {
		AnalyticsOverviewResponse overview = analyticsOverviewService.getOverview(userId, from, to);
		StringBuilder csv = new StringBuilder("date,clicks\r\n");
		overview.dailyClicks().forEach(day -> csv
				.append(day.date())
				.append(',')
				.append(day.clicks())
				.append("\r\n"));

		byte[] content = csv.toString().getBytes(StandardCharsets.UTF_8);
		byte[] excelCompatibleContent = new byte[UTF_8_BOM.length + content.length];
		System.arraycopy(UTF_8_BOM, 0, excelCompatibleContent, 0, UTF_8_BOM.length);
		System.arraycopy(content, 0, excelCompatibleContent, UTF_8_BOM.length, content.length);

		String filename = "shortwave-analytics-%s-to-%s.csv"
				.formatted(overview.from(), overview.to());
		return new AnalyticsCsvExport(filename, excelCompatibleContent);
	}

	public record AnalyticsCsvExport(String filename, byte[] content) {
	}
}
