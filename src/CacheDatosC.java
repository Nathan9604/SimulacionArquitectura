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

        private final int ENTRADASVIA = 4;

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
         * @return numCiclos Cantidad de ciclos que toma escribir el dato en caché
         */
        public int escribirDato(int numBloque, int numDato, int dato, int dir){
            int numEntrada;
            int numCiclos = 1;

            // Si el bloque no esta en la caché lo carga, caso contrario intenta invalidar el mismo bloque en la
            // otra caché (si esta lo tiene)
            if(!existeBloque(numBloque))
                numCiclos = cargarBloque(dir, numBloque);
            else
                otraCache.cambiarBandera(numBloque, 'I');

            // Busca la posición del bloque en la vía
            numEntrada = indiceBloque(numBloque);

            // Guarda el nuevo dato y marca el bloque como Modificado
            entrada[numDato][numEntrada] = dato;
            estado[numEntrada] = 'M';

            return numCiclos;
        }

        /**
         * Busca el bloque en donde se va a leer el dato si el caché lo tiene, sino lo busca en la otro caché de
         * datos o la memoria principal y luego lee el dato esperado
         * @param numBloque Número de bloque que se desea conseguir
         * @param numDato Posición con respecto al bloque en donde se encuentra el dato que se desea leer
         * @param dir Dirección de memoria en donde se va a leer el dato
         * @param datos Vector para almacenar el dato que se desea leer y numCiclos Cantidad de ciclos que toma leer el dato en caché
         */
        public void leerDato(int numBloque, int numDato, int dir, int datos[]){
            int numEntrada;
            datos[1] = 1; // Si el dato está en nuestro caché solo toma 1 ciclo de reloj

            // Si la caché no tiene el bloque lo carga
            if(!existeBloque(numBloque))
                datos[1] = cargarBloque(dir, numBloque);

            // Busca el indice en la vía en donde está el bloque, cómo fue anteriormente cargado es un hecho que esta
            // en la vía
            numEntrada = indiceBloque(numBloque);

            datos[0] = entrada[numDato][numEntrada];
        }

        /**
         * Carga el bloque que se desea desde la otra caché o la memoria principal. Si el bloque en caché en donde se
         * va a almacenar el nuevo bloque esta modificado lo guarda en memoria y luego carga el nuevo bloque en
         * esa posición
         * @param dir Dirección del bloque que se desea cargar
         * @param numBloque Número del bloque que se desea cargar
         * @return numCiclos Cantidad de ciclos para realizar la instrucción
         */
        public int cargarBloque(int dir, int numBloque){
            int[] bloque = new int[4];
            int numEntrada = bloqueMasViejo[0]; // Vamos a cambiar el bloque más viejo
            int numCiclos = 0;

            // Si el bloque de memoria en donde se va a cargar el nuevo esta modificado lo guarda en memoria
            if(estado[numEntrada] == 'M') {
                for(int i = 0; i < TAMENTRADA; ++i)
                    bloque[i] = entrada[i][numEntrada];

                memoria.escribirBloqueDatos(etiqueta[numEntrada] * 16, bloque);

                numCiclos = 32;
            }

            // Si el bloque deseado está en la otra caché lo trae de ella sino lo trae desde la memoria principla
            if(otraCache.existeBloque(numBloque)) {
                otraCache.enviarBloque(numBloque, bloque);
                numCiclos += 2;
            }
            else {
                memoria.leerBloqueDatos(dir, bloque);
                numCiclos += 32;
            }

            // Modifica el índice que indica cual es la entrada más vieja de la via
            modificarMasViejo();

            // Carga el nuevo bloque en la entrada del caché
            for(int i = 0; i < TAMENTRADA; ++i)
                entrada[i][numEntrada] = bloque[i];

            estado[numEntrada] = 'C';
            etiqueta[numEntrada] = numBloque;

            return numCiclos;
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

                    memoria.escribirBloqueDatos(etiqueta[numEntrada] * 16, bloqueGuardar);
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
         * Busca el índice del bloque en la vía en donde se encuentra el bloque que se desea
         * @param numBloque Número de bloque que se desea buscar
         * @return El número de la entrada del caché en donde se encuentra el bloque deseado
         */
        public int indiceBloque(int numBloque){
            int posicion = -1;
            int iterador = 0;
            boolean encontro = false;

            // Busca el índice de la entrada en donde se encuentran el bloque deseado
            while(iterador < ENTRADASVIA && encontro == false){
                if(numBloque == etiqueta[iterador]){
                    encontro = true;
                    posicion = iterador;
                }
                else
                    ++iterador;
            }

            return posicion;
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
         * Cambia el valor de la bandera de estado de cierto bloque, puede haber varias veces el mismo bloque (invalido
         * o no) por lo que cambiará la bandera a invalido todas las veces que ese bloque este en la vía
         * @param numBloque Número del bloque al que se le desea cambiar la bandera
         * @param bandera Valor de la bandera que se desea colocar
         */
        public void cambiarBandera(int numBloque, char bandera){
            int numEntrada = 0;

            //do {
                numEntrada = indiceBloque(numBloque);

                if (numEntrada != -1)
                    estado[numEntrada] = bandera;
            //}while(numEntrada != -1);
        }

        /**
         * Busca el valor de la bandera de cierto bloque
         * @param numBloque bloque al que se le desea conocer el valor de la bandera
         * @return el estado del bloque en la caché
         */
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
                System.out.print(entrada[fila][i] + "\t\t");
        }

        /**
         * Imprime en pantalla la etiqueta de cada una de las entradas de la vía
         */
        public void printEtiqueta(){
            for(int i = 0; i < ENTRADASVIA; ++i)
                System.out.print(etiqueta[i] + "\t\t");
        }

        /**
         * Imprime en pantalla el estado de cada una de las entradas de la vía
         */
        public void printEstado(){
            for(int i = 0; i < ENTRADASVIA; ++i)
                System.out.print(estado[i] + "\t\t");
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
     * @return numCiclos Cantidad de ciclos que toma escribir el dato en caché
     */
    @Override
    public int escribirDato(int dir, int dato){
        int numDato = (dir % 16) / 4;
        int numBloque = dir / 16;
        int numCiclos = 1;

        if((numBloque % 2) == 0)
            numCiclos = viaPar.escribirDato(numBloque, numDato, dato, dir);
        else
            numCiclos = viaImpar.escribirDato(numBloque, numDato, dato, dir);

        return numCiclos;
    }

    /**
     * Busca la vía en donde se desea leer el dato
     * @param dir Dirección de memoria del dato que se desea leer
     * @param datos vector para almacenar el dato leído y numCiclos que es la cantidad de ciclos que tomo leer el dato de la caché
     */
    @Override
    public void leerDato(int dir, int[] datos){
        int numDato = (dir % 16) / 4;
        int numBloque = dir / 16;

        if((numBloque % 2) == 0)
            viaPar.leerDato(numBloque, numDato, dir, datos);
        else
            viaImpar.leerDato(numBloque, numDato, dir, datos);
    }

    /**
     * Busca el bloque que desea cargar en cierta posición de memoria.
     * No sé usa pero debe de estar presente para que no hay errores en la interfaz CacheDatos
     * @param dir Dirección en memoria del dato que se dese leer
     * @param numBloque Número del bloque que se desea cargar
     * @return numCiclos Cantidad de ciclos que tomo cargar el bloque a caché
     */
    @Override
    public int cargarBloque(int dir, int numBloque) {
        int numCiclos;

        if((numBloque % 2) == 0)
            numCiclos = viaPar.cargarBloque(dir, numBloque);
        else
            numCiclos = viaImpar.cargarBloque(dir, numBloque);

        return numCiclos;
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