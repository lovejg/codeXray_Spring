package com.codeXray.backend.problem.dto;

import com.codeXray.backend.problem.entity.AlgorithmTag;

public record TagResponse(Long id, String name) {

    public static TagResponse from(AlgorithmTag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }
}
