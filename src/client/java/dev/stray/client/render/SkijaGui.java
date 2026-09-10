package dev.stray.client.render;

import com.github.noamm9.skijarenderer.skia.Skija;
import com.github.noamm9.skijarenderer.skia.SkijaGradient;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Applies the current {@link GuiGraphicsExtractor} pose, then queues a Skija
 * primitive. The batch is flushed onto the framebuffer before vanilla GUI text.
 */
final class SkijaGui {
	private SkijaGui() {
	}

	static void fill(GuiGraphicsExtractor graphics, float x, float y, float w, float h, int color) {
		if (w <= 0f || h <= 0f || (color >>> 24) == 0) {
			return;
		}
		pose(graphics);
		Skija.INSTANCE.rect(x, y, w, h, color);
		Skija.INSTANCE.pop();
	}

	static void rounded(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float radius, int color) {
		if (w <= 0f || h <= 0f || (color >>> 24) == 0) {
			return;
		}
		float r = Math.min(Math.max(0f, radius), Math.min(w, h) / 2f);
		pose(graphics);
		if (r < 0.75f) {
			Skija.INSTANCE.rect(x, y, w, h, color);
		} else {
			Skija.INSTANCE.rect(x, y, w, h, color, r);
		}
		Skija.INSTANCE.pop();
	}

	static void roundedSides(
		GuiGraphicsExtractor graphics,
		float x,
		float y,
		float w,
		float h,
		float leftRadius,
		float rightRadius,
		int color
	) {
		if (w <= 0f || h <= 0f || (color >>> 24) == 0) {
			return;
		}
		float max = Math.min(w, h) / 2f;
		float l = Math.min(Math.max(0f, leftRadius), max);
		float r = Math.min(Math.max(0f, rightRadius), max);
		pose(graphics);
		if (l < 0.75f && r < 0.75f) {
			Skija.INSTANCE.rect(x, y, w, h, color);
		} else {
			Skija.INSTANCE.rrect(x, y, w, h, color, l, r, r, l);
		}
		Skija.INSTANCE.pop();
	}

	static void hollow(
		GuiGraphicsExtractor graphics,
		float x,
		float y,
		float w,
		float h,
		float radius,
		int color,
		float thickness
	) {
		if (w <= 0f || h <= 0f || (color >>> 24) == 0 || thickness <= 0f) {
			return;
		}
		float r = Math.min(Math.max(0f, radius), Math.min(w, h) / 2f);
		pose(graphics);
		Skija.INSTANCE.hollowRect(x, y, w, h, thickness, color, r);
		Skija.INSTANCE.pop();
	}

	static void gradient(
		GuiGraphicsExtractor graphics,
		float x,
		float y,
		float w,
		float h,
		int top,
		int bottom
	) {
		if (w <= 0f || h <= 0f) {
			return;
		}
		pose(graphics);
		Skija.INSTANCE.gradientRect(x, y, w, h, top, bottom, SkijaGradient.TOP_BOTTOM, 0f);
		Skija.INSTANCE.pop();
	}

	static void circle(GuiGraphicsExtractor graphics, float cx, float cy, float radius, int color) {
		if (radius <= 0f || (color >>> 24) < 2) {
			return;
		}
		pose(graphics);
		Skija.INSTANCE.circle(cx, cy, radius, color);
		Skija.INSTANCE.pop();
	}

	static void line(
		GuiGraphicsExtractor graphics,
		float x0,
		float y0,
		float x1,
		float y1,
		float width,
		int color
	) {
		if (width <= 0f || (color >>> 24) < 2) {
			return;
		}
		pose(graphics);
		Skija.INSTANCE.line(x0, y0, x1, y1, width, color);
		Skija.INSTANCE.pop();
	}

	static void pushScissor(GuiGraphicsExtractor graphics, float x, float y, float w, float h) {
		pose(graphics);
		Skija.INSTANCE.pushScissor(x, y, w, h);
	}

	static void popScissor() {
		Skija.INSTANCE.popScissor();
		Skija.INSTANCE.pop();
	}

	private static void pose(GuiGraphicsExtractor graphics) {
		Skija.INSTANCE.push();
		Skija.INSTANCE.transform(graphics.pose());
	}
}
