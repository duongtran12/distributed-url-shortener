package com.duong.url_shortener.shorturl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

@Service
public class ShortUrlQrCodeService {

	private static final int IMAGE_SIZE = 512;

	private final ShortUrlService shortUrlService;

	public ShortUrlQrCodeService(ShortUrlService shortUrlService) {
		this.shortUrlService = shortUrlService;
	}

	public QrCodeImage generate(Long userId, Long shortUrlId) {
		ShortUrlResponse shortUrl = shortUrlService.findOwnedById(userId, shortUrlId);
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			var matrix = new QRCodeWriter().encode(
					shortUrl.shortUrl(),
					BarcodeFormat.QR_CODE,
					IMAGE_SIZE,
					IMAGE_SIZE,
					Map.of(
							EncodeHintType.CHARACTER_SET, "UTF-8",
							EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
							EncodeHintType.MARGIN, 2));
			MatrixToImageWriter.writeToStream(matrix, "PNG", output);
			return new QrCodeImage(shortUrl.shortCode() + "-qr.png", output.toByteArray());
		} catch (WriterException | IOException exception) {
			throw new IllegalStateException("Could not generate the short URL QR code", exception);
		}
	}

	public record QrCodeImage(String filename, byte[] content) {
	}
}
