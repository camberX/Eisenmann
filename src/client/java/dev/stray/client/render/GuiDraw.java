package dev.stray.client.render;

import dev.stray.Stray;
import dev.stray.client.config.StrayConfig;
import dev.stray.client.ui.ControlChrome;
import dev.stray.client.ui.MenuFont;
import dev.stray.client.ui.Theme;
import dev.stray.client.visual.WorldTint;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public final class GuiDraw {
	private static final Identifier CIRCLE_HOLE = Stray.id("textures/gui/circle_hole.png");
	private static final int CIRCLE_TEX = 64;
	private static final int CIRCLE_HALF = 32;

	private GuiDraw() {
	}

	public static boolean scissor(GuiGraphicsExtractor graphics, float x, float y, float w, float h) {
		int x0 = Math.round(x);
		int y0 = Math.round(y);
		int x1 = Math.round(x + w);
		int y1 = Math.round(y + h);
		if (x1 <= x0 || y1 <= y0) {
			return false;
		}
		graphics.enableScissor(x0, y0, x1, y1);
		SkijaGui.pushScissor(graphics, x, y, w, h);
		return true;
	}

	public static void disableScissor(GuiGraphicsExtractor graphics) {
		graphics.disableScissor();
		SkijaGui.popScissor();
	}

	public static void fill(GuiGraphicsExtractor graphics, float x, float y, float w, float h, int color) {
		SkijaGui.fill(graphics, x, y, w, h, color);
	}

	/** 18px item well: 1px outline, flat fill. Rounded panels are too expensive per slot. */
	public static void well(GuiGraphicsExtractor graphics, float x, float y, float size, int fill, int outline) {
		if (size <= 2f) {
			return;
		}
		fill(graphics, x, y, size, size, outline);
		fill(graphics, x + 1f, y + 1f, size - 2f, size - 2f, fill);
	}

	public static void fillGradient(GuiGraphicsExtractor graphics, float x, float y, float w, float h, int top, int bottom) {
		SkijaGui.gradient(graphics, x, y, w, h, top, bottom);
	}

	/** Smooth HSV saturation/value square: 1px columns, vertical value gradient per column. */
	public static void hsvSquare(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float hue) {
		int cols = Math.max(1, Math.round(w));
		float cw = w / cols;
		for (int i = 0; i < cols; i++) {
			float sat = cols == 1 ? 1f : i / (float) (cols - 1);
			int top = 0xFF000000 | WorldTint.hsvToRgb(hue, sat, 1f);
			int bot = 0xFF000000 | WorldTint.hsvToRgb(hue, sat, 0f);
			fillGradient(graphics, x + i * cw, y, cw + 0.35f, h, top, bot);
		}
	}

	/** Smooth alpha strip: current color from transparent to solid. */
	public static void alphaBar(GuiGraphicsExtractor graphics, float x, float y, float w, float h, int rgb) {
		int cols = Math.max(1, Math.round(w));
		float cw = w / cols;
		int light = 0xFFC8C8C8;
		int dark = 0xFF8E8E8E;
		for (int i = 0; i < cols; i++) {
			float px = x + i * cw;
			int check = ((i / 2) & 1) == 0 ? light : dark;
			fill(graphics, px, y, cw + 0.35f, h, check);
			float a = cols == 1 ? 1f : i / (float) (cols - 1);
			fill(graphics, px, y, cw + 0.35f, h, Theme.withAlpha(rgb, Math.round(a * 255f)));
		}
	}
	public static void hueBar(GuiGraphicsExtractor graphics, float x, float y, float w, float h) {
		int cols = Math.max(1, Math.round(w));
		float cw = w / cols;
		for (int i = 0; i < cols; i++) {
			float hue = cols == 1 ? 0f : (i / (float) (cols - 1)) * 360f;
			fill(graphics, x + i * cw, y, cw + 0.35f, h, 0xFF000000 | WorldTint.hsvToRgb(hue, 1f, 1f));
		}
	}

	public static void border(GuiGraphicsExtractor graphics, float x, float y, float w, float h, int color, float thickness) {
		fill(graphics, x, y, w, thickness, color);
		fill(graphics, x, y + h - thickness, w, thickness, color);
		fill(graphics, x, y + thickness, thickness, h - thickness * 2, color);
		fill(graphics, x + w - thickness, y + thickness, thickness, h - thickness * 2, color);
	}

	public static void circle(GuiGraphicsExtractor graphics, float cx, float cy, float radius, int color) {
		SkijaGui.circle(graphics, cx, cy, radius, color);
	}

	/** Anti-aliased round-cap stroke, like a NanoVG line. */
	public static void stroke(GuiGraphicsExtractor graphics, float x0, float y0, float x1, float y1, float width, int color) {
		SkijaGui.line(graphics, x0, y0, x1, y1, width, color);
	}

	private static void hole(GuiGraphicsExtractor graphics, float x, float y, float radius, float u, float v, int color) {
		if (radius <= 0 || (color >>> 24) < 2) {
			return;
		}
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(radius, radius);
		graphics.blit(RenderPipelines.GUI_TEXTURED, CIRCLE_HOLE, 0, 0, u, v, 1, 1, CIRCLE_HALF, CIRCLE_HALF, CIRCLE_TEX, CIRCLE_TEX, color);
		graphics.pose().popMatrix();
	}

	public static void circleClip(GuiGraphicsExtractor graphics, float x, float y, float size, int cover) {
		float r = size * 0.5f;
		hole(graphics, x, y, r, 0f, 0f, cover);
		hole(graphics, x + r, y, r, CIRCLE_HALF, 0f, cover);
		hole(graphics, x, y + r, r, 0f, CIRCLE_HALF, cover);
		hole(graphics, x + r, y + r, r, CIRCLE_HALF, CIRCLE_HALF, cover);
	}

	public static void blit(GuiGraphicsExtractor graphics, Identifier id, float x, float y, float w, float h, float u, float v, int regionW, int regionH, int texW, int texH) {
		if (w <= 0 || h <= 0) {
			return;
		}
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(w, h);
		graphics.blit(RenderPipelines.GUI_TEXTURED, id, 0, 0, u, v, 1, 1, regionW, regionH, texW, texH, 0xFFFFFFFF);
		graphics.pose().popMatrix();
	}

	/**
	 * Blits a square texture then paints the four corner ears so the photo
	 * follows the same rounded silhouette as {@link #rounded}.
	 */
	public static void roundedBlit(
		GuiGraphicsExtractor graphics,
		Identifier id,
		float x,
		float y,
		float w,
		float h,
		float radius,
		int texSize,
		int earColor
	) {
		blit(graphics, id, x, y, w, h, 0f, 0f, texSize, texSize, texSize, texSize);
		paintRoundedEars(graphics, x, y, w, h, radius, earColor);
	}

	public static void roundedBlitEars(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float radius, int earColor) {
		paintRoundedEars(graphics, x, y, w, h, radius, earColor);
	}

	private static void paintRoundedEars(
		GuiGraphicsExtractor graphics,
		float x,
		float y,
		float w,
		float h,
		float radius,
		int color
	) {
		float r = Math.min(radius, Math.min(w, h) / 2f);
		if (r < 0.75f) {
			return;
		}
		hole(graphics, x, y, r, 0f, 0f, color);
		hole(graphics, x + w - r, y, r, CIRCLE_HALF, 0f, color);
		hole(graphics, x, y + h - r, r, 0f, CIRCLE_HALF, color);
		hole(graphics, x + w - r, y + h - r, r, CIRCLE_HALF, CIRCLE_HALF, color);
	}

	public static void rounded(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float radius, int color) {
		SkijaGui.rounded(graphics, x, y, w, h, radius, color);
	}

	/** Control chrome: 256px AA corners so large menu radii stay smooth. */
	public static void roundedFine(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float radius, int color) {
		SkijaGui.rounded(graphics, x, y, w, h, radius, color);
	}

	public static void roundedSides(
		GuiGraphicsExtractor graphics,
		float x,
		float y,
		float w,
		float h,
		float leftRadius,
		float rightRadius,
		int color
	) {
		SkijaGui.roundedSides(graphics, x, y, w, h, leftRadius, rightRadius, color);
	}

	public static void roundLeft(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float radius, int color) {
		SkijaGui.roundedSides(graphics, x, y, w, h, radius, 0f, color);
	}

	public static void roundRight(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float radius, int color) {
		SkijaGui.roundedSides(graphics, x, y, w, h, 0f, radius, color);
	}

	public static void panel(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float radius, int fill, int outline) {
		panel(graphics, x, y, w, h, radius, fill, outline, 0);
	}

	public static void panel(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float radius, int fill, int outline, int accent) {
		panel(graphics, x, y, w, h, radius, fill, outline, accent, false);
	}

	public static void panel(
		GuiGraphicsExtractor graphics,
		float x,
		float y,
		float w,
		float h,
		float radius,
		int fill,
		int outline,
		int accent,
		boolean accentRight
	) {
		if (StrayConfig.get().guiDesignControl()) {
			ControlChrome.glass(graphics, x, y, w, h, radius, fill);
			return;
		}
		rounded(graphics, x, y, w, h, radius, outline);
		rounded(graphics, x + 1, y + 1, w - 2, h - 2, Math.max(0.5f, radius - 1f), fill);
		if ((accent & 0xFF000000) != 0) {
			if (accentRight) {
				accentRight(graphics, x, y, w, h, radius, 3f, accent);
			} else {
				accentLeft(graphics, x, y, h, radius, 3f, accent);
			}
		}
	}

	/**
	 * Settings / subsetting popover: pane fill first, then a hairline stroke.
	 * Fill-first matters because the pane color is translucent — drawing the
	 * accent as a full underlay would tint the whole sheet.
	 */
	public static void sheet(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float radius, int fill, int outline) {
		rounded(graphics, x, y, w, h, radius, fill);
		roundedOutline(graphics, x, y, w, h, radius, outline, 0.5f);
	}

	public static void gradientRim(
		GuiGraphicsExtractor graphics,
		float x,
		float y,
		float w,
		float h,
		float radius,
		int high,
		int low
	) {
		ringOutline(graphics, x, y, w, h, radius, high, low);
	}

	private static int mixArgb(int from, int to, float t) {
		t = Math.max(0f, Math.min(1f, t));
		int a = Math.round(((from >>> 24) & 0xFF) + ((((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t));
		int r = Math.round(((from >> 16) & 0xFF) + ((((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t));
		int g = Math.round(((from >> 8) & 0xFF) + ((((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t));
		int b = Math.round((from & 0xFF) + (((to & 0xFF) - (from & 0xFF)) * t));
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	/**
	 * One texture for sides and corners so the rim keeps the same thickness and
	 * the same outer edge. Hard-filled sides against a filtered ring were what
	 * made every join step and every corner look thinner or thicker than the bar.
	 */
	private static void ringOutline(
		GuiGraphicsExtractor graphics,
		float x,
		float y,
		float w,
		float h,
		float radius,
		int high,
		int low
	) {
		if (w <= 0 || h <= 0 || ((high | low) & 0xFF000000) == 0) {
			return;
		}
		float r = Math.min(Math.max(0f, radius), Math.min(w, h) / 2f);
		SkijaGui.hollow(graphics, x, y, w, h, r, mixArgb(high, low, 0.4f), 1f);
	}

	public static void roundedOutline(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float radius, int color, float thickness) {
		if ((color & 0xFF000000) == 0 || w <= 0 || h <= 0) {
			return;
		}
		float r = Math.min(radius, Math.min(w, h) / 2f);
		if (r < 0.75f) {
			border(graphics, x, y, w, h, color, Math.max(0.5f, thickness));
			return;
		}
		SkijaGui.hollow(graphics, x, y, w, h, r, color, Math.max(0.5f, thickness));
	}

	/**
	 * Vertical left rail of the panel's full height. The outer edge follows the
	 * rounded silhouette; nothing is drawn along the top or bottom edges.
	 */
	public static void accentLeft(
		GuiGraphicsExtractor graphics,
		float x,
		float y,
		float h,
		float radius,
		float thickness,
		int accent
	) {
		float t = Math.max(1.5f, thickness);
		float r = Math.min(radius, h * 0.5f);
		if (r < 0.75f) {
			fill(graphics, x, y, t, h, accent);
			return;
		}
		float strip = Math.min(t, Math.max(1f, r - 0.35f));
		SkijaGui.roundedSides(graphics, x, y, strip, h, r, 0f, accent);
	}

	/**
	 * Vertical right rail of the panel's full height. Mirrors {@link #accentLeft} so a HUD
	 * pane on the right edge can keep its rail against the screen.
	 */
	public static void accentRight(
		GuiGraphicsExtractor graphics,
		float x,
		float y,
		float w,
		float h,
		float radius,
		float thickness,
		int accent
	) {
		float t = Math.max(1.5f, thickness);
		float r = Math.min(radius, h * 0.5f);
		if (r < 0.75f) {
			fill(graphics, x + w - t, y, t, h, accent);
			return;
		}
		float strip = Math.min(t, Math.max(1f, r - 0.35f));
		SkijaGui.roundedSides(graphics, x + w - strip, y, strip, h, 0f, r, accent);
	}

	public static void text(GuiGraphicsExtractor graphics, Font font, String value, float x, float y, int color, boolean shadow) {
		graphics.text(font, value, Math.round(x), Math.round(y), color, shadow);
	}

	public static void text(GuiGraphicsExtractor graphics, Font font, String value, float x, float y, float scale, int color, boolean shadow) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		if (scale != 1.0f) {
			graphics.pose().scale(scale, scale);
		}
		graphics.text(font, value, 0, 0, color, shadow);
		graphics.pose().popMatrix();
	}

	public static void text(GuiGraphicsExtractor graphics, Font font, Component value, float x, float y, int color, boolean shadow) {
		graphics.text(font, value, Math.round(x), Math.round(y), color, shadow);
	}

	public static void text(GuiGraphicsExtractor graphics, Font font, Component value, float x, float y, float scale, int color, boolean shadow) {
		if (Math.abs(scale - 1.0f) < 0.001f) {
			text(graphics, font, value, x, y, color, shadow);
			return;
		}
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);
		graphics.text(font, value, 0, 0, color, shadow);
		graphics.pose().popMatrix();
	}

	public static void menu(GuiGraphicsExtractor graphics, Font font, String value, float x, float y, int color) {
		float scale = MenuFont.bodyScale();
		text(graphics, font, MenuFont.body(value), x, MenuFont.menuY(y, scale), scale, color, false);
	}

	public static void small(GuiGraphicsExtractor graphics, Font font, String value, float x, float y, int color) {
		float scale = MenuFont.smallScale();
		text(graphics, font, MenuFont.small(value), x, MenuFont.menuY(y, scale), scale, color, false);
	}

	public static void title(GuiGraphicsExtractor graphics, Font font, String value, float x, float y, int color) {
		float scale = MenuFont.titleScale();
		text(graphics, font, MenuFont.title(value), x, MenuFont.menuY(y, scale), scale, color, false);
	}

	public static void brand(GuiGraphicsExtractor graphics, Font font, String value, float x, float y, int color) {
		text(graphics, font, MenuFont.brand(value), x, y - 1.0f, color, false);
	}

	public static void brandSmall(GuiGraphicsExtractor graphics, Font font, String value, float x, float y, int color) {
		text(graphics, font, MenuFont.brandSmall(value), x, y - 1.0f, color, false);
	}

	public static void hud(GuiGraphicsExtractor graphics, Font font, Component value, float x, float y, int color) {
		float scale = MenuFont.bodyScale();
		text(graphics, font, MenuFont.applyBody(value), x, MenuFont.hudY(y, scale), scale, color, false);
	}

	public static void icon(GuiGraphicsExtractor graphics, Font font, String glyph, float x, float y, int color) {
		icon(graphics, font, glyph, x, y, 1.0f, color);
	}

	public static void icon(GuiGraphicsExtractor graphics, Font font, String glyph, float x, float y, float scale, int color) {
		text(graphics, font, MenuFont.icon(glyph), x, y + 0.5f * scale, scale, color, false);
	}

	public static float middle(float y, float height) {
		return y + (height - 9.0f) * 0.5f;
	}

	public static int iconWidth(Font font, String glyph) {
		return font.width(MenuFont.icon(glyph));
	}

	public static float iconWidth(Font font, String glyph, float scale) {
		return iconWidth(font, glyph) * scale;
	}

	public static int menuWidth(Font font, String value) {
		return scaledWidth(font.width(MenuFont.body(value)), MenuFont.bodyScale());
	}

	public static int smallWidth(Font font, String value) {
		return scaledWidth(font.width(MenuFont.small(value)), MenuFont.smallScale());
	}

	public static int titleWidth(Font font, String value) {
		return scaledWidth(font.width(MenuFont.title(value)), MenuFont.titleScale());
	}

	public static int brandWidth(Font font, String value) {
		return font.width(MenuFont.brand(value));
	}

	public static int brandSmallWidth(Font font, String value) {
		return font.width(MenuFont.brandSmall(value));
	}

	public static String ellipsize(Font font, String value, float max, boolean small) {
		if (value == null || value.isEmpty()) {
			return "";
		}
		int full = small ? smallWidth(font, value) : menuWidth(font, value);
		if (full <= max) {
			return value;
		}
		int lo = 1;
		int hi = value.length();
		String best = "..";
		while (lo <= hi) {
			int mid = (lo + hi) >>> 1;
			String shown = value.substring(0, mid) + "..";
			int width = small ? smallWidth(font, shown) : menuWidth(font, shown);
			if (width <= max) {
				best = shown;
				lo = mid + 1;
			} else {
				hi = mid - 1;
			}
		}
		return best;
	}

	public static int hudWidth(Font font, Component value) {
		return scaledWidth(font.width(MenuFont.applyBody(value)), MenuFont.bodyScale());
	}

	private static int scaledWidth(int width, float scale) {
		if (scale >= 0.999f) {
			return width;
		}
		return Math.max(0, Math.round(width * scale));
	}

	public static boolean hovered(double mouseX, double mouseY, float x, float y, float w, float h) {
		return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
	}

	public static String compass(float deltaYaw) {
		float wrapped = wrapDegrees(deltaYaw);
		float abs = Math.abs(wrapped);
		if (abs < 18) {
			return "ahead";
		}
		if (abs > 162) {
			return "behind";
		}
		return wrapped < 0 ? "left" : "right";
	}

	public static String meters(double distance) {
		if (distance < 10) {
			return String.format(Locale.ROOT, "%.1fm", distance);
		}
		return String.format(Locale.ROOT, "%.0fm", distance);
	}

	public static float wrapDegrees(float value) {
		float wrapped = value % 360.0f;
		if (wrapped >= 180.0f) {
			wrapped -= 360.0f;
		}
		if (wrapped < -180.0f) {
			wrapped += 360.0f;
		}
		return wrapped;
	}

	public static void knob(GuiGraphicsExtractor graphics, float cx, float cy, float radius, int color) {
		circle(graphics, cx, cy, radius, color);
	}

	public static void pill(GuiGraphicsExtractor graphics, float x, float y, float w, float h, int color) {
		rounded(graphics, x, y, w, h, h / 2f, color);
	}

	public static void hline(GuiGraphicsExtractor graphics, float x, float y, float w, int color) {
		fill(graphics, x, y, w, 1, color);
	}
}
