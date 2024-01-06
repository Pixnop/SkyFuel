package leon.fievet.skyfuel.models;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import leon.fievet.skyfuel.tools.outils;

public class Battery {
    private UUID id;
    private Integer nbCells;
    private Integer capacity;
    private Integer etatCharge; // 0 = low, 1 = storage, 2 = full
    private OffsetDateTime dateEnregistrement;
    private OffsetDateTime dateDerniereMisAJour;
    private String data;
    private Bitmap QRCode;

    public Battery(Integer nbCells, Integer capacity, Integer etatCharge, OffsetDateTime dateEnregistrement) throws JSONException {
        this.id = UUID.randomUUID();
        this.nbCells = nbCells;
        this.capacity = capacity;
        this.etatCharge = etatCharge;
        this.dateEnregistrement = dateEnregistrement;
        this.dateDerniereMisAJour = dateEnregistrement;
        this.data = toJSONString();
    }

    public Battery(UUID id, Integer nbCells, Integer capacity, Integer etatCharge, OffsetDateTime dateEnregistrement) throws JSONException {
        this.id = id;
        this.nbCells = nbCells;
        this.capacity = capacity;
        this.etatCharge = etatCharge;
        this.dateEnregistrement = dateEnregistrement;
        this.dateDerniereMisAJour = dateEnregistrement;
        this.data = toJSONString();
    }

    public String toJSONString() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id.toString());
        json.put("nbCells", nbCells);
        json.put("capacity", capacity);
        json.put("dateEnregistrement", dateEnregistrement.toString());
        return json.toString();
    }

    public Integer getNbCells() {
        return nbCells;
    }

    public void setNbCells(Integer nbCells) {
        this.nbCells = nbCells;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getEtatCharge() {
        return etatCharge;
    }

    public void setEtatCharge(Integer etatCharge) {
        this.etatCharge = etatCharge;
    }

    public OffsetDateTime getDateEnregistrement() {
        return dateEnregistrement;
    }

    public void setDateEnregistrement(OffsetDateTime dateEnregistrement) {
        this.dateEnregistrement = dateEnregistrement;
    }

    public OffsetDateTime getDateDerniereMisAJour() {
        return dateDerniereMisAJour;
    }

    public void setDateDerniereMisAJour(OffsetDateTime dateDerniereMisAJour) {
        this.dateDerniereMisAJour = dateDerniereMisAJour;
    }

    public String getData() {
        return data;
    }
    public void setData(String data) {
        this.data = data;
    }
    public Bitmap getQRCode(Context context) throws WriterException, IOException {
        genQRCode(context);
        return QRCode;
    }
    public void setQRCode(Bitmap QRCode) {
        this.QRCode = QRCode;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void genQRCode(Context context) throws IOException, WriterException {
        BitMatrix bitMatrix = new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, 500, 500);
        int[] pixels = new int[bitMatrix.getHeight() * bitMatrix.getWidth()];
        for (int y = 0; y < bitMatrix.getHeight(); y++) {
            int offset = y * bitMatrix.getWidth();
            for (int x = 0; x < bitMatrix.getWidth(); x++) {
                pixels[offset + x] = bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
            }
        }
        QRCode = Bitmap.createBitmap(bitMatrix.getWidth(), bitMatrix.getHeight(), Bitmap.Config.ARGB_8888);
        QRCode.setPixels(pixels, 0, 500, 0, 0, bitMatrix.getWidth(), bitMatrix.getHeight());
        QRCode = outils.writeTextOnBitmap(QRCode, id.toString());

        String fileName = id + ".png";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SkyFuel");
        Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
        QRCode.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        outputStream.flush();
        outputStream.close();
    }

    public boolean isFull() {
        return etatCharge == 2;
    }
    public boolean isLow() {
        return etatCharge == 0;
    }


    public void updateBattery(Integer etatCharge) {
        this.etatCharge = etatCharge;
        this.dateDerniereMisAJour = OffsetDateTime.now();
    }
}
