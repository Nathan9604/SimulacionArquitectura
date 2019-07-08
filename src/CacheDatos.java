interface CacheDatos {
    /**
     * Verifica que el bloque de datos al que pertenece el dato que se desea escribir este en el caché, si no lo está
     * lo trae del otro caché o la memoria y luego escribe en ese bloque el dato deseado
     *
     * @param dir  Dirección del memoria en donde se desea escribir el dato
     * @param dato Dato que se desea escribir
     */
    public void escribirDato(int dir, int dato);

    /**
     * Verifica que el bloque en donde se encuentra la dirección de memoria este en el caché, si no lo está lo trae del
     * otro caché o la memoria principal y luego lee el dato deseado
     *
     * @param dir Dirección de memoria que se desea leer
     * @return El dato que se leyó en esa dirección de memoria
     */
    public int leerDato(int dir);

    /**
     * Verifica que el bloque de caché en donde se va a colocar el nuevo bloque no este modificado, si lo está lo guarda
     * en memoria. Luego carga el bloque deseado desde la otra caché(si lo tiene) o desde la memoria principal
     *
     * @param dir            De memoria que se desea cargar
     * @param numBloque      Número del bloque que se desea cargar
     */
    public void cargarBloque(int dir, int numBloque);

    /**
     * Busca el bloque de memoria solicitado en la caché para enviarlo a la otra caché. Si este bloque está modificado
     * lo almacena en memoria para que este compartido
     *
     * @param numBloque Número del bloque que se desea enviar
     * @param bloque    Vector en donde se puede almacenar el bloque para transmitir los datos
     */
    public void enviarBloque(int numBloque, int[] bloque);

    /**
     * Busca en el caché si el bloque deseado se encuentra en el
     *
     * @param numBloque      Número del bloque que se desea verificar si existe
     * @return False si el bloque deseado no existe en el caché y True si existe
     */
    public boolean existeBloque(int numBloque);

    /**
     * Busca si el bloque deseado esta en la caché, si lo esta cambia el valor de su bandera de estado
     *
     * @param numBloque      Número del bloque deseado
     * @param bandera        Nuevo valor de la bandera de ese bloque de datos
     */
    public void cambiarBandera(int numBloque, char bandera);

    public char obtenerBandera(int numBloqueCache);

    /**
     * Imprime toda la información deseada del caché de datos
     */
    public void print();

    public int getRl();

    public void setRl(int rl);

    public void setOtraCache(CacheDatos otraCache);
}