package xyz.piod.keeper.dto;

public record LinkPreviewRequest(
        String url,
        String title,
        String description,
        String image
) {}