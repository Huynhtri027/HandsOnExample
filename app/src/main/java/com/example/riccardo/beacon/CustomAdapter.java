package com.example.riccardo.beacon;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;


public class CustomAdapter extends ArrayAdapter<Item> {

    public CustomAdapter(Context context, ArrayList<Item> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Item item = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row, parent, false);
        }
        // Lookup view for data population
        TextView tvName = (TextView) convertView.findViewById(R.id.name);
        TextView tvPrice = (TextView) convertView.findViewById(R.id.price);
        ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView);
        // Populate the data into the template view using the data object
        tvName.setText(item.name);
        tvPrice.setText(item.price);
        Picasso.with(getContext()).load(item.photoPath).into(imageView);
        // Return the completed view to render on screen
        return convertView;
    }
}
