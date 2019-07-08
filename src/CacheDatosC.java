class CacheDatosC implements CacheDatos {
    private Memoria memoria;        // Referencia de la memoria principal
    private CacheDatos otraCache;  // Referencia de la otra caché de datos
    private int rl;                 // Referencia de el RL del núcleo

    private final int ENTRADASCACHE = 8;
    private final int TAMENTRADA = 4;

    private class Via{
        private int entrada[][];        // Vector de bloque de datos
        private int etiqueta[];         // Vector que indica el número del bloque en cierta entrada
        private char estado[];          // Vector que indica el estado del bloque en cierta entrada
        private int bloqueMasViejo[];   // Vector que nos indica la posición del caché a la que se le dio un uso más tiempo

        private final int ENTRADASVIA = 8;

        public Via(){
            entrada = new int[TAMENTRADA][ENTRADASVIA];
            bloqueMasViejo = new int[ENTRADASVIA];
            etiqueta = new int[ENTRADASVIA];
            estado = new char[ENTRADASVIA];

            for(int i = 0; i < ENTRADASVIA; ++i) {
                etiqueta[i] = -1;
                estado[i] = 'I';
                bloqueMasViejo[i] = i;
            }
        }

        /**
         * Busca el bloque en donde se va a escribir el dato si el caché lo tiene, sino lo busca en la otro caché de
         * datos o la memoria principal y escribe el dato en la posición deseada
         * @param numBloque Número de bloque que se desea conseguir
         * @param numDato Posición con respecto al bloque en donde se encuentra el dato que se desea modificar
         * @param dato Dato que se desea almacenar en el bloque
         * @param dir Dirección de memoria en donde se va a almacenar el dato
         */
        public void escribirDato(int numBloque, int numDato, int dato, int dir){
            int numEntrada;

            if(!existeBloque(numBloque))
                // El 0 que se envía de parámetro se debe a que en Java no se pueden definir parámetros por defecto
                cargarBloque(dir, numBloque);
            else
                otraCache.cambiarBandera(numBloque, 'I');

            numEntrada = indiceBloque(numBloque);

            // Guarda el nuevo dato y marca el bloque como Modificado
            entrada[numDato][numEntrada] = dato;
            estado[numEntrada] = 'M';
        }

        /**
         * Busca el bloque en donde se va a leer el dato si el caché lo tiene, sino lo busca en la otro caché de
         * datos o la memoria principal y luego lee el dato esperado
         * @param numBloque Número de bloque que se desea conseguir
         * @param numDato Posición con respecto al bloque en donde se encuentra el dato que se desea leer
         * @param dir Dirección de memoria en donde se va a leer el dato
         * @return El dato que se desea leer
         */
        public int leerDato(int numBloque, int numDato, int dir){
            int dato;
            int numEntrada;

            if(!existeBloque(numBloque))
                // El 0 que se envía de parámetro se debe a que en Java no se pueden definir parámetros por defecto
                cargarBloque(dir, numBloque);

            numEntrada = indiceBloque(numBloque);

            dato = entrada[numDato][numEntrada];

            return dato;
        }

        /**
         * Carga el bloque que se desea desde la otra caché o la memoria principal. Si el bloque en caché en donde se
         * va a almacenar el nuevo bloque esta modificado lo guarda en memoria y luego carga el nuevo bloque en
         * esa posición
         * @param dir Dirección del bloque que se desea cargar
         * @param numBloque Número del bloque que se desea cargar
         */
        public void cargarBloque(int dir, int numBloque){
            int[] bloque = new int[4];
            int numEntrada = bloqueMasViejo[0]; // Vamos a cambiar el bloque más viejo

            // Si el bloque de memoria en donde se va a cargar el nuevo esta modificado lo guarda en memoria
            if(estado[numEntrada] == 'M') {
                for(int i = 0; i < TAMENTRADA; ++i)
                    bloque[i] = entrada[i][numEntrada];

                memoria.escribirBloqueDatos(etiqueta[numEntrada] * TAMENTRADA, bloque);
            }

            // Si el bloque deseado está en la otra caché lo trae de ella sino lo trae desde la memoria principla
            if(otraCache.existeBloque(numBloque))
                otraCache.enviarBloque(numBloque, bloque);
            else
                memoria.leerBloqueDatos(dir, bloque);

            // Modifica el índice que indica cual es la entrada más vieja de la via
            modificarMasViejo();

            // Carga el nuevo bloque en la entrada del caché
            for(int i = 0; i < TAMENTRADA; ++i)
                entrada[i][numEntrada] = bloque[i];

            estado[numEntrada] = 'C';
            etiqueta[numEntrada] = numBloque;
        }

        /**
         * Busca el bloque que se desea transmitir y si se encuentra modificado lo guarda en memoria para luego
         * transmitirlo
         * @param numBloque Número de bloque que se desea enviar
         * @param bloque Vector en donde se puede almacenar el bloque que se desea transmitir
         */
        public void enviarBloque(int numBloque, int[] bloque){
            int numEntrada = indiceBloque(numBloque);

            if(numEntrada != -1) {
                // Si el bloque que vamos a enviar esta modificado lo guarda en memoria primero
                if (estado[numEntrada] == 'M') {
                    int[] bloqueGuardar = new int[TAMENTRADA];

                    for (int i = 0; i < TAMENTRADA; ++i)
                        bloqueGuardar[i] = entrada[i][numEntrada];

                    memoria.escribirBloqueDatos(etiqueta[numEntrada] * TAMENTRADA, bloqueGuardar);
                    estado[numEntrada] = 'C';
                }

                // Carga el bloque que se va a enviar en el vector para poder transmitirlo
                for (int i = 0; i < TAMENTRADA; ++i)
                    bloque[i] = entrada[i][numEntrada];
            }
        }

        /**
         * Verifica si el bloque deseado existe en alguna de las entradas de la vía
         * @param numBloque Bloque de memoria que se desea verificar
         * @return False si el bloque no existe y True si existe
         */
        public boolean existeBloque(int numBloque){
            boolean existe = false;
            int iterador = 0;

            // Si lo encuentra quiere decir que existe sino no existe en el caché
            while(iterador < ENTRADASVIA && existe == false){
                if(numBloque == etiqueta[iterador]) {
                    if(estado[iterador] != 'I')
                        existe = true;

                    iterador = ENTRADASVIA; // Ya lo encontró, no siga buscando
                }
                ++iterador;
            }

            return existe;
        }

        /**
         * Busca el índice del bloque en donde se encuentra el bloque que se desea
         * @param numBloque Número de bloque que se desea buscar
         * @return El número de la entrada del caché en donde se encuentra el bloque deseado
         */
        public int indiceBloque(int numBloque){
            int iterador = -1;
            boolean encontro = false;

            // Busca el índice de la entrada en donde se encuentran el bloque deseado
            while(iterador < ENTRADASVIA && encontro == false){
                if(numBloque == etiqueta[iterador])
                    encontro = true;
                else
                    ++iterador;
            }

            return iterador;
        }

        /**
         * Mueve el índice del bloque más viejo, actualizando el bloque e indicando cual es el nuevo bloque más viejo
         */
        public void modificarMasViejo(){
            int masNuevo = bloqueMasViejo[0];

            for(int i = 1; i < ENTRADASVIA; ++i)
                bloqueMasViejo[i - 1] = bloqueMasViejo[i];

            bloqueMasViejo[ENTRADASVIA - 1] = masNuevo;
        }

        /**
         * Cambia el valor de la bandera de estado de cierto bloque
         * @param numBloque Número del bloque al que se le desea cambiar la bandera
         * @param bandera Valor de la bandera que se desea colocar
         */
        public void cambiarBandera(int numBloque, char bandera){
            int numEntrada = indiceBloque(numBloque);

            if(numEntrada != -1)
                etiqueta[numEntrada] = bandera;
        }

        public char obtenerBandera(int numBloque){
            int numEntrada = indiceBloque(numBloque);
            return estado[numEntrada];
        }

        /**
         * Imprime una posición de dato de cada una de las entradas de la vía
         * @param fila Fila que se imprimirá en pantalla
         */
        public void printFilaCache(int fila){
            for(int i = 0; i < ENTRADASVIA; ++i)
                System.out.print(entrada[fila][i] + " ");
        }

        /**
         * Imprime en pantalla la etiqueta de cada una de las entradas de la vía
         */
        public void printEtiqueta(){
            for(int i = 0; i < ENTRADASVIA; ++i)
                System.out.print(etiqueta[i] + " ");
        }

        /**
         * Imprime en pantalla el estado de cada una de las entradas de la vía
         */
        public void printEstado(){
            for(int i = 0; i < ENTRADASVIA; ++i)
                System.out.print(estado[i] + " ");
        }
    }

    private Via viaPar;
    private Via viaImpar;

    public CacheDatosC(Memoria memoria){
        this.memoria = memoria;
        this.viaPar = new Via();
        this.viaImpar = new Via();
    }

    /**
     * Busca la vía en donde se desea escribir el dato
     * @param dir Dirección de memoria en donde se desea escribir el dato
     * @param dato Dato que se desea escribir
     */
    @Override
    public void escribirDato(int dir, int dato){
        int numDato = (dir % 16) / 4;
        int numBloque = dir / 16;

        if((numBloque % 2) == 0)
            viaPar.escribirDato(numBloque, numDato, dato, dir);
        else
            viaImpar.escribirDato(numBloque, numDato, dato, dir);
    }

    /**
     * Busca la vía en donde se desea leer el dato
     * @param dir Dirección de memoria del dato que se desea leer
     * @return El dato leído
     */
    @Override
    public int leerDato(int dir){
        int dato;
        int numDato = (dir % 16) / 4;
        int numBloque = dir / 16;

        if((numBloque % 2) == 0)
            dato = viaPar.leerDato(numBloque, numDato, dir);
        else
            dato = viaImpar.leerDato(numBloque, numDato, dir);

        return dato;
    }

    /**
     * Busca el bloque que desea cargar en cierta posición de memoria.
     * No sé usa pero debe de estar presente para que no hay errores en la interfaz CacheDatos
     * @param dir Dirección en memoria del dato que se dese leer
     * @param numBloque Número del bloque que se desea cargar
     */
    @Override
    public void cargarBloque(int dir, int numBloque) {
        if((numBloque % 2) == 0)
            viaPar.cargarBloque(dir, numBloque);
        else
            viaImpar.cargarBloque(dir, numBloque);
    }

    /**
     * Busca la vía en donde se encuentra el bloque que se desea enviar
     * @param numBloque Número de bloque que se desea enviar
     * @param bloque Vector en donde se puede almacenar el bloque que se desea transmitir
     */
    @Override
    public void enviarBloque(int numBloque, int[] bloque){
        if((numBloque % 2) == 0)
            viaPar.enviarBloque(numBloque, bloque);
        else
            viaImpar.enviarBloque(numBloque, bloque);
    }

    /**
     * Indica la vía en donde se debe de encontrar el bloque deseado
     * @param numBloque Número del bloque que se desea verificar si existe o no
     * @return False si el bloque no se encuentra en la caché de datos y True si lo hace
     */
    @Override
    public boolean existeBloque(int numBloque){
        boolean existe = false;

        if((numBloque % 2) == 0)
            existe = viaPar.existeBloque(numBloque);
        else
            existe = viaImpar.existeBloque(numBloque);

        return existe;
    }

    /**
     * Busca la vía en donde debe de estar el bloque al que se le desea cambiar el valor de su bandera de estado
     * @param numBloque Es el número del bloque solicitado
     * @param bandera Es el valor de la bandera que se quiere establecer
     */
    @Override
    public void cambiarBandera(int numBloque, char bandera){
        if((numBloque % 2) == 0)
            viaPar.cambiarBandera(numBloque, bandera);
        else
            viaImpar.cambiarBandera(numBloque, bandera);
    }

    @Override
    public char obtenerBandera(int numBloque){
        if((numBloque % 2) == 0)
            return viaPar.obtenerBandera(numBloque);
        else
            return viaImpar.obtenerBandera(numBloque);
    }

    /**
     * Imprime en pantalla la información del caché necesaria
     */
    @Override
    public void print(){
        System.out.println("\n*************************************************************************");

        for(int i = 0; i < TAMENTRADA; ++i) {
            viaPar.printFilaCache(i);
            viaImpar.printFilaCache(i);
            System.out.print("\n");
        }

        viaPar.printEtiqueta();
        viaImpar.printEtiqueta();
        System.out.print("\n");

        viaPar.printEstado();
        viaImpar.printEstado();
        System.out.print("\n");

        System.out.println("*************************************************************************");
    }

    @Override
    public void setOtraCache(CacheDatos otraCache){
        this.otraCache = otraCache;
    }

    @Override
    public void setRl(int rl) {
        this.rl = rl;
    }

    @Override
    public int getRl() {
        return rl;
    }
}