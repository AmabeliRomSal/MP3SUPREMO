package Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mp3v2.R;

import java.util.List;
import java.util.concurrent.TimeUnit;

import POJO.Cancion;

public class AdapterCancion extends RecyclerView.Adapter<AdapterCancion.ViewHolderCancion> {
    //variables
    private List<Cancion> listaCanciones;
    private OnSongClickListener listener;

    public interface OnSongClickListener{
        void onClickCancion(int posicion);
    }

    public AdapterCancion(List<Cancion> listaCanciones, OnSongClickListener listener){
        this.listaCanciones= listaCanciones;
        this.listener= listener;
    }

    @Override
    public ViewHolderCancion onCreateViewHolder(@NonNull ViewGroup parent, int posicion) {
        View vista= LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cancion, parent, false);
        return new ViewHolderCancion(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderCancion holder, int position) {
        Cancion cancion= listaCanciones.get(position);
        holder.tv_tituloCancion.setText(cancion.getTitulo());
        holder.tv_artistaCancion.setText(cancion.getArtista());
        holder.tv_tiempoActual.setText(formatearDuracion(cancion.getDuracion()));

        holder.itemView.setOnClickListener(v -> {
            if(listener != null){ listener.onClickCancion(position);}
        });
    }

    @Override
    public int getItemCount() {
        return listaCanciones.size();
    }

    private String formatearDuracion(long duracion){
        long minutos= TimeUnit.MILLISECONDS.toMinutes(duracion);
        long segundos= TimeUnit.MILLISECONDS.toSeconds(duracion) % 60;

        return String.format("%02d:%02d", minutos, segundos);
    }

    static class ViewHolderCancion extends RecyclerView.ViewHolder{
        TextView tv_tituloCancion, tv_artistaCancion, tv_tiempoActual;

        public ViewHolderCancion(@NonNull View viewItem){
            super(viewItem);
            tv_tituloCancion= viewItem.findViewById(R.id.tv_item_tituloCancion);
            tv_artistaCancion= viewItem.findViewById(R.id.tv_item_artistaCancion);
            tv_tiempoActual= viewItem.findViewById(R.id.tv_item_tiempoCancion);
        }
    }
}
