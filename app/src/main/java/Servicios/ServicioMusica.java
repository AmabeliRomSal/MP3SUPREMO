package Servicios;

import static android.app.Service.START_STICKY;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.example.mp3v2.MainActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import POJO.Cancion;

/** ServicioMusica: manager de servicio de la música de la app
 *
 * Puede:
 * - Modo aleatorio/repetir lista o canción
 * - Lista de reproducción
 * - Canción siguiente/anterior
 * - Detección fin canción
 */

public class ServicioMusica extends Service {
    //constantes
    //modos de repeticion
    public static final int REPETIR_OFF=0;
    public static final int REPETIR_LISTA=1;
    public static final int REPETIR_CANCION=2;

    private final String ID_CANAL= "canal_musica";
    private final IBinder binder= new BinderMusica();


    //variables
    private ExoPlayer player;
    private MediaSessionCompat mediaSession;

    //manejo playlist
    private List<Cancion> listaCanciones= new ArrayList<>();
        private List<Integer> listaAleatorio= new ArrayList<>();
    private int indexCancion= 0;

    //modos de reproduccion
    private boolean modoAleatorio= false;
    private int modoRepetir= REPETIR_OFF;

    private String titulo= "No hay canción";
    private String artista= "Desconocido";

    public class BinderMusica extends Binder{
        public ServicioMusica getService(){
            return ServicioMusica.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent){
        return binder;
    }

    @Override
    public void onCreate(){
        super.onCreate();

        crearCanalNotificaciones();

        mediaSession= new MediaSessionCompat(this, "MusicService");
        mediaSession.setActive(true);

        player= new ExoPlayer.Builder(this).build();

        //listener (detectar fin cancion)
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if(playbackState == Player.STATE_ENDED){ onSongEnded();}
                actualizarNotificacion();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) { actualizarNotificacion();}
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        if(intent != null){
            String pathExtra= intent.getStringExtra("path");
            String tituloExtra= intent.getStringExtra("titulo");
            String artistaExtra= intent.getStringExtra("artista");

            String accion= intent.getAction();

            if(pathExtra != null){
                playUri(pathExtra, tituloExtra, artistaExtra);
            }
            else{
                if(accion != null){
                    switch (accion){
                        case "ACTION_PLAY":
                            play();
                            break;
                        case "ACTION_PAUSE":
                            pausar();
                            break;
                        case "ACTION_NEXT":
                            playSiguienteCancion();
                            break;
                        case "ACTION_PREVIOUS":
                            playAnteriorCancion();
                            break;
                        case "ACTION_STOP":
                            stop();
                            break;
                    }
                }
            }
        }

        startForeground(1, armarNotificacion());
        return START_STICKY;
    }

    private void playUri(String path, String tituloExtra, String artistaExtra){
        if(path == null){ return;}

        //actualizar metadatos si hay
        if(tituloExtra != null){ this.titulo= tituloExtra;}
        if(artistaExtra != null){ this.artista= artistaExtra;}

        //crear mediaItem + reproducir
        try{
            MediaItem item= MediaItem.fromUri(Uri.parse(path));
            player.setMediaItem(item);
            player.prepare();
            player.play();
        }catch (Exception e){
            e.printStackTrace();
        }

        actualizarNotificacion();
    }

    public void setPlaylist(List<Cancion> canciones){
        this.listaCanciones= new ArrayList<>(canciones);
        generarIndexListaAleatorio();
    }

    public void playCancion(int index){
        if(listaCanciones.isEmpty() || index < 0 || index >= listaCanciones.size()){ return;}

        indexCancion= index;
        Cancion cancion= listaCanciones.get(indexCancion);

        titulo= cancion.getTitulo();
        artista= cancion.getArtista();

        MediaItem item= MediaItem.fromUri(Uri.parse(cancion.getPath()));
        player.setMediaItem(item);
        player.prepare();
        player.play();

        actualizarNotificacion();
    }

    public void playSiguienteCancion(){
        if(listaCanciones.isEmpty()){ return;}

        if(modoAleatorio){
            int indexActual= listaAleatorio.indexOf(indexCancion);
            int indexSiguiente= (indexActual + 1) % listaAleatorio.size();
            indexCancion= listaAleatorio.get(indexSiguiente);
        }
        else{
            indexCancion= (indexCancion + 1) % listaCanciones.size();
        }

        playCancion(indexCancion);
    }

    public void playAnteriorCancion() {
        if(listaCanciones.isEmpty()) { return;}

        if(modoAleatorio) {
            int indexActual = listaAleatorio.indexOf(indexCancion);
            int indexAnterior = (indexActual - 1 + listaAleatorio.size()) % listaAleatorio.size();
            indexCancion = listaAleatorio.get(indexAnterior);
        }
        else{
            indexCancion= (indexCancion - 1 + listaCanciones.size()) % listaCanciones.size();
        }

        playCancion(indexCancion);
    }

    public void retrocede10(){
        long pos= player.getCurrentPosition();
        player.seekTo(Math.max(0, pos - 10000));
    }

    public void adelanta10(){
        long pos= player.getCurrentPosition();
        player.seekTo(Math.min(player.getDuration(), pos + 10000));
    }

    private void onSongEnded(){
        switch (modoRepetir){
            case REPETIR_CANCION:
                player.seekTo(0);
                player.play();
                break;
            case REPETIR_LISTA:
                playSiguienteCancion();
                break;
            case REPETIR_OFF:
                if(indexCancion < listaCanciones.size() - 1){ playSiguienteCancion();}
                break;
        }
    }

    private void generarIndexListaAleatorio(){
        listaAleatorio.clear();

        for(int i=0; i < listaCanciones.size(); i++){
            listaAleatorio.add(i);
        }
        Collections.shuffle(listaAleatorio, new Random());
    }

    public void play(){ player.play();}
    public void pausar(){ player.pause();}
    public void stop(){ player.stop();}
    public boolean isPlaying(){ return player.isPlaying();}
    public ExoPlayer getPlayer(){ return player;}

    public Cancion getCancionActual(){
        if(listaCanciones.isEmpty() || indexCancion < 0 || indexCancion >= listaCanciones.size()){ return null;}
        return listaCanciones.get(indexCancion);
    }

    public long getCurrentPosition(){ return player.getCurrentPosition();}

    public void seekTo(long posicion){ player.seekTo(posicion);}

    public void setShuffleMode(boolean activado){
        this.modoAleatorio= activado;
        if(activado){ generarIndexListaAleatorio();}
    }

    public boolean isModoAleatorio(){ return modoAleatorio;}
    public void setRepetirLista(int modo){ this.modoRepetir= modo;}
    public int getModoRepetir(){ return modoRepetir;}
    public void setIndexCancion(int index){ this.indexCancion= index;}




    //NOTIFICACIONESSSSSSSSSSSSSSSSSSSSS
    private Notification armarNotificacion(){
        Intent intentNotif= new Intent(this, MainActivity.class);

        PendingIntent intentContenido= PendingIntent.getActivity(
                this, 0, intentNotif,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        PendingIntent intentAnterior= PendingIntent.getService(
                this, 0,
                new Intent(this, ServicioMusica.class).setAction("ACTION_PREVIOUS"),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        PendingIntent intentPlayPausa= PendingIntent.getService(
                this, 0,
                new Intent(this, ServicioMusica.class).setAction(
                        player.isPlaying()? "ACTION_PAUSE" : "ACTION_PLAY"
                ),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        PendingIntent intentSiguiente= PendingIntent.getService(
                this, 0,
                new Intent(this, ServicioMusica.class).setAction("ACTION_NEXT"),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Action accionAnterior= new NotificationCompat.Action(
                android.R.drawable.ic_media_previous,
                "Anterior",
                intentAnterior
        );

        NotificationCompat.Action accionPlayPausa= new NotificationCompat.Action(
                player.isPlaying()? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                player.isPlaying()? "Pausar" : "Reproducir",
                intentPlayPausa
        );

        NotificationCompat.Action accionSiguiente= new NotificationCompat.Action(
                android.R.drawable.ic_media_next,
                "Siguiente",
                intentSiguiente
        );

        return new NotificationCompat.Builder(this, ID_CANAL)
                .setContentTitle(titulo)
                .setContentText(artista)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(intentContenido)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0,1,2))
                .addAction(accionAnterior)
                .addAction(accionPlayPausa)
                .addAction(accionSiguiente)
                .build();
    }

    private void actualizarNotificacion(){
        NotificationManager manager= null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager = getSystemService(NotificationManager.class);
        }
        if(manager != null){
            manager.notify(1, armarNotificacion());
        }
    }

    //ON DESTROY
    @Override
    public void onDestroy() {
        if(player != null){ player.release();}
        if(mediaSession != null){ mediaSession.release();}

        super.onDestroy();
    }

    private void crearCanalNotificaciones(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel canal= new NotificationChannel(
                    ID_CANAL,
                    "Canal de música",
                    NotificationManager.IMPORTANCE_LOW
            );
            canal.setDescription("Tamales oaxaqueños");

            NotificationManager manager= getSystemService(NotificationManager.class);
            if(manager != null){ manager.createNotificationChannel(canal);}
        }
    }
}
