package id.selayar.gpskamera;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/** Pratinjau cap GPS yang sama dengan cap permanen pada hasil foto. */
public class GpsOverlayView extends View {

    private OverlayRenderer.OverlayData data = new OverlayRenderer.OverlayData();

    public GpsOverlayView(Context context) {
        super(context);
        init();
    }

    public GpsOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GpsOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        setClickable(false);
        setFocusable(false);
    }

    public void setData(OverlayRenderer.OverlayData value) {
        if (value != null) data = value;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        OverlayRenderer.draw(canvas, getWidth(), getHeight(), data);
    }
}
