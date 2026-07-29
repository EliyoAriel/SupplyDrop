package com.supplydrop.seasons;

import com.supplydrop.Config;
import com.supplydrop.SupplyDrop;
import com.supplydrop.config.ConfigKeys.TemplateWeight;
import org.bukkit.configuration.ConfigurationSection;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SeasonManager {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private List<Season> seasons = List.of();

    public void load(Config config) {
        if (config == null) {
            seasons = List.of();
            return;
        }

        ConfigurationSection section = config.getConfig().getConfigurationSection("auto-drop.seasons");
        if (section == null) {
            seasons = List.of();
            return;
        }

        List<Season> loaded = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection seasonSection = section.getConfigurationSection(key);
            if (seasonSection == null) continue;

            String start = seasonSection.getString("start");
            String end = seasonSection.getString("end");
            if (start == null || end == null) continue;

            List<TemplateWeight> templates = new ArrayList<>();
            ConfigurationSection templatesSection = seasonSection.getConfigurationSection("templates");
            if (templatesSection != null) {
                for (String tplName : templatesSection.getKeys(false)) {
                    int weight = templatesSection.getInt(tplName + ".weight", 10);
                    if (weight > 0) {
                        templates.add(new TemplateWeight(tplName, weight));
                    }
                }
            }

            String announcePrefix = seasonSection.getString("announce-prefix", "");

            loaded.add(new Season(key, start.trim(), end.trim(), templates, announcePrefix));
        }

        this.seasons = Collections.unmodifiableList(loaded);
    }

    public Season getActiveSeason() {
        MonthDay today = MonthDay.from(LocalDate.now());
        for (Season season : seasons) {
            if (isInSeason(today, season)) {
                return season;
            }
        }
        return null;
    }

    private boolean isInSeason(MonthDay today, Season season) {
        MonthDay start = MonthDay.parse(season.start(), FORMATTER);
        MonthDay end = MonthDay.parse(season.end(), FORMATTER);

        if (start.isAfter(end)) {
            return !today.isBefore(start) || !today.isAfter(end);
        } else {
            return !today.isBefore(start) && !today.isAfter(end);
        }
    }

    public List<Season> getSeasons() {
        return seasons;
    }
}
