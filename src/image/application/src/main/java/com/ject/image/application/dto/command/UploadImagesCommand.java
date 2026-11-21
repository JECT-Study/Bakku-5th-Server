package com.ject.image.application.dto.command;

import com.ject.common.core.cqrs.command.Command;
import com.ject.image.application.dto.ImageUpload;

import java.util.List;

public record UploadImagesCommand(Long memberId, String purpose, List<ImageUpload> files)
        implements Command<List<String>> {
}
