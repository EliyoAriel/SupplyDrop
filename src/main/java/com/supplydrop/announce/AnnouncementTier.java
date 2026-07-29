package com.supplydrop.announce;

public record AnnouncementTier(
        String name,
        String prefix,
        String message,
        boolean actionbar,
        Integer coordRevealDelay
) {}
