package com.example.mp3v2;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import Adapters.AdapterCancion;
import POJO.Cancion;
import Servicios.ServicioMusica;

/**
* MainActivity: versión tomar canciones de carpeta
 *
 * Esta activity maneja:
 * 1. Despliegue de canciones
 * 2. Botones de control de canción
 * 3. Actualización de Seekbar, tiempo, botones pause/play
 * 4. Permisos de lectura de archivos
 * 5. Selección de carpeta de canciones
* */

public class MainActivity extends AppCompatActivity {
    //constantes
    private static final int PERMISSION_REQUEST_CODE= 123;

    //variables
    private RecyclerView recyclerView;
    private AdapterCancion adapterCancion;      //AdapterCancion.java
    private List<Cancion> listaCanciones= new ArrayList<>();    //Cancion.java
    //views
    private Button b_cargarCanciones;
    private TextView tv_tituloCancion, tv_artistaCancion, tv_tiempoActual, tv_tiempoCancion;
    private SeekBar seekBar;
    private ImageButton b_cancionAnterior, b_pausarCancion, b_cancionSiguiente;
    private ImageButton b_retroceder, b_adelantar, b_detener, b_modoAleatorio, b_modos;
    private ImageView i_portada;

    //conexión al servicio de música
    private ServicioMusica servicioMusica;      //ServicioMusica.java
    private boolean servicioUnido= false;       //comprobar si servicio se unió (bound)

    //handler para actualizar UI
    private Handler handler= new Handler();

    //index donde va en la lista de canciones
    private int indexCanciones= 0;

    //launcher para elegir carpeta local
    private ActivityResultLauncher<Uri> launcherEligeCarpeta;

    //conseguir conexión al servicio
    private ServiceConnection serviceConnection= new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder servicio) {
            ServicioMusica.BinderMusica binder= (ServicioMusica.BinderMusica) servicio;
            servicioMusica= binder.getService();
            servicioUnido= true;

            servicioMusica.setCallbackUI(new ServicioMusica.CallbackUI() {
                @Override
                public void onActualizarPortada(Bitmap portada) {
                    runOnUiThread(() -> {
                        if(portada != null){ i_portada.setImageBitmap(portada);}
                        else{ i_portada.setImageResource(R.drawable.ic_launcher_foreground);}
                    });
                }

                @Override
                public void onActualizarCancion(String titulo, String artista) {
                    runOnUiThread(() -> {
                        tv_tituloCancion.setText(titulo);
                        tv_artistaCancion.setText(artista);
                    });
                }
            });

            actualizarSeekBar();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            servicioUnido= false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        //inicializar todo
        inicializarViews();
        ponerRecyclerView();
        ponerClickListeners();
        ponerSeleccionadorCarpeta();
        checarPermisos();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    //métodos de inicialización
    private void inicializarViews(){
        recyclerView= findViewById(R.id.recyclerView);
        b_cargarCanciones= findViewById(R.id.b_cargarCanciones);
        tv_tituloCancion= findViewById(R.id.tv_tituloCancion);
        tv_artistaCancion= findViewById(R.id.tv_artistaCancion);
        tv_tiempoActual= findViewById(R.id.tv_tiempoActual);
        tv_tiempoCancion= findViewById(R.id.tv_tiempoCancion);
        seekBar= findViewById(R.id.seekBar);

        b_cancionAnterior= findViewById(R.id.b_cancionAnterior);
        b_pausarCancion= findViewById(R.id.b_pausarCancion);
        b_cancionSiguiente= findViewById(R.id.b_cancionSiguiente);
        i_portada= findViewById(R.id.i_portada);

        b_retroceder= findViewById(R.id.b_retroceder);
        b_adelantar= findViewById(R.id.b_adelantar);
        b_detener= findViewById(R.id.b_detener);
        b_modoAleatorio= findViewById(R.id.b_modoAleatorio);
        b_modos= findViewById(R.id.b_modos);
        //b_loopLista= findViewById(R.id.b_loopLista);
        //b_unaCancion= findViewById(R.id.b_unaCancion);

        b_pausarCancion.setImageResource(android.R.drawable.ic_media_pause);
        b_cancionAnterior.setImageResource(android.R.drawable.ic_media_previous);
        b_cancionSiguiente.setImageResource(android.R.drawable.ic_media_next);

        b_retroceder.setImageResource(android.R.drawable.ic_media_rew);
        b_adelantar.setImageResource(android.R.drawable.ic_media_ff);
        b_detener.setImageResource(android.R.drawable.ic_delete);
        b_modoAleatorio.setImageResource(R.drawable.shuffle_off);
        //opciones iconos: off, repeat y repeat_one
        b_modos.setImageResource(android.R.drawable.ic_lock_power_off);
        //b_loopLista.setImageResource(R.drawable.repeat);
        //b_unaCancion.setImageResource(R.drawable.repeat_one);
    }
    private void ponerRecyclerView(){
        //adapter con clickListener (función Lambda)
        adapterCancion= new AdapterCancion(listaCanciones, this::enClickCancion);

        //LLM (LinearLayoutManager) lista vertical
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapterCancion);
    }
    private void ponerClickListeners(){
        b_pausarCancion.setOnClickListener(view -> togglePlayPausa());
        b_cancionAnterior.setOnClickListener(view -> playCancionAnterior());
        b_cancionSiguiente.setOnClickListener(view -> playCancionSiguiente());
        b_cargarCanciones.setOnClickListener(view -> cargarCanciones());

        b_retroceder.setOnClickListener(view -> retrocede10());
        b_adelantar.setOnClickListener(view -> adelanta10());
        b_detener.setOnClickListener(view -> detenerCancion());
        b_modoAleatorio.setOnClickListener(view -> toggleAleatorio());
        b_modos.setOnClickListener(view -> toggleModos());

        //Listener Seekbar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progreso, boolean desdeUsuario) {
                //sólo si usuario cambió manualmente la seekBar
                if(desdeUsuario && servicioUnido){
                    servicioMusica.getPlayer().seekTo(progreso);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
    private void ponerSeleccionadorCarpeta(){
        launcherEligeCarpeta= registerForActivityResult(
                new ActivityResultContracts.OpenDocumentTree(),
                uri -> {
                    if (uri != null) {
                        //cargar canciones
                        cargarCancionesDesdeCarpeta(uri);

                        //tomar permiso persistable (para acceder carpeta después)
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                }
        );
    }
    private void checarPermisos(){
        //caso Android 13+ (necesita permisos audio/notificaciones)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED){
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE
                );
            }
        }
    }



    //resto de métodos
    private void cargarCanciones(){
        launcherEligeCarpeta.launch(null);
    }

    private void cargarCancionesDesdeCarpeta(Uri uriCarpeta){
        listaCanciones.clear();

        DocumentFile carpeta= DocumentFile.fromTreeUri(this, uriCarpeta);

        if(carpeta != null && carpeta.isDirectory()){
            //desplegar todas las canciones de la carpeta
            DocumentFile[] archivos= carpeta.listFiles();

            for(DocumentFile archivo : archivos){
                if(archivo.isFile()){
                    String nombre= archivo.getName();
                    String tipo= archivo.getType();

                    //caso es archivo de audio
                    if(nombre != null && tipo != null &&
                            (tipo.startsWith("audio/") || nombre.endsWith(".mp3"))){
                        String titulo= nombre.substring(0, nombre.lastIndexOf('.'));
                        String autor= "Autor desconocido";

                        Cancion cancion= new Cancion(titulo, autor, archivo.getUri().toString(), 0);

                        listaCanciones.add(cancion);
                    }
                }
            }

            //avisar adapter que cambiaron los datos
            adapterCancion.notifyDataSetChanged();

            if(!servicioUnido){
                Intent intent= new Intent(this, ServicioMusica.class);
                bindService(intent, serviceConnection, BIND_AUTO_CREATE);

                //esperar un poco para conexion, luego pasar playlist
                handler.postDelayed(() -> {
                    if(servicioUnido){ servicioMusica.setPlaylist(listaCanciones);}
                }, 500);
            }
            else{ servicioMusica.setPlaylist(listaCanciones);}

            Toast.makeText(this,
                    String.format("Se cargaron %d canciones.", listaCanciones.size()),
                    Toast.LENGTH_SHORT).show();
        } else{
            Toast.makeText(this, "No se seleccionó una carpeta válida.", Toast.LENGTH_SHORT).show();
        }
    }

    private void enClickCancion(int posicion){
        indexCanciones= posicion;
        reproducirCancion(listaCanciones.get(posicion));
    }

    private void reproducirCancion(Cancion cancion){
        Intent intent= new Intent(this, ServicioMusica.class);

        //poner datos al Intent
        intent.setAction("ACTION_PLAY");
        intent.putExtra("path", cancion.getPath());
        intent.putExtra("titulo", cancion.getTitulo());
        intent.putExtra("artista", cancion.getArtista());

        //iniciar servicio
        //caso Android 8+
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            startForegroundService(intent);
        } else{
            startService(intent);
        }

        //bindService ya puesto en cargarCancionesDesdeCarpeta
        //bindService(intent, serviceConnection, BIND_AUTO_CREATE);

        if(servicioUnido){ servicioMusica.setPlaylist(listaCanciones);}

        //actualizar UI
        tv_tituloCancion.setText(cancion.getTitulo());
        tv_artistaCancion.setText(cancion.getArtista());
        Bitmap portada= servicioMusica.getPortada();
        if(portada != null){ i_portada.setImageBitmap(portada);}
        else{ i_portada.setImageResource(R.drawable.ic_launcher_foreground);}
        actualizarBotonPlayPause();
    }

    private void togglePlayPausa(){
        if(servicioUnido){
            Intent intent= new Intent(this, ServicioMusica.class);

            //hacer accion opuesta al estado actual
            if(servicioMusica.isPlaying()){
                intent.setAction("ACTION_PAUSE");
            } else{
                intent.setAction("ACTION_PLAY");
            }

            startService(intent);

            //esperar un poco para cambiar de estado + actualizar boton
            handler.postDelayed(this::actualizarBotonPlayPause, 250);
        }
        else{
            Toast.makeText(this, "No hay canción cargada.", Toast.LENGTH_SHORT).show();
        }
    }

    private void playCancionSiguiente(){
        if(!listaCanciones.isEmpty()){
            //operacion para continuar en 1er cancion si estas en el fin de la lista
            indexCanciones= (indexCanciones + 1) % listaCanciones.size();
            reproducirCancion(listaCanciones.get(indexCanciones));
        }
    }

    private void playCancionAnterior(){
        if(!listaCanciones.isEmpty()){
            //operacion para continuar en ultima cancion si estas en la 1er de la lista
            indexCanciones= (indexCanciones - 1 + listaCanciones.size()) % listaCanciones.size();
            reproducirCancion(listaCanciones.get(indexCanciones));
        }
    }

    private void retrocede10(){
        if(servicioMusica != null){ servicioMusica.retrocede10();}
    }

    private void adelanta10(){
        if(servicioMusica != null){ servicioMusica.adelanta10();}
    }

    private void detenerCancion(){
        if(servicioMusica != null){
            servicioMusica.stop();

            b_pausarCancion.setImageResource(android.R.drawable.ic_media_play);
            seekBar.setProgress(0);
            tv_tiempoActual.setText("00:00");
        }
    }

    private void toggleAleatorio(){
        if (servicioMusica != null) {
            boolean nuevo= !servicioMusica.isModoAleatorio();
            servicioMusica.setShuffleMode(nuevo);

            b_modoAleatorio.setImageResource(
                    nuevo? R.drawable.shuffle : R.drawable.shuffle_off
            );
        }
    }

    //ciclo repetición: off -> lista -> canción -> off
    private void toggleModos(){
        if(servicioMusica == null){ return;}

        int modo= servicioMusica.getModoRepetir();

        if(modo == ServicioMusica.REPETIR_OFF){ modo= ServicioMusica.REPETIR_LISTA;}
        else if(modo == ServicioMusica.REPETIR_LISTA){ modo= ServicioMusica.REPETIR_CANCION;}
        else{ modo= ServicioMusica.REPETIR_OFF;}

        servicioMusica.setRepetirLista(modo);

        //cambiar icono
        switch (modo){
            case ServicioMusica.REPETIR_OFF:
                b_modos.setImageResource(android.R.drawable.ic_lock_power_off);
                break;
            case ServicioMusica.REPETIR_LISTA:
                b_modos.setImageResource(R.drawable.repeat);
                break;
            case ServicioMusica.REPETIR_CANCION:
                b_modos.setImageResource(R.drawable.repeat_one);
        }
    }

    private void actualizarBotonPlayPause(){
        if(servicioUnido && servicioMusica.isPlaying()){
            b_pausarCancion.setImageResource(android.R.drawable.ic_media_pause);
        }
        else{
            b_pausarCancion.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void actualizarSeekBar(){
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(servicioUnido && servicioMusica.getPlayer() != null){
                    ExoPlayer player= servicioMusica.getPlayer();

                    //conseguir posicion y tiempo duracion
                    long posActual= player.getCurrentPosition();
                    long duracion= player.getDuration();

                    //actualizar UI (si valores validos, claro)
                    if(duracion > 0){
                        seekBar.setMax((int) duracion);
                        seekBar.setProgress((int) posActual);

                        tv_tiempoActual.setText(formatearTiempo(posActual));
                        tv_tiempoCancion.setText(formatearTiempo(duracion));
                    }
                    actualizarBotonPlayPause();
                }

                //poner que se actualice cada segundo
                handler.postDelayed(this, 1000);
            }
        }, 0);
    }

    //convertir ms -> mm:ss
    private String formatearTiempo(long milisegundos){
        long minutos= TimeUnit.MILLISECONDS.toMinutes(milisegundos);
        long segundos= TimeUnit.MILLISECONDS.toSeconds(milisegundos) % 60;      //contar num minutos (y eliminar segundos) con modulus

        return String.format("%02d:%02d", minutos, segundos);
    }



    //ON DESTROY
    @Override
    protected void onDestroy() {
        super.onDestroy();

        //dejar de usar servicio
        if(servicioUnido){
            unbindService(serviceConnection);
            servicioUnido= false;
        }

        //eliminar todos los callbacks restantes
        handler.removeCallbacksAndMessages(null);
    }

    //cuando usuario responde al request de permiso
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, int deviceId) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId);

        if(requestCode == PERMISSION_REQUEST_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Acceso permitido", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}