package leon.fievet.skyfuel.tools;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public abstract class outils {

    public static Bitmap writeTextOnBitmap(Bitmap bitmap, String text) {
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setTextSize(50);
        canvas.drawText(text, 10, 50, paint);
        return mutableBitmap;
    }

    public static OffsetDateTime stringToDate(String dateString) {
        return OffsetDateTime.parse(dateString, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
