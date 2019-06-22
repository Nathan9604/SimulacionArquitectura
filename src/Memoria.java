public class Memoria {
    private int datos[];
    private int instrucciones[];

    public Memoria(){
        datos = new int[96];
        instrucciones = new int[640];

        for(int i = 0; i < datos.length; ++i)
            datos[i] = 1;
    }

    public void escribirInstruccion(int dir, int[] instruccion){
        dir -= 384;

        // Si la dirección es negativa o empieza después del inicio del último bloque
        // Indique el error y salga del programa.
        if(dir < 0 || dir > instrucciones.length){
            System.out.println("La dirección de memoria " + dir + " no existe, Revise el código");
            System.exit(-1);
        }

        for(int i = 0; i < instruccion.length; ++i){
            instrucciones[dir + i] = instruccion[i];
        }
    }

    public void escribirBloqueDatos(int dir, int[] bloque){
        // Si la dirección es negativa o empieza después del inicio del último bloque
        // Indique el error y salga del programa.
        if(dir < 0 || dir > datos.length){
            System.out.println("La dirección de memoria " + dir + " no existe, Revise el código");
            System.exit(-1);
        }

        int numBloque = dir/4;
        int posicionMem = numBloque * 4;

        for(int offset = 0; offset < 4; ++offset)
            datos[posicionMem + offset] = bloque[offset];
    }

    public int leerBloqueInstrucciones(int numBloque, int[] bloque){
        int posicionMem = numBloque * 16;

        // Si la dirección es negativa o empieza después del inicio del último bloque
        // Indique el error y salga del programa.
        if(posicionMem < 0 || posicionMem > instrucciones.length){
            System.out.println("La dirección de memoria " + posicionMem + " no existe, Revise el código");
            System.exit(-1);
        }

        for(int offset = 0; offset < 16; ++offset)
            bloque[offset] = instrucciones[posicionMem + offset];

        return numBloque;
    }

    public int leerBloqueDatos(int dir, int[] bloque){
        // Si la dirección es negativa o empieza después del inicio del último bloque
        // Indique el error y salga del programa.
        if(dir < 0 || dir > datos.length){
            System.out.println("La dirección de memoria " + dir + " no existe, Revise el código");
            System.exit(-1);
        }

        int numBloque = dir/4;
        int posicionMem = numBloque * 4;

        for(int offset = 0; offset < 4; ++offset)
            bloque[offset] = datos[posicionMem + offset];

        return numBloque;
    }

    public void print(){
        int offset = 0;
        int dir = 0;

        System.out.println("\n*************************************************************************");
        for(int fila = 0; fila < 6; ++fila){

            System.out.print("Dir  ");
            for(int numDir = 0; numDir < 4; ++numDir, ++dir)
                System.out.print(dir + " ");

            System.out.print("\nDato ");

            for(int col = 0; col < 4; ++col, ++offset)
                System.out.print(datos[offset] + " ");

            System.out.print("\n");
        }

        // Es para imprimir la memoria de instrucciones, esto debe de borrarse antes de presentarse
        /*int k = 0;
        for(int i = 0; i < 40; ++i){
            for(int j = 0; j < 16; ++j) {
                System.out.print(instrucciones[k] + " ");
                ++k;
            }
            System.out.print("\n");
        }*/

        System.out.println("*************************************************************************");
    }
}
