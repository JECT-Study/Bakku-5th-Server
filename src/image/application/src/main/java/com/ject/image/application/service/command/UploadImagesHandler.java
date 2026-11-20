package com.ject.image.application.service.command;

import com.ject.common.core.cqrs.command.CommandHandler;
import com.ject.image.application.dto.ImageUpload;
import com.ject.image.application.dto.command.UploadImagesCommand;
import com.ject.image.application.port.ImageStorage;
import com.ject.image.application.service.ImageFileValidator;
import com.ject.image.domain.domain.ImageFormat;
import com.ject.image.domain.domain.ImageKey;
import com.ject.image.domain.domain.ImagePurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UploadImagesHandler implements CommandHandler<UploadImagesCommand, List<String>> {

    private final ImageFileValidator imageFileValidator;
    private final ImageStorage imageStorage;

    @Override
    public List<String> handle(UploadImagesCommand command) {
        ImagePurpose purpose = ImagePurpose.valueOf(command.purpose());
        List<ImageUpload> files = command.files();

        List<ImageFormat> formats = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            ImageUpload file = files.get(i);
            formats.add(imageFileValidator.validate(i + 1, file.size(), readHeader(file)));
        }

        List<String> keys = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            ImageFormat format = formats.get(i);
            ImageKey key = ImageKey.newTmp(purpose, command.memberId(), format);
            store(key, files.get(i), format);
            keys.add(key.value());
        }
        return keys;
    }

    private void store(ImageKey key, ImageUpload file, ImageFormat format) {
        try (InputStream content = file.opener().open()) {
            imageStorage.put(key.value(), content, file.size(), format.getContentType());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] readHeader(ImageUpload file) {
        try (InputStream content = file.opener().open()) {
            return content.readNBytes(ImageFormat.HEADER_SIZE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
