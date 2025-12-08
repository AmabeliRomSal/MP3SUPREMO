package POJO;

import androidx.annotation.NonNull;

/** Modelo datos representando una canción
 * es un POJO, puros getters/setters, etc
 */

public class Cancion {
    //encapsulación usando private
    private String titulo, artista, path;       //nombre cancion, artista cancion, camino a carpeta
    private long duracion;      //tiempo que dura cancion en miliseg.

    //constructor
    public Cancion(String titulo, String artista, String path, long duracion){
        this.titulo= titulo;
        this.artista= artista;
        this.path= path;
        this.duracion= duracion;
    }

    //getters y setters
    public String getTitulo() {
        return titulo;
    }
    public String getArtista() {
        return artista;
    }
    public String getPath() {
        return path;
    }
    public long getDuracion() {
        return duracion;
    }

    public void setTitulo(String titulo) {this.titulo = titulo;}
    public void setArtista(String artista) {this.artista = artista;}
    public void setPath(String path) {this.path = path;}
    public void setDuracion(long duracion) {this.duracion = duracion;}

    //override de toString
    @NonNull
    @Override
    public String toString() {
        return "Cancion{" +
                "title='" + titulo + '\'' +
                ", artist='" + artista + '\'' +
                ", path='" + path + '\'' +
                ", duration=" + duracion + "ms" +
                '}';
    }
}
