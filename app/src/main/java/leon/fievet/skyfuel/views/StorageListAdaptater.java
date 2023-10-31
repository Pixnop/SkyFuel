package leon.fievet.skyfuel.views;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import leon.fievet.skyfuel.R;
import leon.fievet.skyfuel.controler.Control;
import leon.fievet.skyfuel.models.Battery;

public class StorageListAdaptater extends BaseAdapter {
    private ArrayList<Battery> lesBattery = new ArrayList<Battery>();
    private LayoutInflater inflater;
    private Control controle;

    public StorageListAdaptater(ArrayList<Battery> lesBattery, Context context) {
        this.lesBattery = lesBattery;
        this.inflater = LayoutInflater.from(context);
        this.controle = Control.getInstance(null);
    }

    @Override
    public int getCount() {
        return lesBattery.size();
    }

    @Override
    public Object getItem(int position) {
        return lesBattery.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.layout_storage_activity, null);
            holder = new ViewHolder();
            holder.imgEtat = (ImageView) convertView.findViewById(R.id.imgEtat);
            holder.txtId = (TextView) convertView.findViewById(R.id.txtId);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Battery battery = lesBattery.get(position);
        holder.txtId.setText(String.valueOf(battery.getId())); // convert ID to string
        if (battery.isFull()) {
            holder.imgEtat.setImageResource(R.drawable.ic_bat_full);
        } else if (battery.isLow()) {
            holder.imgEtat.setImageResource(R.drawable.ic_bat_low);
        }

        // Ajout de l'OnClickListener
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), EditBatteryActivity.class); // Nom hypothétique de l'activité d'édition
                intent.putExtra("batteryId", battery.getId());
                v.getContext().startActivity(intent);
            }
        });

        return convertView;
    }
    private static class ViewHolder {
        private ImageView imgEtat;
        private TextView txtId;

    }
}
