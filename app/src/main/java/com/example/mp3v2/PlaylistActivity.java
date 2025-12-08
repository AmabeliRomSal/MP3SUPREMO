package com.example.mp3v2;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import android.os.Handler;

import Adapters.AdapterCancion;
import POJO.Cancion;
import Servicios.ServicioMusica;

/** PlaylistActivity: pantalla de lista de canciones
 *
 * Esta activity:
 * - Permite seleccionar carpeta de musica
 * - Muestra lista canciones
 * - Reproducir cancion dandole click
 * - Mostrar cancion reproduciendo
 */

public class PlaylistActivity extends AppCompatActivity {
    //constantes
    private static final int PERMISSION_REQUEST_CODE= 123;

    //variables
    private Button b_seleccionarCarpeta, b_volverAMain;
    private RecyclerView recyclerViewPlaylist;

    private AdapterCancion adapterCancion;
    private List<Cancion> listaCanciones= new ArrayList<>();

    //sobre servicios
    private ServicioMusica servicioMusica;
    private boolean servicioUnido= false;

    private ActivityResultLauncher<Uri> launcherEligeCarpeta;
    private Handler handler= new Handler(Looper.getMainLooper());


    //conseguir conexion a servicio
    private ServiceConnection serviceConnection= new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName nombre, IBinder servicio) {
            ServicioMusica.BinderMusica binder= (ServicioMusica.BinderMusica) servicio;
            servicioMusica= binder.getService();
            servicioUnido= true;
        }

        @Override
        public void onServiceDisconnected(ComponentName nombre) {
            servicioUnido= false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_playlist);

        inicializarViews();
        ponerRecyclerView();
        ponerClickListeners();
        ponerSeleccionadorCarpeta();
        checarPermisos();
        unirAlServicio();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    //métodos de inicialización
    private void inicializarViews(){
        b_seleccionarCarpeta= findViewById(R.id.b_seleccionarCarpeta);
        b_volverAMain= findViewById(R.id.b_volverAMain);
        recyclerViewPlaylist= findViewById(R.id.recyclerViewPlaylist);
    }
    private void ponerRecyclerView(){
        adapterCancion= new AdapterCancion(listaCanciones, this::enClickCancion);
        recyclerViewPlaylist.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPlaylist.setAdapter(adapterCancion);
    }
    private void ponerClickListeners(){
        b_seleccionarCarpeta.setOnClickListener(view -> abrirSeleccionadorCarpeta());
        b_volverAMain.setOnClickListener(v -> finish());            //finalizar activity
    }
    private void ponerSeleccionadorCarpeta(){
        launcherEligeCarpeta= registerForActivityResult(
                new ActivityResultContracts.OpenDocumentTree(),
                uri -> {
                    if(uri != null){
                        cargarCancionesDesdeCarpeta(uri);
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                }
        );
    }
    private void checarPermisos(){
        //permiso notificaciones
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
    private void unirAlServicio(){
        Intent intent= new Intent(this, ServicioMusica.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void abrirSeleccionadorCarpeta(){ launcherEligeCarpeta.launch(null);}

    private void cargarCancionesDesdeCarpeta(Uri uriCarpeta){
        listaCanciones.clear();

        DocumentFile carpeta= DocumentFile.fromTreeUri(this, uriCarpeta);

        if(carpeta != null && carpeta.isDirectory()){
            DocumentFile[] archivos= carpeta.listFiles();

            for(DocumentFile archivo : archivos){
                if(archivo.isFile()){
                    String nombre= archivo.getName();
                    String tipo= archivo.getType();

                    if(nombre != null && tipo != null &&
                        (tipo.startsWith("audio/") || nombre.endsWith(".mp3") || nombre.endsWith(".wav") || nombre.endsWith(".mp4"))){
                        String titulo= nombre.substring(0, nombre.lastIndexOf('.'));
                        String artista= "Artista desconocido";

                        Cancion cancion= new Cancion(titulo, artista, archivo.getUri().toString(), 0);

                        listaCanciones.add(cancion);
                    }
                }
            }

            adapterCancion.notifyDataSetChanged();

            handler.postDelayed(() -> {
                if(servicioUnido){ servicioMusica.setPlaylist(listaCanciones);}
            }, 300);

            if(servicioUnido){ servicioMusica.setPlaylist(listaCanciones);}

            Toast.makeText(this, "Se cargaron " + listaCanciones.size() + " canciones", Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(this, "No se seleccionó una carpeta válida", Toast.LENGTH_SHORT).show();
        }
    }

    private void enClickCancion(int posicion){
        if(servicioUnido){
            servicioMusica.setIndexCancion(posicion);
            servicioMusica.playCancion(posicion);

            setResult(RESULT_OK);       //avisar cancion seleccionada

            Toast.makeText(this, "Reproduciendo: " + listaCanciones.get(posicion).getTitulo(), Toast.LENGTH_SHORT).show();
        }
        else{ Toast.makeText(this, "Servicio no conectado", Toast.LENGTH_SHORT).show();}
    }



    //ON DESTROY
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(servicioUnido){
            unbindService(serviceConnection);
            servicioUnido= false;
        }

        handler.removeCallbacksAndMessages(null);
    }
}