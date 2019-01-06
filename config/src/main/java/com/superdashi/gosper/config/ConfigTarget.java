package com.superdashi.gosper.config;

import com.superdashi.gosper.config.Config.AlignConfig;
import com.superdashi.gosper.config.Config.ColorConfig;
import com.superdashi.gosper.config.Config.ColoringConfig;
import com.superdashi.gosper.config.Config.DurationConfig;
import com.superdashi.gosper.config.Config.LengthConfig;
import com.superdashi.gosper.config.Config.OffsetConfig;
import com.superdashi.gosper.config.Config.PaletteConfig;
import com.superdashi.gosper.config.Config.PathConfig;
import com.superdashi.gosper.config.Config.ResourceConfig;
import com.superdashi.gosper.config.Config.ScheduleConfig;
import com.superdashi.gosper.config.Config.VectorConfig;

public interface ConfigTarget extends AutoCloseable {

	@Override
	default public void close() {}

	default void applyStyling(ConfigProperty property, AlignConfig align)       {}
	default void applyStyling(ConfigProperty property, DurationConfig duration) {}
	default void applyStyling(ConfigProperty property, ColorConfig color)       {}
	default void applyStyling(ConfigProperty property, ColoringConfig coloring) {}
	default void applyStyling(ConfigProperty property, LengthConfig length)     {}
	default void applyStyling(ConfigProperty property, OffsetConfig offset)     {}
	default void applyStyling(ConfigProperty property, PaletteConfig palette)   {}
	default void applyStyling(ConfigProperty property, PathConfig path)         {}
	default void applyStyling(ConfigProperty property, ResourceConfig resource) {}
	default void applyStyling(ConfigProperty property, ScheduleConfig schedule) {}
	default void applyStyling(ConfigProperty property, VectorConfig vector)     {}
	default void inheritStyling(ConfigProperty property) {}

}
