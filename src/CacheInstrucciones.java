public class CacheInstrucciones {
    int entradas[][];
    int etiquetas[];

    public CacheInstrucciones(){
        entradas = new int[16][8];
        etiquetas = new int[8];

        for(int i = 0; i < etiquetas.length; ++i)
            etiquetas[i] = -1;
    }

    public void leerInstruccion(int ir, int[] instruccion){
        ir -= 384;
        int numBloque = ir / 16;
        int numBloqueCache = numBloque % 8;
        int numInstruccion = (ir % 16) / 4;

        if(!estaEnCache(numBloque))
            leerMemoriaInstruccion(numBloque, numBloqueCache);

        cargarInstruccion(numBloqueCache, numInstruccion, instruccion);
    }

    private void leerMemoriaInstruccion(int numBloque, int numBloqueCache){
        int[] bloque = new int[16];

        // TODO memoria.leerBloqueInstrucciones(numBloque, bloque);

        etiquetas[numBloqueCache] = numBloque;

        for(int i = 0; i < 16; ++i)
            entradas[i][numBloqueCache] = bloque[i];
    }

    private void cargarInstruccion(int numBloqueCache, int numInstruccion, int[] instruccion){
        int posicionBloque = numInstruccion * 4;

        for(int i = 0; i < 4; ++i)
            instruccion[i] = entradas[posicionBloque + i][numBloqueCache];
    }

    private boolean estaEnCache(int numBloque){
        boolean esta = false;

        if(numBloque == etiquetas[numBloque % 8])
            esta = true;

        return esta;
    }
}
