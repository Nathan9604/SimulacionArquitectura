public class CacheInstrucciones {
    private int entrada[][];    // Vector de bloques del caché de instrucciones
    private int etiqueta[];     // Vector de etiquetas para cada bloque
    private Memoria memoria;    // Referencia de la memoria principal

    private final int ENTRADASCACHE = 8;
    private final int TAMENTRADA = 16;

    public CacheInstrucciones(Memoria memoria){
        entrada = new int[TAMENTRADA][ENTRADASCACHE];
        etiqueta = new int[ENTRADASCACHE];
        this.memoria = memoria;

        for(int i = 0; i < ENTRADASCACHE; ++i)
            etiqueta[i] = -1;
    }

    /**
     * Se busca el bloque en donde se encuentra la instrucción deseada para ser leída
     * @param ir Indicador de la instrucción que desea ser leída
     * @param instruccion Vector en donde se almacena la instrucción para transmitirla
     * @return numCiclos es el valor de ciclos que tomo realizar la lectura del caché
     */
    public int leerInstruccion(int ir, int[] instruccion){
        ir -= 384;
        int numBloque = ir / 16;
        int numBloqueCache = numBloque % 8;
        int numInstruccion = (ir % 16) / 4;
        int numCiclos = 1;

        if(!estaEnCache(numBloque)) {
            leerMemoriaInstruccion(numBloque, numBloqueCache);
            numCiclos = 32;
        }

        cargarInstruccion(numBloqueCache, numInstruccion, instruccion);

        return numCiclos;
    }

    /**
     * Busca el bloque de memoria deseado en la memoria principal para cargarlo en el caché de instrucciones
     * @param numBloque El bloque que se desea cargar de memoria
     * @param numBloqueCache Número de entrada en el caché para almacenar el nuevo bloque
     */
    private void leerMemoriaInstruccion(int numBloque, int numBloqueCache){
        int[] bloque = new int[16];

        memoria.leerBloqueInstrucciones(numBloque, bloque);

        etiqueta[numBloqueCache] = numBloque;

        for(int i = 0; i < 16; ++i)
            entrada[i][numBloqueCache] = bloque[i];
    }

    /**
     * Se busca la instrucción en el bloque al que pertenece y se carga en un vector para ser transmitido
     * @param numBloqueCache Número de entrada en el caché donde está el bloque deseado
     * @param numInstruccion Número de instrucción que se desea con respecto al bloque
     * @param instruccion Vector en donde almacenar la instrucción deseada para ser transmitida
     */
    private void cargarInstruccion(int numBloqueCache, int numInstruccion, int[] instruccion){
        int posicionBloque = numInstruccion * 4;

        for(int i = 0; i < 4; ++i)
            instruccion[i] = entrada[posicionBloque + i][numBloqueCache];
    }

    /**
     * Busca si el bloque deseado se encuentra en cache
     * @param numBloque Bloque que se desea buscar
     * @return Falso si el bloque no está en caché y True si lo está
     */
    public boolean estaEnCache(int numBloque){
        boolean esta = false;

        if(numBloque == etiqueta[numBloque % 8])
            esta = true;

        return esta;
    }
}
