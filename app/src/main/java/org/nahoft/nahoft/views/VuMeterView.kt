package org.nahoft.nahoft.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import org.nahoft.nahoft.R
import org.operatorfoundation.signalbridge.models.AudioLevelInfo

/**
 * VU meter view for displaying audio signal level during receive.
 *
 * Renders a horizontal bar with a gradient track showing signal quality zones,
 * a fill driven by the current RMS level, and a peak-hold tick marker.
 */
class VuMeterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr)
{
    companion object
    {

        const val FLOOR_DBFS      = -60f  // Practical noise floor — below this is silence
        const val TOO_WEAK_DBFS   = -22f  // Below this, decodes are unlikely
        const val GOOD_MAX_DBFS   = -10f  // Above this, signal is getting strong
        const val CLIP_WARN_DBFS  =  -1f  // Peak above this risks clipping

        private const val SEGMENT_COUNT   = 60
        private const val SEGMENT_GAP_DP  = 2f
        private const val MIN_HEIGHT_DP   = 6f
        private const val MAX_HEIGHT_DP   = 28f
        private const val CORNER_RADIUS_DP = 2f
        private const val PEAK_TICK_WIDTH_DP = 3f
        private const val LABEL_MARGIN_DP = 4f
        private const val DEFAULT_VIEW_HEIGHT_DP = 76f  // bar + zone labels + status label

        private const val COLOR_YELLOW = 0xFFFFD000.toInt()  // warm amber-yellow
    }

    // Current levels — set by update()
    private var currentLevel = 0f
    private var peakLevel    = 0f

    // False until the first update() call — prevents "too weak" label at rest
    private var isActive = false

    // Pre-scaled pixel values — computed once in onSizeChanged
    private var segmentGapPx    = 0f
    private var minHeightPx     = 0f
    private var maxHeightPx     = 0f
    private var cornerRadiusPx  = 0f
    private var peakTickWidthPx = 0f
    private var labelMarginPx   = 0f

    // Reusable rect to avoid allocation in onDraw
    private val segRect = RectF()

    // ── Colors ────────────────────────────────────────────────────────────────
    private val colorGreen  by lazy { ContextCompat.getColor(context, R.color.caribbeanGreen) }
    private val colorOrange by lazy { ContextCompat.getColor(context, R.color.tangerine) }
    private val colorRed    by lazy { ContextCompat.getColor(context, R.color.madderLake) }

    // Dim versions of each zone color for unlit segments
    private val dimGreen  by lazy { colorGreen  and 0x00FFFFFF or 0x26000000 }
    private val dimOrange by lazy { colorOrange and 0x00FFFFFF or 0x26000000 }
    private val dimRed    by lazy { colorRed    and 0x00FFFFFF or 0x26000000 }

    // ── Paints ────────────────────────────────────────────────────────────────
    private val segPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize  = 11f * resources.displayMetrics.scaledDensity
    }

    private val zoneLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 9f * resources.displayMetrics.scaledDensity
        color    = 0xFF88AACC.toInt()
    }

    private val dimLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f * resources.displayMetrics.scaledDensity
        // Color set per-draw from coolGrey
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Call from the audio level flow collector on the main thread. */
    fun update(info: AudioLevelInfo)
    {
        currentLevel = info.currentLevel
        peakLevel    = info.peakLevel
        isActive     = true
        invalidate()
    }

    /** Call when the session stops to blank the meter. */
    fun reset()
    {
        currentLevel = 0f
        peakLevel    = 0f
        isActive     = false
        invalidate()
    }


    /**
     * Converts a normalized linear level (0.0–1.0) to dBFS.
     * Returns FLOOR_DBFS for zero or near-zero input to avoid log10(0).
     */
    private fun levelToDbfs(level: Float): Float
    {
        if (level <= 0f) return FLOOR_DBFS
        return (20f * Math.log10(level.toDouble()).toFloat()).coerceAtLeast(FLOOR_DBFS)
    }

    /**
     * Converts a dBFS value to a 0.0–1.0 visual position along the bar.
     * FLOOR_DBFS maps to 0.0 (left edge), 0 dBFS maps to 1.0 (right edge).
     */
    private fun dbfsToVisualPos(dbfs: Float): Float
    {
        return ((dbfs - FLOOR_DBFS) / (0f - FLOOR_DBFS)).coerceIn(0f, 1f)
    }


    // ── Layout ────────────────────────────────────────────────────────────────


    private data class ColorStop(val dbfs: Float, val color: Int)

    /**
     * Color stops defining the gradient across the dBFS scale.
     *
     * Too weak zone  (FLOOR to TOO_WEAK_DBFS): red → orange → yellow → green
     * Good zone      (TOO_WEAK to GOOD_MAX):   solid green
     * Warn zone      (GOOD_MAX to CLIP_WARN):  green → yellow → orange
     * Clip zone      (CLIP_WARN to 0):         orange → red
     */
    private val colorStops: List<ColorStop> by lazy {
        listOf(
            ColorStop(FLOOR_DBFS,      colorRed),
            ColorStop(FLOOR_DBFS + 20f, colorOrange),   // -40 dBFS
            ColorStop(FLOOR_DBFS + 30f, COLOR_YELLOW),  // -30 dBFS
            ColorStop(TOO_WEAK_DBFS,   colorGreen),     // -22 dBFS
            ColorStop(GOOD_MAX_DBFS,   colorGreen),     // -10 dBFS
            ColorStop(-5f,             COLOR_YELLOW),
            ColorStop(CLIP_WARN_DBFS,  colorOrange),    // -1 dBFS
            ColorStop(0f,              colorRed)
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int)
    {
        val defaultH = (DEFAULT_VIEW_HEIGHT_DP * resources.displayMetrics.density).toInt()
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            resolveSize(defaultH, heightMeasureSpec)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int)
    {
        super.onSizeChanged(w, h, oldw, oldh)
        val dp = resources.displayMetrics.density
        segmentGapPx    = SEGMENT_GAP_DP   * dp
        minHeightPx     = MIN_HEIGHT_DP    * dp
        maxHeightPx     = MAX_HEIGHT_DP    * dp
        cornerRadiusPx  = CORNER_RADIUS_DP * dp
        peakTickWidthPx = PEAK_TICK_WIDTH_DP * dp
        labelMarginPx   = LABEL_MARGIN_DP  * dp
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas)
    {
        super.onDraw(canvas)

        val w = width.toFloat()

        // Total gap space between segments
        val totalGapWidth = segmentGapPx * (SEGMENT_COUNT - 1)
        val segmentWidth  = (w - totalGapWidth) / SEGMENT_COUNT


        // Convert levels to dBFS, then to visual position, then to segment index
        val currentDbfs = levelToDbfs(currentLevel)
        val peakDbfs    = levelToDbfs(peakLevel)
        val litCount    = (dbfsToVisualPos(currentDbfs) * SEGMENT_COUNT).toInt()
        val peakIndex   = (dbfsToVisualPos(peakDbfs)    * SEGMENT_COUNT).toInt() - 1

        // ── Draw segments ─────────────────────────────────────────────────────
        for (i in 0 until SEGMENT_COUNT)
        {
            val visualPos    = (i + 1).toFloat() / SEGMENT_COUNT
            // Convert this segment's visual position back to the dBFS value it represents
            val segmentDbfs  = FLOOR_DBFS + visualPos * (0f - FLOOR_DBFS)

            // Height grows linearly from minHeightPx to maxHeightPx
            val segH = minHeightPx + (maxHeightPx - minHeightPx) * (i.toFloat() / (SEGMENT_COUNT - 1))

            val x = i * (segmentWidth + segmentGapPx)
            // Align all segments to the bottom of the bar area
            val top = maxHeightPx - segH

            segRect.set(x, top, x + segmentWidth, maxHeightPx)

            segPaint.color = when
            {
                i == peakIndex -> 0xFFFFFFFF.toInt()
                i < litCount   -> litColor(segmentDbfs)
                else           -> dimColor(segmentDbfs)
            }

            canvas.drawRoundRect(segRect, cornerRadiusPx, cornerRadiusPx, segPaint)
        }

        // ── Zone labels ───────────────────────────────────────────────────────
        val zoneY = maxHeightPx + labelMarginPx + zoneLabelPaint.textSize
        drawZoneLabels(canvas, w, zoneY)

        // ── Status label ──────────────────────────────────────────────────────

        if (isActive)
        {
            val lineY = zoneY + labelMarginPx + labelPaint.textSize

            // ── Line 1: quality label (left) + percentages (right) ───────────────
            val (statusText, statusColor) = statusLabel()
            labelPaint.color = statusColor
            canvas.drawText(statusText, 0f, lineY, labelPaint)

            val pctText = "${(currentLevel * 100).toInt()}% RMS   peak ${(peakLevel * 100).toInt()}%"
            labelPaint.color = ContextCompat.getColor(context, R.color.white)
            val pctX = w - labelPaint.measureText(pctText)
            canvas.drawText(pctText, pctX, lineY, labelPaint)

            // ── Line 2: dBFS values dimmed below ─────────────────────────────────
            dimLabelPaint.color = ContextCompat.getColor(context, R.color.coolGrey)
            val dbfsText = "%.0f dBFS   peak %.0f dBFS".format(currentDbfs, peakDbfs)
            val dbfsY = lineY + labelMarginPx + dimLabelPaint.textSize
            val dbfsX = w - dimLabelPaint.measureText(dbfsText)
            canvas.drawText(dbfsText, dbfsX, dbfsY, dimLabelPaint)
        }
    }

    /**
     * Draws GOOD / WARN / CLIP zone labels beneath the segments,
     * each centered over its respective zone.
     */

    private fun drawZoneLabels(canvas: Canvas, viewWidth: Float, y: Float)
    {
        // Derive label positions from where the thresholds fall on the dBFS scale
        val goodEndX  = dbfsToVisualPos(TOO_WEAK_DBFS)  * viewWidth  // left edge of good zone
        val goodX     = dbfsToVisualPos(GOOD_MAX_DBFS)  * viewWidth  // right edge of good zone
        val clipX     = dbfsToVisualPos(CLIP_WARN_DBFS) * viewWidth  // start of clip zone

        // WEAK — centered in the too-weak zone (left of good)
        zoneLabelPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("WEAK", goodEndX / 2f, y, zoneLabelPaint)

        // GOOD — centered between too-weak and good-max
        canvas.drawText("GOOD", (goodEndX + goodX) / 2f, y, zoneLabelPaint)

        // WARN — centered between good-max and clip
        canvas.drawText("WARN", (goodX + clipX) / 2f, y, zoneLabelPaint)

        // CLIP — left-aligned from clip threshold
        zoneLabelPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("CLIP", clipX + labelMarginPx, y, zoneLabelPaint)
    }


    /**
     * Returns an interpolated color for a segment at the given dBFS level,
     * by finding the two surrounding color stops and linearly blending between them.
     */
    private fun litColor(segmentDbfs: Float): Int
    {
        // Clamp to defined range
        val dbfs = segmentDbfs.coerceIn(FLOOR_DBFS, 0f)

        // Find the stop immediately below and above the segment's dBFS value
        val lower = colorStops.lastOrNull { it.dbfs <= dbfs } ?: colorStops.first()
        val upper = colorStops.firstOrNull { it.dbfs >  dbfs } ?: colorStops.last()

        if (lower == upper) return lower.color

        // Normalise position between the two stops (0.0 = at lower, 1.0 = at upper)
        val t = (dbfs - lower.dbfs) / (upper.dbfs - lower.dbfs)
        return interpolateColor(lower.color, upper.color, t)
    }

    /**
     * Returns a dim (unlit) version of the lit color at the same dBFS position.
     * Uses the same gradient so zone boundaries are visible even when unlit.
     */
    private fun dimColor(segmentDbfs: Float): Int
    {
        val lit = litColor(segmentDbfs)
        // Keep hue from the gradient, reduce to ~15% opacity
        return (lit and 0x00FFFFFF) or 0x26000000.toInt()
    }

    /**
     * Linearly interpolates between two ARGB colors.
     */
    private fun interpolateColor(colorA: Int, colorB: Int, t: Float): Int
    {
        val a = (Color.alpha(colorA) + (Color.alpha(colorB) - Color.alpha(colorA)) * t).toInt()
        val r = (Color.red(colorA)   + (Color.red(colorB)   - Color.red(colorA))   * t).toInt()
        val g = (Color.green(colorA) + (Color.green(colorB) - Color.green(colorA)) * t).toInt()
        val b = (Color.blue(colorA)  + (Color.blue(colorB)  - Color.blue(colorA))  * t).toInt()
        return Color.argb(a, r, g, b)
    }

    /**
     * Returns the status label text and color for the current signal levels.
     * Clipping takes priority over weak signal.
     */
    private fun statusLabel(): Pair<String, Int> = when
    {
        levelToDbfs(peakLevel)    >= CLIP_WARN_DBFS -> Pair(context.getString(R.string.audio_signal_clipping), colorRed)
        levelToDbfs(currentLevel) <  TOO_WEAK_DBFS  -> Pair(context.getString(R.string.audio_signal_too_weak), colorOrange)
        else                                         -> Pair(context.getString(R.string.audio_signal_good),     colorGreen)
    }
}