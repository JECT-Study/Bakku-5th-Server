package com.ject.image.domain.domain;

import com.ject.image.domain.exception.ImageNotFoundException;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ImageKey(ImagePurpose purpose, Long uploaderId, Long targetId, String fileName) {

    private static final Pattern PATTERN = Pattern.compile(
            "^(post|exchange|chat)/(tmp/)?(\\d+)/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp))$");

    public static ImageKey newTmp(ImagePurpose purpose, Long uploaderId, ImageFormat format) {
        return new ImageKey(purpose, uploaderId, null, UUID.randomUUID() + "." + format.getExtension());
    }

    public static ImageKey parse(String value) {
        Matcher matcher = PATTERN.matcher(value);
        if (!matcher.matches()) {
            throw new ImageNotFoundException(value);
        }
        ImagePurpose purpose = ImagePurpose.fromPrefix(matcher.group(1));
        Long id = Long.valueOf(matcher.group(3));
        boolean tmp = matcher.group(2) != null;
        return new ImageKey(purpose, tmp ? id : null, tmp ? null : id, matcher.group(4));
    }

    public boolean isTmp() {
        return uploaderId != null;
    }

    public String value() {
        String owner = isTmp() ? "tmp/" + uploaderId : String.valueOf(targetId);
        return purpose.prefix() + "/" + owner + "/" + fileName;
    }

    public ImageFormat format() {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
        return ImageFormat.fromExtension(extension).orElseThrow();
    }

    public ImageKey toFinal(Long targetId) {
        if (!isTmp()) {
            throw new IllegalStateException("tmp 이미지만 정식 경로로 옮길 수 있습니다. key=" + value());
        }
        return new ImageKey(purpose, null, targetId, fileName);
    }

    public boolean canBeViewedBy(Long viewerId) {
        if (isTmp()) {
            return uploaderId.equals(viewerId);
        }
        return purpose == ImagePurpose.POST;
    }
}
