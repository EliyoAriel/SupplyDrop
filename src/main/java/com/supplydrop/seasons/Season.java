package com.supplydrop.seasons;

import com.supplydrop.config.ConfigKeys.TemplateWeight;

import java.util.List;

public record Season(
        String name,
        String start,
        String end,
        List<TemplateWeight> templates,
        String announcePrefix
) {}
